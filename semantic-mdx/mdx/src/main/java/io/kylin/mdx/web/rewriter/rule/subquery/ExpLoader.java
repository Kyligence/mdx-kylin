/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.kylin.mdx.web.rewriter.rule.subquery;

import mondrian.mdx.UnresolvedFunCall;
import mondrian.olap.Exp;

import java.util.HashMap;
import java.util.Map;

public class ExpLoader {

    private static final ExpTranslator DEFAULT_TRANSLATOR = new DefaultFunctionTranslator();

    private static final Map<String, ExpTranslator> TRANSLATOR_MAP = new HashMap<>();

    static {
        TRANSLATOR_MAP.put("Generate".toLowerCase(), new GenerateFunctionTranslator());
        TRANSLATOR_MAP.put("Hierarchize".toLowerCase(), new HierarchizeFunctionRewriter());
        TRANSLATOR_MAP.put("NONEMPTY".toLowerCase(), new HierarchizeFunctionRewriter());
        TRANSLATOR_MAP.put("{}".toLowerCase(), new HierarchizeFunctionRewriter());
    }

    public static UnresolvedFunCall translate(UnresolvedFunCall funCall, Object... args) {
        String funName = funCall.getFunName().toLowerCase();
        Exp result = TRANSLATOR_MAP.getOrDefault(funName, DEFAULT_TRANSLATOR)
                .translate(funCall, args);
        return (UnresolvedFunCall) result;
    }

    public static class DefaultFunctionTranslator implements ExpTranslator {

        @Override
        public Exp translate(Exp exp, Object... args) {
            return exp;
        }

    }

}
