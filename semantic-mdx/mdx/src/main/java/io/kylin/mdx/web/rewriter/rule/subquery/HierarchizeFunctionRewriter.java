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
import mondrian.olap.Formula;

/**
 * 改写示意：
 * #  Hierarchize( SET_EXPRESSION [, POST])
 * #    ->
 * #  Hierarchize( SET_EXPRESSION , [XL_Row_Dim_i]) [, POST])
 */
public class HierarchizeFunctionRewriter implements ExpTranslator {

    @Override
    public Exp translate(Exp exp, Object... args) {
        UnresolvedFunCall hierarchize = (UnresolvedFunCall) exp;
        UnresolvedFunCall intersect = new UnresolvedFunCall(
                "Intersect",
                new Exp[]{hierarchize.getArg(0), ((Formula) args[0]).getIdentifier()});
        hierarchize.setArg(0, intersect);
        return exp;
    }

}
