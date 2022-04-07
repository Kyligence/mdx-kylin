/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2006-2013 Pentaho
// All Rights Reserved.
*/
package mondrian.olap.fun;

import mondrian.calc.*;
import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.AbstractStringCalc;
import mondrian.calc.impl.ConstantCalc;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;
import mondrian.olap.fun.vba.Vba;
import mondrian.olap.type.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Definition of the <code>Generate</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
public class GenerateFunDef extends FunDefBase {
    static final ReflectiveMultiResolver ListResolver =
            new ReflectiveMultiResolver(
                    "Generate",
                    "Generate(<Set1>, <Set2>[, ALL])",
                    "Applies a set to each member of another set and joins the resulting sets by union.",
                    new String[] {"fxxx", "fxxxy"},
                    GenerateFunDef.class);

    static final ReflectiveMultiResolver StringResolver =
            new ReflectiveMultiResolver(
                    "Generate",
                    "Generate(<Set>, <String>[, <String>])",
                    "Applies a set to a string expression and joins resulting sets by string concatenation.",
                    new String[] {"fSxS", "fSxSS"},
                    GenerateFunDef.class);

    static final ReflectiveMultiResolver IntegerResolver =
            new ReflectiveMultiResolver(
                    "Generate",
                    "Generate(<Set>, <Integer>[, <String>])",
                    "Applies a set to a integer expression and joins resulting sets by string concatenation.",
                    new String[] {"fSxi", "fSxiS"},
                    GenerateFunDef.class);

    private static final String[] ReservedWords = new String[] {"ALL"};

    public GenerateFunDef(FunDef dummyFunDef) {
        super(dummyFunDef);
    }

    public Type getResultType(Validator validator, Exp[] args) {
        final Type type = args[1].getType();
        if (type instanceof StringType) {
            // Generate(<Set>, <String>[, <String>])
            return type;
        } else if (type instanceof NumericType) {
            // Generate(<Set>, <Number>[, <String>])
            return new StringType();
        } else {
            final Type memberType = TypeUtil.toMemberOrTupleType(type);
            return new SetType(memberType);
        }
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        final IterCalc iterCalc = compiler.compileIter(call.getArg(0));
        if (call.getArg(1).getType() instanceof StringType || call.getArg(1).getType() instanceof NumericType) {
            StringCalc stringCalc = null;
            IntegerCalc integerCalc = null;
            if (call.getArg(1).getType() instanceof StringType) {
                stringCalc = compiler.compileString(call.getArg(1));
            } else {
                integerCalc = compiler.compileInteger(call.getArg(1));
            }
            final StringCalc delimCalc;
            if (call.getArgCount() == 3) {
                delimCalc = compiler.compileString(call.getArg(2));
            } else {
                delimCalc = ConstantCalc.constantString("");
            }
            return new GenerateStringCalcImpl(call, iterCalc, stringCalc, integerCalc, delimCalc);
        } else {
            final ListCalc listCalc2 = compiler.compileList(call.getArg(1));
            final String literalArg = getLiteralArg(call, 2, "", ReservedWords);
            final boolean all = literalArg.equalsIgnoreCase("ALL");
            final int arityOut = call.getType().getArity();
            return new GenerateListCalcImpl(call, iterCalc, listCalc2, arityOut, all);
        }
    }

    private static class GenerateListCalcImpl extends AbstractListCalc {
        private final IterCalc iterCalc1;
        private final ListCalc listCalc2;
        private final int arityOut;
        private final boolean all;

        public GenerateListCalcImpl(
                ResolvedFunCall call,
                IterCalc iterCalc,
                ListCalc listCalc2,
                int arityOut,
                boolean all)
        {
            super(call, new Calc[]{iterCalc, listCalc2});
            this.iterCalc1 = iterCalc;
            this.listCalc2 = listCalc2;
            this.arityOut = arityOut;
            this.all = all;
        }

        public TupleList evaluateList(Evaluator evaluator) {
            final int savepoint = evaluator.savepoint();
            try {
                evaluator.setNonEmpty(false);
                final TupleIterable iterable = iterCalc1.evaluateIterable(evaluator);
                evaluator.restore(savepoint);
                TupleList result = TupleCollections.createList(arityOut);
                if (all) {
                    final TupleCursor cursor = iterable.tupleCursor();
                    while (cursor.forward()) {
                        cursor.setContext(evaluator);
                        final TupleList result2 =
                                listCalc2.evaluateList(evaluator);
                        result.addAll(result2);
                    }
                } else {
                    final Set<List<Member>> emitted = new HashSet<>();
                    final TupleCursor cursor = iterable.tupleCursor();
                    while (cursor.forward()) {
                        cursor.setContext(evaluator);
                        final TupleList result2 =
                                listCalc2.evaluateList(evaluator);
                        addDistinctTuples(result, result2, emitted);
                    }
                }
                return result;
            } finally {
                evaluator.restore(savepoint);
            }
        }

        private static void addDistinctTuples(
                TupleList result,
                TupleList result2,
                Set<List<Member>> emitted)
        {
            for (List<Member> row : result2) {
                // wrap array for correct distinctness test
                if (emitted.add(row)) {
                    result.add(row);
                }
            }
        }

        public boolean dependsOn(Hierarchy hierarchy) {
            return anyDependsButFirst(getCalcs(), hierarchy);
        }
    }

    private static class GenerateStringCalcImpl extends AbstractStringCalc {
        private final IterCalc iterCalc;
        private final StringCalc stringCalc;
        private final IntegerCalc integerCalc;
        private final StringCalc sepCalc;

        public GenerateStringCalcImpl(
                ResolvedFunCall call,
                IterCalc iterCalc,
                StringCalc stringCalc,
                IntegerCalc integerCalc,
                StringCalc sepCalc)
        {
            super(call, new Calc[]{iterCalc, stringCalc});
            this.iterCalc = iterCalc;
            this.stringCalc = stringCalc;
            this.integerCalc = integerCalc;
            this.sepCalc = sepCalc;
        }

        public String evaluateString(Evaluator evaluator) {
            final int savepoint = evaluator.savepoint();
            try {
                StringBuilder buf = new StringBuilder();
                int k = 0;
                final TupleIterable iterable = iterCalc.evaluateIterable(evaluator);
                final TupleCursor cursor = iterable.tupleCursor();
                while (cursor.forward()) {
                    cursor.setContext(evaluator);
                    if (k++ > 0) {
                        String sep = sepCalc.evaluateString(evaluator);
                        buf.append(sep);
                    }
                    String result;
                    if(stringCalc != null) {
                        result = stringCalc.evaluateString(evaluator);
                    }else{
                        result = Vba.str(integerCalc.evaluateInteger(evaluator));
                    }
                    buf.append(result);
                }
                return buf.toString();
            } finally {
                evaluator.restore(savepoint);
            }
        }

        public boolean dependsOn(Hierarchy hierarchy) {
            return anyDependsButFirst(getCalcs(), hierarchy);
        }
    }
}

// End GenerateFunDef.java
