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


package io.kylin.mdx.web.transfer.rule.excel;

import io.kylin.mdx.web.transfer.rule.generic.IfSearchRule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelIfSearchRule extends IfSearchRule {
    private static final Pattern filterSearchPattern =
            Pattern.compile("(?i)CurrentMember[.]Properties\\(\"caption\"\\)");

    private static final Pattern filterSearchMemberCaptionPattern =
            Pattern.compile("(?i)CurrentMember[.]member\\_caption");

    private static final Pattern filterSearchPattern1 =
            Pattern.compile("(?i)Set\\s+FilteredMembers\\s+As\\s+'Head\\s+\\(Filter\\(" +
                    "(AddCalculatedMembers\\((.+?)\\)),[\\s\\S]+?(InStr\\(\\d+,([^,]+),([\\s]*\"[^\"]+?\")\\)>0)\\)");

    @Override
    public String apply(String mdx) {
        if (!super.available(mdx)) {
            return mdx;
        }

        Matcher filterCaptionMatch = filterSearchMemberCaptionPattern.matcher(mdx);
        StringBuffer captionMdx = new StringBuffer();
        while (filterCaptionMatch.find()) {
            filterCaptionMatch.appendReplacement(captionMdx, "CurrentMember.properties(\"caption\")");
        }
        filterCaptionMatch.appendTail(captionMdx);
        mdx = captionMdx.toString();

        Matcher filterMatcher = filterSearchPattern1.matcher(mdx);
        if (!filterMatcher.find()) {
            return mdx;
        }
        StringBuffer sb = new StringBuffer();
        mdx = mdx.replace(filterMatcher.group(1), filterMatcher.group(2)).replace(filterMatcher.group(3),
                filterMatcher.group(4) + " MATCHES " + filterMatcher.group(5));

        filterMatcher = filterSearchPattern.matcher(mdx);
        while (filterMatcher.find()) {
            filterMatcher.appendReplacement(sb, "CurrentMember.name");
        }
        filterMatcher.appendTail(sb);
        setCompleted(true);
        return sb.toString();
    }
}
