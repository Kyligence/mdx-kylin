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


package io.kylin.mdx.web.rewriter.utils;

import mondrian.olap.Exp;

/**
 * 表达式访问器
 */
public class ExpVisitor {

    private Exp exp;

    public ExpVisitor(Exp exp) {
        this.exp = exp;
    }

    public ExpVisitor arg(int index) {
        this.exp = ExpUtils.getArg(exp, index);
        return this;
    }

    public ExpVisitor argId(int index) {
        this.exp = ExpUtils.getArg2Id(exp, index);
        return this;
    }

    public ExpVisitor argFun(int index) {
        this.exp = ExpUtils.getArg2Fun(exp, index);
        return this;
    }

    public ExpVisitor argFunWithName(int index, String funName) {
        this.exp = ExpUtils.getArg2FunWithName(exp, index, funName);
        return this;
    }

    public Exp execute() {
        return exp;
    }

//    /**
//     * 表达式访问构建器
//     */
//    public static class ExpVisitorBuilder {
//
//    }

}
