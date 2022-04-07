/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 TONBELLER AG
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2014 Pentaho and others
// All Rights Reserved.
*/
package mondrian.rolap;

import io.kylin.mdx.rolap.cache.CacheManager;
import io.kylin.mdx.rolap.cache.HierarchyCache;
import io.kylin.mdx.rolap.cache.HierarchyMemberTree;
import mondrian.calc.ResultStyle;
import mondrian.calc.TupleList;
import mondrian.calc.impl.DelegatingTupleList;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;
import mondrian.rolap.TupleReader.MemberBuilder;
import mondrian.rolap.cache.HardSmartCache;
import mondrian.rolap.cache.SmartCache;
import mondrian.rolap.cache.SoftSmartCache;
import mondrian.rolap.sql.*;
import mondrian.spi.DataServicesProvider;
import mondrian.spi.Dialect;
import mondrian.xmla.XmlaRequestContext;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import javax.sql.DataSource;
import java.util.*;

import static mondrian.spi.DataServicesLocator.getDataServicesProvider;
import static org.apache.commons.collections.CollectionUtils.exists;
import static org.apache.commons.collections.CollectionUtils.filter;

/**
 * Analyses set expressions and executes them in SQL if possible.
 * Supports crossjoin, member.children, level.members and member.descendants -
 * all in non empty mode, i.e. there is a join to the fact table.<p/>
 *
 * <p>TODO: the order of the result is different from the order of the
 * enumeration. Should sort.
 *
 * @author av
 * @since Nov 12, 2005
  */
public abstract class RolapNativeSet extends RolapNative {
    protected static final Logger LOGGER =
        Logger.getLogger(RolapNativeSet.class);

    private SmartCache<Object, TupleList> cache =
        new SoftSmartCache<Object, TupleList>();

    /**
     * Returns whether certain member types (e.g. calculated members) should
     * disable native SQL evaluation for expressions containing them.
     *
     * <p>If true, expressions containing calculated members will be evaluated
     * by the interpreter, instead of using SQL.
     *
     * <p>If false, calc members will be ignored and the computation will be
     * done in SQL, returning more members than requested.  This is ok, if
     * the superflous members are filtered out in java code afterwards.
     *
     * @return whether certain member types should disable native SQL evaluation
     */
    protected abstract boolean restrictMemberTypes();

    protected CrossJoinArgFactory crossJoinArgFactory() {
        return new CrossJoinArgFactory(restrictMemberTypes());
    }

    /**
     * Constraint for non empty {crossjoin, member.children,
     * member.descendants, level.members}
     */
    protected static abstract class SetConstraint extends SqlContextConstraint {
        CrossJoinArg[] args;

        /**
         * Creates a SetConstraint.
         *
         * @param args Cross-join arguments
         * @param evaluator Evaluator
         * @param measureGroupList List of stars to join to
         * @param strict Whether to fail if context contains calculated members
         */
        SetConstraint(
            CrossJoinArg[] args,
            RolapEvaluator evaluator,
            List<RolapMeasureGroup> measureGroupList,
            boolean strict)
        {
            super(evaluator, measureGroupList, strict);
            this.args = args;
        }

        /**
         * {@inheritDoc}
         *
         * <p>If there is a crossjoin, we need to join the fact table - even if
         * the evaluator context is empty.
         */
        public boolean isJoinRequired() {
            return args.length > 1 || super.isJoinRequired();
        }

        public void addConstraint(
            SqlQueryBuilder queryBuilder,
            RolapStarSet starSet)
        {
            super.addConstraint(queryBuilder, starSet);
            for (CrossJoinArg arg : args) {
                // if the cross join argument has calculated members in its
                // enumerated set, ignore the constraint since we won't
                // produce that set through the native sql and instead
                // will simply enumerate through the members in the set
                if (!(arg instanceof MemberListCrossJoinArg)
                    || !((MemberListCrossJoinArg) arg).hasCalcMembers())
                {
                    RolapLevel level = arg.getLevel();
                    if (level == null
                        || levelIsOnBaseCube(starSet.getMeasureGroup(), level))
                    {
                        arg.addConstraint(queryBuilder, starSet);
                    }
                }
            }
        }

        private boolean levelIsOnBaseCube(
            final RolapMeasureGroup measureGroup, final RolapLevel level)
        {
            if (measureGroup == null) {
                return false;
            }
            return measureGroup.getPath(
                level.getHierarchy().getDimension()) != null;
        }

        /**
         * Returns null to prevent the member/childern from being cached. There
         * exists no valid MemberChildrenConstraint that would fetch those
         * children that were extracted as a side effect from evaluating a non
         * empty crossjoin
         */
        public MemberChildrenConstraint getMemberChildrenConstraint(
            RolapMember parent)
        {
            return null;
        }

        /**
         * returns a key to cache the result
         */
        public Object getCacheKey() {
            List<Object> key = new ArrayList<Object>();
            key.add(super.getCacheKey());
            // only add args that will be retrieved through native sql;
            // args that are sets with calculated members aren't executed
            // natively
            for (CrossJoinArg arg : args) {
                if (!(arg instanceof MemberListCrossJoinArg)
                    || !((MemberListCrossJoinArg) arg).hasCalcMembers())
                {
                    key.add(arg);
                }
            }
            return key;
        }
    }

    protected class SetEvaluator implements NativeEvaluator {
        protected final CrossJoinArg[] args;
        protected final SchemaReaderWithMemberReaderAvailable schemaReader;
        protected final TupleConstraint constraint;
        private int maxRows = 0;

        public SetEvaluator(
            CrossJoinArg[] args,
            SchemaReader schemaReader,
            TupleConstraint constraint)
        {
            this.args = args;
            if (schemaReader instanceof SchemaReaderWithMemberReaderAvailable) {
                this.schemaReader =
                    (SchemaReaderWithMemberReaderAvailable) schemaReader;
            } else {
                this.schemaReader =
                    new SchemaReaderWithMemberReaderCache(schemaReader);
            }
            this.constraint = constraint;
        }

        public Object execute(ResultStyle desiredResultStyle) {
            switch (desiredResultStyle) {
            case ITERABLE:
            case MUTABLE_LIST:
            case LIST:
                DataServicesProvider provider =
                    getDataServicesProvider(
                        schemaReader.getSchema().getDataServiceProviderName());
                return executeList(provider.getTupleReader(constraint));
            }
            throw ResultStyleException.generate(
                ResultStyle.ITERABLE_MUTABLELIST_LIST,
                Collections.singletonList(desiredResultStyle));
        }

        protected TupleList executeList(final TupleReader tr) {
            XmlaRequestContext context = XmlaRequestContext.getContext();
            boolean isFromSmartBI = context != null && XmlaRequestContext.ClientType.SMARTBI.equals(context.clientType);
            tr.setMaxRows(maxRows);
            for (CrossJoinArg arg : args) {
                addLevel(tr, arg);
            }

            // Look up the result in cache; we can't return the cached
            // result if the tuple reader contains a target with calculated
            // members because the cached result does not include those
            // members; so we still need to cross join the cached result
            // with those enumerated members.
            //
            // The key needs to include the arguments (projection) as well as
            // the constraint, because it's possible (see bug MONDRIAN-902)
            // that independent axes have identical constraints but different
            // args (i.e. projections). REVIEW: In this case, should we use the
            // same cached result and project different columns?
            List<Object> key = new ArrayList<Object>();
            key.add(tr.getCacheKey());
            key.addAll(Arrays.asList(args));
            TupleList result = null;
            if (!isFromSmartBI) {
                result = cache.get(key);
            }

            boolean hasEnumTargets = (tr.getEnumTargetCount() > 0);
            if (result != null && !hasEnumTargets) {
                if (listener != null) {
                    TupleEvent e = new TupleEvent(this, tr);
                    listener.foundInCache(e);
                }
                // Why use args.length instead of result.getArity() here?
                return new DelegatingTupleList(
                    result.getArity(), Util.<List<Member>>cast(result));
            }

            // execute sql and store the result
            if (result == null && listener != null) {
                TupleEvent e = new TupleEvent(this, tr);
                listener.executingSql(e);
            }

            // Try fuzzy searching using the hierarchy cache. Return to the original way if
            // cache not available.
            TupleList partialResult = result;
            List<List<RolapMember>> newPartialResult = null;
            Pair<RolapCubeLevel, Object> fuzzySearchParameters = checkFuzzySearch();
            if (fuzzySearchParameters != null) {
                result = fuzzySearchWithHierarchyCache(
                        fuzzySearchParameters.getLeft(),
                        fuzzySearchParameters.getRight());
            }

            // if we don't have a cached result in the case where we have
            // enumerated targets, then retrieve and cache that partial result
            if (result == null) {
                if (hasEnumTargets && partialResult == null) {
                    newPartialResult = new ArrayList<List<RolapMember>>();
                }
                DataSource dataSource = schemaReader.getDataSource();
                final Dialect dialect = schemaReader.getSchema().getDialect();
                if (args.length == 1) {
                    result =
                            tr.readMembers(
                                    dialect, dataSource, partialResult, newPartialResult);
                } else {
                    result =
                            tr.readTuples(
                                    dialect, dataSource, partialResult, newPartialResult);
                }
            }

            if (!MondrianProperties.instance().DisableCaching.get() && !isFromSmartBI) {
                if (hasEnumTargets) {
                    if (newPartialResult != null) {
                        cache.put(
                            key,
                            new DelegatingTupleList(
                                args.length,
                                Util.<List<Member>>cast(newPartialResult)));
                    }
                } else {
                    cache.put(key, result);
                }
            }
            if (isFromSmartBI && context.queryPage != null && context.queryPage.inOnePage) {
                    if (context.queryPage.pageEnd <= result.size()) {
                        return result.subList(context.queryPage.pageStart, context.queryPage.pageEnd);
                    } else if (context.queryPage.queryStart < result.size()) {
                        return result.subList(context.queryPage.pageStart, result.size());
                    }
            }
            return filterInaccessibleTuples(result);
        }

        /**
         * Checks access rights and hidden status on the members
         * in each tuple in tupleList.
         */
        private TupleList filterInaccessibleTuples(TupleList tupleList) {
            if (needsFiltering(tupleList)) {
                final Predicate memberInaccessible =
                    memberInaccessiblePredicate();
                filter(
                    tupleList, tupleAccessiblePredicate(memberInaccessible));
            }
            return tupleList;
        }

        private boolean needsFiltering(TupleList tupleList) {
            return tupleList.size() > 0
                   && exists(tupleList.get(0), needsFilterPredicate());
        }

        private Predicate needsFilterPredicate() {
            return new Predicate() {
                public boolean evaluate(Object o) {
                    Member member = (Member) o;
                    return isRaggedLevel(member.getLevel())
                           || isCustomAccess(member.getHierarchy());
                }
            };
        }

        private boolean isRaggedLevel(Level level) {
            if (level instanceof RolapLevel) {
                return ((RolapLevel) level).getHideMemberCondition()
                       != RolapLevel.HideMemberCondition.Never;
            }
            // don't know if it's ragged, so assume it is.
            // should not reach here
            return true;
        }

        private boolean isCustomAccess(Hierarchy hierarchy) {
            if (constraint.getEvaluator() == null) {
                return false;
            }
            Access access =
                constraint
                    .getEvaluator()
                    .getSchemaReader()
                    .getRole()
                    .getAccess(hierarchy);
            return access == Access.CUSTOM;
        }

        private Predicate memberInaccessiblePredicate() {
            if (constraint.getEvaluator() != null) {
                return new Predicate() {
                    public boolean evaluate(Object o) {
                        Role role =
                            constraint
                                .getEvaluator().getSchemaReader().getRole();
                        Member member = (Member) o;
                        return member.isHidden() || !role.canAccess(member);
                    }
                };
            }
            return new Predicate() {
                public boolean evaluate(Object o) {
                    return ((Member) o).isHidden();
                }
            };
        }

        private Predicate tupleAccessiblePredicate(
            final Predicate memberInaccessible)
        {
            return new Predicate() {
                @SuppressWarnings("unchecked")
                public boolean evaluate(Object o) {
                    return !exists((List<Member>) o, memberInaccessible);
                }};
        }

        private void addLevel(TupleReader tr, CrossJoinArg arg) {
            RolapCubeLevel level = arg.getLevel();
            if (level == null) {
                // Level can be null if the CrossJoinArg represent
                // an empty set.
                // This is used to push down the "false" predicate
                // into the emerging CJ so that the entire CJ can
                // be natively evaluated.
                tr.incrementEmptySets();
                return;
            }

            RolapCubeHierarchy hierarchy = level.getHierarchy();
            MemberReader mr = schemaReader.getMemberReader(hierarchy);
            MemberBuilder mb = mr.getMemberBuilder();
            Util.assertTrue(mb != null, "MemberBuilder not found");

            if (arg instanceof MemberListCrossJoinArg
                    && ((MemberListCrossJoinArg) arg).hasCalcMembers()) {
                // only need to keep track of the members in the case
                // where there are calculated members since in that case,
                // we produce the values by enumerating through the list
                // rather than generating the values through native sql
                tr.addLevelMembers(level, mb, arg.getMembers());
            } else if (arg instanceof DescendantsCrossJoinArg
                    && ((DescendantsCrossJoinArg) arg).isLeaves()) {
                // To get all leaf members, we must contain all descendant
                // levels in targets
                while (level != null) {
                    tr.addLevelMembers(level, mb, null);
                    level = level.getChildLevel();
                }
            } else {
                tr.addLevelMembers(level, mb, null);
            }
        }

        void setMaxRows(int maxRows) {
            this.maxRows = maxRows;
        }

        private Pair<RolapCubeLevel, Object> checkFuzzySearch() {
            if (!(constraint instanceof RolapNativeFilter.FilterConstraint)) {
                return null;
            }

            RolapNativeFilter.FilterConstraint filterConstraint = (RolapNativeFilter.FilterConstraint)constraint;
            Exp filterExp = filterConstraint.filterExpr;
            if (!(filterExp instanceof ResolvedFunCall)) {
                return null;
            }

            ResolvedFunCall filterFunCall = (ResolvedFunCall)filterExp;
            if (!"MATCHES".equalsIgnoreCase(filterFunCall.getFunName()) || filterFunCall.getArgCount() != 2) {
                return null;
            }

            RolapCubeLevel level = filterConstraint.args[0].getLevel();

            Exp arg1 = filterFunCall.getArg(1);
            if (!(arg1 instanceof Literal)) {
                return null;
            }
            Object searchTarget = ((Literal)arg1).getValue();

            return Pair.of(level, searchTarget);
        }

        private TupleList fuzzySearchWithHierarchyCache(RolapCubeLevel level, Object searchTarget) {
            HierarchyCache hierarchyCache = CacheManager.getCacheManager().getHierarchyCache();
            if (!hierarchyCache.isCacheEnabled()) {
                return null;
            }

            List<? extends Member> result;
            HierarchyMemberTree hierarchyMemberTree = hierarchyCache.getMemberTreeIfPresent(level.getHierarchy());
            if (hierarchyMemberTree == null) {
                return null;
            }
            result = hierarchyMemberTree.fuzzySearchByName(searchTarget.toString(), level.getDepth());

            return new UnaryTupleList((List<Member>)result);
        }
    }

    public abstract class ValueEvaluator implements NativeEvaluator {
        protected final CrossJoinArg[] args;
        protected final SchemaReaderWithMemberReaderAvailable schemaReader;
        protected final TupleConstraint constraint;

        public ValueEvaluator(
                CrossJoinArg[] args,
                SchemaReader schemaReader,
                TupleConstraint constraint) {
            this.args = args;
            if (schemaReader instanceof SchemaReaderWithMemberReaderAvailable) {
                this.schemaReader =
                        (SchemaReaderWithMemberReaderAvailable) schemaReader;
            } else {
                this.schemaReader =
                        new SchemaReaderWithMemberReaderCache(schemaReader);
            }
            this.constraint = constraint;
        }

        @Override
        public Object execute(ResultStyle resultStyle) {
            DataServicesProvider provider = getDataServicesProvider(schemaReader.getSchema().getDataServiceProviderName());
            switch (resultStyle) {
                case VALUE:
                case VALUE_NOT_NULL:
                    return executeValue(provider.getValueReader(constraint));
                default:
                    throw ResultStyleException.generate(
                            Arrays.asList(ResultStyle.VALUE, ResultStyle.VALUE_NOT_NULL),
                            Collections.singletonList(resultStyle));
            }
        }

        abstract Number executeValue(final SqlDescendantsLeavesAggrNumValuesReader valueReader);

        /**
         * Similar to {@link SetEvaluator#addLevel(TupleReader, CrossJoinArg)}
         */
        protected void addLevel(SqlDescendantsLeavesAggrNumValuesReader valueReader, CrossJoinArg arg) {
            RolapCubeLevel level = arg.getLevel();
            if (level == null) {
                valueReader.incrementEmptySets();
                return;
            }

            RolapCubeHierarchy hierarchy = level.getHierarchy();
            MemberReader mr = schemaReader.getMemberReader(hierarchy);
            MemberBuilder mb = mr.getMemberBuilder();
            Util.assertTrue(mb != null, "MemberBuilder not found");

            if (arg instanceof DescendantsCrossJoinArg
                    && ((DescendantsCrossJoinArg) arg).isLeaves()) {
                while (level != null) {
                    valueReader.addLevelMembers(level, mb, null);
                    level = level.getChildLevel();
                }
            } else {
                valueReader.addLevelMembers(level, mb, null);
            }
        }
    }

    public static class FixedValueEvaluator implements NativeEvaluator {
        private Object value;

        public FixedValueEvaluator(Object value) {
            this.value = value;
        }

        @Override
        public Object execute(ResultStyle resultStyle) {
            return value;
        }
    }

    /**
     * Tests whether non-native evaluation is preferred for the
     * given arguments.
     *
     * @param joinArg true if evaluating a cross-join; false if
     * evaluating a single-input expression such as filter
     *
     * @return true if <em>all</em> args prefer the interpreter
     */
    protected boolean isPreferInterpreter(
        CrossJoinArg[] args,
        boolean joinArg)
    {
        for (CrossJoinArg arg : args) {
            if (!arg.isPreferInterpreter(joinArg)) {
                return false;
            }
        }
        return true;
    }

    /** disable garbage collection for test */
    void useHardCache(boolean hard) {
        if (hard) {
            cache = new HardSmartCache();
        } else {
            cache = new SoftSmartCache();
        }
    }

    /**
     * Overrides current members in position by default members in
     * hierarchies which are involved in this filter/topcount.
     * Stores the RolapStoredMeasure into the context because that is needed to
     * generate a cell request to constraint the sql.
     *
     * <p>The current context may contain a calculated measure, this measure
     * was translated into an sql condition (filter/topcount). The measure
     * is not used to constrain the result but only to access the star.
     *
     * @param evaluator Evaluation context to modify
     * @param cargs Cross join arguments
     * @param storedMeasure Stored measure
     *
     * @see RolapAggregationManager#makeRequest(RolapEvaluator, boolean)
     */
    protected void overrideContext(
        RolapEvaluator evaluator,
        CrossJoinArg[] cargs,
        RolapStoredMeasure storedMeasure)
    {
        SchemaReader schemaReader = evaluator.getSchemaReader();
        for (CrossJoinArg carg : cargs) {
            RolapLevel level = carg.getLevel();
            if (level != null) {
                Hierarchy hierarchy = level.getHierarchy();
                Member defaultMember =
                    schemaReader.getHierarchyDefaultMember(hierarchy);
                evaluator.setContext(defaultMember);
            }
        }
        if (storedMeasure != null) {
            evaluator.setContext(storedMeasure);
        }
    }


    public interface SchemaReaderWithMemberReaderAvailable
        extends SchemaReader
    {
        MemberReader getMemberReader(RolapCubeHierarchy hierarchy);
    }

    protected static class SchemaReaderWithMemberReaderCache
        extends DelegatingSchemaReader
        implements SchemaReaderWithMemberReaderAvailable
    {
        private final Map<Hierarchy, MemberReader> hierarchyReaders =
            new HashMap<Hierarchy, MemberReader>();

        SchemaReaderWithMemberReaderCache(SchemaReader schemaReader) {
            super(schemaReader);
        }

        public synchronized MemberReader getMemberReader(
            RolapCubeHierarchy hierarchy)
        {
            MemberReader memberReader = hierarchyReaders.get(hierarchy);
            if (memberReader == null) {
                memberReader =
                    RolapSchemaLoader.createMemberReader(
                        hierarchy, schemaReader.getRole());
                hierarchyReaders.put(hierarchy, memberReader);
            }
            return memberReader;
        }
    }
}

// End RolapNativeSet.java

