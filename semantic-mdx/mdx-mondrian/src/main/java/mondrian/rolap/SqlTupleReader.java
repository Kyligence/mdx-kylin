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

import mondrian.calc.TupleList;
import mondrian.calc.impl.ListTupleList;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.Evaluator;
import mondrian.olap.Member;
import mondrian.olap.MondrianProperties;
import mondrian.olap.Util;
import mondrian.olap.fun.FunUtil;
import mondrian.resource.MondrianResource;
import mondrian.rolap.agg.AggregationManager;
import mondrian.rolap.agg.CellRequest;
import mondrian.rolap.aggmatcher.AggStar;
import mondrian.rolap.sql.CrossJoinArg;
import mondrian.rolap.sql.DescendantsCrossJoinArg;
import mondrian.rolap.sql.MemberListCrossJoinArg;
import mondrian.rolap.sql.TupleConstraint;
import mondrian.server.monitor.SqlStatementEvent;
import mondrian.spi.Dialect;
import mondrian.util.Pair;
import mondrian.xmla.XmlaRequestContext;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Reads the members of a single level (level.members) or of multiple levels
 * (crossjoin).
 *
 * <p>Allows the result to be restricted by a {@link TupleConstraint}. So
 * the SqlTupleReader can also read Member.Descendants (which is level.members
 * restricted to a common parent) and member.children (which is a special case
 * of member.descendants). Other constraints, especially for the current slicer
 * or evaluation context, are possible.
 *
 * <h3>Caching</h3>
 *
 * <p>When a SqlTupleReader reads level.members, it groups the result into
 * parent/children pairs and puts them into the cache. In order that these can
 * be found later when the children of a parent are requested, a matching
 * constraint must be provided for every parent.
 *
 * <ul>
 *
 * <li>When reading members from a single level, then the constraint is not
 * required to join the fact table in
 * {@link TupleConstraint#addLevelConstraint(mondrian.rolap.sql.SqlQuery, RolapStarSet, RolapCubeLevel)}
 * although it may do so to restrict
 * the result. Also it is permitted to cache the parent/children from all
 * members in MemberCache, so
 * {@link TupleConstraint#getMemberChildrenConstraint(RolapMember)}
 * should not return null.</li>
 *
 * <li>When reading multiple levels (i.e. we are performing a crossjoin),
 * then we can not store the parent/child pairs in the MemberCache and
 * {@link TupleConstraint#getMemberChildrenConstraint(RolapMember)}
 * must return null. Also
 * {@link TupleConstraint#addConstraint(mondrian.rolap.sql.SqlQueryBuilder, RolapStarSet)}
 * is required to join the fact table for the levels table.</li>
 * </ul>
 *
 * @author av
 * @since Nov 11, 2005
 */
public class SqlTupleReader extends SqlReaderBase implements TupleReader {
    int maxRows = 0;

    /**
     * How many members could not be instantiated in this iteration. This
     * phenomenon occurs in a parent-child hierarchy, where a member cannot be
     * created before its parent. Populating the hierarchy will take multiple
     * passes and will terminate in success when missedMemberCount == 0 at the
     * end of a pass, or failure if a pass generates failures but does not
     * manage to load any more members.
     */
    private int missedMemberCount;

    public SqlTupleReader(TupleConstraint constraint) {
        super(constraint);
    }

    public Object getCacheKey() {
        List<Object> key = new ArrayList<Object>();
        key.add(constraint.getCacheKey());
        key.add(SqlTupleReader.class);
        for (Target target : targets) {
            // don't include the level in the key if the target isn't
            // processed through native sql
            if (target.srcMembers != null) {
                key.add(target.getLevel());
            }
        }
        return key;
    }

    /**
     * @return number of targets that contain enumerated sets with calculated
     * members
     */
    public int getEnumTargetCount()
    {
        int enumTargetCount = 0;
        for (Target target : targets) {
            if (target.getSrcMembers() != null) {
                enumTargetCount++;
            }
        }
        return enumTargetCount;
    }

    private void prepareTuples(
        Dialect dialect,
        DataSource dataSource,
        TupleList partialResult,
        List<List<RolapMember>> newPartialResult)
    {
        String message = "Populating member cache with members for " + targets;
        SqlStatement stmt = null;
        final ResultSet resultSet;
        boolean execQuery = Objects.isNull(partialResult);
        try {
            if (execQuery) {
                List<Target> partialTargets = getPartialTargets();

                boolean shouldHasLimit = true;
                if (constraint instanceof RolapNativeCrossJoin.NonEmptyCrossJoinConstraint) {
                    RolapNativeCrossJoin.NonEmptyCrossJoinConstraint nonEmptyCrossJoinConstraint =
                            (RolapNativeCrossJoin.NonEmptyCrossJoinConstraint) constraint;
                    if (nonEmptyCrossJoinConstraint.args[0] instanceof DescendantsCrossJoinArg
                        && ((DescendantsCrossJoinArg) nonEmptyCrossJoinConstraint.args[0]).isLeaves()
                        && nonEmptyCrossJoinConstraint.args[1] instanceof DescendantsCrossJoinArg
                        && ((DescendantsCrossJoinArg) nonEmptyCrossJoinConstraint.args[1]).isLeaves())
                        shouldHasLimit = false;
                }
                if (constraint instanceof RolapNativeFilter.FilterDescendantsConstraint) {
                    if (((RolapNativeFilter.FilterDescendantsConstraint) constraint).isLeaves()) {
                        shouldHasLimit = false;
                    }
                }

                final Pair<String, List<SqlStatement.Type>> pair =
                    makeLevelMembersSql(dialect, shouldHasLimit);
                String sql = pair.left;
                List<SqlStatement.Type> types = pair.right;
                assert sql != null && !sql.equals("");
                if (!sql.contains("limit")
                        && (XmlaRequestContext.getContext().filterRowLimitFlag
                            || (sql.contains("LIKE '%")
                                && MondrianProperties.instance().FilterRowLimit.get() > 0))) {
                    sql = sql + " limit " + MondrianProperties.instance().FilterRowLimit.get();
                }
                stmt = RolapUtil.executeQuery(
                    dataSource, sql, types, maxRows, 0,
                    new SqlStatement.StatementLocus(
                        getExecution(),
                        "SqlTupleReader.prepareTuples " + partialTargets,
                        message,
                        SqlStatementEvent.Purpose.TUPLES, 0),
                    -1, -1, null);
                resultSet = stmt.getResultSet();
            } else {
                resultSet = null;
            }

            for (Target target : targets) {
                target.open();
            }

            int limit = MondrianProperties.instance().ResultLimit.get();
            int fetchCount = 0;

            // determine how many enum targets we have
            int enumTargetCount = getEnumTargetCount();
            int[] srcMemberIdxes = null;
            if (enumTargetCount > 0) {
                srcMemberIdxes = new int[enumTargetCount];
            }

            boolean moreRows;
            int currPartialResultIdx = 0;
            if (execQuery) {
                moreRows = resultSet.next();
                if (moreRows) {
                    ++stmt.rowCount;
                }
            } else {
                moreRows = currPartialResultIdx < partialResult.size();
            }
            while (moreRows) {
                if (limit > 0 && limit < ++fetchCount) {
                    // result limit exceeded, throw an exception
                    throw MondrianResource.instance().MemberFetchLimitExceeded
                        .ex((long) limit);
                }

                if (enumTargetCount == 0) {
                    if (constraint instanceof RolapNativeFilter.FilterDescendantsConstraint
                            && ((RolapNativeFilter.FilterDescendantsConstraint) constraint).isLeaves()) {
                        // Only read the lowest-level member from one result row, this member should be a leaf
                        List<Target> targets2 = new ArrayList<>(targets);
                        Collections.sort(targets2);
                        Collections.reverse(targets2);
                        for (Target target : targets2) {
                            target.setCurrMember(null);
                            if (target.addRow(stmt) != null)
                                break;
                        }
                    } else {
                        for (Target target : targets) {
                            target.setCurrMember(null);
                            target.addRow(stmt);
                        }
                    }
                } else {
                    // find the first enum target, then call addTargets()
                    // to form the cross product of the row from resultSet
                    // with each of the list of members corresponding to
                    // the enumerated targets
                    int firstEnumTarget = 0;
                    for (; firstEnumTarget < targets.size();
                        firstEnumTarget++)
                    {
                        if (targets.get(firstEnumTarget).srcMembers != null) {
                            break;
                        }
                    }
                    List<RolapMember> partialRow;
                    if (execQuery) {
                        partialRow = null;
                    } else {
                        partialRow =
                            Util.cast(partialResult.get(currPartialResultIdx));
                    }
                    resetCurrMembers(partialRow);
                    addTargets(
                        0, firstEnumTarget, enumTargetCount, srcMemberIdxes,
                        stmt, message);
                    if (newPartialResult != null) {
                        savePartialResult(newPartialResult);
                    }
                }

                if (execQuery) {
                    moreRows = resultSet.next();
                    if (moreRows) {
                        ++stmt.rowCount;
                    }
                } else {
                    currPartialResultIdx++;
                    moreRows = currPartialResultIdx < partialResult.size();
                }
            }
        } catch (SQLException e) {
            if (stmt == null) {
                throw Util.newError(e, message);
            } else {
                throw stmt.handle(e);
            }
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    @Override
    public TupleList readMembers(
        Dialect dialect,
        DataSource dataSource,
        TupleList partialResult,
        List<List<RolapMember>> newPartialResult)
    {
        int memberCount = countMembers();
        while (true) {
            missedMemberCount = 0;
            int memberCountBefore = memberCount;
            prepareTuples(dialect, dataSource, partialResult, newPartialResult);
            memberCount = countMembers();
            if (missedMemberCount == 0) {
                // We have successfully read all members. This is always the
                // case in a regular hierarchy. In a parent-child hierarchy
                // it may take several passes, because we cannot create a member
                // before we create its parent.
                break;
            }
            if (memberCount == memberCountBefore) {
                // This pass made no progress. This must be because of a cycle.
                throw Util.newError(
                    "Parent-child hierarchy contains cyclic data");
            }
        }

        if (constraint instanceof RolapNativeFilter.FilterDescendantsConstraint
                    && ((RolapNativeFilter.FilterDescendantsConstraint) constraint).isLeaves()) {
            List<Member> descendantsResults = new ArrayList<>(targets.get(targets.size() - 1).getList().size());
            for (Target target : targets) {
                descendantsResults.addAll(target.close());
            }
            return new UnaryTupleList(bumpNullMember(descendantsResults));
        } else {
            assert targets.size() == 1;

            return new UnaryTupleList(
                    bumpNullMember(
                            targets.get(0).close()));
        }
    }

    protected List<Member> bumpNullMember(List<Member> members) {
        if (members.size() > 0
            && ((RolapMemberBase)members.get(members.size() - 1)).getKey()
                == RolapUtil.sqlNullValue)
        {
            Member removed = members.remove(members.size() - 1);
            members.add(0, removed);
        }
        return members;
    }

    /**
     * Returns the number of members that have been read from all targets.
     *
     * @return Number of members that have been read from all targets
     */
    private int countMembers() {
        int n = 0;
        for (Target target : targets) {
            if (target.getList() != null) {
                n += target.getList().size();
            }
        }
        return n;
    }

    public TupleList readTuples(
        Dialect dialect,
        DataSource dataSource,
        TupleList partialResult,
        List<List<RolapMember>> newPartialResult)
    {
        prepareTuples(dialect, dataSource, partialResult, newPartialResult);

        // List of tuples
        final int n = targets.size();
        @SuppressWarnings({"unchecked"})
        final Iterator<Member>[] iter = new Iterator[n];
        for (int i = 0; i < n; i++) {
            Target t = targets.get(i);
            iter[i] = t.close().iterator();
        }
        List<Member> members = new ArrayList<Member>();
        while (iter[0].hasNext()) {
            for (int i = 0; i < n; i++) {
                members.add(iter[i].next());
            }
        }

        TupleList tupleList =
            n + emptySets == 1
                ? new UnaryTupleList(members)
                : new ListTupleList(n + emptySets, members);

        // need to hierarchize the columns from the enumerated targets
        // since we didn't necessarily add them in the order in which
        // they originally appeared in the cross product
        int enumTargetCount = getEnumTargetCount();
        if (enumTargetCount > 0) {
            tupleList = FunUtil.hierarchizeTupleList(tupleList, false);
        }
        return tupleList;
    }

    /**
     * Sets the current member for those targets that retrieve their column
     * values from native sql
     *
     * @param partialRow if set, previously cached result set
     */
    private void resetCurrMembers(List<RolapMember> partialRow) {
        int nativeTarget = 0;
        for (Target target : targets) {
            if (target.srcMembers == null) {
                // if we have a previously cached row, use that by picking
                // out the column corresponding to this target; otherwise,
                // we need to retrieve a new column value from the current
                // result set
                if (partialRow != null) {
                    target.setCurrMember(partialRow.get(nativeTarget++));
                } else {
                    target.setCurrMember(null);
                }
            }
        }
    }

    /**
     * Recursively forms the cross product of a row retrieved through sql
     * with each of the targets that contains an enumerated set of members.
     *
     * @param currEnumTargetIdx current enum target that recursion
     *     is being applied on
     * @param currTargetIdx index within the list of a targets that
     *     currEnumTargetIdx corresponds to
     * @param nEnumTargets number of targets that have enumerated members
     * @param srcMemberIdxes for each enumerated target, the current member
     *     to be retrieved to form the current cross product row
     * @param stmt Statement containing the result set corresponding to rows
     *     retrieved through native SQL
     * @param message Message to issue on failure
     */
    private void addTargets(
        int currEnumTargetIdx,
        int currTargetIdx,
        int nEnumTargets,
        int[] srcMemberIdxes,
        SqlStatement stmt,
        String message)
    {
        // loop through the list of members for the current enum target
        Target currTarget = targets.get(currTargetIdx);
        for (int i = 0; i < currTarget.srcMembers.size(); i++) {
            srcMemberIdxes[currEnumTargetIdx] = i;
            // if we're not on the last enum target, recursively move
            // to the next one
            if (currEnumTargetIdx < nEnumTargets - 1) {
                int nextTargetIdx = currTargetIdx + 1;
                for (; nextTargetIdx < targets.size(); nextTargetIdx++) {
                    if (targets.get(nextTargetIdx).srcMembers != null) {
                        break;
                    }
                }
                addTargets(
                    currEnumTargetIdx + 1, nextTargetIdx, nEnumTargets,
                    srcMemberIdxes, stmt, message);
            } else {
                // form a cross product using the columns from the current
                // result set row and the current members that recursion
                // has reached for the enum targets
                int enumTargetIdx = 0;
                for (Target target : targets) {
                    if (target.srcMembers == null) {
                        try {
                            target.addRow(stmt);
                        } catch (Throwable e) {
                            throw Util.newError(e, message);
                        }
                    } else {
                        RolapMember member =
                            target.srcMembers.get(
                                srcMemberIdxes[enumTargetIdx++]);
                        target.getList().add(member);
                    }
                }
            }
        }
    }

    /**
     * Retrieves the current members fetched from the targets executed
     * through sql and form tuples, adding them to partialResult
     *
     * @param partialResult list containing the columns and rows corresponding
     * to data fetched through sql
     */
    private void savePartialResult(List<List<RolapMember>> partialResult) {
        List<RolapMember> row = new ArrayList<RolapMember>();
        for (Target target : targets) {
            if (target.srcMembers == null) {
                row.add(target.getCurrMember());
            }
        }
        partialResult.add(row);
    }

    /**
     * Obtains the AggStar instance which corresponds to an aggregate table
     * which can be used to support the member constraint.
     *
     * @param constraint Constraint
     * @param measureGroup1 Measure group
     * @param evaluator the current evaluator to obtain the cube and members to
     *        be queried
     * @return AggStar for aggregate table
     */
    AggStar chooseAggStar(
        TupleConstraint constraint,
        RolapMeasureGroup measureGroup1,
        Evaluator evaluator)
    {
        if (!MondrianProperties.instance().UseAggregates.get()) {
            return null;
        }

        if (evaluator == null) {
            return null;
        }

        // Convert global ordinal to cube based ordinal (the 0th dimension
        // is always [Measures]). In the case of filter constraint this will
        // be the measure on which the filter will be done.
        final Member[] members = evaluator.getNonAllMembers();

        // if measure is calculated, we can't continue
        if (!(members[0] instanceof RolapBaseCubeMeasure)) {
            return null;
        }
        RolapBaseCubeMeasure measure = (RolapBaseCubeMeasure)members[0];
        int bitPosition = measure.getStarMeasure().getBitPosition();

        // set a bit for each level which is constrained in the context
        final CellRequest request =
            RolapAggregationManager.makeRequest(members);
        if (request == null) {
            // One or more calculated members. Cannot use agg table.
            return null;
        }
        // TODO: RME why is this using the array of constrained columns
        // from the CellRequest rather than just the constrained columns
        // BitKey (method getConstrainedColumnsBitKey)?
        final RolapMeasureGroup measureGroup = measure.getMeasureGroup();
        final RolapStar star = measureGroup.getStar();
        final int starColumnCount = star.getColumnCount();
        BitKey measureBitKey = BitKey.Factory.makeBitKey(starColumnCount);
        BitKey levelBitKey = BitKey.Factory.makeBitKey(starColumnCount);

        RolapStar.Column[] columns = request.getConstrainedColumns();
        for (RolapStar.Column column1 : columns) {
            levelBitKey.set(column1.getBitPosition());
        }

        // set the masks
        for (Target target : targets) {
            RolapCubeLevel level = target.level;
            if (!level.isAll()) {
                RolapStar.Column starColumn =
                    level.getBaseStarKeyColumn(measureGroup);
                if (starColumn != null) {
                    levelBitKey.set(starColumn.getBitPosition());
                }
            }
        }

        measureBitKey.set(bitPosition);

        if (constraint
            instanceof RolapNativeCrossJoin.NonEmptyCrossJoinConstraint)
        {
            // Cannot evaluate NonEmptyCrossJoinConstraint using an agg
            // table if one of its args is a DescendantsConstraint.
            RolapNativeCrossJoin.NonEmptyCrossJoinConstraint necj =
                (RolapNativeCrossJoin.NonEmptyCrossJoinConstraint)
                    constraint;
            for (CrossJoinArg arg : necj.args) {
                if (arg instanceof DescendantsCrossJoinArg
                    || arg instanceof MemberListCrossJoinArg)
                {
                    final RolapLevel level = arg.getLevel();
                    if (level != null && !level.isAll()) {
                        final RolapCubeLevel cubeLevel = (RolapCubeLevel) level;
                        for (RolapSchema.PhysColumn physColumn
                            : cubeLevel.attribute.getKeyList())
                        {
                            RolapStar.Column column =
                                measureGroup1.getRolapStarColumn(
                                    cubeLevel.cubeDimension,
                                    physColumn,
                                    true);
                            levelBitKey.set(column.getBitPosition());
                        }
                    }
                }
            }
        }

        // find the aggstar using the masks
        return AggregationManager.findAgg(
            star, levelBitKey, measureBitKey, new boolean[]{false});
    }

    int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

}

// End SqlTupleReader.java
