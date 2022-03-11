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

import io.kylin.mdx.web.transfer.rule.generic.IfKeepOneRule;
import mondrian.xmla.XmlaRequestContext;

public class ExcelIfKeepOneRule extends IfKeepOneRule {
    @Override
    public String apply(String mdx) {
        if (!super.available(mdx)) {
            return mdx;
        }
        // replace .unique_name to .uniquename
        mdx = mdx.replaceAll("(?i)[.]unique_name", ".uniquename");

        // add missing cube part
        XmlaRequestContext context = XmlaRequestContext.getContext();
        String cubeName = context.getParameter("Catalog");
        mdx = mdx.replaceAll("(?i)from[\\s\\S]+cell[\\s\\S]+properties[\\s\\S]+value",
                "from [" + cubeName + "] cell properties value");

        // add missing bracket and change measures to Measures
        mdx = mdx.replaceAll("(?i)measures.__XlItemPath", "[Measures].[__XlItemPath]");
        mdx = mdx.replaceAll("(?i)measures.__XlSiblingCount", "[Measures].[__XlSiblingCount]");
        mdx = mdx.replaceAll("(?i)measures.__XlChildCount", "[Measures].[__XlChildCount]");

        // change generate arg type to string
        mdx = mdx.replaceAll("(?i)AddCalculatedMembers\\([^(]+[.]siblings\\).count", "Str($0)");
        setCompleted(true);
        return mdx;
    }
}
