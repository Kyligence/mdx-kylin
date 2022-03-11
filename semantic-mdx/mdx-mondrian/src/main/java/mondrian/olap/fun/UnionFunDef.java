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
import mondrian.olap.*;
import org.apache.commons.lang.ArrayUtils;

import java.util.*;

/**
 * Definition of the <code>Union</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
public class UnionFunDef extends FunDefBase {

    /**
     * Resolver for: [Set Expression] + [Set Expression]
     * 这里直接定义 UnionFunDef 而不是 Resolver 的原因是 Resolver 会造成 + 的函数冲突
     */
    static final UnionFunDef AddUnionFunDef = new UnionFunDef(
            "+",
            Syntax.Infix,
            "Returns the union of two sets, removing duplicate elements",
            new int[]{Category.Set, Category.Set},
            -1,
            false);

    /**
     * Resolver for: Union( [Set Expression] ... [,ALL] )
     */
    static final ResolverImpl UnionResolver = new ResolverImpl(
            "Union",
            "Returns the union of two or more sets, optionally retaining duplicate elements.",
            Syntax.Function);

    private static final String[] ReservedWords = new String[]{"ALL", "DISTINCT"};

    private final int[] argTypes;

    private final int setArgLen;

    private final boolean allMember;

    private UnionFunDef(String funName, Syntax syntax, String description,
                        int[] argTypes, int setArgLen, boolean allMember) {
        super(funName, null, description, syntax, Category.Set, argTypes);
        this.argTypes = argTypes;
        this.setArgLen = setArgLen;
        this.allMember = allMember;
    }

    @Override
    public int[] getParameterCategories() {
        return argTypes;
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        int argLen = setArgLen;
        if (argLen < 0) {
            // if args.length < 0,  check arguments
            argLen = call.getArgCount();
            if (argLen != 2) {
                throw newEvalException(this, "Expressions must have two sets.");
            }
            checkCompatible(call.getArg(0), call.getArg(1), null);
        }
        final Calc[] calcs = new Calc[argLen];
        for (int i = 0; i < argLen; i++) {
            calcs[i] = compiler.compileList(call.getArg(i));
        }
        return new AbstractListCalc(call, calcs) {
            public TupleList evaluateList(Evaluator evaluator) {
                Calc[] calcs = getCalcs();
                List<TupleList> tupleLists = new ArrayList<>(calcs.length);
                for (Calc calc : calcs) {
                    tupleLists.add(((ListCalc) calc).evaluateList(evaluator));
                }
                return union(tupleLists, allMember);
            }
        };
    }

    private TupleList union(List<TupleList> tupleLists, final boolean all) {
        TupleList result = TupleCollections.createList(tupleLists.get(0).getArity());
        Set<List<Member>> added = new HashSet<>();
        for (TupleList tupleList : tupleLists) {
            if (all) {
                result.addAll(tupleList);
            } else {
                FunUtil.addUnique(result, tupleList, added);
            }
        }
        return result;
    }

    private static class ResolverImpl extends ResolverBase {

        public ResolverImpl(String funName, String description, Syntax syntax) {
            super(funName, null, description, syntax);
        }

        @Override
        public String[] getReservedWords() {
            return ReservedWords;
        }

        @Override
        public FunDef resolve(Exp[] args, Validator validator, List<Conversion> conversions) {
            int argLength = args.length;
            int[] argTypes = new int[argLength];
            boolean allMember = false;
            if (args[argLength - 1] instanceof Literal) {
                Literal literal = (Literal) args[argLength - 1];
                String lastArg = literal.getValue().toString().toUpperCase();
                if (!ArrayUtils.contains(ReservedWords, lastArg)) {
                    throw newEvalException(null, "Allowed values are: " + Arrays.toString(ReservedWords));
                }
                argTypes[argLength - 1] = Category.Symbol;
                allMember = lastArg.equalsIgnoreCase("ALL");
                argLength--;
            }
            if (argLength <= 1) {
                throw newEvalException(null, "Expressions must have at least two sets.");
            }
            for (int i = 0; i < argLength - 1; i++) {
                checkCompatible(args[i], args[i + 1], null);
            }
            for (int i = 0; i < argLength; i++) {
                if (!validator.canConvert(i, args[i], Category.Set, conversions)) {
                    return null;
                }
                argTypes[i] = Category.Set;
            }
            return new UnionFunDef(getName(), getSyntax(), getDescription(),
                    argTypes, argLength, allMember);
        }

    }

}

// End UnionFunDef.java
