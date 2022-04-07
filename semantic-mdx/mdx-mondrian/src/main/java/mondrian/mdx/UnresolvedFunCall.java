/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2006-2007 Pentaho
// All Rights Reserved.
*/
package mondrian.mdx;

import lombok.Getter;
import lombok.Setter;
import mondrian.calc.Calc;
import mondrian.calc.ExpCompiler;
import mondrian.olap.*;
import mondrian.olap.fun.FunUtil;
import mondrian.olap.type.Type;
import mondrian.util.ArrayStack;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;

/**
 * An expression consisting of a named function or operator
 * applied to a set of arguments. The syntax determines whether this is
 * called infix, with function call syntax, and so forth.
 *
 * @author jhyde
 * @since Sep 28, 2005
 */
public class UnresolvedFunCall extends ExpBase implements FunCall {

    // Allow modify name, syntax and args

    @Getter
    @Setter
    private String funName;

    @Getter
    @Setter
    private Syntax syntax;

    @Getter
    @Setter
    private Exp[] args;

    /**
     * Creates a function call with {@link Syntax#Function} syntax.
     */
    public UnresolvedFunCall(String funName, Exp[] args) {
        this(funName, Syntax.Function, args);
    }

    /**
     * Creates a function call.
     */
    public UnresolvedFunCall(String funName, Syntax syntax, Exp[] args) {
        assert funName != null;
        assert syntax != null;
        assert args != null;
        this.funName = funName;
        this.syntax = syntax;
        this.args = args;
        switch (syntax) {
        case Braces:
            Util.assertTrue(funName.equals("{}"));
            break;
        case Parentheses:
            Util.assertTrue(funName.equals("()"));
            break;
        case Internal:
            Util.assertTrue(funName.startsWith("$"));
            break;
        case Empty:
            Util.assertTrue(funName.equals(""));
            break;
        default:
            Util.assertTrue(
                !funName.startsWith("$")
                && !funName.equals("{}")
                && !funName.equals("()"));
            break;
        }
    }

    public UnresolvedFunCall clone() {
        return new UnresolvedFunCall(funName, syntax, ExpBase.cloneArray(args));
    }

    public int getCategory() {
        throw new UnsupportedOperationException();
    }

    public Type getType() {
        throw new UnsupportedOperationException();
    }

    public void unparse(PrintWriter pw) {
        syntax.unparse(funName, args, pw);
    }

    public Object accept(MdxVisitor visitor) {
        final Object o = visitor.visit(this);
        if (visitor.shouldVisitChildren()) {
            // visit the call's arguments
            for (Exp arg : args) {
                arg.accept(visitor);
            }
        }
        return o;
    }

    public Exp accept(Validator validator) {
        Exp[] newArgs = new Exp[args.length];
        FunDef funDef =
            FunUtil.resolveFunArgs(
                validator, null, args, newArgs, funName, syntax);
        return funDef.createCall(validator, newArgs);
    }

    public Calc accept(ExpCompiler compiler) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the Exp argument at the specified index.
     *
     * @param      index   the index of the Exp.
     * @return     the Exp at the specified index of this array of Exp.
     *             The first Exp is at index <code>0</code>.
     * @see #getArgs()
     */
    public Exp getArg(int index) {
        return args[index];
    }

    public void setArg(int index, Exp arg) {
        this.args[index] = arg;
    }

    /**
     * Returns the number of arguments.
     *
     * @return number of arguments.
     * @see #getArgs()
     */
    public final int getArgCount() {
        return args.length;
    }

    public Object[] getChildren() {
        return args;
    }

    public String toStringInStack(ArrayStack<QueryPart> stack, int pos) {
        StringBuilder buf = new StringBuilder();
        buf.append("< ").append(StringUtils.substringAfterLast(super.toString(), "."));
        buf.append(" ").append("name=").append(funName);
        buf.append(" ").append("syntax=").append(syntax.name());

        if (args != null && args.length > 0) {
            buf.append(" ").append("args=").append("[");
            for ( int i = 0; i < args.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                Exp exp = args[i];
                addExp(exp, buf, stack, pos);
            }
            buf.append("]");
        }
        buf.append(" >");
        return buf.toString();
    }
}

// End UnresolvedFunCall.java
