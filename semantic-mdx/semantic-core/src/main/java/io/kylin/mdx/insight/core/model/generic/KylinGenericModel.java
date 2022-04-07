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


import lombok.Data;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.*;

@Data
public class KylinGenericModel {

    private String modelName;

    private Long lastModified;

    private String signature;

    /**
     * 实际表
     */
    private MutablePair<String, ActualTable> factTable;

    /**
     * 实际表 映射 表别名列表
     */
    private Map<ActualTable, List<String>> actTbl2TblAliases;

    /**
     * 表别名 映射 实际表
     */
    private Map<String, ActualTable> tblAlias2ActTbl;

    /**
     * 表关联之间的基础信息
     */
    private JoinTableInfo joinTableInfo;

    /**
     * 表关联原始信息
     */
    private List<RawJoinTable> rawJoinTableInfo;

    /**
     * 列鉴别名 映射 列的基本属性
     */
    private Map<ActualTableCol, ColumnInfo> actCol2ColInfo;

    /**
     * data模型中的维度
     */
    private Set<ColumnIdentity> modelDimensions;

    /**
     * data模型中的度量
     */
    private Set<ColumnIdentity> modelMeasures;

    /**
     * cube模型中的维度
     */
    private Set<ColumnIdentity> cubeDimensions;

    /**
     * cube模型中的度量
     */
    private List<CubeMeasure> cubeMeasures;

    /**
     * cube模型中的层级 hierarchy
     */
    private List<HierachyInfo> hierachyInfos;


    public Map<Integer, ColumnIdentity> createId2Column(Set<ColumnIdentity> cubeDimensions) {
        Map<Integer, ColumnIdentity> id2ColumnMap = new HashMap<>();

        for (ColumnIdentity columnIdentity : cubeDimensions) {
            id2ColumnMap.put(columnIdentity.getId(), columnIdentity);
        }

        return id2ColumnMap;
    }

    public Map<String, List<ColumnIdentity>> createTbl2DimensionsMap() {
        Map<String, List<ColumnIdentity>> tbl2DimensionsMap = new HashMap<>();

        for (ColumnIdentity columnIdentity : cubeDimensions) {
            List<ColumnIdentity> columnIdentities = tbl2DimensionsMap.get(columnIdentity.getTableAlias());
            if (columnIdentities == null) {
                columnIdentities = new LinkedList<>();
                tbl2DimensionsMap.put(columnIdentity.getTableAlias(), columnIdentities);
            }

            columnIdentities.add(columnIdentity);
        }

        return tbl2DimensionsMap;
    }

    public Map<String, List<HierachyInfo>> createTable2HierarchyInfoMap() {
        Map<String, List<HierachyInfo>> tbl2HierarchyMap = new HashMap<>();

        for (HierachyInfo hierachyInfo : hierachyInfos) {
            //the first level in hierarchy decide which dimension it belongs to
            if (hierachyInfo.getHierachy().get(0) == null) {
                continue;
            }
            String tableAlias = hierachyInfo.getHierachy().get(0).getTableAlias();

            List<HierachyInfo> hierarchies = tbl2HierarchyMap.get(tableAlias);
            if (hierarchies == null) {
                hierarchies = new LinkedList<>();
                tbl2HierarchyMap.put(tableAlias, hierarchies);
            }
            hierarchies.add(hierachyInfo);
        }

        return tbl2HierarchyMap;
    }

    public void mapTblAliasAndActualTable(String tableAlias, ActualTable actualTable) {
        putTblAlias2ActualTable(tableAlias, actualTable);
        putActualTable2TblAlias(tableAlias, actualTable);
    }

    private void putTblAlias2ActualTable(String tableAlias, ActualTable actualTable) {
        if (this.tblAlias2ActTbl == null) {
            this.tblAlias2ActTbl = new HashMap<>();
        }

        this.tblAlias2ActTbl.put(tableAlias, actualTable);
    }

    private void putActualTable2TblAlias(String tableAlias, ActualTable actualTable) {
        if (this.actTbl2TblAliases == null) {
            this.actTbl2TblAliases = new HashMap<>();
        }

        List<String> aliases = actTbl2TblAliases.get(actualTable);
        if (aliases == null) {
            aliases = new ArrayList<>(2);
            actTbl2TblAliases.put(actualTable, aliases);
        }

        aliases.add(tableAlias);
    }

}
