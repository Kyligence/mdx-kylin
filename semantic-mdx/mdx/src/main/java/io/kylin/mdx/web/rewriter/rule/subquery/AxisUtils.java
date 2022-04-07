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


package io.kylin.mdx.web.rewriter.rule.subquery;

import mondrian.olap.Exp;
import mondrian.olap.Id;
import mondrian.olap.Util;

import java.util.List;
import java.util.stream.Collectors;

public class AxisUtils {

    private static final String MBR_REPLACE_ID = "\\$\\{ID}";

    private static final String MBR_EXPRESSION = "Aggregate({${ID}})";

    private static final String MEMBERS_IN_SET = "Ascendants(${ID}), Descendants(${ID})";

    private static final String HIERARCHIZE_ID = "Hierarchize({${ID}})";

    private static final String SET_EXPRESSION = "VisualTotals(Distinct(${ID}))";

    /**
     * 生成一个 Member 表达式
     */
    public static String makeMbrExp(RuleContext context, String hierarchy) {
        List<Id> members = context.allMembers.get(hierarchy);
        String memberStr = members.stream().map(Id::toString).collect(Collectors.joining(", "));
        return MBR_EXPRESSION.replaceAll("\\$\\{ID}", memberStr);
    }

    /**
     * 生成一个 Set 表达式
     */
    public static String makeSetExp(RuleContext context, String hierarchy) {
        // 构造 SET EXP
        String hierarchyStr;
        if (context.allMembers.containsKey(hierarchy)) {
            String membersStr = context.allMembers.remove(hierarchy)
                    .stream()
                    .map(id -> MEMBERS_IN_SET.replaceAll(MBR_REPLACE_ID, id.toString()))
                    .collect(Collectors.joining(", "));
            hierarchyStr = HIERARCHIZE_ID.replaceAll(MBR_REPLACE_ID, membersStr);
        } else {
            List<Exp> functions = context.allFunctions.get(hierarchy);
            Exp function = functions.remove(functions.size() - 1);
            if (functions.size() == 0) {
                context.allFunctions.remove(hierarchy);
            }
            hierarchyStr = Util.unparse(function);
        }
//        if (hierarchyStr.contains("CrossJoin")) {
//            return "Distinct(${ID})".replaceAll(MBR_REPLACE_ID, hierarchyStr);
//        }
        return SET_EXPRESSION.replaceAll(MBR_REPLACE_ID, hierarchyStr);
    }

}
