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

import io.kylin.mdx.web.transfer.rule.MdxQueryPartOptimizeRule;
import lombok.extern.slf4j.Slf4j;
import mondrian.xmla.XmlaRequestContext;
import mondrian.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ExcelQueryPartOptimizeRule extends MdxQueryPartOptimizeRule {

    protected static final int MEASURE_TYPE = 1;
    protected static final int ONE_ATTRIBUTE_HIERARCHY_TYPE = 2;
    protected static final int MANY_ATTRIBUTES_HIERARCHY_TYPE = 3;
    protected static final int FILTERED_DIMENSION_TYPE = 4;
    protected static final int FILTERED_HIERARCHY_TYPE = 5;
    protected static final int NAMED_SET_TYPE = 6;

    private static final Pattern measureOrDimension = Pattern.compile("(?i)(\\{\\[Measures\\][\\s\\S]+?\\}" +
            "|Hierarchize\\(AddCalculatedMembers\\([\\s\\S]+?\\)\\)" +
            "|Hierarchize\\(\\{DrilldownLevel\\(\\{[\\s\\S]+?\\}\\)\\}\\)" +
            "|Hierarchize\\(Intersect([\\s\\S]+?)\\[XL_[\\s\\S]{3}_Dim_(\\d+)\\]\\)\\))" +
            "|Hierarchize\\(Distinct\\(\\{([\\s\\S]+?)\\}\\)\\)" +
            "|Hierarchize\\(\\{([\\s\\S]+?)(\\}\\s*\\))+");

    private static final Pattern allElePatternV2 = Pattern.compile("(\\[((?!\\[)[\\s\\S])+?\\])[.]\\[All\\]");
    private static final Pattern dimensionPattern = Pattern.compile("Hierarchize\\(AddCalculatedMembers\\(\\{DrilldownLevel\\((\\{([\\s\\S]+?)\\})\\)\\}\\)\\)");
    private static final Pattern namedSetPattern = Pattern.compile("Hierarchize\\((?:Distinct\\()?\\{(?<NamedSet>\\[[\\s\\S]+?])}\\)?\\s*\\)");
    //处理以Hierarchize({DrilldownLevel开头的语句
    private static final Pattern dimensionPattern2 = Pattern.compile("Hierarchize\\(\\{DrilldownLevel\\((\\{([\\s\\S]+?)\\})\\)\\}\\)");

    @Override
    public String applyClause(String clause) {
        return clause;
    }

    @Override
    public String applyQueryAxis(String queryAxis) {
        if (queryAxis == null || queryAxis.trim().length() == 0) {
            return null;
        }
        // resolve measure and dimensions
        List<Pair<Integer, String>> measureOrDims = extractMeasureOrDims(queryAxis);
        long measuresCount = measureOrDims
                .stream()
                .filter(measureOrDim -> measureOrDim.getLeft() == MEASURE_TYPE)
                .count();
        if (measuresCount == measureOrDims.size()) {
            return queryAxis.replaceFirst("^\\s*,\\s?", "");
        }
        // resolve non empty
        // resolve dimension properties
        String dimensionProperties = null;
        String onPhaseNoDimensions = null;
        int idxOfDimProp = queryAxis.indexOf(" DIMENSION PROPERTIES");
        if (idxOfDimProp != -1) {
            dimensionProperties = queryAxis.substring(idxOfDimProp);
        } else {
            int idxOfOnPhase = queryAxis.lastIndexOf(" ON ");
            if (idxOfOnPhase != -1) {
                onPhaseNoDimensions = queryAxis.substring(idxOfOnPhase);
            }
        }
        boolean isNonEmpty = queryAxis.contains("NON EMPTY ");
        if (!needTotal()) {
            queryAxis = createQueryAxisWithoutTotal(measureOrDims);
        } else {
            queryAxis = createQueryAxisWithTotal(measureOrDims);
        }
        if (isNonEmpty) {
            queryAxis = "NON EMPTY " + queryAxis;
        }
        if (dimensionProperties != null) {
            queryAxis = queryAxis + dimensionProperties;
        } else if (onPhaseNoDimensions != null) {
            queryAxis = queryAxis + onPhaseNoDimensions;
        }
        return queryAxis;
    }

    protected List<Pair<Integer, String>> extractMeasureOrDims(String queryAxis) {
        List<Pair<Integer, String>> measureOrDims = new ArrayList<Pair<Integer, String>>();
        Matcher queryAxisMatcher = measureOrDimension.matcher(queryAxis);
        while (queryAxisMatcher.find()) {
            String measureOrDim = queryAxisMatcher.group(0);
            // resolve measure
            if (measureOrDim.contains("{[Measures]")) {
                measureOrDims.add(new Pair<>(MEASURE_TYPE, measureOrDim));
                continue;
            }
            // resolve filtered dimension/hierarchy
            if (measureOrDim.contains("Intersect")) {
                if (measureOrDim.contains("Hierarchy")) {
                    measureOrDims.add(new Pair<>(FILTERED_HIERARCHY_TYPE, measureOrDim));
                } else {
                    // resolve filtered set index
                    String simplifiedFilteredDim = null;
                    String filteredSetIdx = queryAxisMatcher.group(3);
                    if (measureOrDim.contains("XL_Row_Dim")) {
                        simplifiedFilteredDim = "[XL_Row_Dim_" + filteredSetIdx + "]";
                    }
                    if (measureOrDim.contains("XL_Col_Dim")) {
                        simplifiedFilteredDim = "[XL_Col_Dim_" + filteredSetIdx + "]";
                    }

                    measureOrDims.add(new Pair<>(FILTERED_DIMENSION_TYPE, simplifiedFilteredDim));
                }
                continue;
            }
            // resolve expanded hierarchy
            if (measureOrDim.contains("Hierarchy")) {
                if (measureOrDim.contains("DrilldownMember")) {
                    // add missing brackets
                    if (!validBracket(measureOrDim)) {
                        measureOrDim = measureOrDim + ")";
                    }
                }
                measureOrDims.add(new Pair<>(MANY_ATTRIBUTES_HIERARCHY_TYPE, measureOrDim));
                continue;
            }
            // resolve dimension pattern
            Matcher dimensionMatcher = dimensionPattern.matcher(measureOrDim);
            if (dimensionMatcher.find()) {
                String simpleDimension = dimensionMatcher.group(2);
                measureOrDims.add(new Pair<>(ONE_ATTRIBUTE_HIERARCHY_TYPE, simpleDimension));
                continue;
            }
            Matcher dimensionMatcher2 = dimensionPattern2.matcher(measureOrDim);
            if (dimensionMatcher2.find()) {
                String simpleDimension = dimensionMatcher2.group(2);
                measureOrDims.add(new Pair<>(ONE_ATTRIBUTE_HIERARCHY_TYPE, simpleDimension));
                continue;
            }
            //resolve named sets
            Matcher namedSetMatcher = namedSetPattern.matcher(measureOrDim);
            if (namedSetMatcher.find()) {
                String namedSet = namedSetMatcher.group("NamedSet");
                measureOrDims.add(new Pair<>(NAMED_SET_TYPE, namedSet));
                continue;
            }

            // resolve default
            log.error("unknown dimension pattern {}", measureOrDim);
        }
        return measureOrDims;
    }

    public boolean validBracket(String clause) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < clause.length(); i++) {
            char ch = clause.charAt(i);
            if (ch == '(' || ch == ')') {
                builder.append(ch);
            }
        }
        final int len = builder.length();
        if (len == 0) {
            return true;
        }
        String s = builder.toString();
        if ((len & 1) != 0) {
            return false;
        }
        final int stack = len >> 1;
        int top = 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == '(') {
                if (top == stack) {
                    return false;
                }
                top++;
            } else if (c == ')') {
                if (top == 0) {
                    return false;
                }
                top--;
            }
        }
        return top == 0;
    }

    protected boolean needTotal() {
        XmlaRequestContext context = XmlaRequestContext.getContext();
        return Boolean.parseBoolean(context.getParameter(XmlaRequestContext.Parameter.NEED_CALCULATE_TOTAL));
    }

    private String createQueryAxisWithoutTotal(List<Pair<Integer, String>> measureOrDims) {
        int idx = measureOrDims.size();
        List<String> memberSets = transferAllMemberBeforeIndex(measureOrDims, idx);
        return createNonEmptyCrossJoinSet(memberSets);
    }

    private String createQueryAxisWithTotal(List<Pair<Integer, String>> measureOrDims) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hierarchize(");
        int excludeIndex = -1;
        for (int i = 0; i < measureOrDims.size(); i++) {
            if (measureOrDims.get(i).getLeft().equals(MEASURE_TYPE)) {
                excludeIndex = i;
            }
            sb.append("Union(");
        }
        for (int i = 0; i <= measureOrDims.size(); i++) {
            List<String> transferredMembers = transferAllMemberBeforeIndex(measureOrDims, i);
            String memberSetsStr = createNonEmptyCrossJoinSet(transferredMembers);
            if (i == 0) {
                sb.append(memberSetsStr);
            } else {
                sb.append(",");
                sb.append(memberSetsStr);
                if (!validBracket(sb.toString())) {
                    sb.append(")");
                }
            }
        }
        if (excludeIndex >= 0) {
            sb.append(",PRE,");
            sb.append(excludeIndex);
        }
        if (!validBracket(sb.toString())) {
            sb.append(")");
        }
        return sb.toString();
    }

    private List<String> transferAllMemberBeforeIndex(List<Pair<Integer, String>> measureOrDims, int idx) {
        List<String> memberSets = new ArrayList<>(measureOrDims.size());
        for (int i = 0; i < measureOrDims.size(); i++) {
            boolean isAll = i >= idx;
            String memberSet;
            Pair<Integer, String> memberPair = measureOrDims.get(i);
            if (memberPair.getLeft() == ONE_ATTRIBUTE_HIERARCHY_TYPE && !isAll) {
                memberSets.add(convertAllEleMemberToMembers(memberPair.getRight()));
                continue;
            }
            if (memberPair.getLeft() == MANY_ATTRIBUTES_HIERARCHY_TYPE) {
                memberSets.add(transferHierarchyMemberSet(memberPair.getRight(), isAll));
                continue;
            }
//            if (memberPair.getKey() == FILTERED_DIMENSION_TYPE) {
//                String memberStr = filterSetMapLocal.get().get(memberPair.getValue());
//                if (memberStr == null || isAll) {
//                    memberSets.add(memberPair.getValue());
//                } else {
//                    memberSets.add(memberStr);
//                }
//                continue;
//            }
            // default
            memberSet = memberPair.getRight();
            memberSets.add(memberSet);
        }
        return memberSets;
    }

    private String convertAllEleMemberToMembers(String allEleMember) {
        Matcher memberMatcher = allElePatternV2.matcher(allEleMember);
        if (memberMatcher.find()) {
            return memberMatcher.replaceAll("$1.$1.Members");
        }
        return allEleMember;
    }

    protected String transferHierarchyMemberSet(String value, boolean isAll) {
        return value;
    }

    private String createNonEmptyCrossJoinSet(List<String> memberSets) {
        if (memberSets.size() == 1) {
            return memberSets.get(0);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < memberSets.size(); i++) {
                sb.append("NonEmptyCrossJoin(");
            }
            sb.append(memberSets.get(0));
            for (int i = 1; i < memberSets.size(); i++) {
                sb.append(",");
                sb.append(memberSets.get(i));
                sb.append(")");
            }
            return sb.toString();
        }
    }

}
