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
import io.kylin.mdx.web.transfer.TransferRuleManager;
import mondrian.mdx.UnresolvedFunCall;
import mondrian.olap.Exp;
import mondrian.olap.Query;
import mondrian.olap.QueryAxis;
import mondrian.olap.SubQuery;
import mondrian.xmla.XmlaRequestContext;
import org.olap4j.xmla.server.impl.Pair;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 将函数 CrossJoin 改写为 NonEmptyCrossJoin
 */
public class ReplaceFunNameRule extends BaseRewriteRule {

    private static final String CROSSJOIN_FUN = "CrossJoin";

    private static final String NONEMPTY_CROSSJOIN_FUN = "NonEmptyCrossJoin";

    @Override
    public Pair<Boolean, Query> rewrite(Query query) {
        XmlaRequestContext context = XmlaRequestContext.getContext();
        if (context == null || context.clientType == null || context.mdxQuery == null) {
            return new Pair<>(false, null);
        }
        TransferRuleManager.ClientRules rules = TransferRuleManager.getSpecificTransferRules(
                context.clientType, context.mdxQuery, !context.useMondrian);
        if (rules != TransferRuleManager.ClientRules.Excel
//                && rules != TransferRuleManager.ClientRules.Excel_Other
                && rules != TransferRuleManager.ClientRules.Tableau
                && rules != TransferRuleManager.ClientRules.Tableau_Other
                && rules != TransferRuleManager.ClientRules.SmartBI
                && rules != TransferRuleManager.ClientRules.SmartBI_Other) {
            return new Pair<>(false, null);
        }

        QueryAxis[] axes = query.getAxes();
        boolean rewritten = false;
        for (QueryAxis axis : axes) {
            if (resolve(axis)) {
                rewritten = true;
            }
        }
        if (query instanceof SubQuery) {
            Pair<Boolean, Query> pair = rewrite(((SubQuery) query).getQuery());
            if (Pair.isPass(pair)) {
                rewritten = true;
            }
        }
        return new Pair<>(rewritten, query);
    }

    private boolean resolve(QueryAxis axis) {
        final AtomicInteger count = new AtomicInteger(0);
        ExpFinder.predicateAndApply(axis.getExp(),
                exp -> {
                    if (exp instanceof UnresolvedFunCall) {
                        return CROSSJOIN_FUN.equalsIgnoreCase(((UnresolvedFunCall) exp).getFunName());
                    }
                    return false;
                }, new ExpFinder.BaseExpConsumer<Exp>() {
                    @Override
                    public void accept(Exp exp) {
                        UnresolvedFunCall funCall = (UnresolvedFunCall) exp;
                        funCall.setFunName(NONEMPTY_CROSSJOIN_FUN);
                        count.getAndIncrement();
                        // 继续执行子节点
                        next();
                    }
                });
        return count.get() > 0;
    }

}
