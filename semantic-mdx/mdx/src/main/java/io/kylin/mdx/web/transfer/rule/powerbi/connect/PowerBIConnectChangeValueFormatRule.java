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

public class PowerBIConnectChangeValueFormatRule extends MdxTransferRule {
    private static final Pattern dimensionValuePattern  = Pattern.compile("(?i)(MEMBER)([\\s\\S]*)(AS[\\s\\S]+?AGGREGATE\\()([\\s\\S]+?)(\\)\\')");

    // Change the dimension table mode filter query field from a single field to a fully qualified name format，
    // for example : from 'China' to '[SELLER_COUNTRY].[COUNTRY].[China]'.
    @Override
    public String apply(String mdx) {
        StringBuffer result = new StringBuffer();
        Matcher valueMatcher = dimensionValuePattern.matcher(mdx);
        while (valueMatcher.find()) {
            String preName = valueMatcher.group(2);
            String[] preNameArray = preName.split("\\.");
            preName = preNameArray[0] + "." + preNameArray[1];
            String lastName = valueMatcher.group(4);
            lastName = lastName.replaceAll("\\{", "");
            lastName = lastName.replaceAll("\\}", "").trim();
            //avoid repeating prefixes
            String[] lastNameArray = lastName.split("\\.");
            if(lastNameArray.length>1){
                break;
            }
            String tmp = valueMatcher.group(4).replaceAll(lastName, preName + ".[" + lastName + "]");
            tmp = valueMatcher.group(1) + valueMatcher.group(2) + valueMatcher.group(3) + tmp + valueMatcher.group(5);
            if (!"".equals(tmp)) {
                valueMatcher.appendReplacement(result, tmp);
            }
        }
        valueMatcher.appendTail(result);
        return result.toString();
    }
}
