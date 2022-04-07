/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2013 Pentaho and others
// Copyright (C) 2004-2005 SAS Institute, Inc.
// All Rights Reserved.
*/
package mondrian.olap.fun;

import mondrian.calc.*;
import mondrian.calc.impl.AbstractListCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;
import mondrian.rolap.RolapEvaluator;
import mondrian.rolap.sql.CrossJoinArgFactory;
import mondrian.xmla.XmlaRequestContext;

import static mondrian.olap.fun.DescendantsFunDef.checkDescendantsLeaves;


/**
 * Definition of the <code>NonEmptyCrossJoin</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 *
 * author 16 December, 2004
 */
public class NonEmptyCrossJoinFunDef extends CrossJoinFunDef {
    static final ReflectiveMultiResolver nonEmptyResolver = new ReflectiveMultiResolver(
            "NonEmpty",
            "NonEmpty(<Set1>, <Set2>)",
            "Returns the cross product of two sets, excluding empty tuples and tuples without associated fact table data.",
            new String[]{"fxxx"},
            NonEmptyCrossJoinFunDef.class);

    static final ReflectiveMultiResolver nonEmptyCrossJoinResolver = new ReflectiveMultiResolver(
        "NonEmptyCrossJoin",
            "NonEmptyCrossJoin(<Set1>, <Set2>)",
            "Returns the cross product of two sets, excluding empty tuples and tuples without associated fact table data.",
            new String[]{"fxxx"},
            NonEmptyCrossJoinFunDef.class);

    public NonEmptyCrossJoinFunDef(FunDef dummyFunDef) {
        super(dummyFunDef);
    }

    public Calc compileCall(final ResolvedFunCall call, ExpCompiler compiler) {
        final ListCalc listCalc1 = compiler.compileList(call.getArg(0));
        final ListCalc listCalc2 = compiler.compileList(call.getArg(1));
        return new AbstractListCalc(
            call, new Calc[] {listCalc1, listCalc2}, false)
        {
            public TupleList evaluateList(Evaluator evaluator) {
                SchemaReader schemaReader = evaluator.getSchemaReader();

                // Evaluate the arguments in non empty mode, but remove from
                // the slicer any members that will be overridden by args to
                // the NonEmptyCrossjoin function. For example, in
                //
                //   SELECT NonEmptyCrossJoin(
                //       [Store].[USA].Children,
                //       [Product].[Beer].Children)
                //    FROM [Sales]
                //    WHERE [Store].[Mexico]
                //
                // we want all beers, not just those sold in Mexico.
                final int savepoint = evaluator.savepoint();
                try {
                    evaluator.setNonEmpty(true);

                    CrossJoinArgFactory crossJoinArgFactory = new CrossJoinArgFactory(true);
                    if (evaluator instanceof RolapEvaluator
                            && !(checkDescendantsLeaves(evaluator, crossJoinArgFactory, call.getArg(0))
                            && checkDescendantsLeaves(evaluator, crossJoinArgFactory, call.getArg(1)))) {
                        for (Member member
                                : ((RolapEvaluator) evaluator).getSlicerMembers()) {
                            if (getType().getElementType().usesHierarchy(
                                    member.getHierarchy(), true)) {
                                evaluator.setContext(
                                        member.getHierarchy().getAllMember());
                            }
                        }
                    }

                    NativeEvaluator nativeEvaluator = MondrianProperties.instance().EnableCrossJoinNative.get()
                            ? schemaReader.getNativeSetEvaluator(
                            call.getFunDef(), call.getArgs(), evaluator, this) : null;
                    if (nativeEvaluator != null) {
                        return
                            (TupleList) nativeEvaluator.execute(
                                ResultStyle.LIST);
                    }

                    final TupleList list1 = listCalc1.evaluateList(evaluator);
                    if (list1.isEmpty()) {
                        return list1;
                    }
                    final TupleList list2 = listCalc2.evaluateList(evaluator);
                    TupleList result = mutableCrossJoin(list1, list2);

                    // remove any remaining empty crossings from the result
                    // skip filter non empty data if less than one tuple
                    if (!skipFilterTuple(result)) {
                        result = nonEmptyList(evaluator, result, call);
                    }
                    return result;
                } finally {
                    evaluator.restore(savepoint);
                }
            }

            private boolean skipFilterTuple(TupleList result) {
                boolean useOtherQueryEngine = !XmlaRequestContext.getContext().useMondrian;
                boolean skipNonEmptyCheck = MondrianProperties.instance().SkipAxisNonEmptyCheck.get()
                        && (useOtherQueryEngine
                            || Boolean.parseBoolean(XmlaRequestContext.getContext().getParameter("isCalculatingAxis")));
                return result == null || result.size() <= 1 || skipNonEmptyCheck;
            }

            @Override
            public boolean dependsOn(Hierarchy hierarchy) {
                if (super.dependsOn(hierarchy)) {
                    return true;
                }
                // Member calculations generate members, which mask the actual
                // expression from the inherited context.
                if (listCalc1.getType().usesHierarchy(hierarchy, true)) {
                    return false;
                }
                if (listCalc2.getType().usesHierarchy(hierarchy, true)) {
                    return false;
                }
                // The implicit value expression, executed to figure out
                // whether a given tuple is empty, depends upon all dimensions.
                return true;
            }
        };
    }
}

// End NonEmptyCrossJoinFunDef.java
