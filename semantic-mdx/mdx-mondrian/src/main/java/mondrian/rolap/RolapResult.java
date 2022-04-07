/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2001-2005 Julian Hyde
// Copyright (C) 2005-2014 Pentaho and others
// All Rights Reserved.
*/
package mondrian.rolap;

import mondrian.calc.*;
import mondrian.calc.impl.DelegatingTupleList;
import mondrian.calc.impl.GenericCalc;
import mondrian.calc.impl.ValueCalc;
import mondrian.mdx.DimensionExpr;
import mondrian.mdx.HierarchyExpr;
import mondrian.mdx.MdxVisitorImpl;
import mondrian.mdx.MemberExpr;
import mondrian.olap.*;
import mondrian.olap.fun.AbstractAggregateFunDef;
import mondrian.olap.fun.AggregateFunDef;
import mondrian.olap.fun.MondrianEvaluationException;
import mondrian.olap.fun.VisualTotalsFunDef.VisualTotalMember;
import mondrian.olap.type.*;
import mondrian.resource.MondrianResource;
import mondrian.rolap.agg.AggregationManager;
import mondrian.rolap.agg.CellRequestQuantumExceededException;
import mondrian.rolap.cell.*;
import mondrian.server.Execution;
import mondrian.server.Locus;
import mondrian.util.ConcatenableList;
import mondrian.xmla.XmlaRequestContext;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A <code>RolapResult</code> is the result of running a query.
 *
 * @author jhyde
 * @since 10 August, 2001
 */
public class RolapResult extends ResultBase {

    static final Logger LOGGER = Logger.getLogger(ResultBase.class);

    protected RolapEvaluator evaluator;
    protected RolapEvaluator slicerEvaluator;
    protected final CellKey point;

    protected CellInfoContainer cellInfos;
    protected FastBatchingCellReader batchingReader;

    protected boolean preserveRegularFormat;

    private final CellReader aggregatingReader;
    private Modulos modulos = null;
    private final int maxEvalDepth = MondrianProperties.instance().MaxEvalDepth.get();
    private final List<RuntimeException> runtimeExceptions = new ArrayList<>();

    /**
     * Creates a RolapResult.
     *
     * @param execution      Execution of a statement
     * @param execute        Whether to execute the query
     * @param otherQueryEngineNamedSet Whether for named Set calculation in other query engine
     */
    public RolapResult(
            final Execution execution,
            boolean execute, boolean errorHandle, boolean otherQueryEngineNamedSet) {
        super(execution, null);

        this.point = CellKey.Generator.newCellKey(axes.length);
        final AggregationManager aggMgr = execution.getMondrianStatement()
                .getMondrianConnection()
                .getServer().getAggregationManager();
        this.aggregatingReader = aggMgr.getCacheCellReader();
        final int expDeps = MondrianProperties.instance().TestExpDependencies.get();
        if (expDeps > 0) {
            this.evaluator = new RolapDependencyTestingEvaluator(this, expDeps);
        } else {
            final RolapEvaluatorRoot root = new RolapResultEvaluatorRoot(this);
            if (statement.getProfileHandler() != null) {
                this.evaluator = new RolapProfilingEvaluator(root);
            } else {
                this.evaluator = new RolapEvaluator(root);
            }
        }
        RolapCube cube = (RolapCube) query.getCube();
        this.batchingReader = new FastBatchingCellReader(execution, cube, aggMgr);

        this.cellInfos = (query.getAxes().length > 4)
                ? new CellInfoMap(point)
                : new CellInfoPool(query.getAxes().length);

        this.preserveRegularFormat = XmlaRequestContext.getContext().shouldPreserveRegularFormat();

        // Initial evaluator, to execute slicer.
        if (!errorHandle) {
            slicerEvaluator = evaluator.push();
        }

        if (!execute) {
            return;
        }

        boolean normalExecution = true;
        try {
            // This call to clear the cube's cache only has an
            // effect if caching has been disabled, otherwise
            // nothing happens.
            // Clear the local cache before a query has run
            for (RolapStar star : cube.getStars()) {
                star.clearCachedAggregations(false);
            }

            /////////////////////////////////////////////////////////////////
            //
            // Evaluation Algorithm
            //
            // There are three basic steps to the evaluation algorithm:
            // 1) Determine all Members for each axis but do not save
            // information (do not build the RolapAxis),
            // 2) Save all Members for each axis (build RolapAxis).
            // 3) Evaluate and store each Cell determined by the Members
            // of the axes.
            // Step 1 converges on the stable set of Members pre axis.
            // Steps 1 and 2 make sure that the data has been loaded.
            //
            // More detail follows.
            //
            // Explicit and Implicit Members:
            // A Member is said to be 'explicit' if it appears on one of
            // the Axes (one of the RolapAxis Position List of Members).
            // A Member is 'implicit' if it is in the query but does not
            // end up on any Axes (its usage, for example, is in a function).
            // When for a Dimension none of its Members are explicit in the
            // query, then the default Member is used which is like putting
            // the Member in the Slicer.
            //
            // Special Dimensions:
            // There are 2 special dimensions.
            // The first is the Time dimension. If in a schema there is
            // no ALL Member, then Whatever happens to be the default
            // Member is used if Time Members are not explicitly set
            // in the query.
            // The second is the Measures dimension. This dimension
            // NEVER has an ALL Member. A cube's default Measure is set
            // by convention - its simply the first Measure defined in the
            // cube.
            //
            // First a RolapEvaluator is created. During its creation,
            // it gets a Member from each Hierarchy. Each Member is the
            // default Member of the Hierarchy. For most Hierarchies this
            // Member is the ALL Member, but there are cases where 1)
            // a Hierarchy does not have an ALL Member or 2) the Hierarchy
            // has an ALL Member but that Member is not the default Member.
            // In these cases, the default Member is still used, but its
            // use can cause evaluation issues (seemingly strange evaluation
            // results).
            //
            // Next, load all root Members for Hierarchies that have no ALL
            // Member and load ALL Members that are not the default Member.
            //
            // Determine the Members of the Slicer axis (Step 1 above).  Any
            // Members found are added to the AxisMember object. If one of these
            // Members happens to be a Measure, then the Slicer is explicitly
            // specifying the query's Measure and this should be put into the
            // evaluator's context (replacing the default Measure which just
            // happens to be the first Measure defined in the cube).  Other
            // Members found in the AxisMember object are also placed into the
            // evaluator's context since these also are explicitly specified.
            // Also, any other Members in the AxisMember object which have the
            // same Hierarchy as Members in the list of root Members for
            // Hierarchies that have no ALL Member, replace those Members - they
            // Slicer has explicitly determined which ones to use. The
            // AxisMember object is now cleared.
            // The Slicer does not depend upon the other Axes, but the other
            // Axes depend upon both the Slicer and each other.
            //
            // The AxisMember object also checks if the number of Members
            // exceeds the ResultLimit property throwing a
            // TotalMembersLimitExceeded Exception if it does.
            //
            // For all non-Slicer axes, the Members are determined (Step 1
            // above). If a Measure is found in the AxisMember, then an
            // Axis is explicitly specifying a Measure.
            // If any Members in the AxisMember object have the same Hierarchy
            // as a Member in the set of root Members for Hierarchies that have
            // no ALL Member, then replace those root Members with the Member
            // from the AxisMember object. In this case, again, a Member
            // was explicitly specified in an Axis. If this replacement
            // occurs, then one must redo this step with the new Members.
            //
            // Now Step 3 above is done. First to the Slicer Axis and then
            // to the other Axes. Here the Axes are actually generated.
            // If a Member of an Axis is an Calculated Member (and the
            // Calculated Member is not a Member of the Measure Hierarchy),
            // then find the Dimension associated with the Calculated
            // Member and remove Members with the same Dimension in the set of
            // root Members for Hierarchies that have no ALL Member.
            // This is done because via the Calculated Member the Member
            // was implicitly specified in the query. If this removal occurs,
            // then the Axes must be re-evaluated repeating Step 3.
            //
            /////////////////////////////////////////////////////////////////


            // The AxisMember object is used to hold Members that are found
            // during Step 1 when the Axes are determined.
            final AxisMemberList axisMembers = new AxisMemberList();


            // list of ALL Members that are not default Members
            final List<Member> nonDefaultAllMembers = new ArrayList<>();

            // List of Members of Hierarchies that do not have an ALL Member
            List<List<Member>> nonAllMembers = new ArrayList<>();

            // List of Measures
            final List<Member> measureMembers = new ArrayList<>();

            final List<List<Member>> emptyNonAllMembers = Collections.emptyList();

            // load all root Members for Hierarchies that have no ALL
            // Member and load ALL Members that are not the default Member.
            // Also, all Measures are are gathered.
            loadSpecialMembers(nonDefaultAllMembers, nonAllMembers, measureMembers);

            // clear evaluation cache
            query.clearEvalCache();

            // Save, may be needed by some Expression Calc's
            query.putEvalCache("ALL_MEMBER_LIST", nonDefaultAllMembers);


            execution.canFriendlyHandleError();
            XmlaRequestContext localContext = XmlaRequestContext.getContext();
            long calcAxesStart = System.currentTimeMillis();

            // Determine Slicer
            RolapEvaluator savedEvaluator = determineSlicer(axisMembers, measureMembers, emptyNonAllMembers, nonAllMembers, nonDefaultAllMembers, errorHandle);

            if (otherQueryEngineNamedSet) {
                return;
            }
            // Determine Axes
            determineAxes(axisMembers, measureMembers, emptyNonAllMembers, nonAllMembers);

            // Execute Slicer
            Axis savedSlicerAxis = evalSlicer(savedEvaluator, nonAllMembers);

            if (errorHandle) {
                evalDummyAxes();
                return;
            }

            // Execute Axes
            evalAxes(nonAllMembers, nonDefaultAllMembers);

            checkCellTotalCount();

            long calcAxesEnd = System.currentTimeMillis();

            if (localContext != null) {
                localContext.runningStatistics.calcAxesTime = calcAxesEnd - calcAxesStart;
            }

            // Get value for each Cell
            final Locus locus = new Locus(execution, null, "Loading cells");
            Locus.push(locus);
            try {
                long calcCellStart = System.currentTimeMillis();
                executeBody(slicerEvaluator);
                long calcCellEnd = System.currentTimeMillis();
                if (localContext != null) {
                    localContext.runningStatistics.calcCellValueTime = calcCellEnd - calcCellStart;
                    if (runtimeExceptions.size() > 0) {
                        localContext.errorMsg = runtimeExceptions.stream().map(Throwable::getMessage)
                                .distinct().collect(Collectors.joining(";", "{", "}"));
                    }
                }
            } finally {
                Locus.pop(locus);
            }

            // If you are very close to running out of memory due to
            // the number of CellInfo's in cellInfos, then calling this
            // may cause the out of memory one is trying to avoid.
            // On the other hand, calling this can reduce the size of
            // the ObjectPool's internal storage by half (but, of course,
            // it will not reduce the size of the stored objects themselves).
            // Only call this if there are lots of CellInfo.
            if (this.cellInfos.size() > 10000) {
                this.cellInfos.trimToSize();
            }
            // revert the slicer axis so that the original slicer
            // can be included in the result.
            this.slicerAxis = savedSlicerAxis;
        } catch (ResultLimitExceededException ex) {
            // If one gets a ResultLimitExceededException, then
            // don't count on anything being worth caching.
            normalExecution = false;

            // De-reference data structures that might be holding
            // partial results but surely are taking up memory.
            evaluator = null;
            slicerEvaluator = null;
            cellInfos = null;
            batchingReader = null;
            Arrays.fill(axes, null);
            slicerAxis = null;

            query.clearEvalCache();

            throw ex;
        } finally {
            if (normalExecution) {
                // Expression cache duration is for each query. It is time to
                // clear out the whole expression cache at the end of a query.
                evaluator.clearExpResultCache(true);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("RolapResult<init>: " + Util.printMemory());
            }
        }
    }

    public List<Member> getEvaluatorContext() {
        return new ArrayList<>(Arrays.asList(evaluator.getNonAllMembers()));
    }

    private void checkCellTotalCount() {
        int cellLimit = MondrianProperties.instance().ResultLimit.get();
        if (axes == null || cellLimit <= 0) {
            return;
        }
        int count = 1;
        for (Axis axis : axes) {
            if (axis != null) {
                count *= axis.getPositions().size();
            }
        }
        if (count > cellLimit) {
            throw MondrianResource.instance().TotalMembersLimitExceeded.ex(count, cellLimit);
        }
    }

    private RolapEvaluator determineSlicer(
            AxisMemberList axisMembers,
            List<Member> measureMembers,
            List<List<Member>> emptyNonAllMembers,
            List<List<Member>> nonAllMembers,
            List<Member> nonDefaultAllMembers,
            boolean errorHandle) {
        axisMembers.setSlicer(true);
        loadMembers(
                emptyNonAllMembers,
                evaluator,
                query.getSlicerAxis(),
                query.slicerCalc,
                axisMembers);
        axisMembers.setSlicer(false);

        // Save unadulterated context for the next time we need to evaluate
        // the slicer.
        final RolapEvaluator savedEvaluator = evaluator.push();

        if (!axisMembers.isEmpty()) {
            for (Member m : axisMembers) {
                if (m == null) {
                    break;
                }
                evaluator.setSlicerContext(m);
                if (m.isMeasure()) {
                    // A Measure was explicitly declared in the
                    // Slicer, don't need to worry about Measures
                    // for this query.
                    measureMembers.clear();
                }
            }
            replaceNonAllMembers(nonAllMembers, axisMembers);
            axisMembers.clearMembers();
        }

        // Save evaluator that has slicer as its context.
        if (!errorHandle) {
            slicerEvaluator = evaluator.push();
            for (Member nonDefaultAllMember : nonDefaultAllMembers) {
                slicerEvaluator.setContext(nonDefaultAllMember);
            }
        }
        return savedEvaluator;
    }

    private void determineAxes(AxisMemberList axisMembers, List<Member> measureMembers,
                               List<List<Member>> emptyNonAllMembers, List<List<Member>> nonAllMembers) {
        // if there is no nonAllMembers, can skip it.
        if (nonAllMembers.size() == 0 && !MondrianProperties.instance().SupportNonDefaultAllMember.get()) {
            return;
        }

        boolean changed = false;

        // reset to total member count
        axisMembers.clearTotalCellCount();

        for (int i = 0; i < axes.length; i++) {
            final QueryAxis axis = query.getAxes()[i];
            final Calc calc = query.axisCalcs[i];
            loadMembers(
                    emptyNonAllMembers, evaluator, axis, calc, axisMembers);
        }

        if (!axisMembers.isEmpty()) {
            for (Member m : axisMembers) {
                if (m.isMeasure()) {
                    // A Measure was explicitly declared on an
                    // axis, don't need to worry about Measures
                    // for this query.
                    measureMembers.clear();
                }
            }
            changed = replaceNonAllMembers(nonAllMembers, axisMembers);
            axisMembers.clearMembers();
        }

        if (changed) {
            // only count number of members, do not collect any
            axisMembers.countOnly(true);
            // reset to total member count
            axisMembers.clearTotalCellCount();

            final int savepoint = evaluator.savepoint();
            try {
                for (int i = 0; i < axes.length; i++) {
                    final QueryAxis axis = query.getAxes()[i];
                    final Calc calc = query.axisCalcs[i];
                    loadMembers(
                            nonAllMembers,
                            evaluator,
                            axis, calc, axisMembers);
                    evaluator.restore(savepoint);
                }
            } finally {
                evaluator.restore(savepoint);
            }
        }

        // throws exception if number of members exceeds limit
        axisMembers.checkLimit();
    }

    private Axis evalSlicer(RolapEvaluator savedEvaluator, List<List<Member>> nonAllMembers) {
        Axis savedSlicerAxis;
        RolapEvaluator slicerEvaluator;
        do {
            TupleIterable tupleIterable =
                    evalExecute(
                            nonAllMembers,
                            nonAllMembers.size() - 1,
                            savedEvaluator,
                            query.getSlicerAxis(),
                            query.slicerCalc);
            // Materialize the iterable as a list. Although it may take
            // memory, we need the first member below, and besides, slicer
            // axes are generally small.
            TupleList tupleList =
                    TupleCollections.materialize(tupleIterable, true);
            this.slicerAxis = new RolapAxis(tupleList);
            // the slicerAxis may be overwritten during slicer execution
            // if there is a compound slicer.  Save it so that it can be
            // reverted before completing result construction.
            savedSlicerAxis = this.slicerAxis;

            // Use the context created by the slicer for the other
            // axes.  For example, "select filter([Customers], [Store
            // Sales] > 100) on columns from Sales where
            // ([Time].[1998])" should show customers whose 1998 (not
            // total) purchases exceeded 100.
            slicerEvaluator = this.evaluator;
            if (tupleList.size() > 1) {
                tupleList =
                        AggregateFunDef.AggregateCalc.optimizeTupleList(
                                slicerEvaluator,
                                tupleList,
                                false);

                final Calc valueCalc =
                        new ValueCalc(
                                new DummyExp(new ScalarType()));
                final TupleList tupleList1 = tupleList;
                final Calc calc =
                        new GenericCalc(
                                new DummyExp(query.slicerCalc.getType())) {
                            public Object evaluate(Evaluator evaluator) {
                                TupleList list =
                                        AbstractAggregateFunDef
                                                .processUnrelatedDimensions(
                                                        tupleList1, evaluator);
                                return AggregateFunDef.AggregateCalc.aggregate(
                                        valueCalc, evaluator, list);
                            }
                        };
                final List<RolapCubeHierarchy> hierarchyList =
                        new AbstractList<RolapCubeHierarchy>() {
                            final List<RolapMember> pos0 =
                                    Util.cast(tupleList1.get(0));

                            public RolapCubeHierarchy get(int index) {
                                return pos0.get(index).getHierarchy();
                            }

                            public int size() {
                                return pos0.size();
                            }
                        };

                // replace the slicer set with a placeholder to avoid
                // interaction between the aggregate calc we just created
                // and any calculated members that might be present in
                // the slicer.
                Member placeholder = setPlaceholderSlicerAxis(
                        tupleList, calc);
                evaluator.setContext(placeholder);
            }
        } while (phase());
        return savedSlicerAxis;
    }

    private void evalAxes(List<List<Member>> nonAllMembers, List<Member> nonDefaultAllMembers) {
        final int savepoint = evaluator.savepoint();
        do {
            try {
                boolean redo;
                do {
                    evaluator.restore(savepoint);
                    redo = false;
                    // remove non default all member when calculate dimensions
                    for (Member nonDefaultAllMember : nonDefaultAllMembers) {
                        evaluator.setContext(nonDefaultAllMember);
                    }

                    for (int i = 0; i < axes.length; i++) {
                        XmlaRequestContext.getContext().setParameter("isCalculatingAxis", "true");
                        QueryAxis axis = query.getAxes()[i];
                        final Calc calc = query.axisCalcs[i];
                        TupleIterable tupleIterable = evalExecute(
                                nonAllMembers,
                                nonAllMembers.size() - 1,
                                evaluator,
                                axis,
                                calc);
                        XmlaRequestContext.getContext().setParameter("isCalculatingAxis", "false");
                        if (!nonAllMembers.isEmpty()) {
                            final TupleIterator tupleIterator = tupleIterable.tupleIterator();
                            if (tupleIterator.hasNext()) {
                                List<Member> tuple0 = tupleIterator.next();
                                // Only need to process the first tuple on
                                // the axis.
                                for (Member m : tuple0) {
                                    if (m.isCalculated()) {
                                        CalculatedMeasureVisitor visitor =
                                                new CalculatedMeasureVisitor();
                                        m.getExpression().accept(visitor);
                                        Dimension dimension =
                                                visitor.dimension;
                                        if (removeDimension(
                                                dimension, nonAllMembers)) {
                                            redo = true;
                                        }
                                    }
                                }
                            }
                        }
                        this.axes[i] =
                                new RolapAxis(
                                        TupleCollections.materialize(
                                                tupleIterable, false));
                    }
                } while (redo);
            } catch (CellRequestQuantumExceededException e) {
                // Safe to ignore. Need to call 'phase' and loop again.
            }
        } while (phase());
        evaluator.restore(savepoint);
    }

    public RolapMemberBase makeDummyChildMember(RolapMember parent, int index) {
        final Larders.LarderBuilder builder = new Larders.LarderBuilder();
        XmlaRequestContext context = XmlaRequestContext.getContext();
        if (context.errorFilled || index != 0) {
            builder.caption("");
        } else {
            if (context.errorMsg != null && !context.errorMsg.isEmpty()) {
                builder.caption(context.errorMsg);
                context.errorFilled = true;
            } else {
                builder.caption("Something error, please refer to mdx log");
            }
        }

        final String nameValue = "dummy";
        builder.add(mondrian.olap.Property.NAME, nameValue);
        RolapMember parentMember = parent;
        RolapCubeLevel currentLevel = parent.getLevel().getChildLevel();
        Comparable keyValue = nameValue;
        Comparable memberKey;

        if (!parent.getLevel().isAll()) {
            List<Comparable> keyList = new ArrayList<>(parent.getKeyAsList());
            keyList.add(keyValue);
            memberKey = (Comparable) Util.flatList(keyList);
        } else {
            memberKey = keyValue;
        }
        RolapMemberBase member = new RolapMemberBase(parentMember, currentLevel, memberKey,
                Member.MemberType.REGULAR,
                RolapMemberBase.deriveUniqueName(parentMember, currentLevel, nameValue, false), builder.build());
        member.setOrdinal(parent.getOrdinal() + 1);
        member.setOrderKey(keyValue);

        return member;
    }

    private void evalDummyAxes() {
        final int savepoint = evaluator.savepoint();
        try {
            for (int i = axes.length - 1; i >= 0; i--) {
                SetType setType = (SetType) query.getAxes()[i].getExp().getType();
                Type elementType = setType.getElementType();
                List<Member> dummyList = new ArrayList<>();

                if (elementType instanceof MemberType) {
                    Member member = ((MemberType) elementType).getMember();
                    if (member == null) {
                        if (elementType.getHierarchy() != null) {
                            member = elementType.getHierarchy().getAllMember();
                        }
                    }
                    if (member != null) {
                        if (member.isAll()) {
                            dummyList.add(makeDummyChildMember((RolapMember) member, 0));
                        } else {
                            dummyList.add(member);
                        }
                    }
                }

                if (elementType instanceof TupleType) {
                    if (((TupleType) elementType).getElementTypes() instanceof MemberType[]) {
                        MemberType[] memberTypes = (MemberType[]) ((TupleType) elementType).getElementTypes();
                        int index = 0;
                        for (MemberType memberType : memberTypes) {
                            Member member = memberType.getMember();
                            if (member == null) {
                                member = memberType.getHierarchy().getAllMember();
                            }
                            if (member != null) {
                                if (member.isAll()) {
                                    dummyList.add(makeDummyChildMember((RolapMember) member, index++));
                                } else {
                                    dummyList.add(member);
                                }
                            }
                        }
                    } else {
                        List<Hierarchy> hierarchies = ((TupleType) elementType).getHierarchies();
                        for (Hierarchy hierarchy : hierarchies) {
                            Member member = hierarchy.getAllMember();
                            int index = 0;
                            if (member != null) {
                                dummyList.add(makeDummyChildMember((RolapMember) member, index++));
                            }
                        }
                    }
                }

                TupleList tupleList = TupleCollections.createList(dummyList.size());
                tupleList.add(dummyList);
                this.axes[i] =
                        new RolapAxis(
                                TupleCollections.materialize(
                                        tupleList, false));
            }

        } catch (CellRequestQuantumExceededException e) {
            // Safe to ignore. Need to call 'phase' and loop again.
        }
        evaluator.restore(savepoint);
    }

    /**
     * Sets slicerAxis to a dummy placeholder RolapAxis containing
     * a single item TupleList with the all member of hierarchy.
     * This is used with compound slicer evaluation to avoid the slicer
     * tuple list from interacting with the aggregate calc which rolls up
     * the set.  This member will contain the AggregateCalc which rolls
     * up the set on the slicer.
     */
    private Member setPlaceholderSlicerAxis(final TupleList tupleList, final Calc calc) {
        // Arbitrarily picks the first dim of the first tuple
        // to use as placeholder.
        RolapMember member = (RolapMember) tupleList.get(0).get(0);
        ValueFormatter formatter;
        if (member.getDimension().isMeasures()) {
            formatter = ((RolapMeasure) member).getFormatter();
        } else {
            formatter = null;
        }

        CompoundSlicerRolapMember placeholderMember =
                new CompoundSlicerRolapMember(
                        tupleList,
                        member.getHierarchy().getAllMember(),
                        calc, formatter);

        placeholderMember.setProperty(
                Property.FORMAT_STRING.getName(),
                member.getPropertyValue(Property.FORMAT_STRING.getName()));
        placeholderMember.setProperty(
                Property.FORMAT_EXP_PARSED.getName(),
                member.getPropertyValue(Property.FORMAT_EXP_PARSED.getName()));

        TupleList dummyList = TupleCollections.createList(1);
        dummyList.addTuple(placeholderMember);

        this.slicerAxis = new RolapAxis(dummyList);
        return placeholderMember;
    }

    protected boolean phase() {
        if (batchingReader.isDirty()) {
            execution.tracePhase(
                    batchingReader.getHitCount(),
                    batchingReader.getMissCount(),
                    batchingReader.getPendingCount());

            return batchingReader.loadAggregations();
        } else {
            return false;
        }
    }

    @Override
    public void close() {
        super.close();
    }

    protected boolean removeDimension(
            Dimension dimension,
            List<List<Member>> memberLists) {
        for (int i = 0; i < memberLists.size(); i++) {
            List<Member> memberList = memberLists.get(i);
            if (memberList.get(0).getDimension().equals(dimension)) {
                memberLists.remove(i);
                return true;
            }
        }
        return false;
    }

    public final Execution getExecution() {
        return execution;
    }

    private static class CalculatedMeasureVisitor
            extends MdxVisitorImpl {
        Dimension dimension;

        CalculatedMeasureVisitor() {
        }

        public Object visit(DimensionExpr dimensionExpr) {
            dimension = dimensionExpr.getDimension();
            return null;
        }

        public Object visit(HierarchyExpr hierarchyExpr) {
            Hierarchy hierarchy = hierarchyExpr.getHierarchy();
            dimension = hierarchy.getDimension();
            return null;
        }

        public Object visit(MemberExpr memberExpr) {
            Member member = memberExpr.getMember();
            dimension = member.getHierarchy().getDimension();
            return null;
        }
    }

    protected boolean replaceNonAllMembers(
            List<List<Member>> nonAllMembers,
            AxisMemberList axisMembers) {
        boolean changed = false;
        List<Member> mList = new ArrayList<>();
        for (ListIterator<List<Member>> it = nonAllMembers.listIterator();
             it.hasNext(); ) {
            List<Member> ms = it.next();
            Hierarchy h = ms.get(0).getHierarchy();
            mList.clear();
            for (Member m : axisMembers) {
                if (m.getHierarchy().equals(h)) {
                    mList.add(m);
                }
            }
            if (!mList.isEmpty()) {
                changed = true;
                it.set(new ArrayList<Member>(mList));
            }
        }
        return changed;
    }

    protected void loadMembers(
            List<List<Member>> nonAllMembers,
            RolapEvaluator evaluator,
            QueryAxis axis,
            Calc calc,
            AxisMemberList axisMembers) {
        int attempt = 0;
        evaluator.setCellReader(batchingReader);
        while (true) {
            axisMembers.clearAxisCount();
            final int savepoint = evaluator.savepoint();
            try {
                evalLoad(
                        nonAllMembers,
                        nonAllMembers.size() - 1,
                        evaluator,
                        axis,
                        calc,
                        axisMembers);
            } catch (CellRequestQuantumExceededException e) {
                // Safe to ignore. Need to call 'phase' and loop again.
                // Decrement count because it wasn't a recursive formula that
                // caused the iteration.
                --attempt;
            } finally {
                evaluator.restore(savepoint);
            }

            if (!phase()) {
                break;
            } else {
                // Clear invalid expression result so that the next evaluation
                // will pick up the newly loaded aggregates.
                evaluator.clearExpResultCache(false);
            }

            if (attempt++ > maxEvalDepth) {
                throw Util.newInternal(
                        "Failed to load all aggregations after "
                                + maxEvalDepth
                                + " passes; there's probably a cycle");
            }
        }
    }

    void evalLoad(
            List<List<Member>> nonAllMembers,
            int cnt,
            Evaluator evaluator,
            QueryAxis axis,
            Calc calc,
            AxisMemberList axisMembers) {
        final int savepoint = evaluator.savepoint();
        try {
            if (cnt < 0) {
                executeAxis(evaluator, axis, calc, false, axisMembers);
            } else {
                for (Member m : nonAllMembers.get(cnt)) {
                    evaluator.setContext(m);
                    evalLoad(
                            nonAllMembers, cnt - 1, evaluator,
                            axis, calc, axisMembers);
                }
            }
        } finally {
            evaluator.restore(savepoint);
        }
    }

    TupleIterable evalExecute(
            List<List<Member>> nonAllMembers,
            int cnt,
            RolapEvaluator evaluator,
            QueryAxis queryAxis,
            Calc calc) {
        final int savepoint = evaluator.savepoint();
        final int arity = calc == null ? 0 : calc.getType().getArity();
        if (cnt < 0) {
            try {
                final TupleIterable axis =
                        executeAxis(evaluator, queryAxis, calc, true, null);
                return axis;
            } finally {
                evaluator.restore(savepoint);
            }
            // No need to clear expression cache here as no new aggregates are
            // loaded(aggregatingReader reads from cache).
        } else {
            try {
                TupleList axisResult = TupleCollections.emptyList(arity);
                for (Member m : nonAllMembers.get(cnt)) {
                    evaluator.setContext(m);
                    TupleIterable axis =
                            evalExecute(
                                    nonAllMembers, cnt - 1,
                                    evaluator, queryAxis, calc);
                    boolean ordered = false;
                    if (queryAxis != null) {
                        ordered = queryAxis.isOrdered();
                    }
                    axisResult = RolapResultUtil.mergeAxes(axisResult, axis, ordered);
                }
                return axisResult;
            } finally {
                evaluator.restore(savepoint);
            }
        }
    }

    /**
     * Finds all root Members 1) whose Hierarchy does not have an ALL
     * Member, 2) whose default Member is not the ALL Member and 3)
     * all Measures.
     *
     * @param nonDefaultAllMembers List of all root Members for Hierarchies
     *                             whose default Member is not the ALL Member.
     * @param nonAllMembers        List of root Members for Hierarchies that have no
     *                             ALL Member.
     * @param measureMembers       List all Measures
     */
    protected void loadSpecialMembers(
            List<Member> nonDefaultAllMembers,
            List<List<Member>> nonAllMembers,
            List<Member> measureMembers) {
        SchemaReader schemaReader = evaluator.getSchemaReader();
        Member[] evalMembers = evaluator.getMembers();
        for (Member em : evalMembers) {
            if (em.isCalculated()) {
                continue;
            }
            Hierarchy h = em.getHierarchy();
            Dimension d = h.getDimension();
            /*if (d.getDimensionType() == org.olap4j.metadata.Dimension.Type.TIME)
            {
                continue;
            }
             */
            if (!em.isAll()) {
                List<Member> rootMembers =
                        schemaReader.getHierarchyRootMembers(h);
                if (em.isMeasure()) {
                    measureMembers.addAll(rootMembers);
                } else {
                    if (h.hasAll()) {
                        for (Member m : rootMembers) {
                            if (m.isAll()) {
                                nonDefaultAllMembers.add(m);
                                break;
                            }
                        }
                    } else {
                        nonAllMembers.add(rootMembers);
                    }
                }
            }
        }
    }

    protected Logger getLogger() {
        return LOGGER;
    }

    public final RolapCube getCube() {
        return evaluator.getCube();
    }

    /**
     * Get the Cell for the given Cell position.
     *
     * @param pos Cell position.
     * @return the Cell associated with the Cell position.
     */
    @Override
    public Cell getCell(int[] pos) {
        if (pos.length != point.size()) {
            throw Util.newError(
                    "coordinates should have dimension " + point.size());
        }

        CellInfo ci = cellInfos.lookup(pos);
        if (ci.value == null) {
            for (int i = 0; i < pos.length; i++) {
                int po = pos[i];
                if (po < 0 || po >= axes[i].getPositions().size()) {
                    throw Util.newError("coordinates out of range");
                }
            }
            ci.value = Util.nullValue;
        }

        return new RolapCell(this, pos.clone(), ci, preserveRegularFormat);
    }

    private TupleIterable executeAxis(
            Evaluator evaluator,
            QueryAxis queryAxis,
            Calc axisCalc,
            boolean construct,
            AxisMemberList axisMembers) {
        if (queryAxis == null) {
            // Create an axis containing one position with no members (not
            // the same as an empty axis).
            return new DelegatingTupleList(
                    0,
                    Collections.singletonList(Collections.emptyList()));
        }
        final int savepoint = evaluator.savepoint();
        try {
            evaluator.setNonEmpty(queryAxis.isNonEmpty());
            evaluator.setEvalAxes(true);
            final TupleIterable iterable =
                    ((IterCalc) axisCalc).evaluateIterable(evaluator);
            if (axisCalc.getClass().getName().contains("OrderFunDef")) {
                queryAxis.setOrdered(true);
            }
            if (iterable instanceof TupleList) {
                TupleList list = (TupleList) iterable;
                if (!construct && axisMembers != null) {
                    axisMembers.mergeTupleList(list);
                }
            } else {
                // Iterable
                TupleCursor cursor = iterable.tupleCursor();
                if (!construct && axisMembers != null) {
                    axisMembers.mergeTupleIter(cursor);
                }
            }
            return iterable;
        } finally {
            evaluator.restore(savepoint);
        }
    }

    private void executeBody(RolapEvaluator evaluator) {

        // Compute the cells several times. The first time, use a dummy
        // evaluator which collects requests.
        int count = 0;
        final int savepoint = evaluator.savepoint();
        final CellInfoMarker marker = new CellInfoMarker(cellInfos, axes.length);

        while (true) {
            evaluator.setCellReader(batchingReader);
            try {
                int[] startPos = marker.getStartPos();
                executeStripe(marker, axes.length - 1, evaluator, true, startPos);
            } catch (CellRequestQuantumExceededException e) {
                // Safe to ignore. Need to call 'phase' and loop again.
                // Decrement count because it wasn't a recursive formula that
                // caused the iteration.
                --count;
            }
            evaluator.restore(savepoint);

            // Retrieve the aggregations collected.
            if (!phase()) {
                // We got all of the cells we needed, so the result must be correct.
                return;
            } else {
                // Clear invalid expression result so that the next evaluation
                // will pick up the newly loaded aggregates.
                evaluator.clearExpResultCache(false);
            }

            if (count++ > maxEvalDepth) {
                if (evaluator instanceof RolapDependencyTestingEvaluator) {
                    // The dependency testing evaluator can trigger new
                    // requests every cycle. So let is run as normal for
                    // the first N times, then run it disabled.
                    ((RolapDependencyTestingEvaluator.DteRoot) evaluator.root).disabled = true;
                    if (count > maxEvalDepth * 2) {
                        throw Util.newInternal("Query required more than " + count + " iterations");
                    }
                } else {
                    throw Util.newInternal("Query required more than " + count + " iterations");
                }
            }

            marker.calculatePos();
        }
    }

    private void executeStripe(CellInfoMarker marker, int axisOrdinal, RolapEvaluator revaluator,
                               boolean noMeasureOnAxis, int[] startPos) {
        if (axisOrdinal < 0) {
            executeSlicer(revaluator, noMeasureOnAxis);
            return;
        }

        RolapAxis axis = (RolapAxis) axes[axisOrdinal];
        TupleList tupleList = axis.getTupleList();
        // force materialize, ensure can random access.
        Util.discard(tupleList.size());
        // process distinct count measures.
        processDistinctMeasureCount(tupleList);

        // 尝试快速定位 axisOrdinal 轴起始点
        marker.setAxesLength(axisOrdinal, tupleList.size());
        int tupleIndex = startPos[axisOrdinal];
        startPos[axisOrdinal] = 0;
        for (; tupleIndex < tupleList.size(); tupleIndex++) {
            List<Member> tuple = tupleList.get(tupleIndex);
            point.setAxis(axisOrdinal, tupleIndex);
            final int savepoint = revaluator.savepoint();
            try {
                revaluator.setContextIncludingCustomRollupAll(tuple);
                execution.checkCancelOrTimeout();
                boolean hasNoneMeasure = containsNoneMeasure(tuple) && noMeasureOnAxis;
                executeStripe(marker, axisOrdinal - 1, revaluator, hasNoneMeasure, startPos);
            } finally {
                revaluator.restore(savepoint);
            }
        }
    }

    private void executeSlicer(RolapEvaluator revaluator, boolean noMeasureOnAxis) {
        RolapAxis axis = (RolapAxis) slicerAxis;
        TupleList tupleList = axis.getTupleList();
        final Iterator<List<Member>> tupleIterator = tupleList.iterator();
        if (tupleIterator.hasNext()) {
            // Create a CellInfo object for the given position integer array.
            // If CellInfo's ready is true, skip this cell calculation.
            CellInfo ci = cellInfos.create(point.getOrdinals());
            if (ci.ready) {
                return;
            }

            final List<Member> members = tupleIterator.next();
            execution.checkCancelOrTimeout();
            final int savepoint = revaluator.savepoint();
            revaluator.setContext(members);
            Object o;
            try {
                RolapResultUtil.resetValueNotReady();
                if (!MondrianProperties.instance().CalculateCellWhenNoneMeasure.get()
                        && noMeasureOnAxis
                        && containsNoneMeasure(members)
                        && containNoneCalcMeasure(revaluator)) {
                    o = 2;
                } else {
                    o = revaluator.evaluateCurrent();
                }
            } catch (MondrianEvaluationException e) {
                LOGGER.warn("Mondrian: exception in evaluation.", e);
                o = e;
            } catch (RuntimeException e) {
                if (e instanceof CellRequestQuantumExceededException) {
                    throw e;
                }
                if (!(e instanceof MondrianException)) {
                    if (e.getMessage() == null || "".equals(e.getMessage())) {
                        e = new RuntimeException(e.toString() + " " + revaluator.getMembers()[0], e);
                    }
                }
                LOGGER.error(e.getMessage());
                o = null;
                runtimeExceptions.add(e);
            } finally {
                revaluator.restore(savepoint);
            }

            // Get the Cell's format string and value formatting Object.
            try {
                // This code is a combination of the code found in the old RolapResult
                // <code>getCellNoDefaultFormatString</code> method and
                // the old RolapCell <code>getFormattedValue</code> method.
                fillCellInfo(ci, revaluator);
            } catch (ResultLimitExceededException e) {
                // Do NOT ignore a ResultLimitExceededException!!!
                throw e;
            } catch (MondrianEvaluationException e) {
                // ignore but warn
                LOGGER.warn("Mondrian: exception in executeStripe.", e);
            }

            if (o != RolapUtil.valueNotReadyException) {
                ci.value = o;
            }
            if (!RolapResultUtil.isValueNotReady()) {
                ci.ready = true;
            }
        }
    }

    protected void fillCellInfo(CellInfo ci, RolapEvaluator revaluator) {
        String cachedFormatString = null;

        // Determine if there is a CellFormatter registered for
        // the current Cube's Measure's Dimension. If so,
        // then find or create a CellFormatterValueFormatter
        // for it. If not, then find or create a Locale based
        // FormatValueFormatter.
        RolapMeasure m = (RolapMeasure) revaluator.getMembers()[0];
        ValueFormatter valueFormatter = m.getFormatter();
        if (valueFormatter == null) {
            cachedFormatString = revaluator.getFormatString();
            Locale locale = statement.getMondrianConnection().getLocale();
            valueFormatter = RolapResultUtil.getValueFormatter(locale);
        }

        ci.formatString = cachedFormatString;
        ci.valueFormatter = valueFormatter;
    }

    private boolean containNoneCalcMeasure(RolapEvaluator revaluator) {
        return revaluator.getCalculationCount() == 0;
    }

    private boolean containsNoneMeasure(List<Member> members) {
        for (Member member : members) {
            if (member instanceof RolapMeasure) {
                return false;
            }
        }
        return true;
    }

    private void processDistinctMeasureCount(TupleList tupleList) {
        List<RolapBaseCubeMeasure> measures = statement.getQuery().getMeasuresMembers().stream()
                .filter(measure -> measure instanceof RolapBaseCubeMeasure)
                .map(measure -> (RolapBaseCubeMeasure) measure)
                .filter(measure -> measure.getAggregator() == RolapAggregator.DistinctCount)
                .collect(Collectors.toList());
        if (measures.isEmpty()) {
            return;
        }
        for (List<Member> tuple : tupleList) {
            for (RolapBaseCubeMeasure measure : measures) {
                if (measure.getAggregator() == RolapAggregator.DistinctCount) {
                    processDistinctMeasureExpr(tuple, measure);
                }
            }
        }
    }

    /**
     * Distinct counts are aggregated separately from other measures.
     * We need to apply filters to each level in the query.
     *
     * <p>Replace VisualTotalMember expressions with new expressions
     * where all leaf level members are included.</p>
     *
     * <p>Example.
     * For MDX query:
     *
     * <blockquote><pre>
     * WITH SET [XL_Row_Dim_0] AS
     *         VisualTotals(
     *           Distinct(
     *             Hierarchize(
     *               {Ascendants([Store].[All Stores].[USA].[CA]),
     *                Descendants([Store].[All Stores].[USA].[CA])})))
     *        select NON EMPTY
     *          Hierarchize(
     *            Intersect(
     *              {DrilldownLevel({[Store].[All Stores]})},
     *              [XL_Row_Dim_0])) ON COLUMNS
     *        from [HR]
     *        where [Measures].[Number of Employees]</pre></blockquote>
     *
     * <p>For member [Store].[All Stores],
     * we replace aggregate expression
     *
     * <blockquote><pre>
     * Aggregate({[Store].[All Stores].[USA]})
     * </pre></blockquote>
     * <p>
     * with
     *
     * <blockquote><pre>
     * Aggregate({[Store].[All Stores].[USA].[CA].[Alameda].[HQ],
     *               [Store].[All Stores].[USA].[CA].[Beverly Hills].[Store 6],
     *               [Store].[All Stores].[USA].[CA].[Los Angeles].[Store 7],
     *               [Store].[All Stores].[USA].[CA].[San Diego].[Store 24],
     *               [Store].[All Stores].[USA].[CA].[San Francisco].[Store 14]
     *              })
     * </pre></blockquote>
     *
     * <p>TODO:
     * Can be optimized. For that particular query
     * we don't need to go to the lowest level.
     * We can simply replace it with:
     * <pre>Aggregate({[Store].[All Stores].[USA].[CA]})</pre>
     * Because all children of [Store].[All Stores].[USA].[CA] are included.</p>
     */
    private void processDistinctMeasureExpr(List<Member> tuple, RolapBaseCubeMeasure measure) {
        for (Member member : tuple) {
            if (!(member instanceof VisualTotalMember)) {
                continue;
            }
            evaluator.setContext(measure);
            List<Member> exprMembers = new ArrayList<>();
            RolapResultUtil.processMemberExpr(member, exprMembers);
            ((VisualTotalMember) member).setExpression(evaluator, exprMembers);
        }
    }

    /**
     * Converts a set of cell coordinates to a cell ordinal.
     *
     * <p>This method can be expensive, because the ordinal is computed from the
     * length of the axes, and therefore the axes need to be instantiated.
     */
    int getCellOrdinal(int[] pos) {
        if (modulos == null) {
            modulos = Modulos.Generator.create(axes);
        }
        return modulos.getCellOrdinal(pos);
    }

    /**
     * Called only by RolapCell. Use this when creating an Evaluator
     * is not required.
     *
     * @param pos Coordinates of cell
     * @return Members which form the context of the given cell
     */
    RolapMember[] getCellMembers(int[] pos) {
        RolapMember[] members = evaluator.getMembers().clone();
        for (int i = 0; i < pos.length; i++) {
            Position position = axes[i].getPositions().get(pos[i]);
            for (Member member : position) {
                RolapMember m = (RolapMember) member;
                int ordinal = m.getHierarchy().getOrdinalInCube();
                members[ordinal] = m;
            }
        }
        return members;
    }

    Evaluator getRootEvaluator() {
        return evaluator;
    }

    Evaluator getEvaluator(int[] pos) {
        // Set up evaluator's context, so that context-dependent format
        // strings work properly.
        Evaluator cellEvaluator = evaluator.push();
        populateEvaluator(cellEvaluator, pos);
        return cellEvaluator;
    }

    void populateEvaluator(Evaluator evaluator, int[] pos) {
        for (int i = -1; i < axes.length; i++) {
            Axis axis;
            int index;
            if (i < 0) {
                axis = slicerAxis;
                if (axis.getPositions().isEmpty()) {
                    continue;
                }
                index = 0;
            } else {
                axis = axes[i];
                index = pos[i];
            }
            Position position = axis.getPositions().get(index);
            evaluator.setContext(position);
        }
    }

    /**
     * Evaluates an expression. Intended for evaluating named sets.
     *
     * <p>Does not modify the contents of the evaluator.
     *
     * @param calc      Compiled expression
     * @param evaluator Evaluation context
     * @return Result
     */
    Object evaluateExp(Calc calc, RolapEvaluator evaluator) {
        int attempt = 0;
        final int savepoint = evaluator.savepoint();
        boolean dirty = batchingReader.isDirty();
        try {
            while (true) {
                evaluator.restore(savepoint);

                evaluator.setCellReader(batchingReader);
                Object preliminaryValue = calc.evaluate(evaluator);
                if (preliminaryValue instanceof TupleIterable
                        && !(preliminaryValue instanceof TupleList)) {
                    TupleIterable iterable = (TupleIterable) preliminaryValue;
                    final TupleCursor cursor = iterable.tupleCursor();
                    while (cursor.forward()) {
                        // ignore
                    }
                }

                if (!phase()) {
                    break;
                } else {
                    // Clear invalid expression result so that the next
                    // evaluation will pick up the newly loaded aggregates.
                    evaluator.clearExpResultCache(false);
                }

                if (attempt++ > maxEvalDepth) {
                    throw Util.newInternal(
                            "Failed to load all aggregations after "
                                    + maxEvalDepth + "passes; there's probably a cycle");
                }
            }

            // If there were pending reads when we entered, some of the other
            // expressions may have been evaluated incorrectly. Set the
            // reader's 'dirty' flag so that the caller knows that it must
            // re-evaluate them.
            if (dirty) {
                batchingReader.setDirty(true);
            }

            evaluator.restore(savepoint);
            evaluator.setCellReader(aggregatingReader);
            return calc.evaluate(evaluator);
        } finally {
            evaluator.restore(savepoint);
        }
    }

    /**
     * Collection of members found on an axis.
     *
     * <p>The behavior depends on the mode (i.e. the kind of axis).
     * If it collects, it generally eliminates duplicates. It also has a mode
     * where it only counts members, does not collect them.</p>
     *
     * <p>This class does two things. First it collects all Members
     * found during the Member-Determination phase.
     * Second, it counts how many Members are on each axis and
     * forms the product, the totalCellCount which is checked against
     * the ResultLimit property value.</p>
     */
    private static class AxisMemberList implements Iterable<Member> {
        private final List<Member> members;
        private final int limit;
        private boolean isSlicer;
        private int totalCellCount;
        private int axisCount;
        private boolean countOnly;

        AxisMemberList() {
            this.countOnly = false;
            this.members = new ConcatenableList<Member>();
            this.totalCellCount = 1;
            this.axisCount = 0;
            // Now that the axes are evaluated, make sure that the number of
            // cells does not exceed the result limit.
            this.limit = MondrianProperties.instance().ResultLimit.get();
        }

        public Iterator<Member> iterator() {
            return members.iterator();
        }

        void setSlicer(final boolean isSlicer) {
            this.isSlicer = isSlicer;
        }

        boolean isEmpty() {
            return this.members.isEmpty();
        }

        void countOnly(boolean countOnly) {
            this.countOnly = countOnly;
        }

        void checkLimit() {
            if (this.limit > 0) {
                this.totalCellCount *= this.axisCount;
                if (this.totalCellCount > this.limit) {
                    throw MondrianResource.instance().TotalMembersLimitExceeded
                            .ex(
                                    this.totalCellCount,
                                    this.limit);
                }
                this.axisCount = 0;
            }
        }

        void clearAxisCount() {
            this.axisCount = 0;
        }

        void clearTotalCellCount() {
            this.totalCellCount = 1;
        }

        void clearMembers() {
            this.members.clear();
            this.axisCount = 0;
            this.totalCellCount = 1;
        }

        List<Member> members() {
            return this.members;
        }

        void mergeTupleList(TupleList list) {
            mergeTupleIter(list.tupleCursor());
        }

        private void mergeTupleIter(TupleCursor cursor) {
            while (cursor.forward()) {
                mergeTuple(cursor);
            }
        }

        private Member getTopParent(Member m) {
            while (true) {
                Member parent = m.getParentMember();
                if (parent == null) {
                    return m;
                }
                m = parent;
            }
        }

        private void mergeTuple(final TupleCursor cursor) {
            final int arity = cursor.getArity();
            for (int i = 0; i < arity; i++) {
                mergeMember(cursor.member(i));
            }
        }

        private void mergeMember(final Member member) {
            this.axisCount++;
            if (!countOnly) {
                if (isSlicer) {
                    if (!members.contains(member)) {
                        members.add(member);
                    }
                } else {
                    if (member.isNull()) {
                        return;
                    } else if (member.isMeasure()) {
                        return;
                    } else if (member.isCalculated()) {
                        return;
                    } else if (member.isAll()) {
                        return;
                    }
                    Member topParent = getTopParent(member);
                    if (!this.members.contains(topParent)) {
                        this.members.add(topParent);
                    }
                }
            }
        }
    }

    /**
     * Member which holds the AggregateCalc used when evaluating
     * a compound slicer.  This is used to better handle some cases
     * where calculated members elsewhere in the query can override
     * the context of the slicer members.
     * See MONDRIAN-1226.
     */
    private static class CompoundSlicerRolapMember extends DelegatingRolapMember implements RolapMeasure {
        private final Calc calc;
        private final ValueFormatter valueFormatter;
        private final TupleList tupleList;

        public CompoundSlicerRolapMember(
                TupleList tupleList,
                RolapMember placeholderMember, Calc calc, ValueFormatter formatter) {
            super(placeholderMember);
            this.calc = calc;
            valueFormatter = formatter;
            this.tupleList = tupleList;
        }

        public boolean equals(Object o) {
            if (!(o instanceof CompoundSlicerRolapMember)) {
                return false;
            }
            TupleList otherTupleList =
                    ((CompoundSlicerRolapMember) o).tupleList;
            if (this.tupleList.size() != otherTupleList.size()) {
                return false;
            }
            for (int i = 0; i < tupleList.size(); i++) {
                if (!otherTupleList.get(0).equals(tupleList.get(0))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = 17;
            for (List<Member> tuple : tupleList) {
                result = 31 * result + Arrays.hashCode(tuple.toArray());
            }
            return result;
        }

        @Override
        public boolean isEvaluated() {
            return true;
        }

        @Override
        public Exp getExpression() {
            return new DummyExp(calc.getType());
        }

        @Override
        public Calc getCompiledExpression(RolapEvaluatorRoot root) {
            return calc;
        }

        @Override
        public int getSolveOrder() {
            return 0;
        }

        public ValueFormatter getFormatter() {
            return valueFormatter;
        }
    }

}

// End RolapResult.java
