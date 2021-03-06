/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2011-2013 Pentaho
// All Rights Reserved.
*/
package mondrian.server;

import mondrian.olap.MondrianServer;
import mondrian.rolap.RolapConnection;
import mondrian.util.ArrayStack;

import java.util.Stack;

/**
 * Point of execution from which a service is invoked.
 */
public class Locus {
    public final Execution execution;
    public final String message;
    public final String component;

    private static final ThreadLocal<ArrayStack<Locus>> THREAD_LOCAL = ThreadLocal.withInitial(ArrayStack::new);

    /**
     * Creates a Locus.
     *
     * @param execution Execution context
     * @param component Description of a the component executing the query,
     *   generally a method name, e.g. "SqlTupleReader.readTuples"
     * @param message Description of the purpose of this statement, to be
     *   printed if there is an error
     */
    public Locus(
        Execution execution,
        String component,
        String message)
    {
        assert execution != null;
        this.execution = execution;
        this.component = component;
        this.message = message;
    }

    public static void pop(Locus locus) {
        final Locus pop = THREAD_LOCAL.get().pop();
        assert locus == pop;
    }

    public static void push(Locus locus) {
        THREAD_LOCAL.get().push(locus);
    }

    public static Locus peek() {
        ArrayStack<Locus> stack = THREAD_LOCAL.get();
        return stack.size() > 0 ? stack.peek() : null;
    }

    public static <T> T execute(
        RolapConnection connection,
        String component,
        Action<T> action)
    {
        final Statement statement = connection.getInternalStatement();
        final Execution execution = new Execution(statement, 0);
        return execute(execution, component, action);
    }

    public static <T> T execute(
        Execution execution,
        String component,
        Action<T> action)
    {
        final Locus locus =
            new Locus(
                execution,
                component,
                null);
        Locus.push(locus);
        try {
            return action.execute();
        } finally {
            Locus.pop(locus);
        }
    }

    public final MondrianServer getServer() {
        return execution.statement.getMondrianConnection().getServer();
    }

    public interface Action<T> {
        T execute();
    }
}

// End Locus.java
