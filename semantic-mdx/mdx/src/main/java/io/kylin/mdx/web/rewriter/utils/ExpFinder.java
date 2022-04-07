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

import mondrian.mdx.UnresolvedFunCall;
import mondrian.olap.*;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 表达式匹配，并且对匹配的表达式执行操作
 *
 * @author hui.wang
 */
public class ExpFinder {

    /**
     * 遍历表达式语法树节点，如果当前节点 test 通过，执行 apply
     * #  如果访问后表示结束，结束当前节点的执行，不再访问子节点
     * #  否则继续执行当前节点的子节点
     * 但是均不会跳过当前节点的后继兄弟节点的执行
     * NOTICE: 调用方不应该关心返回值
     */
    public static boolean predicateAndApply(Exp exp, Predicate<Exp> predicate, BaseExpConsumer<Exp> consumer) {
        if (predicate.test(exp)) {
            consumer.accept(exp);
            try {
                if (consumer.isSkipped()) {
                    return true;
                }
            } finally {
                consumer.clear();
            }
        }
        boolean finish = false;
        if (exp instanceof UnresolvedFunCall) {
            UnresolvedFunCall fun = (UnresolvedFunCall) exp;
            for (Exp arg : fun.getArgs()) {
                if (predicateAndApply(arg, predicate, consumer)) {
                    finish = true;
                }
            }
        }
        return finish;
    }

    /**
     * 遍历表达式语法树节点，可选前序访问当前节点或者是后序访问（非二叉树无中序访问）
     * #  如果 apply 返回 true，将中断整颗树的全部访问（后序访问依旧执行）
     * #  否则继续执行当前节点的子节点
     * NOTICE: 调用方不应该关心返回值
     */
    public static boolean traversalAndApply(Exp exp, Function<Exp, Boolean> function, boolean preorder) {
        Objects.requireNonNull(exp);
        if (preorder) {
            if (function.apply(exp)) {
                return true;
            }
        }
        if (exp instanceof UnresolvedFunCall) {
            UnresolvedFunCall funCall = (UnresolvedFunCall) exp;
            for (Exp arg : funCall.getArgs()) {
                if (traversalAndApply(arg, function, preorder)) {
                    return true;
                }
            }
        }
        if (!preorder) {
            return function.apply(exp);
        }
        return false;
    }

    /**
     * 遍历查询语句上面的全部表达式
     * 包括： formula、query-axis、slicer-axis
     *
     * @param query 表达式
     */
    public static void traversalAndApply(Query query, Consumer<Exp> consumer, boolean preorder) {
        if (query instanceof SubQuery) {
            traversalAndApply(((SubQuery) query).getQuery(), consumer, preorder);
        }
        Function<Exp, Boolean> function = new Consumer2FunctionAdapter<>(consumer, false);
        if (query.getFormulas() != null) {
            for (Formula formula : query.getFormulas()) {
                traversalAndApply(formula.getExpression(), function, preorder);
            }
        }
        for (QueryAxis queryAxis : query.getAxes()) {
            traversalAndApply(queryAxis.getExp(), function, preorder);
        }
        if (query.getSlicerAxis() != null) {
            traversalAndApply(query.getSlicerAxis().getExp(), function, preorder);
        }
    }

    /**
     * Consumer 转 Function 适配器
     */
    public static class Consumer2FunctionAdapter<T, R> implements Function<T, R> {

        private final Consumer<T> consumer;

        private final R result;

        public Consumer2FunctionAdapter(Consumer<T> consumer, R result) {
            this.consumer = consumer;
            this.result = result;
        }

        @Override
        public R apply(T value) {
            consumer.accept(value);
            return result;
        }

    }

    /**
     * 包装 Consumer，用于描述是否在迭代过程中继续执行子节点行为
     * 避免单纯的通过返回 true or false 意义不够明确
     */
    public abstract static class BaseExpConsumer<T> implements Consumer<T> {

        /**
         * 执行标志
         * #    -1   初始值，异常值
         * #    0    跳过子节点执行
         * #    1    继续执行子节点
         */
        private int flag = -1;

        public final void next() {
            flag = 1;
        }

        public final void skip() {
            flag = 0;
        }

        public final void clear() {
            flag = -1;
        }

        public final boolean isSkipped() {
            if (flag == -1) {
                throw new IllegalStateException("Please call next() or skip().");
            }
            return flag == 0;
        }

    }

}
