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


package io.kylin.mdx.web.transfer.rule;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class MdxQueryPartOptimizeRule extends MdxTransferRule {

    private static final Pattern MDX_QUERY_PATTERN = Pattern.compile("(?i)([\\s\\S]*SELECT )((NON EMPTY)?[\\s\\S]+?ON (?:COLUMNS|0))([\\s\\S]+?)(FROM[\\s]+\\[[\\s\\S]+)");

    private MdxQueryPart mdxQueryPart;

    protected ThreadLocal<Map<String, String>> filterSetMapLocal = new ThreadLocal<>();

    public abstract String applyClause(String clause);
    public abstract String applyQueryAxis(String queryAxis);

    public String apply(String mdx) {
        if (!resolve()) {
            return mdx;
        }

        try {
            String newPreClause = applyClause(mdxQueryPart.getPreClause());
            String newRowStr = applyQueryAxis(mdxQueryPart.getRowsStr());
            String newColumnStr = applyQueryAxis(mdxQueryPart.getColumnsStr());
            mdxQueryPart.setPreClause(newPreClause);
            mdxQueryPart.setRowsStr(newRowStr);
            mdxQueryPart.setColumnsStr(newColumnStr);
            return mdxQueryPart.toString();
        } catch (Exception e) {
            log.error("unable to optimize mdx {}", mdx);
            return mdx;
        }
    }

    public boolean resolve() {
        mdxQueryPart = resolveMdxQueryPart(getMdx());
        if (mdxQueryPart == null || mdxQueryPart.toString() == null) {
            setCompleted(true);
            return false;
        }

        return true;
    }

    private MdxQueryPart resolveMdxQueryPart(String mdx) {
        // 找出要替换的部分
        Matcher mdxQueryMatcher = MDX_QUERY_PATTERN.matcher(mdx);
        if (mdxQueryMatcher.find()) {
            MdxQueryPart mdxQueryPart = new MdxQueryPart();
            String preClause = mdxQueryMatcher.group(1);
            String columnsStr = mdxQueryMatcher.group(2);
            String rowsStr = mdxQueryMatcher.group(4);
            String postClause = mdxQueryMatcher.group(5);
            mdxQueryPart.setPreClause(preClause);
            mdxQueryPart.setColumnsStr(columnsStr);
            mdxQueryPart.setRowsStr(rowsStr);
            mdxQueryPart.setPostClause(postClause);
            return mdxQueryPart;
        }
        return null;
    }


}
