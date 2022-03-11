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

import io.kylin.mdx.insight.core.entity.CustomHierarchy;
import io.kylin.mdx.insight.core.entity.NamedDimCol;
import io.kylin.mdx.insight.core.model.generic.ColumnIdentity;
import lombok.Data;

@Data
public class DatasetValidateResult {

    private DatasetValidateType datasetValidateType;

    private DatasetBrokenInfo brokenInfo = new DatasetBrokenInfo();

    public void addBrokenModelName(String modelName) {
        brokenInfo.addBrokenModelName(modelName);

    }

    public void addCommonTableBroken(String modelName, String tableAlias) {
        brokenInfo.addCommonTableBroken(modelName, tableAlias);
    }

    public void addBridgeDimTableBroken(String modelName, String tableAlias) {
        brokenInfo.addBridgeDimTableBroken(modelName, tableAlias);
    }

    public void addBrokenHierarchy(CustomHierarchy customHierarchy, int brokenStatus) {
        String model = customHierarchy.getModel();
        String table = customHierarchy.getDimTable();
        String hierarchy = customHierarchy.getName();
        String column = customHierarchy.getDimCol();
        String weightCol = customHierarchy.getWeightCol();

        brokenInfo.addBrokenHierarchy(model, table, column, weightCol, hierarchy, brokenStatus);
    }

    public void addBrokenNameColumn(NamedDimCol namedDimCol) {
        String model = namedDimCol.getModel();
        String table = namedDimCol.getDimTable();
        String nameColumn = namedDimCol.getNameColumn();
        String column = namedDimCol.getDimCol();

        brokenInfo.addBrokenNameColumn(model, table, column, nameColumn);
    }

    public void addBrokenValueColumn(NamedDimCol namedDimCol) {
        String model = namedDimCol.getModel();
        String table = namedDimCol.getDimTable();
        String valueColumn = namedDimCol.getValueColumn();
        String column = namedDimCol.getDimCol();

        brokenInfo.addBrokenValueColumn(model, table, column, valueColumn);
    }

    public void addBrokenProperties(NamedDimCol namedDimCol, String propertyCol) {
        String model = namedDimCol.getModel();
        String table = namedDimCol.getDimTable();
        String column = namedDimCol.getDimCol();

        brokenInfo.addBrokenProperties(model, table, column, propertyCol);
    }

    public void addBrokenManyToManyKey(String model, ColumnIdentity primaryKey) {

        brokenInfo.addBrokenManyToManyKey(model, primaryKey.getTableAlias(), primaryKey.getColName());
    }

    public enum DatasetValidateType {

        /**
         * 数据集损坏
         */
        BROKEN,

        /**
         * 数据集自行修复
         */
        SELF_FIX,

        /**
         * 数据集正常
         */
        NORMAL

    }

}
