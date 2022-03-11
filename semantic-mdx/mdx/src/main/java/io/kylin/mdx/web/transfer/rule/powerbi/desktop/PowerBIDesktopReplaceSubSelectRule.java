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


package io.kylin.mdx.web.transfer.rule.powerbi.desktop;

import io.kylin.mdx.web.transfer.rule.MdxTransferRule;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PowerBIDesktopReplaceSubSelectRule extends MdxTransferRule {
    private static final String subSelectRegex =
            "from\\s*(?<SubClause>\\(\\s*select\\s*(?<SubSelectClause>.*)\\s*on\\s+\\S+\\s+from\\s+(?<SubFromClause>.*)\\s*\\)\\s*(?:where\\s+(?<WhereClause>.*))?)\\s*cell properties";
    private static final Pattern subSelectPattern = Pattern.compile(subSelectRegex,
            Pattern.CASE_INSENSITIVE + Pattern.DOTALL);

    /**
     * Replace a subselect expression with a "where" clause, that is because Mondrian does not support
     * subselect expressions.
     * @param mdx The input MDX statement to process.
     * @return The MDX statement with its subselect expression replaced by a "where" clause.
     */
    @Override
    public String apply(String mdx) {
        Matcher subSelectMatcher = subSelectPattern.matcher(mdx);
        if (subSelectMatcher.find()) {
            String subSelectClause = subSelectMatcher.group("SubSelectClause");
            String subFromClause = subSelectMatcher.group("SubFromClause");
            String prefix = mdx.substring(0, subSelectMatcher.start("SubClause"));
            String suffix = mdx.substring(subSelectMatcher.end("SubClause"));

            if (subSelectClause != null && subFromClause != null) {
                subSelectClause = subSelectClause.trim();
                subFromClause = subFromClause.trim();

                if (subSelectClause.startsWith("{") && subSelectClause.endsWith("}")) {
                    subSelectClause = subSelectClause.substring(1, subSelectClause.length() - 1);
                }
                String[] slicers = subSelectClause.split(",");

                String whereClause = subSelectMatcher.group("WhereClause");
                if (whereClause != null) {
                    if (whereClause.startsWith("(") && whereClause.endsWith(")")) {
                        whereClause = whereClause.substring(1, whereClause.length() - 1);
                    }
                    String[] originalSlicers = whereClause.split(",");
                    slicers = (String[]) ArrayUtils.addAll(originalSlicers, slicers);
                }

                StringBuilder replacedStatement = new StringBuilder();
                replacedStatement.append(prefix);
                replacedStatement.append(' ');
                replacedStatement.append(subFromClause);
                replacedStatement.append("\nWHERE\n    (");
                processWhereClause(replacedStatement, slicers);
                replacedStatement.append(")\n");
                replacedStatement.append(suffix);
                mdx = replacedStatement.toString();
            }
        }

        return mdx;
    }

    private static void processWhereClause(StringBuilder replacedStatement, String[] slicers) {
        Map<String, List<String>> whereClause = new HashMap<String, List<String>>();
        for (String slicer : slicers) {
            slicer = slicer.trim();
            String slicerKey = slicer.substring(0, slicer.lastIndexOf('.'));

            //Can use computeIfAbsent here with JDK >=8
            if (whereClause.get(slicerKey) == null) {
                whereClause.put(slicerKey, new ArrayList<String>());
            }
            whereClause.get(slicerKey).add(slicer);
        }

        List<String> slicerSetStrings = new ArrayList<String>();
        for (List<String> slicerValues : whereClause.values()) {
            String slicerSetString = String.join(",", slicerValues);
            if (slicerValues.size() > 1) slicerSetString = '{' + slicerSetString + '}';
            slicerSetStrings.add(slicerSetString);
        }
        replacedStatement.append(String.join(",", slicerSetStrings));
    }
}
