/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2013 Pentaho and others
// All Rights Reserved.
*/
package mondrian.olap.fun;

import mondrian.calc.*;
import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.mdx.MemberExpr;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;
import mondrian.rolap.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Definition of the <code>EXISTS</code> MDX function.
 *
 * @author kvu
 * @since Mar 23, 2008
 */
class ExistsFunDef extends FunDefBase
{
    static final Resolver resolver =
        new ReflectiveMultiResolver(
            "Exists",
            "Exists(<Set1>, <Set2>[, <String>])",
            "Returns the the set of tuples of the first set that exist with one or more tuples of the second set.",
            new String[] {"fxxx", "fxxxS"},
            ExistsFunDef.class);

    public ExistsFunDef(FunDef dummyFunDef)
    {
        super(dummyFunDef);
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final ListCalc listCalc1 = compiler.compileList(call.getArg(0));
        final ListCalc listCalc2 = compiler.compileList(call.getArg(1));

        if (call.getArgs().length == 2) {
            return new AbstractListCalc(call, new Calc[]{listCalc1, listCalc2}) {
                public TupleList evaluateList(Evaluator evaluator) {
                    TupleList leftTuples = listCalc1.evaluateList(evaluator);
                    if (leftTuples.isEmpty()) {
                        return TupleCollections.emptyList(leftTuples.getArity());
                    }
                    TupleList rightTuples = listCalc2.evaluateList(evaluator);
                    if (rightTuples.isEmpty()) {
                        return TupleCollections.emptyList(leftTuples.getArity());
                    }
                    TupleList result =
                            TupleCollections.createList(leftTuples.getArity());

                    List<Hierarchy> leftHierarchies = getHierarchies(leftTuples.get(0));
                    List<Hierarchy> rightHierarchies = getHierarchies(rightTuples.get(0));
                    int leftLength = leftHierarchies.size();
                    int rightLength = rightHierarchies.size();

                    leftHierarchies.retainAll(rightHierarchies);
                    // TODO 1. all hierarchies are not the same
//                    if (leftHierarchies.size() == 0) {
//                        return processAllDiffHierarchies(leftTuples, rightTuples, leftHierarchies, rightHierarchies);
//                    }
                    // 2. all hierarchies are the same
                    if (leftHierarchies.size() == leftLength && leftHierarchies.size() == rightLength) {
                        return retainTuples(leftTuples, rightTuples);
                    }
                    // 3. has partial same hierarchies
                    leftLoop:
                    for (List<Member> leftTuple : leftTuples) {
                        for (List<Member> rightTuple : rightTuples) {
                            if (existsInTuple(leftTuple, rightTuple,
                                    leftHierarchies, rightHierarchies)) {
                                result.add(leftTuple);
                                continue leftLoop;
                            }
                        }
                    }
                    return result;
                }
            };
        }

        if (call.getArgs().length == 3) {
            final StringCalc stringCalc = compiler.compileString(call.getArg(2));
            return new AbstractListCalc(call, new Calc[]{listCalc1, listCalc2, stringCalc}) {
                @Override
                public TupleList evaluateList(Evaluator evaluator) {
                    String measureGroupName = stringCalc.evaluateString(evaluator);
                    // TODO, check there is any visible measure in measure group, see MDX-669
                    RolapMeasureGroup measureGroup = getMeasureGroupByName(measureGroupName, evaluator);
                    if (measureGroup == null) {
                        return new UnaryTupleList();
                    }
                    TupleList leftTuples = listCalc1.evaluateList(evaluator);
                    if (leftTuples.isEmpty()) {
                        return TupleCollections.emptyList(leftTuples.getArity());
                    }
                    TupleList rightTuples = listCalc2.evaluateList(evaluator);
                    if (rightTuples.isEmpty()) {
                        return TupleCollections.emptyList(leftTuples.getArity());
                    }
                    RolapStoredMeasure countMeasure = findBaseCountMeasure(measureGroup);
                    TupleList result = TupleCollections.createList(leftTuples.getArity());
                    final int savepoint = evaluator.savepoint();
                    try {
                        evaluator.setContext(countMeasure);
                        return findExistsTuples(leftTuples, rightTuples, evaluator, result);
                    } finally {
                        evaluator.restore(savepoint);
                    }
                }

                private TupleList findExistsTuples(TupleList leftTuples, TupleList rightTuples, Evaluator evaluator, TupleList result) {
                    for (List<Member> leftTuple : leftTuples) {
                        if (checkTupleExistsInSet(leftTuple, rightTuples, evaluator)) {
                            result.add(leftTuple);
                        }
                    }
                    return result;
                }

                private boolean checkTupleExistsInSet(List<Member> leftTuple, TupleList rightTuples, Evaluator evaluator) {
                    final int savepoint = evaluator.savepoint();
                    try {
                        for (Member member : leftTuple) {
                            if (!checkContextConflictAndSetDetail(evaluator, member)) {
                                return false;
                            }
                        }

                        for (List<Member> rightTuple : rightTuples) {
                            if (checkExistsInTuple(rightTuple, evaluator)) {
                                return true;
                            }
                        }
                    } finally {
                        evaluator.restore(savepoint);
                    }
                    return false;
                }

                private boolean checkExistsInTuple(List<Member> rightTuple, Evaluator evaluator) {
                    final int savepoint = evaluator.savepoint();
                    try {
                        for (Member member : rightTuple) {
                            if (!checkContextConflictAndSetDetail(evaluator, member)) {
                                return false;
                            }
                        }
                        Object object = evaluator.evaluateCurrent();
                        return object != null;
                    } finally {
                        evaluator.restore(savepoint);
                    }
                }

                private boolean checkContextConflictAndSetDetail(Evaluator evaluator, Member member) {
                    Member contextMember = evaluator.getContext(member.getHierarchy());
                    if (contextMember.isAll()) {
                        evaluator.setContext(member);
                        return true;
                    }
                    // member from report filter
                    if (contextMember.isCalculated()) {
                        RolapCalculatedMember calcMember = (RolapCalculatedMember) contextMember;
                        // TODO, only consider excel filter pattern
                        List<RolapMember> filterMembers = flatAggregateMember(calcMember);
                        if (filterMembers.size() == 0) {
                            evaluator.setContext(member);
                            return true;
                        }
                        for (RolapMember filterMember : filterMembers) {
                            if (isOnSameHierarchyChain(member, filterMember)) {
                                Member detailMember = member.getDepth() > filterMember.getDepth() ? member : filterMember;
                                evaluator.setContext(detailMember);
                                return true;
                            }
                        }
                        return false;
                    }

                    // existing member in context, maybe from slicer or parent context
                    if (contextMember instanceof RolapMemberBase
                            && isOnSameHierarchyChain(member, contextMember)) {
                        if (member.getDepth() > contextMember.getDepth()) {
                            evaluator.setContext(member);
                        }
                        return true;
                    }

                    return false;
                }

                private List<RolapMember> flatAggregateMember(RolapCalculatedMember calcMember) {
                    List<RolapMember> members = new ArrayList<>();
                    Exp exp = calcMember.getExpression();
                    if (exp instanceof ResolvedFunCall
                            && ((ResolvedFunCall) exp).getFunName().equalsIgnoreCase("Aggregate")
                            && ((ResolvedFunCall) exp).getArgCount() == 1) {
                        Exp setArg = ((ResolvedFunCall) exp).getArg(0);
                        if (setArg instanceof ResolvedFunCall
                                && ((ResolvedFunCall) setArg).getFunName().equalsIgnoreCase("{}")) {
                            for (Exp memberExp : ((ResolvedFunCall) setArg).getArgs()) {
                                members.add((RolapMember) ((MemberExpr)memberExp).getMember());
                            }
                        }
                    }
                    return members;
                }

                private RolapStoredMeasure findBaseCountMeasure(RolapMeasureGroup measureGroup) {
                    for (RolapStoredMeasure measure : measureGroup.measureList) {
                        if (measure.getExpression() == null && measure.getAggregator().equals(RolapAggregator.Count)) {
                            return measure;
                        }
                    }
                    return measureGroup.measureList.get(0);
                }
            };
        }

        throw Util.newError("ExistsFun Arg Count is not correct!");
    }

    private TupleList processAllDiffHierarchies(TupleList leftTuples, TupleList rightTuples,
                                                List<Hierarchy> leftHierarchies, List<Hierarchy> rightHierarchies) {
        Set<Dimension> leftDims = getDimensions(leftHierarchies);
        Set<Dimension> rightDims = getDimensions(rightHierarchies);
        // all dimensions not same
        leftDims.retainAll(rightDims);
        if (leftDims.size() == 0) {
            return leftTuples;
        }

        // TODO has same dimension
        return null;
    }

    private TupleList retainTuples(TupleList leftTuples, TupleList rightTuples) {
        List<Hierarchy> leftHierarchies = getHierarchies(leftTuples.get(0));
        Set<String> rightTupleUniqNames = new HashSet<>();
        for (List<Member> members : rightTuples) {
            StringBuffer uniqBuff = new StringBuffer();
            memberLoop:
            for (Hierarchy hierarchy : leftHierarchies) {
                for (Member member : members) {
                    if (member.getHierarchy().equals(hierarchy)) {
                        uniqBuff.append(member.getUniqueName());
                        continue memberLoop;
                    }
                }
            }
            rightTupleUniqNames.add(uniqBuff.toString());
        }

        TupleList result = TupleCollections.createList(leftTuples.getArity());
        for (List<Member> members : leftTuples) {
            StringBuffer uniqBuff = new StringBuffer();
            for (Member member : members) {
                uniqBuff.append(member.getUniqueName());
            }
            if (rightTupleUniqNames.contains(uniqBuff.toString())) {
                result.add(members);
            }
        }

        return result;
    }

    private Set<Dimension> getDimensions(List<Hierarchy> hierarchies) {
        Set<Dimension> dimensions = new HashSet<>();
        for (Hierarchy hierarchy : hierarchies) {
            dimensions.add(hierarchy.getDimension());
        }
        return dimensions;
    }

    private RolapMeasureGroup getMeasureGroupByName(String measureGroupName, Evaluator evaluator) {
        RolapCube cube = (RolapCube) evaluator.getCube();
        for (RolapMeasureGroup measureGroup : cube.getMeasureGroups()) {
            if (measureGroup.getName().equals(measureGroupName)) {
                return measureGroup;
            }
        }
        return null;
    }

    private static boolean isOnSameHierarchyChain(Member mA, Member mB)
    {
        return (FunUtil.isAncestorOf(mA, mB, false))||
            (FunUtil.isAncestorOf(mB, mA, false));
    }


    /**
     * Returns true if leftTuple Exists w/in rightTuple
     *
     *
     *
     * @param leftTuple tuple from arg one of EXISTS()
     * @param rightTuple tuple from arg two of EXISTS()
     * @param leftHierarchies list of hierarchies from leftTuple, in the same
     *                        order
     * @param rightHierarchies list of the hiearchies from rightTuple,
     *                         in the same order
     * @return true if each member from leftTuple is somewhere in the
     *         hierarchy chain of the corresponding member from rightTuple,
     *         false otherwise.
     *         If there is no explicit corresponding member from either
     *         right or left, then the default member is used.
     */
    private boolean existsInTuple(
        final List<Member> leftTuple, final List<Member> rightTuple,
        final List<Hierarchy> leftHierarchies,
        final List<Hierarchy> rightHierarchies)
    {
        List<Member> checkedMembers = new ArrayList<Member>();

        for (Member leftMember : leftTuple) {
            Member rightMember = getCorrespondingMember(
                leftMember, rightTuple, rightHierarchies);
            checkedMembers.add(rightMember);
            if (!isOnSameHierarchyChain(leftMember, rightMember)) {
                return false;
            }
        }
        // this loop handles members in the right tuple not present in left
        // Such a member could only impact the resulting tuple list if the
        // default member of the hierarchy is not the all member.
        for (Member rightMember : rightTuple) {
            if (checkedMembers.contains(rightMember)) {
                // already checked in the previous loop
                continue;
            }
            Member leftMember = getCorrespondingMember(
                rightMember, leftTuple, leftHierarchies);
            if (!isOnSameHierarchyChain(leftMember, rightMember)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the corresponding member from tuple, or the default member
     * for the hierarchy if member is not explicitly contained in the tuple.
     *
     *
     * @param member source member
     * @param tuple tuple containing the target member
     * @param tupleHierarchies list of the hierarchies explicitly contained
     *                         in the tuple, in the same order.
     * @return target member
     */
    private Member getCorrespondingMember(
        final Member member, final List<Member> tuple,
        final List<Hierarchy> tupleHierarchies)
    {
        assert tuple.size() == tupleHierarchies.size();
        int dimPos = tupleHierarchies.indexOf(member.getHierarchy());
        if (dimPos >= 0) {
            return tuple.get(dimPos);
        } else {
            return member.getHierarchy().getDefaultMember();
        }
    }

    private static List<Hierarchy> getHierarchies(final List<Member> members)
    {
        List<Hierarchy> hierarchies = new ArrayList<Hierarchy>();
        for (Member member : members) {
            hierarchies.add(member.getHierarchy());
        }
        return hierarchies;
    }

}

// End ExistsFunDef.java
