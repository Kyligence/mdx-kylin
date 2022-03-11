/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 TONBELLER AG
// Copyright (C) 2006-2009 Pentaho and others
// All Rights Reserved.
*/
package mondrian.rolap;

import mondrian.olap.*;
import mondrian.olap.fun.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Composite of {@link RolapNative}s. Uses chain of responsibility
 * to select the appropriate {@link RolapNative} evaluator.
 */
public class RolapNativeRegistry extends RolapNative {

    private Map<Class, RolapNative> nativeEvaluatorMap =
        new HashMap<Class, RolapNative>();

    public RolapNativeRegistry() {
        super.setEnabled(true);

        /*
         * Mondrian functions which might be evaluated natively.
         * TODO An alternative solution uses function names to registry and match,
         *  and can process more functions which are defined by anonymous classes.
         */
        register(NonEmptyCrossJoinFunDef.class, new RolapNativeCrossJoin());
        register(CrossJoinFunDef.class, new RolapNativeCrossJoin());
        register(TopBottomCountFunDef.class, new RolapNativeTopCount());
        register(CountFunDef.class, new RolapNativeCount());
        register(FilterFunDef.class, new RolapNativeFilter());
        register(DescendantsFunDef.class, new RolapNativeDescendants());
    }

    /**
     * Returns the matching NativeEvaluator or null if <code>fun</code> can not
     * be executed in SQL for the given context and arguments.
     */
    public NativeEvaluator createEvaluator(
        RolapEvaluator evaluator, FunDef fun, Exp[] args)
    {
        if (!isEnabled()) {
            return null;
        }

        RolapNative rn = getRolapNative(fun);

        if (rn == null) {
            return null;
        }

        NativeEvaluator ne = rn.createEvaluator(evaluator, fun, args);

        if (ne != null) {
            if (listener != null) {
                NativeEvent e = new NativeEvent(this, ne);
                listener.foundEvaluator(e);
            }
        }
        return ne;
    }

    public void register(Class funDefClass, RolapNative rn) {
        nativeEvaluatorMap.put(funDefClass, rn);
    }

    /** for testing */
    void setListener(Listener listener) {
        super.setListener(listener);
        for (RolapNative rn : nativeEvaluatorMap.values()) {
            rn.setListener(listener);
        }
    }

    RolapNative getRolapNative(FunDef fun) {
        return nativeEvaluatorMap.get(fun.getClass());
    }

    /** for testing */
    void useHardCache(boolean hard) {
        for (RolapNative rn : nativeEvaluatorMap.values()) {
            rn.useHardCache(hard);
        }
    }
}

// End RolapNativeRegistry.java
