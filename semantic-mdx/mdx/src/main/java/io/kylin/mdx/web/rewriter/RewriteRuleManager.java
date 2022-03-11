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

import io.kylin.mdx.web.rewriter.rule.AddCalcMembersRule;
import io.kylin.mdx.web.rewriter.rule.MstrFilterFlattenRule;
import io.kylin.mdx.web.rewriter.rule.RemoveSubQueryRule;
import io.kylin.mdx.web.rewriter.rule.ReplaceFunNameRule;
import io.kylin.mdx.web.rewriter.rule.SetsInWhereToCalcMemberRule;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hui.wang
 */
public class RewriteRuleManager {

    @Getter
    private final List<RewriteRule> rules = new ArrayList<>();

    public RewriteRuleManager(SimpleValidator mdxValidator) {
        /*
         * 去除函数中的 INCLUDE_CALC_MEMBERS 标签
         */
        rules.add(new AddCalcMembersRule());

        /*
         * 改写子查询语句为非子查询, 支持更复杂功能
         */
        rules.add(new RemoveSubQueryRule(mdxValidator));

        /*
         * 将 CrossJoin 改写为 NonEmptyCrossJoin
         */
        rules.add(new ReplaceFunNameRule());

        /*
         *
         */
        rules.add(new SetsInWhereToCalcMemberRule());

        /*
         *
         */
        rules.add(new MstrFilterFlattenRule());
    }

}
