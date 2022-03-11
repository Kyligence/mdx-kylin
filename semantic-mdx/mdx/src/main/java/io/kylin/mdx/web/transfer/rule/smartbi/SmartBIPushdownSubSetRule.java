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


package io.kylin.mdx.web.transfer.rule.smartbi;

import io.kylin.mdx.web.transfer.rule.MdxTransferRule;
import mondrian.xmla.XmlaRequestContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartBIPushdownSubSetRule extends MdxTransferRule {
    private static final Pattern subSetMemberPattern = Pattern.compile("(?i)SubSet\\((((?!SubSet).)*?),\\s*(\\d+),\\s*(\\d+)\\)\\s*ON\\s*ROWS");

    @Override
    public String apply(String mdx) {
        XmlaRequestContext context = XmlaRequestContext.getContext();
        StringBuffer sb = new StringBuffer();
        Matcher subsetMatcher = subSetMemberPattern.matcher(mdx);
        if (subsetMatcher.find()) {
            int start = Integer.parseInt(subsetMatcher.group(3));
            int end = Integer.parseInt(subsetMatcher.group(4)) + start;
            context.queryPage = new XmlaRequestContext.QueryPage();
            context.queryPage.queryStart = start;
            context.queryPage.queryEnd = end;
            subsetMatcher.appendReplacement(sb, subsetMatcher.group(1) + " ON ROWS ");
            subsetMatcher.appendTail(sb);
            mdx = sb.toString();
        }
        return mdx;
    }
}
