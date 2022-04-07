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


package io.kylin.mdx.insight.core.model.generic;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.*;

@Data
public class JoinTableInfo {

    private Map<TwoJoinTable, JoinCondition> joinConditionMap;

    private TableNode rootTableNode;

    @Data
    public static class TableNode implements Comparable<TableNode> {

        private String tableName;

        private List<String> primaryKeys;

        private List<TableNode> childNodes;

        public TableNode(String tableName) {
            this.tableName = tableName;
        }

        public void addChildNode(TableNode childNode) {
            if (childNodes == null) {
                childNodes = new LinkedList<>();
            }

            childNodes.add(childNode);
        }

        public void addPrimaryKey(List<String> pks) {
            if (this.primaryKeys == null) {
                this.primaryKeys = new ArrayList<>(2);
            }

            for (String pk : pks) {
                this.primaryKeys.add(StringUtils.substringAfter(pk, "."));
            }
        }

        @Override
        public int compareTo(TableNode otherNode) {
            return this.tableName.compareTo(otherNode.tableName);
        }
    }

    @Data
    @AllArgsConstructor
    public static class TwoJoinTable {

        private String leftTable;

        private String rightTable;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TwoJoinTable that = (TwoJoinTable) o;
            return leftTable.equals(that.leftTable) &&
                    rightTable.equals(that.rightTable);
        }

        @Override
        public int hashCode() {
            return Objects.hash(leftTable, rightTable);
        }
    }

    @Data
    public static class JoinCondition {

        private String joinType;

        //Pair<L,R>中，L、R分别表示左右表字段，形如：KYLIN_SALES.ACCOUNT_COUNTRY
        private List<MutablePair<String, String>> conditions;

        public JoinCondition(String joinType) {
            this.joinType = joinType;
        }

        public void addJoinCondition(String leftTblAliasWithCol, String rightTblAliasWithCol) {
            if (conditions == null) {
                conditions = new LinkedList<>();
            }

            conditions.add(
                    new MutablePair<>(StringUtils.substringAfter(leftTblAliasWithCol, "."),
                            StringUtils.substringAfter(rightTblAliasWithCol, "."))
            );
        }

    }


}
