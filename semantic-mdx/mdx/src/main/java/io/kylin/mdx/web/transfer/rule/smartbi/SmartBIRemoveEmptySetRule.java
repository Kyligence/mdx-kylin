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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartBIRemoveEmptySetRule extends MdxTransferRule {
    private static final Pattern normalizePattern = Pattern.compile("(?i)(SELECT .*?ON columns), \\{\\} ON rows(.*)");

    @Override
    public String apply(String mdx) {
        //mdx-service #43
        StringBuffer sb = new StringBuffer(64);
        Matcher normalizeMatcher = normalizePattern.matcher(mdx);
        if (normalizeMatcher.find()) {
            String replacement = normalizeMatcher.group(1) + normalizeMatcher.group(2);
            normalizeMatcher.appendReplacement(sb, replacement);
        }
        normalizeMatcher.appendTail(sb);
        return sb.toString();
    }
}
