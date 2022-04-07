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
import mondrian.olap.Util;
import mondrian.xmla.MdxRewriter;
import org.olap4j.xmla.server.impl.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author hui.wang
 */
public class MdxRewriterImpl implements MdxRewriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MdxRewriterImpl.class);

    private final SimpleValidator mdxValidator = new SimpleValidator();

    private final RewriteRuleManager ruleManager = new RewriteRuleManager(mdxValidator);

    @Override
    public Pair<Boolean, String> rewrite(String mdx) {
        try {
            Query query = (Query) mdxValidator.parseInternal(mdx);
            String newMdx = apply(query);
            if (newMdx != null) {
                return new Pair<>(true, newMdx);
            } else {
                return new Pair<>(false, null);
            }
        } catch (Exception e) {
            LOGGER.error("Rewrite mdx statement failed! mdx = '{}'", mdx, e);
            return new Pair<>(false, e.getMessage());
        }
    }

    private String apply(Query query) {
        List<RewriteRule> rules = ruleManager.getRules();
        boolean rewritten = false;
        for (RewriteRule rule : rules) {
            rule = rule.getOrNew();
            Pair<Boolean, Query> pair = rule.rewrite(query);
            if (Pair.isPass(pair)) {
                query = pair.getRight();
                rewritten = true;
            }
        }
        return rewritten ? Util.unparse(query) : null;
    }

}
