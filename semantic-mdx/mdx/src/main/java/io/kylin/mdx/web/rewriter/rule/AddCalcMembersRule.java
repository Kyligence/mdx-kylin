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



package io.kylin.mdx.web.rewriter.rule;

import io.kylin.mdx.web.rewriter.BaseRewriteRule;
import io.kylin.mdx.web.rewriter.utils.ExpFinder;
import mondrian.mdx.UnresolvedFunCall;
import mondrian.olap.*;
import org.olap4j.xmla.server.impl.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 支持包含 INCLUDE_CALC_MEMBERS 参数的函数改写
 *
 * @author hui.wang
 */
public class AddCalcMembersRule extends BaseRewriteRule {

    private static final String IDENTITY = "INCLUDE_CALC_MEMBERS";

    private static final String[] DRILLDOWN_FUN_NAMES = new String[]{
            "DrilldownLevel",
            "DrilldownLevelBottom",
            "DrilldownLevelTop",
            "DrilldownMember",
            "DrilldownMemberBottom",
            "DrilldownMemberTop"
    };

    @Override
    public Pair<Boolean, Query> rewrite(Query query) {
        QueryAxis[] axes = query.getAxes();
        boolean rewritten = false;
        for (QueryAxis axis : axes) {
            if (resolve(axis)) {
                rewritten = true;
            }
        }
        if (query instanceof SubQuery) {
            // iterative processing of sub-query
            Pair<Boolean, Query> pair = rewrite(((SubQuery) query).getQuery());
            if (Pair.isPass(pair)) {
                rewritten = true;
            }
        }
        return new Pair<>(rewritten, query);
    }

    /**
     * 改写示意：
     * #    DrilldownLevel(Set_Exp[,Level_Exp[,Index]],INCLUDE_CALC_MEMBERS)
     * ->
     * #    AddCalculatedMembers(DrilldownLevel(Set_Exp[,Level_Exp[,Index]]))
     */
    public boolean resolve(QueryAxis axis) {
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicReference<String> matchFun = new AtomicReference<>();
        ExpFinder.predicateAndApply(axis.getExp(),
                exp -> {
                    // 检查当前表达式是否是指定的函数之一，如：DrilldownLevel
                    if (exp instanceof UnresolvedFunCall) {
                        UnresolvedFunCall funCall = (UnresolvedFunCall) exp;
                        for (String funName : DRILLDOWN_FUN_NAMES) {
                            if (funName.equalsIgnoreCase(funCall.getFunName())) {
                                int argCount = funCall.getArgCount();
                                if (argCount >= 2 && funCall.getArg(argCount - 1) instanceof Id) {
                                    Id id = (Id) funCall.getArg(argCount - 1);
                                    if (IDENTITY.equals(id.toString())) {
                                        matchFun.set(funCall.getFunName());
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                    return false;
                }, new ExpFinder.BaseExpConsumer<Exp>() {
                    @Override
                    public void accept(Exp exp) {
                        // 改写符合条件的函数
                        UnresolvedFunCall funCall = (UnresolvedFunCall) exp;
                        List<Exp> args = new ArrayList<>();
                        int argCount = funCall.getArgCount();
                        for (int i = 0; i < argCount - 1; i++) {
                            args.add(funCall.getArg(i));
                        }
                        // 移除末尾的空白参数
                        while (args.size() > 0) {
                            Exp arg = args.get(args.size() - 1);
                            if (arg instanceof UnresolvedFunCall && ((UnresolvedFunCall) arg).getSyntax() == Syntax.Empty) {
                                args.remove(args.size() - 1);
                            } else {
                                break;
                            }
                        }
                        // 当前节点构造新函数
                        String funName = matchFun.get();
                        UnresolvedFunCall drilldownLevelFunCall = new UnresolvedFunCall(funName, args.toArray(new Exp[0]));
                        funCall.setFunName("AddCalculatedMembers");
                        funCall.setArgs(new Exp[]{drilldownLevelFunCall});
                        count.getAndIncrement();
                        // 继续执行子节点
                        next();
                    }
                });
        return count.get() > 0;
    }

}
