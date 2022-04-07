/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2009 Pentaho and others
// All Rights Reserved.
*/
package mondrian.olap.fun;

import mondrian.calc.Calc;
import mondrian.calc.ExpCompiler;
import mondrian.mdx.ResolvedFunCall;
import mondrian.olap.*;
import mondrian.olap.type.Type;

import java.io.PrintWriter;
import java.util.List;

public class GroupFunDef extends FunDefBase {
    static final String NAME = "Group";
    private static final String SIGNATURE = "Group(<<Exp>>)";
    private static final String DESCRIPTION =
            "Capture expressions in pattern matching";
    private static final Syntax SYNTAX = Syntax.Function;
    static final GroupFunResolver Resolver = new GroupFunResolver();

    private GroupFunDef(
            String name,
            String signature,
            String description,
            Syntax syntax,
            int category,
            Type type) {
        super(
                name, signature, description, syntax,
                category, new int[]{category});
        Util.discard(type);
    }

    public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler) {
        throw new UnsupportedOperationException();
    }

    private static class GroupFunResolver extends ResolverBase {
        private GroupFunResolver() {
            super(NAME, SIGNATURE, DESCRIPTION, SYNTAX);
        }

        public FunDef resolve(
                Exp[] args,
                Validator validator,
                List<Conversion> conversions) {
            if (args.length != 1) {
                return null;
            }
            final Exp exp = args[0];
            final int category = exp.getCategory();
            final Type type = exp.getType();
            return new GroupFunDef(
                    NAME, SIGNATURE, DESCRIPTION, SYNTAX,
                    category, type);
        }

        public boolean requiresExpression(int k) {
            return false;
        }
    }
}

// End CacheFunDef.java
