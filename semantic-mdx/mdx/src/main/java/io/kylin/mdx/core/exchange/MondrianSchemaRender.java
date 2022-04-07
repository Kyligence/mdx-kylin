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


package io.kylin.mdx.core.exchange;

import io.kylin.mdx.insight.core.model.generic.*;
import io.kylin.mdx.core.mondrian.MdnCube;
import io.kylin.mdx.core.mondrian.MdnSchema;
import io.kylin.mdx.core.mondrian.dimension.MdnAttribute;
import io.kylin.mdx.core.mondrian.dimension.MdnDimension;
import io.kylin.mdx.core.mondrian.measuregroup.MdnMeasure;
import io.kylin.mdx.core.mondrian.measuregroup.MdnMeasureGroup;
import io.kylin.mdx.core.mondrian.physicalschema.MdnPhysicalSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.*;


/**
  convert schema from KYLIN Cube
 */
@Slf4j
public class MondrianSchemaRender {

    public MdnSchema render(KylinGenericModel genericModel) {

        MdnSchema mdnSchema = new MdnSchema();

        mdnSchema.setName(genericModel.getModelName());

        createPhysicalSchema(genericModel, mdnSchema);

        createCube(genericModel, mdnSchema);

        return mdnSchema;
    }

    private void createCube(KylinGenericModel genericModel, MdnSchema mdnSchema) {

        MdnCube mdnCube = new MdnCube();

        mdnCube.setName(genericModel.getModelName());
        mdnCube.setDefaultMeasure(genericModel.getCubeMeasures().get(0).getMeasureName());

        createDimension(mdnCube, genericModel);

        createMeasureGroup(mdnCube, genericModel);

        mdnSchema.setMdnCube(mdnCube);
    }

    private void createMeasureGroup(MdnCube mdnCube, KylinGenericModel genericModel) {
        MdnMeasureGroup mdnMeasureGroup = new MdnMeasureGroup();

        mdnMeasureGroup.setTable(genericModel.getFactTable().getLeft());

        List<MdnMeasure> mdnMeasures = new LinkedList<>();
        for (CubeMeasure cubeMeasure : genericModel.getCubeMeasures()) {
            // not support topN, percentile and corr
            AggFunctionTyper aggFunctionTyper = null;
            try {
                aggFunctionTyper = AggFunctionTyper.valueOf(cubeMeasure.getExpression());
            } catch (IllegalArgumentException e) {
                log.warn("Unexpected aggregation function: {}", cubeMeasure.getExpression());
            }
            if (aggFunctionTyper == null) {
                continue;
            }
            MdnMeasure mdnMeasure = new MdnMeasure();

            mdnMeasure.setName(cubeMeasure.getMeasureName());
            if (cubeMeasure.getColMeasured() != null && !"constant".equals(cubeMeasure.getTableColumnStr())) {
                mdnMeasure.setColumn(cubeMeasure.getColMeasured().getColName());
                mdnMeasure.setTable(cubeMeasure.getColMeasured().getTableAlias());
            }
            mdnMeasure.setAggregator(aggFunctionTyper.getMdnFuncType());
            mdnMeasure.setFormatString(aggFunctionTyper.getFormatString(cubeMeasure.getDataType()));

            mdnMeasures.add(mdnMeasure);
        }
        mdnMeasureGroup.setMeasureWrapper(new MdnMeasureGroup.MeasureWrapper(mdnMeasures));
        List<MdnMeasureGroup.MdnFactLink> mdnFactLinks = new LinkedList<>();
        for (MdnDimension mdnDimension : mdnCube.getMdnDimensionWrapper().getMdnDimensions()) {
            mdnFactLinks.add(new MdnMeasureGroup.MdnFactLink(mdnDimension.getName()));
        }
        mdnMeasureGroup.setMdnDimensionLinkWrapper(new MdnMeasureGroup.MdnDimensionLinkWrapper(mdnFactLinks, null, null));
        mdnCube.setMdnMeasureGroupWrapper(new MdnCube.MdnMeasureGroupWrapper(Arrays.asList(mdnMeasureGroup)));
    }

    private void createDimension(MdnCube mdnCube, KylinGenericModel genericModel) {
        List<MdnDimension> mdnDimensions = new LinkedList<>();

        Map<String, List<HierachyInfo>> table2HierarchyInfoMap = genericModel.createTable2HierarchyInfoMap();

        Map<String, List<ColumnIdentity>> tbl2DimensionsMap = genericModel.createTbl2DimensionsMap();

        for (Map.Entry<String, List<ColumnIdentity>> entry : tbl2DimensionsMap.entrySet()) {
            String tableAlias = entry.getKey();

            MdnDimension mdnDimension = new MdnDimension();
            mdnDimension.setName(tableAlias);
            mdnDimension.setTable(tableAlias);

            List<MdnAttribute> mdnAttributes = new LinkedList<>();

            List<HierachyInfo> hierarchyOfThisTable = table2HierarchyInfoMap.get(tableAlias);
            if (hierarchyOfThisTable != null && hierarchyOfThisTable.size() != 0) {
                mdnDimension.setMdnHierarchies(hierarchyOfThisTable);
            }

            List<ColumnIdentity> colAttribute = entry.getValue();
            buildDimAttributesNotInHierarchy(colAttribute, mdnAttributes);

            mdnDimension.setAttributeWrapper(new MdnDimension.AttributeWrapper(mdnAttributes));
            mdnDimension.setKey(mdnAttributes.get(0).getName());

            mdnDimensions.add(mdnDimension);
        }

        mdnCube.setMdnDimensionWrapper(new MdnCube.MdnDimensionWrapper(mdnDimensions));
    }

    private void buildDimAttributesNotInHierarchy(List<ColumnIdentity> colAttribute,
                                                  List<MdnAttribute> mdnAttributes) {
        for (ColumnIdentity colLevel : colAttribute) {
            MdnAttribute mdnAttr = new MdnAttribute();
            mdnAttr.setName(colLevel.getColName());
            mdnAttr.setKeyColumn(colLevel.getColName());

            mdnAttributes.add(mdnAttr);
        }
    }

    private void createPhysicalSchema(KylinGenericModel genericModel, MdnSchema mdnSchema) {

        Map<String, ActualTable> tblAlias2ActTbl = genericModel.getTblAlias2ActTbl();

        List<MdnPhysicalSchema.MdnTable> mdnTables = new LinkedList<>();
        createMondrianTables(genericModel.getJoinTableInfo().getRootTableNode(), mdnTables,
                tblAlias2ActTbl, genericModel.getFactTable().getLeft());

        List<MdnPhysicalSchema.MdnLink> mdnLinks = new LinkedList<>();
        createMondrianLinks(genericModel.getJoinTableInfo().getJoinConditionMap(), mdnLinks, genericModel);

        mdnSchema.setMdnPhysicalSchema(new MdnPhysicalSchema(mdnTables, mdnLinks));
    }

    private void createMondrianLinks(Map<JoinTableInfo.TwoJoinTable, JoinTableInfo.JoinCondition> joinConditionMap,
                                     List<MdnPhysicalSchema.MdnLink> mdnLinks, KylinGenericModel genericModel) {

        for (Map.Entry<JoinTableInfo.TwoJoinTable, JoinTableInfo.JoinCondition> entry : joinConditionMap.entrySet()) {

            JoinTableInfo.JoinCondition joinCond = entry.getValue();
            MdnPhysicalSchema.MdnLink link = new MdnPhysicalSchema.MdnLink(entry.getKey().getLeftTable(), entry.getKey().getRightTable(), joinCond.getJoinType());

            List<MutablePair<String, String>> conditions = joinCond.getConditions();

            if (conditions == null || conditions.size() == 0) {
                throw new RuntimeException("The table [" + entry.getKey().getLeftTable() + "] and table [" + entry.getKey().getRightTable() + "] have no join key, please check it.");
            }

            if (conditions.size() == 1) {
                link.setForeignKeyColumn(conditions.get(0).getLeft());
            } else {
                link.addForeignKeys(conditions);
            }

            mdnLinks.add(link);
        }

    }

    private void createMondrianTables(JoinTableInfo.TableNode parentTblNode, List<MdnPhysicalSchema.MdnTable> mdnTables,
                                      Map<String, ActualTable> tblAlias2ActTbl, String factTblAlias) {

        String tblAlias = parentTblNode.getTableName();
        ActualTable actualTable = tblAlias2ActTbl.get(tblAlias);
        MdnPhysicalSchema.MdnTable mdnTable = new MdnPhysicalSchema.MdnTable();
        mdnTable.setAlias(tblAlias);
        mdnTable.setName(actualTable.getTableName());
        mdnTable.setSchema(actualTable.getSchema());

        List<String> primaryKeys = parentTblNode.getPrimaryKeys();
        if (!factTblAlias.equalsIgnoreCase(tblAlias) &&
                (primaryKeys == null || primaryKeys.size() == 0)) {
            throw new RuntimeException("The table:[" + tblAlias + "] need the primary key, please set it.");
        }

        if (primaryKeys == null) {
            mdnTable.setColumn("");
        } else if (primaryKeys.size() == 1) {
            mdnTable.setColumn(primaryKeys.get(0));
        } else {
            mdnTable.addPrimaryKeys(primaryKeys);
        }
        mdnTables.add(mdnTable);

        List<JoinTableInfo.TableNode> childNodes = parentTblNode.getChildNodes();
        if (childNodes == null || childNodes.size() == 0) {
            return;
        }

        for (JoinTableInfo.TableNode childTblNode : childNodes) {
            createMondrianTables(childTblNode, mdnTables, tblAlias2ActTbl, factTblAlias);
        }

    }


    private final static String INT_FORMAT = "#,###";

    private final static String FLOAT_FORMAT = "#,###.00";

    public enum AggFunctionTyper {


        COUNT("count"),

        SUM("sum"),

        MIN("min"),

        MAX("max"),

        COUNT_DISTINCT("distinct-count"),

        AVG("avg");

        private String mdnFuncType;

        AggFunctionTyper(String mdnFuncType) {
            this.mdnFuncType = mdnFuncType;
        }

        public String getMdnFuncType() {
            return this.mdnFuncType;
        }

        public String getFormatString(String returnType) {
            if (COUNT.name().equals(this.name())
                    || COUNT_DISTINCT.name().equals(this.name())) {
                return INT_FORMAT;
            }

            if (returnType.equalsIgnoreCase("int")
                    || returnType.equalsIgnoreCase("bigint")
                    || returnType.equalsIgnoreCase("tinyint")) {

                return INT_FORMAT;

            } else {
                return FLOAT_FORMAT;
            }
        }

    }
}
