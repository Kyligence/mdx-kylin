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

public class PowerBIConnectIfChangeFilterFormatRule extends MdxTransferRule {
    private static final Pattern filterPattern  = Pattern.compile("FILTER\\([\\s\\S]+?\\)[\\s]*?\\)[\\s]*?\\)");
    private static final Pattern filterDimensionPattern = Pattern.compile("\\[[\\S]+\\].\\[[\\S]+\\].\\[[\\S]+\\].ALLMEMBERS");
    private static final Pattern filterValuePattern  = Pattern.compile("\"[\\s\\S]+?\"");

    @Override
    public String apply(String mdx) {
        StringBuffer sb = new StringBuffer();
        Matcher m = filterPattern.matcher(mdx);
        while (m.find()){
            Matcher n = filterDimensionPattern.matcher(m.group());
            String ntmp = "";
            while(n.find()){
                String  tmp = n.group().replaceAll("ALLMEMBERS","");
                Matcher valueMatch = filterValuePattern.matcher(m.group());
                while (valueMatch.find()) {
                    ntmp = "FILTER(" + tmp + "members" + "," + tmp + "CURRENTMEMBER.name matches " + valueMatch.group() + ")";
                }
            }
            if(! "".equals(ntmp)){
                m.appendReplacement(sb,ntmp);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    @Override
    public boolean available(String mdx) {
        return mdx.contains("FILTER");
    }
}
