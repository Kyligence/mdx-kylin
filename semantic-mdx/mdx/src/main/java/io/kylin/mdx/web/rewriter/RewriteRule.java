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



package io.kylin.mdx.web.rewriter;

import mondrian.olap.Query;
import org.olap4j.xmla.server.impl.Pair;

/**
 * @author hui.wang
 */
public interface RewriteRule {

    /**
     * 获取一个改写规则
     *
     * @return 改写规则
     */
    RewriteRule getOrNew();

    /**
     * 尝试执行改写
     *
     * @param query 查询
     * @return 结果，第一个参数表示是否执行，第二个结果返回改写后的 query
     */
    Pair<Boolean, Query> rewrite(Query query);

}
