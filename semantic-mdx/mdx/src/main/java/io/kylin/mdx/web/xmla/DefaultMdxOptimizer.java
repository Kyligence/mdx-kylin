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


package io.kylin.mdx.web.xmla;

import io.kylin.mdx.web.transfer.TransferRuleManager;
import mondrian.olap.MondrianProperties;
import mondrian.xmla.MdxOptimizer;
import mondrian.xmla.XmlaRequestContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultMdxOptimizer implements MdxOptimizer {

    private static final Pattern FILTER_EXPAND_CHILDREN_PATTERN =
            Pattern.compile("\\{AddCalculatedMembers\\(\\{[\\s\\S]+?[.]Children\\}\\)\\}");

    private static final Pattern FILTER_EXPAND_MEMBER_PATTERN =
            Pattern.compile("\\{AddCalculatedMembers\\(\\{[\\s\\S]+?[.]Members\\}\\)\\}");

    @Override
    public String rewriteMdx(String mdx) {
        XmlaRequestContext context = XmlaRequestContext.getContext();
        // if context has not been set or unable to optimize, skip
        if (context == null || !Boolean.parseBoolean(context.getParameter(XmlaRequestContext.Parameter.ENABLE_OPTIMIZE_MDX))) {
            return mdx;
        }
        if (filterExpand(mdx) && MondrianProperties.instance().FilterRowLimit.get() > 0) {
            context.filterRowLimitFlag = true;
        }

        return TransferRuleManager.applyAllRules(
                context.clientType,
                mdx,
                !context.useMondrian);
    }

    private boolean filterExpand(String mdx) {
        Matcher childrenMatcher = FILTER_EXPAND_CHILDREN_PATTERN.matcher(mdx);
        Matcher memberMatcher = FILTER_EXPAND_MEMBER_PATTERN.matcher(mdx);
        return childrenMatcher.find() || memberMatcher.find();
    }

}
