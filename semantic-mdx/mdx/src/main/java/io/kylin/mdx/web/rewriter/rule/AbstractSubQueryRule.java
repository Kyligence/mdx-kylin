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
import mondrian.olap.Query;
import mondrian.olap.SubQuery;
import org.olap4j.xmla.server.impl.Pair;

/**
 * @author hui.wang
 */
public abstract class AbstractSubQueryRule extends BaseRewriteRule {

    @Override
    public Pair<Boolean, Query> rewrite(Query query) {
        if (!(query instanceof SubQuery)) {
            return new Pair<>(false, query);
        }
        SubQuery subQuery = (SubQuery) query;
        if (!isMatches(subQuery)) {
            return new Pair<>(false, query);
        }
        boolean result = doRewrite(subQuery);
        return new Pair<>(result, subQuery);
    }

    /**
     * 检查是否匹配子查询
     *
     * @param subQuery 传入的子查询
     * @return 可否改写
     */
    public abstract boolean isMatches(SubQuery subQuery);

    /**
     * 执行改写
     *
     * @param subQuery 传入的子查询
     * @return 改写执行返回 true
     */
    public abstract boolean doRewrite(SubQuery subQuery);

}
