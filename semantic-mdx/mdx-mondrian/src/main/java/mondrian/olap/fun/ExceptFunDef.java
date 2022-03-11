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

import mondrian.calc.Calc;
import mondrian.calc.ExpCompiler;
import mondrian.calc.ListCalc;
import mondrian.calc.MemberCalc;
import mondrian.calc.TupleList;
import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.ArrayTupleList;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.Evaluator;
import mondrian.olap.FunDef;
import mondrian.olap.Member;
import mondrian.olap.type.MemberType;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Definition of the <code>Except</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
public class ExceptFunDef extends FunDefBase {
    static final ReflectiveMultiResolver exceptResolver =
        new ReflectiveMultiResolver(
            "Except",
            "Except(<Set1>, <Set2>[, ALL])",
            "Finds the difference between two sets, optionally retaining duplicates.",
            new String[]{"fxxx", "fxxxy"},
            ExceptFunDef.class);

    static final ReflectiveMultiResolver minusResolver =
            new ReflectiveMultiResolver(
                    "-",
                    "<Set1> - <Set2>/<Member>",
                    "Finds the difference between two sets, optionally retaining duplicates.",
                    new String[]{"ixxm", "ixxx"},
                    ExceptFunDef.class);

    public ExceptFunDef(FunDef dummyFunDef) {
        super(dummyFunDef);
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        // todo: implement ALL
        final ListCalc listCalc0 = compiler.compileList(call.getArg(0));
        if (call.getArg(1).getType() instanceof MemberType) {
            final MemberCalc memberCalc = compiler.compileMember(call.getArg(1));
            return new AbstractListCalc(call, new Calc[]{listCalc0, memberCalc}) {
                public TupleList evaluateList(Evaluator evaluator) {
                    TupleList list0 = listCalc0.evaluateList(evaluator);
                    if (list0.isEmpty()) {
                        return list0;
                    }
                    Member member = memberCalc.evaluateMember(evaluator);
                    if (member == null ||member.isNull()) {
                        return list0;
                    }
                    final Set<List<Member>> set1 = new HashSet<List<Member>>(Collections.singletonList(Collections.singletonList(member)));
                    final TupleList result =
                            new ArrayTupleList(list0.getArity(), list0.size());
                    for (List<Member> tuple1 : list0) {
                        if (!set1.contains(tuple1)) {
                            result.add(tuple1);
                        }
                    }
                    return result;
                }
            };
        } else {
            final ListCalc listCalc1 = compiler.compileList(call.getArg(1));
            return new AbstractListCalc(call, new Calc[]{listCalc0, listCalc1}) {
                public TupleList evaluateList(Evaluator evaluator) {
                    TupleList list0 = listCalc0.evaluateList(evaluator);
                    if (list0.isEmpty()) {
                        return list0;
                    }
                    TupleList list1 = listCalc1.evaluateList(evaluator);
                    if (list1.isEmpty()) {
                        return list0;
                    }
                    final Set<List<Member>> set1 = new HashSet<List<Member>>(list1);
                    final TupleList result =
                            new ArrayTupleList(list0.getArity(), list0.size());
                    for (List<Member> tuple1 : list0) {
                        if (!set1.contains(tuple1)) {
                            result.add(tuple1);
                        }
                    }
                    return result;
                }
            };
        }
    }
}

// End ExceptFunDef.java
