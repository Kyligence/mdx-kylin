/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2006-2011 Pentaho
// All Rights Reserved.
*/
package mondrian.olap.fun;

import mondrian.calc.*;
import mondrian.calc.impl.AbstractListCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.Evaluator;
import mondrian.olap.FunDef;
import mondrian.olap.Member;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Definition of the <code>Hierarchize</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
public class HierarchizeFunDef extends FunDefBase {
    static final String[] prePost = {"PRE", "POST"};
    static final ReflectiveMultiResolver Resolver =
        new ReflectiveMultiResolver(
            "Hierarchize",
            "Hierarchize(<Set>[, POST[, Exclude Index]])",
            "Orders the members of a set in a hierarchy.",
            new String[] {"fxx", "fxxy", "fxxyi"},
            HierarchizeFunDef.class,
            prePost);

    public HierarchizeFunDef(FunDef dummyFunDef) {
        super(dummyFunDef);
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final ListCalc listCalc =
            compiler.compileList(call.getArg(0), true);
        String order = getLiteralArg(call, 1, "PRE", prePost);
        final boolean post = order.equals("POST");

        if (call.getArgCount() == 3) {
            final IntegerCalc integerCalc =
                    compiler.compileInteger(call.getArg(2));
            return new AbstractListCalc(call, new Calc[] {listCalc}) {
                public TupleList evaluateList(Evaluator evaluator) {
                    TupleList list = listCalc.evaluateList(evaluator);

                    int fixedIndex = integerCalc.evaluateInteger(evaluator);
                    Map<Member, Integer> fixedMemberIndices = new HashMap<>();
                    for (List<Member> tuple : list) {
                        if (!fixedMemberIndices.containsKey(tuple.get(fixedIndex))) {
                            fixedMemberIndices.put(tuple.get(fixedIndex), fixedMemberIndices.size());
                        }
                    }
                    return hierarchizeTupleListWithFixedIndex(list, post, fixedIndex, fixedMemberIndices);
                }
            };
        }
        return new AbstractListCalc(call, new Calc[] {listCalc}) {
            public TupleList evaluateList(Evaluator evaluator) {
                TupleList list = listCalc.evaluateList(evaluator);
                return hierarchizeTupleList(list, post);
            }
        };
    }
}

// End HierarchizeFunDef.java
