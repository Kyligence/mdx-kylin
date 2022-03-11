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

import io.kylin.mdx.web.rewriter.utils.ExpFinder;
import io.kylin.mdx.web.rewriter.utils.ExpUtils;
import mondrian.mdx.UnresolvedFunCall;
import mondrian.olap.Exp;
import mondrian.olap.Id;
import mondrian.olap.QueryAxis;
import mondrian.olap.Syntax;

import java.util.ArrayList;
import java.util.List;

/**
 * MDX 语句 Axis 扫描器
 */
public class AxisScanner {

    private final RuleContext context;

    public AxisScanner(RuleContext context) {
        this.context = context;
    }

    /**
     * 提取子查询中 Axis 信息，包含 Query-Axis 和 Slicer-Axis
     */
    public boolean scanAxis(QueryAxis axis) {
        if (axis.getExp() instanceof UnresolvedFunCall) {
            UnresolvedFunCall function = (UnresolvedFunCall) axis.getExp();
            if (function.getSyntax() == Syntax.Parentheses) {
                // 处理 (Set_Exp, ...) 语句
                doTplExpOnAxis(context, function);
                return true;
            } else if (function.getSyntax() == Syntax.Braces) {
                // 处理 {Mbr_Exp, ...} 语句
                doSetExpOnAxis(context, function);
                return true;
            } else if (function.getSyntax() == Syntax.Function) {
                // 处理 Function(args) 语句
                doFunExpOnAxis(context, function);
                return true;
            }
        }
        return false;
    }

    private static void doTplExpOnAxis(RuleContext context, UnresolvedFunCall tplExp) {
        // 消掉最外层 ()
        if (tplExp.getArgCount() == 1 && tplExp.getArg(0) instanceof UnresolvedFunCall
                && ((UnresolvedFunCall) tplExp.getArg(0)).getSyntax() == Syntax.Parentheses) {
            doTplExpOnAxis(context, (UnresolvedFunCall) tplExp.getArg(0));
            return;
        }
        for (int i = 0; i < tplExp.getArgCount(); i++) {
            Exp arg = tplExp.getArg(i);
            if (arg instanceof UnresolvedFunCall) {
                UnresolvedFunCall argFun = (UnresolvedFunCall) tplExp.getArg(i);
                // 分别考虑 {} 和 Function 情况
                if (argFun.getSyntax() == Syntax.Braces) {
                    doSetExpOnAxis(context, argFun);
                } else if (argFun.getSyntax() == Syntax.Function) {
                    doFunExpOnAxis(context, argFun);
                }
                // TODO: 需要考虑是否可能直接出现一个 Mbr_Exp
            } else if (arg instanceof Id) {
                // 考虑 (...) 中包含了 Member 的情况
                doMbrExpOnAxis(context, (Id) arg);
            }
        }
    }

    /**
     * 目前允许子查询中同 hierarchy 出现在不同的 Set_Exp 中，但是这种对于 MDX 来说应该是不符合语法的
     */
    private static void doSetExpOnAxis(RuleContext context, UnresolvedFunCall setExp) {
        // 消掉最外层 {}
        if (setExp.getArgCount() == 1 && setExp.getArg(0) instanceof UnresolvedFunCall
                && ((UnresolvedFunCall) setExp.getArg(0)).getSyntax() == Syntax.Braces) {
            doSetExpOnAxis(context, (UnresolvedFunCall) setExp.getArg(0));
            return;
        }
        for (int i = 0; i < setExp.getArgCount(); i++) {
            if (setExp.getArg(i) instanceof Id) {
                Id id = (Id) setExp.getArg(i);
                doMbrExpOnAxis(context, id);
            }
            // TODO: 需要考虑 Set_Exp 中出现的是一个 member 函数生成式
        }
    }

    /**
     * 函数表达式默认为生成一个集，直接找到对应的 Hierarchy
     */
    private static void doFunExpOnAxis(RuleContext context, UnresolvedFunCall funExp) {
        ExpFinder.traversalAndApply(funExp, exp -> {
            if (exp instanceof Id) {
                String hierarchyName = ExpUtils.getHierarchyName((Id) exp, false);
                if (hierarchyName != null) {
                    UnresolvedFunCall newFun = ExpLoader.translate(funExp);
                    context.allFunctions.computeIfAbsent(hierarchyName, k -> new ArrayList<>()).add(newFun);
                    return true;
                }
            }
            return false;
        }, true);
    }

    /**
     * 按照 Hierarchy 聚合 Id
     */
    private static void doMbrExpOnAxis(RuleContext context, Id id) {
        List<Id> ids = context.allMembers
                .computeIfAbsent(
                        ExpUtils.getHierarchyName(id),
                        k -> new ArrayList<>());
        if (!ids.contains(id)) {
            ids.add(id);
        }
    }

}
