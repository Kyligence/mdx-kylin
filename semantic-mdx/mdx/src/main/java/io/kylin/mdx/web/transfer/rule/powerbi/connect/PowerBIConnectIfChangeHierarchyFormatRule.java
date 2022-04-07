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

public class PowerBIConnectIfChangeHierarchyFormatRule extends MdxTransferRule {
    private static final Pattern normalHierarchyPattern = Pattern.compile("GENERATE\\([\\s\\S]+?LEAVES[\\s]*?\\)[\\s]*?\\)" );
    private static final Pattern memberHierarchyPattern = Pattern.compile("\\[[\\S]+\\].\\[[\\S]+\\].\\[[\\S]+\\],");
    private static final Pattern deepenHierarchyPattern = Pattern.compile("GENERATE\\([\\s\\S]+LEAVES[\\s]*?\\)[\\s]*?\\)" );
    private static final Pattern childrenHierarchyPattern = Pattern.compile("\\{[\\s\\S]+?\\}");

    @Override
    public String apply(String mdx) {
        if (!available(mdx)) {
            return mdx;
        }

        StringBuffer sb = new StringBuffer();
        //正常模式的下钻
        if (mdx.contains("ALLMEMBERS")) {
            Matcher m = normalHierarchyPattern.matcher(mdx);
            String ntmp = "";
            while (m.find()) {
                Matcher n = memberHierarchyPattern.matcher(m.group());
                while (n.find()) {
                    ntmp = n.group().replaceAll(",", "");
                    ntmp += ".members";
                }
                if (!"".equals(ntmp)) {
                    m.appendReplacement(sb, ntmp);
                }
            }
            m.appendTail(sb);
            //深化模式下钻
        } else {
            Matcher m = deepenHierarchyPattern.matcher(mdx);
            String ntmp = "";
            while (m.find()) {
                Matcher n = childrenHierarchyPattern.matcher(m.group());
                String tmp = "";
                while(n.find()){
                    tmp = n.group().replaceAll("\\{", "");
                    tmp = tmp.replaceAll("\\}", "");
                    tmp = tmp.replaceAll("\\n\\r", "");
                    tmp += ".children";
                    ntmp = "(" + tmp +")";
                }
                if (!"".equals(ntmp)) {
                    m.appendReplacement(sb, ntmp);
                }
            }
            m.appendTail(sb);
        }
        return sb.toString();

    }

    @Override
    public boolean available(String mdx) {
        return mdx.contains("Hierarchy");
    }
}
