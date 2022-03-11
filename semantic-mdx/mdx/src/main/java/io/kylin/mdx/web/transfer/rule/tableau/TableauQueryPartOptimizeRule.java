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


package io.kylin.mdx.web.transfer.rule.tableau;

import io.kylin.mdx.web.transfer.rule.excel.ExcelQueryPartOptimizeRule;
import mondrian.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TableauQueryPartOptimizeRule extends ExcelQueryPartOptimizeRule {

    private static final Pattern measureOrDimensionPattern = Pattern.compile("(?i)\\{\\[Measures\\][\\s\\S]+?\\}" +
            "|Generate\\(.*AddCalculatedMembers\\(Descendants.*CurrentMember, ([\\s\\S]*)?, LEAVES\\)\\) \\)" +
            "|\\[Tableau Set [\\S]+\\]" +
            "|(\\[[^\\[]+\\][.]){3}AllMembers");

    private static final Pattern allMembersPattern = Pattern.compile("(\\[[^\\[]+\\][.])AllMembers");

    private static final Pattern levelAsDimPattern = Pattern.compile("\\{(\\[[^\\[]+\\][.]){2}\\[[^\\[]+\\]\\}");

    private static final Pattern memberListAsDimPattern = Pattern.compile("AddCalculatedMembers\\( Except\\([\\s\\S]*?\\)\\,");

    @Override
    public String applyClause(String preClause) {
        preClause = preClause.replaceAll("(?i)\\.MemberValue", ".Caption");
        if(preClause.contains("Filter")) {
            preClause = adjustDataFormat(preClause);
        }
        return super.applyClause(preClause);
    }

    protected String adjustDataFormat(String mdxPart) {
        if (mdxPart.startsWith(",")) {
            mdxPart = mdxPart.substring(1);
        }
        //Convert member to CDate
        Pattern replacePatternGOE = Pattern.compile("\\[Measures\\]\\.\\[LEVEL INSTANCE none:[\\S]+:qk[:\\S]* - lev00] >=");
        Matcher slicerMatcher = replacePatternGOE.matcher(mdxPart);
        while (slicerMatcher.find()) {
            String replacePart = slicerMatcher.group(0);
            String newPart = "CDate(" + replacePart.substring(0, replacePart.indexOf(">=")) + ") >=";
            mdxPart = mdxPart.replace(replacePart, newPart);
        }

        Pattern replacePatternLOE = Pattern.compile("\\[Measures\\]\\.\\[LEVEL INSTANCE none:[\\S]+:qk[:\\S]* - lev00] <=");
        slicerMatcher = replacePatternLOE.matcher(mdxPart);
        while (slicerMatcher.find()) {
            String replacePart = slicerMatcher.group(0);
            String newPart = "CDate(" + replacePart.substring(0, replacePart.indexOf("<=")) + ") <=";
            mdxPart = mdxPart.replace(replacePart, newPart);
        }

        return mdxPart;
    }

    @Override
    public String applyQueryAxis(String queryAxis) {
        if (queryAxis.contains("Filter")) {
            queryAxis = adjustDataFormat(queryAxis);
            return queryAxis;
        } else {
            return super.applyQueryAxis(queryAxis);
        }
    }

    protected boolean needTotal() {
        return false;
    }

    @Override
    protected List<Pair<Integer, String>> extractMeasureOrDims(String queryAxis) {
        List<Pair<Integer, String>> measureOrDims = new ArrayList<>();
        Matcher queryAxisMatcher = measureOrDimensionPattern.matcher(queryAxis);
        while (queryAxisMatcher.find()) {
            String measureOrDim = queryAxisMatcher.group(0);
            Pair<Integer, String> measureOrDimPair;
            if (measureOrDim.contains("{[Measures]")) {
                measureOrDimPair = new Pair<>(MEASURE_TYPE, measureOrDim);
            } else if (measureOrDim.contains("Tableau Set")) {
                measureOrDimPair = new Pair<>(FILTERED_DIMENSION_TYPE, measureOrDim);
            } else if (measureOrDim.contains("-Hierarchy")) {
                // hierarchy 下钻了
                if (measureOrDim.contains("Generate")) {
                    measureOrDim = queryAxisMatcher.group(1);
                } else {
                    measureOrDim = measureOrDim.replace(".AllMembers", "");
                }
                measureOrDimPair = new Pair<>(MANY_ATTRIBUTES_HIERARCHY_TYPE, measureOrDim);
            } else {
                Matcher allMembersMatcher = allMembersPattern.matcher(measureOrDim);
                if (allMembersMatcher.find()) {
                    measureOrDim = allMembersMatcher.replaceAll("[All]");
                }
                measureOrDimPair = new Pair<>(ONE_ATTRIBUTE_HIERARCHY_TYPE, measureOrDim);
            }
            measureOrDims.add(measureOrDimPair);
        }
        queryAxisMatcher = memberListAsDimPattern.matcher(queryAxis);
        while (queryAxisMatcher.find()) {
            String measureOrDim = queryAxisMatcher.group(0);
            if (measureOrDim.endsWith(",")) {
                measureOrDim = measureOrDim.substring(0, measureOrDim.length() - 1);
            }
            Pair<Integer, String> measureOrDimPair = new Pair<>(FILTERED_DIMENSION_TYPE, measureOrDim);
            measureOrDims.add(measureOrDimPair);
        }
        queryAxisMatcher = levelAsDimPattern.matcher(queryAxis);
        while (queryAxisMatcher.find()) {
            String measureOrDim = queryAxisMatcher.group(0);
            Pair<Integer, String> measureOrDimPair = new Pair<>(FILTERED_DIMENSION_TYPE, measureOrDim);
            measureOrDims.add(measureOrDimPair);
        }
        return measureOrDims;
    }

    @Override
    protected String transferHierarchyMemberSet(String value, boolean isAll) {
        return value + ".Members";
    }
}
