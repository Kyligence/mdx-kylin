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

import io.kylin.mdx.web.transfer.rule.MdxTransferRule;
import io.kylin.mdx.web.util.Brackets;
import io.kylin.mdx.web.util.BracketsFinder;
import mondrian.xmla.impl.DefaultXmlaRequest;

public class ExcelDrillThroughWhereRule extends MdxTransferRule {
    private static class Singleton {
        private static final ExcelDrillThroughWhereRule INSTANCE = new ExcelDrillThroughWhereRule();
    }

    private ExcelDrillThroughWhereRule() {}

    private static final String MDX_WHERE = "WHERE";

    public static ExcelDrillThroughWhereRule getInstance() {
        return Singleton.INSTANCE;
    }

    @Override
    public String apply(String mdx) {
        String upperMdx = mdx.toUpperCase();
        if (!upperMdx.startsWith(DefaultXmlaRequest.DRILLTHROUGH_PREFIX)) {
            return mdx;
        }

        int whereStartIndex = upperMdx.indexOf(MDX_WHERE);
        if (whereStartIndex < 0) {
            return mdx;
        }

        Brackets brackets = BracketsFinder.find(mdx, whereStartIndex + 5);
        StringBuilder mdxBuilder = new StringBuilder();
        mdxBuilder.append(mdx, 0, brackets.getStartIndex() + 1);
        brackets.flattenParentheses(mdx, mdxBuilder);
        mdxBuilder.append(mdx, brackets.getEndIndex(), mdx.length());

        return mdxBuilder.toString();
    }
}
