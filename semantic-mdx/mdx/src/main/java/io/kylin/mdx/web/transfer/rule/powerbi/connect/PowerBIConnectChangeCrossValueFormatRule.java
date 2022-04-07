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



package io.kylin.mdx.web.transfer.rule.powerbi.connect;

import io.kylin.mdx.web.transfer.rule.MdxTransferRule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PowerBIConnectChangeCrossValueFormatRule extends MdxTransferRule {
    private static final Pattern dimensionCrossValuePattern  = Pattern.compile("(?i)(MEMBER)([\\s\\S]*)(AS[\\s\\S]+?AGGREGATE\\()([\\s\\S]*)(CROSSJOIN\\()([\\s\\S]+?)(\\)\\')");

    @Override
    public String apply(String mdx) {
        StringBuffer result = new StringBuffer();
        Matcher valueMatcher = dimensionCrossValuePattern.matcher(mdx);
        while (valueMatcher.find()) {
            String preName = valueMatcher.group(2);
            String[] preNameArray = preName.split("\\.");
            preName = preNameArray[0] + "." + preNameArray[1];
            String[] lastArray = valueMatcher.group(6).split("\\,");
            String lastName = lastArray[0];
            lastName = lastName.replaceAll("\\{", "");
            lastName = lastName.replaceAll("\\}", "").trim();
            //avoid repeating prefixes
            String[] lastNameArray = lastName.split("\\.");
            if (lastNameArray.length > 1) {
                break;
            }
            String tmp = valueMatcher.group(6).replaceAll(lastName, preName + ".[" + lastName + "]");
            tmp = valueMatcher.group(1) + valueMatcher.group(2) + valueMatcher.group(3) + valueMatcher.group(4) + valueMatcher.group(5) + tmp + valueMatcher.group(7);
            if (!"".equals(tmp)) {
                valueMatcher.appendReplacement(result, tmp);
            }
        }
        valueMatcher.appendTail(result);
        return result.toString();
    }
}
