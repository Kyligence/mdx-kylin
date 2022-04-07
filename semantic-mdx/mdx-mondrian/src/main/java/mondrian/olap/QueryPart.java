/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 1998-2005 Julian Hyde
// Copyright (C) 2005-2011 Pentaho and others
// All Rights Reserved.
*/
package mondrian.olap;

import mondrian.util.ArrayStack;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;

/**
 * Component of an MDX query (derived classes include Query, Axis, Exp, Level).
 *
 * @author jhyde, 23 January, 1999
 */
public abstract class QueryPart implements Walkable {
    /**
     * Creates a QueryPart.
     */
    public QueryPart() {
    }

    /**
     * implement Walkable
     */
    @Override
    public Object[] getChildren() {
        // By default, a QueryPart is atomic (has no children).
        return null;
    }

    /**
     * Writes a string representation of this parse tree
     * node to the given writer.
     *
     * @param pw writer
     */
    public void unparse(PrintWriter pw) {
        pw.print(toString());
    }

    /**
     * Returns the plan that Mondrian intends to use to execute this query.
     *
     * @param pw Print writer
     */
    public void explain(PrintWriter pw) {
        throw new UnsupportedOperationException(
            "explain not implemented for " + this + " (" + getClass() + ")");
    }

    public String toStringInStack(ArrayStack<QueryPart> stack, int pos) {
        return StringUtils.substringAfterLast(super.toString(), ".");
    }

    public void addExp(Exp exp, StringBuilder buf, ArrayStack<QueryPart> stack, int pos) {
        int p = stack.size() - 1;
        for (; p > pos; p--) {
            if (exp == stack.get(p)) {
                break;
            }
        }
        if (p > pos || !(exp instanceof ExpBase)) {
            buf.append("< ").append(StringUtils.substringAfterLast(exp.toString(), ".")).append(" >");
        } else {
            buf.append(((ExpBase)exp).toStringInStack(stack, pos));
        }
    }
}

// End QueryPart.java
