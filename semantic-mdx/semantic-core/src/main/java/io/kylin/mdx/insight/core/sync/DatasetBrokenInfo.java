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


package io.kylin.mdx.insight.core.sync;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.util.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class DatasetBrokenInfo {

    private List<BrokenInfo> models;

    @JsonProperty("dimension_tables")
    @JSONField(name = "dimension_tables")
    private List<BrokenInfo> dimensionTables;

    @JsonProperty("hierarchys")
    @JSONField(name = "hierarchys")
    private List<BrokenInfo> hierarchies;

    @JsonProperty("dimension_cols")
    @JSONField(name = "dimension_cols")
    private List<BrokenInfo> dimensionCols;

    private List<BrokenInfo> measures;

    @JsonProperty("name_columns")
    @JSONField(name = "name_columns")
    private List<BrokenInfo> nameColumns;

    @JsonProperty("value_columns")
    @JSONField(name = "value_columns")
    private List<BrokenInfo> valueColumns;

    @JsonProperty("property")
    @JSONField(name = "property")
    private List<BrokenInfo> property;

    public List<String> extractBrokenModels() {
        if (Utils.isCollectionEmpty(models)) {
            return Collections.emptyList();
        }

        return models.stream().map(BrokenInfo::getName).collect(Collectors.toList());
    }

    public void setBrokenModelList(Set<String> modelNames) {
        if (Utils.isCollectionEmpty(modelNames)) {
            return;
        }

        this.models =
                modelNames.stream()
                        .map(name -> new BrokenInfo(name, DatasetBrokenType.MODEL_DELETED, null))
                        .collect(Collectors.toList());


    }

    public void addBrokenModelName(String modelName) {
        if (models == null) {
            models = new LinkedList<>();
        }

        models.add(new BrokenInfo(modelName, DatasetBrokenType.MODEL_DELETED, null));
    }

    public void addCommonTableBroken(String modelName, String tableAlias) {
        if (dimensionTables == null) {
            dimensionTables = new LinkedList<>();
        }

        dimensionTables.add(new BrokenInfo(Utils.concat(SemanticConstants.DOT, modelName, tableAlias),
                DatasetBrokenType.COMMON_TABLE_DELETED, null));
    }

    public void addBridgeDimTableBroken(String modelName, String tableAlias) {
        if (dimensionTables == null) {
            dimensionTables = new LinkedList<>();
        }

        dimensionTables.add(new BrokenInfo(Utils.concat(SemanticConstants.DOT, modelName, tableAlias),
                DatasetBrokenType.BRIDGE_TABLE_DELETED, null));

    }

    public void addBrokenHierarchy(String model, String table, String column, String weightColumn, String hierarchy, int brokenStatus) {
        if (hierarchies == null) {
            hierarchies = new LinkedList<>();
        }

        DatasetBrokenType brokenType;
        switch (brokenStatus) {
            case 0x10:
                brokenType = DatasetBrokenType.HIERARCHY_WEIGHT_COL_DELETED;
                break;
            case 0x11:
                brokenType = DatasetBrokenType.HIERARCHY_DIM_WEIGHT_COL_DELETED;
                break;
            default:
                brokenType = DatasetBrokenType.HIERARCHY_DIM_COL_DELETED;
                break;
        }

        hierarchies.add(new BrokenInfo(
                Utils.concat(SemanticConstants.DOT, model, table, hierarchy),
                brokenType,
                Utils.concat(SemanticConstants.DOT, model, table, column, weightColumn)));
    }

    public void addBrokenManyToManyKey(String model, String tableAlias, String colName) {
        if (dimensionCols == null) {
            dimensionCols = new LinkedList<>();
        }

        dimensionCols.add(new BrokenInfo(Utils.concat(SemanticConstants.DOT, model, tableAlias, colName),
                DatasetBrokenType.MANY_TO_MANY_KEY_DELETED, null));
    }

    public void addBrokenNameColumn(String model, String table, String column, String nameColumn) {
        if (nameColumns == null) {
            nameColumns = new LinkedList<>();
        }

        nameColumns.add(new BrokenInfo(Utils.concat(SemanticConstants.DOT, model, table, column),
                DatasetBrokenType.HIERARCHY_DIM_COL_DELETED, Utils.concat(SemanticConstants.DOT, model, table, nameColumn)));
    }

    public void addBrokenValueColumn(String model, String table, String column, String valueColumn) {
        if (valueColumns == null) {
            valueColumns = new LinkedList<>();
        }

        valueColumns.add(new BrokenInfo(Utils.concat(SemanticConstants.DOT, model, table, column),
                DatasetBrokenType.HIERARCHY_DIM_COL_DELETED, Utils.concat(SemanticConstants.DOT, model, table, valueColumn)));
    }

    public void addBrokenProperties(String model, String table, String column, String properties) {
        if (property == null) {
            property = new LinkedList<>();
        }

        property.add(new BrokenInfo(Utils.concat(SemanticConstants.DOT, model, table, column),
                DatasetBrokenType.HIERARCHY_DIM_COL_DELETED, Utils.concat(SemanticConstants.DOT, model, table, properties)));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BrokenInfo {

        private String name;

        @JsonProperty("type")
        private DatasetBrokenType datasetBrokenType;

        private String obj;

    }

}
