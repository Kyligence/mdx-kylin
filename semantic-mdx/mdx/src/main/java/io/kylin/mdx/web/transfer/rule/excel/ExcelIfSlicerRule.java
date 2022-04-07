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

import io.kylin.mdx.web.transfer.rule.generic.IfSlicerRule;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelIfSlicerRule extends IfSlicerRule {


    private static final Pattern slicerPattern1 = Pattern.compile("(?i)(NonEmpty\\()[\\s\\S]*?,([\\s\\S]*?)set\\s__XLExistingRangeMembers");

    private static final Pattern unionPattern = Pattern.compile("(?i)Union\\(([\\s\\S]*)\\)[\\s\\S]*?set\\s__XLExistingRangeMembers");

    private static final Pattern unionPattern_1 = Pattern.compile("([^(]*?\\([\\s\\S]*?\\)[\\s\\S]*?)\\,([^(]*?\\([\\s\\S]*?\\)[\\s\\S]*?)\\,([\\s\\S]*)");

    private static final Pattern slicerPattern1_1 = Pattern.compile("(?i)CrossJoin\\(([\\s\\S]*?)\\)");
    private static final Pattern slicerPattern1_2 = Pattern.compile("(?i)[^,]*,([^,]*),([\\s\\S]*)");

    private static final Pattern slicerPattern2 = Pattern.compile("(?i)\\.unique_name");

    @Override
    public String apply(String mdx) {
        if (!super.available(mdx)) {
            return mdx;
        }

        mdx = mdx.replace("NonEmpty(", "Filter(");
        Matcher m;
        mdx = reconstructMultiArgsCrossJoin(mdx);
        mdx = replace2rdArgOfNonEmpty(mdx);
        m = slicerPattern2.matcher(mdx);
        if (m.find()) {
            mdx = mdx.replace(m.group(0), ".UniqueName");
        }
        mdx = reconstructMultiArgsUnion(mdx);
        setCompleted(true);
        return mdx;
    }

    private String replace2rdArgOfNonEmpty(String mdx) {
        int filterFunIndex = mdx.contains("Filter(") ? mdx.indexOf("Filter(") + 6 : -1;
        int endIndex = mdx.indexOf("set __XLExistingRangeMembers");
        while (filterFunIndex != -1 && filterFunIndex < endIndex) {
            mdx = searchAndReplace2ndArg(mdx, filterFunIndex);
            filterFunIndex = mdx.indexOf("Filter(", filterFunIndex) != -1 ? mdx.indexOf("Filter(", filterFunIndex) + 6 : -1;
            endIndex = mdx.indexOf("set __XLExistingRangeMembers");
        }
        return mdx;
    }

    private String searchAndReplace2ndArg(String mdx, int startIndex) {
        int discoveredLeftBracket = 0, discoveredRightBracket = 0, commaIndex = -1, endIndex = mdx.indexOf("set __XLExistingRangeMembers");;
        for (int i = startIndex; i < endIndex; i++) {
            char currentChar = mdx.charAt(i);
            if (currentChar == '(') {
                discoveredLeftBracket++;
            } else if (currentChar == ')') {
                discoveredRightBracket++;
            } else if (currentChar == ',') {
                if (discoveredLeftBracket == discoveredRightBracket + 1) {
                    commaIndex = i;
                }
            }
            if (discoveredRightBracket != 0 && discoveredLeftBracket == discoveredRightBracket) {
                //Needs to keep the CrossJoin in the second argument, otherwise an empty result would result in NullMember error in CellRequest making.
                String secondArg = mdx.substring(commaIndex + 1, i);
                secondArg = secondArg.replaceAll("NonEmptyCrossJoin", "CrossJoin");
                mdx = mdx.substring(0, commaIndex + 1) + "not isEmpty(" + secondArg + ".Item(0))" + mdx.substring(i);
                break;
            }
        }
        return mdx;
    }

    private String reconstructMultiArgsCrossJoin(String mdx) {
        mdx = mdx.replaceAll("NonEmptyCrossJoin", "CrossJoin");
        int crossJoinFunIndex = mdx.indexOf("CrossJoin(");
        int endIndex = mdx.indexOf("set __XLExistingRangeMembers");
        boolean reconstructed = false;
        String searchFunName = "CrossJoin";
        String replaceFunName = "NonEmptyCrossJoin";
        while (crossJoinFunIndex != -1 && crossJoinFunIndex < endIndex) {
            ReplaceResult replaceResult = replaceMultiArgsFun(mdx, crossJoinFunIndex, searchFunName, replaceFunName);
            mdx = replaceResult.mdx;
            reconstructed = reconstructed || replaceResult.reconstructed;
            //Find start position of next crossjoin function
            crossJoinFunIndex = mdx.indexOf(searchFunName, crossJoinFunIndex + replaceResult.nextStartIndex);
            endIndex = mdx.indexOf("set __XLExistingRangeMembers");
        }
        if (reconstructed) {
            //if reconstructed, there will be NonEmptyCrossJoin and CrossJoin in the mdx statement
            mdx = mdx.replaceAll("NonEmptyCrossJoin", "CrossJoin");
        }
        mdx = mdx.replaceAll("CrossJoin", "NonEmptyCrossJoin");
        return mdx;
    }

    private String reconstructMultiArgsUnion(String mdx) {
        String funName = "Union";
        int unionFunIndex = mdx.indexOf(funName + "(");
        int endIndex = mdx.indexOf("set __XLExistingRangeMembers");
        while (unionFunIndex != -1 && unionFunIndex < endIndex) {
            ReplaceResult replaceResult = replaceMultiArgsFun(mdx, unionFunIndex, funName, funName);
            mdx = replaceResult.mdx;
            //Find start position of next union function
            unionFunIndex = mdx.indexOf(funName + "(", unionFunIndex + replaceResult.nextStartIndex);
            endIndex = mdx.indexOf("set __XLExistingRangeMembers");
        }
        return mdx;
    }

    /**
     * This method replaces functions with more than 2 args to nested form with only 2 args for each level
     * @param mdx
     * @param startIndex
     * @param searchFunName the name of function to search in the mdx statement
     * @param replaceFunName the name of function to replace the function searched. Usually the same as searchFunName except for CrossJoin
     * @return
     */
    private ReplaceResult replaceMultiArgsFun(String mdx,
                                                      int startIndex,
                                                      String searchFunName,
                                                      String replaceFunName
                                                      ) {
        int discoveredLeftBracket = 0, discoveredRightBracket = 0, nextStartIndx = startIndex, endIndex = mdx.indexOf("set __XLExistingRangeMembers");
        boolean reconstructed = false;
        List<Integer> argStartIndexList = new ArrayList<>();
        List<Integer> argEndIndexList = new ArrayList<>();
        for (int i = startIndex; i < endIndex; i++) {
            char currentChar = mdx.charAt(i);
            if (currentChar == '(') {
                discoveredLeftBracket++;
            } else if (currentChar == ')') {
                discoveredRightBracket++;
            } else if (currentChar == ',') {
                if (discoveredLeftBracket == discoveredRightBracket + 1) {
                    if (!argEndIndexList.isEmpty()) {
                        argStartIndexList.add(argEndIndexList.get(argEndIndexList.size() - 1) + 1);
                    } else {
                        argStartIndexList.add(startIndex + searchFunName.length() + 1);
                    }
                    argEndIndexList.add(i);
                }
            }
            if (discoveredRightBracket != 0 && discoveredLeftBracket == discoveredRightBracket) {
                argStartIndexList.add(argEndIndexList.get(argEndIndexList.size() - 1) + 1);
                argEndIndexList.add(i);
                if (argStartIndexList.size() > 2) {
                    reconstructed = true;
                    StringBuilder newCrossJoinBuilder = new StringBuilder();
                    newCrossJoinBuilder.append(replaceFunName + "(" +
                            mdx.substring(argStartIndexList.get(0), argEndIndexList.get(0)) +
                            ", " +
                            mdx.substring(argStartIndexList.get(1), argEndIndexList.get(1)) +
                            ")");
                    for (int j = 2; j < argStartIndexList.size(); j++) {
                        newCrossJoinBuilder.insert(0, replaceFunName + "(");
                        newCrossJoinBuilder.append(",").append(mdx, argStartIndexList.get(j), argEndIndexList.get(j)).append(")");
                    }
                    nextStartIndx = newCrossJoinBuilder.toString().length();
                    mdx = mdx.substring(0, startIndex) + newCrossJoinBuilder.toString() + mdx.substring(i + 1);
                }
                break;
            }
        }
        return new ReplaceResult(mdx, reconstructed, nextStartIndx);
    }

    @Value
    class ReplaceResult {
        String mdx;
        boolean reconstructed;
        int nextStartIndex;
    }
}
