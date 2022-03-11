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

import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.CalculatedMemberNonEmptyBehaviorMeasure;
import io.kylin.mdx.insight.core.model.generic.RawJoinTable;
import io.kylin.mdx.insight.core.model.semantic.SemanticDataset;
import io.kylin.mdx.insight.core.model.semantic.SemanticDataset.AugmentedModel.AugmentDimensionTable;
import io.kylin.mdx.insight.core.model.semantic.SemanticDataset.AugmentedModel.AugmentMeasure;
import io.kylin.mdx.core.mondrian.*;
import io.kylin.mdx.core.mondrian.calculation.CalculatedMember;
import io.kylin.mdx.core.mondrian.calculation.MdnFormula;
import io.kylin.mdx.core.mondrian.calculation.MdnNamedSet;
import io.kylin.mdx.core.mondrian.dimension.MdnAttribute;
import io.kylin.mdx.core.mondrian.dimension.MdnDimension;
import io.kylin.mdx.core.mondrian.dimension.MdnHierarchy;
import io.kylin.mdx.core.mondrian.measuregroup.MdnMeasure;
import io.kylin.mdx.core.mondrian.measuregroup.MdnMeasureGroup;
import io.kylin.mdx.core.mondrian.physicalschema.MdnPhysicalSchema;
import io.kylin.mdx.core.mondrian.MdnCube;
import io.kylin.mdx.core.mondrian.MdnCustomTranslation;
import io.kylin.mdx.core.mondrian.MdnCustomTranslationWrapper;
import io.kylin.mdx.core.mondrian.MdnForeignKey;
import io.kylin.mdx.core.mondrian.MdnKeyColumn;
import io.kylin.mdx.core.mondrian.MdnSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.kylin.mdx.insight.core.support.SemanticUtils.*;

/**
 * convert schema from MDX dataset
 */
@Slf4j
public class MondrianSchemaRenderV2 {

    private DatasetConverter getDatasetConverter(int modelNum) {
        if (modelNum == 1) {
            return new SingleModelConverter();
        } else {
            return new MultipleModelConverter();
        }
    }

    public MdnSchema create(SemanticDataset dataset) {
        assert dataset.getModels() != null && dataset.getModels().size() > 0;

        int modelNum = dataset.getModels().size();

        DatasetConverter datasetConverter = getDatasetConverter(modelNum);
        return datasetConverter.convertDataset2Schema(dataset);
    }


    public abstract class DatasetConverter {

        protected Map<String, Integer> mapModel2Position = new HashMap<>(16);

        // key is modelName.dimTableName
        // note that fact table also in dimTablePool
        protected Map<String, AugmentDimensionTable> dimTablePool = new LinkedHashMap<>(32);

        // key is modelName.dimTableName, value is commonModelName.dimTableName
        protected Map<String, String> mapModelTable2CommonModelTable = new HashMap<>(32);

        protected Map<String, Set<String>> mapModel2UsedTables = new HashMap<>(32);

        MdnSchema convertDataset2Schema(SemanticDataset dataset) {
            MdnSchema mdnSchema = new MdnSchema();

            mdnSchema.setName(dataset.getDatasetName());

            resolveModelDimension(dataset);

            createPhysicalSchema(dataset, mdnSchema);

            createCube(dataset, mdnSchema);

            return mdnSchema;
        }

        // first, set model position
        // second, build full dimension table pool
        // third, resolve model relation, and remove common table follower
        protected abstract void resolveModelDimension(SemanticDataset dataset);

        void createPhysicalSchema(SemanticDataset dataset, MdnSchema mdnSchema) {
            MdnPhysicalSchema physicalSchema = new MdnPhysicalSchema();
            initiateModelUsedTables(dataset);
            createPhysicalTables(dataset, physicalSchema);
            createPhysicalLinks(dataset, physicalSchema);
            mdnSchema.setMdnPhysicalSchema(physicalSchema);
        }

        void initiateModelUsedTables(SemanticDataset dataset) {
            dataset.getModels().forEach(model -> {
                Set<String> currentModelUsedTables = new HashSet();
                if (model.getLookups() != null) {
                    Map<String, RawJoinTable> mapAlias2LookupTable = new HashMap<>();
                    for (RawJoinTable rawJoinTable : model.getLookups()) {
                        mapAlias2LookupTable.put(rawJoinTable.getAlias(), rawJoinTable);
                    }
                    if (model.getDimensionTables() != null) {
                        for (AugmentDimensionTable dimensionTable : model.getDimensionTables()) {
                            currentModelUsedTables.add(dimensionTable.getName());
                            addTablesInLinkToFact(dimensionTable.getName(), model, currentModelUsedTables, mapAlias2LookupTable);
                        }
                    }
                }
                mapModel2UsedTables.put(model.getModelName(), currentModelUsedTables);
            });
        }

        void addTablesInLinkToFact(String tableAlias,
                                   SemanticDataset.AugmentedModel model,
                                   Set<String> currentModelUsedTables,
                                   Map<String, RawJoinTable> mapAlias2LookupTable
        ) {
            //fact table is as SCHEMA.TABLE
            String factTable = getSchemaAndTable(model.getFactTable())[1];
            while (!tableAlias.contentEquals(factTable)) {
                currentModelUsedTables.add(tableAlias);
                //the foreign_key is as TABLE.COLUMN
                tableAlias = getTableAndCol(mapAlias2LookupTable.get(tableAlias).getJoin().getForeign_key().get(0))[0];
            }
        }

        void createPhysicalTables(SemanticDataset dataset, MdnPhysicalSchema physicalSchema) {
            List<MdnPhysicalSchema.MdnTable> physicalTables = new ArrayList<>(16);
            dataset.getModels().forEach(model -> {
                // set fact table
                Set<String> currentModelUsedTables = mapModel2UsedTables.get(model.getModelName());
                String[] factTableWithSchema = getSchemaAndTable(model.getFactTable());
                int modelPosition = mapModel2Position.get(model.getModelName());
                // in order to distinguish different model with same table name
                String factTableAlias = factTableWithSchema[1] + "_" + modelPosition;
                physicalTables.add(new MdnPhysicalSchema.MdnTable(factTableWithSchema[1], factTableWithSchema[0], factTableAlias, null, null));
                // set lookup table
                if (model.getLookups() != null) {
                    for (RawJoinTable joinTable : model.getLookups()) {
                        if (!currentModelUsedTables.contains(joinTable.getAlias())) {
                            continue;
                        }
                        String modelTable = model.getModelName() + "." + joinTable.getAlias();
                        String commonModelTable = mapModelTable2CommonModelTable.get(modelTable);
                        // if model M1 has table A and model M2 has table A, they are common tables,
                        // if we choose M1.A as the representative, and M2.A as the follower, then follower will not show in physical table and dimension
                        if (commonModelTable == null || modelTable.equals(commonModelTable)) {
                            physicalTables.add(createPhysicalLookupTable(joinTable, modelPosition));
                        }
                    }
                }
            });

            physicalSchema.setMdnTables(physicalTables);
        }

        void createPhysicalLinks(SemanticDataset dataset, MdnPhysicalSchema physicalSchema) {
            List<MdnPhysicalSchema.MdnLink> physicalLinks = new ArrayList<>(16);
            dataset.getModels().forEach(model -> {
                Set<String> currentModelUsedTables = mapModel2UsedTables.get(model.getModelName());
                if (model.getLookups() != null) {
                    for (RawJoinTable joinTable : model.getLookups()) {
                        if (!currentModelUsedTables.contains(joinTable.getAlias())) {
                            continue;
                        }
                        physicalLinks.add(createPhysicalLink(joinTable, model.getModelName()));
                    }
                }
            });
            physicalSchema.setMdnLinks(physicalLinks);
        }

        MdnPhysicalSchema.MdnLink createPhysicalLink(RawJoinTable joinTable, String modelName) {
            List<String> primaryKeys = joinTable.getJoin().getPrimary_key();
            List<String> foreignKeys = joinTable.getJoin().getForeign_key();
            int keySize = primaryKeys.size();

            assert keySize >= 1;
            String[] pk_tableAndCol = getTableAndCol(primaryKeys.get(0));
            String[] fk_tableAndCol = getTableAndCol(foreignKeys.get(0));

            // if current table is common table follower, should replace it with common table representative
            pk_tableAndCol = replaceWithCommonPhysicalTableAlias(pk_tableAndCol, modelName);
            fk_tableAndCol = replaceWithCommonPhysicalTableAlias(fk_tableAndCol, modelName);

            if (keySize == 1) {

                return new MdnPhysicalSchema.MdnLink(fk_tableAndCol[0], pk_tableAndCol[0],
                        fk_tableAndCol[1], joinTable.getJoin().getType(), null);
            } else {
                MdnForeignKey mdnForeignKey = new MdnForeignKey();
                for (String foreignKey : foreignKeys) {
                    String[] schemaAndTable = getSchemaAndTable(foreignKey);
                    mdnForeignKey.addMdnColumn(schemaAndTable[1]);
                }
                return new MdnPhysicalSchema.MdnLink(fk_tableAndCol[0], pk_tableAndCol[0],
                        null, joinTable.getJoin().getType(), mdnForeignKey);

            }
        }

        private String[] replaceWithCommonPhysicalTableAlias(String[] tableAndCol, String modelName) {
            String modelTable = modelName + "." + tableAndCol[0];
            String commonModelTable = mapModelTable2CommonModelTable.get(modelTable);
            // it seems the table is not as dimension, occur in fact table, lookup table and bridge table.
            if (commonModelTable == null) {
                commonModelTable = modelName + "." + tableAndCol[0];
            }
            String[] commonModelAndTable = getModelAndTable(commonModelTable);
            String commonModelName = commonModelAndTable[0];
            String commonPhysicalTableAlias = commonModelAndTable[1] + "_" + mapModel2Position.get(commonModelName);
            return new String[]{commonPhysicalTableAlias, tableAndCol[1]};
        }

        MdnPhysicalSchema.MdnTable createPhysicalLookupTable(RawJoinTable joinTable, int modelPosition) {
            String[] schemaAndTable = getSchemaAndTable(joinTable.getTable());

            assert joinTable.getJoin() != null && joinTable.getJoin().getPrimary_key() != null;
            List<String> primaryKeys = joinTable.getJoin().getPrimary_key();
            assert primaryKeys.size() >= 1;
            String joinTableAlias = joinTable.getAlias() + "_" + modelPosition;

            if (primaryKeys.size() == 1) {
                return new MdnPhysicalSchema.MdnTable(schemaAndTable[1], schemaAndTable[0],
                        joinTableAlias, getSchemaAndTable(primaryKeys.get(0))[1], null);
            } else {
                MdnKeyColumn mdnKeyColumn = new MdnKeyColumn();
                for (String primaryKey : primaryKeys) {
                    String[] pk_schemaAndTable = getSchemaAndTable(primaryKey);
                    mdnKeyColumn.addKeyColumn(pk_schemaAndTable[1], null);
                }
                return new MdnPhysicalSchema.MdnTable(schemaAndTable[1], schemaAndTable[0],
                        joinTableAlias, null, mdnKeyColumn);
            }

        }

        void createCube(SemanticDataset dataset, MdnSchema mdnSchema) {
            MdnCube mdnCube = new MdnCube();
            mdnCube.setName(dataset.getDatasetName());
            mdnSchema.setMdnCube(mdnCube);

            createCubeDimensions(mdnCube);
            createMeasureGroups(dataset, mdnCube);
            createCalculatedMeasures(dataset, mdnCube);
            createNamedSets(dataset, mdnCube);

            // create default measure
            mdnCube.setDefaultMeasure(mdnCube.getMdnMeasureGroupWrapper().getMdnMeasureGroups().get(0).getDefaultMeasure());
        }

        MdnDimension createCubeDimension(String modelTable, AugmentDimensionTable dimTable) {
            MdnDimension mdnDimension = new MdnDimension();
            String[] modelAndTable = getModelAndTable(modelTable);
            int modelPosition = mapModel2Position.get(modelAndTable[0]);

            mdnDimension.setName(dimTable.getAlias());
            mdnDimension.setTable(dimTable.getName() + "_" + modelPosition);

            if (dimTable.getType() != null && dimTable.getType().equalsIgnoreCase("time")) {
                mdnDimension.setType("TIME");
            }

            // set first column in dimension
            assert dimTable.getDimColumns() != null;
            mdnDimension.setKey(dimTable.getDimColumns().get(0).getAlias());

            // create attributes
            createDimensionAttributes(dimTable, mdnDimension);

            // create hierarchies
            createDimensionHierarchies(dimTable, mdnDimension);

            // create translations
            createTranslations(dimTable.getCustomTranslations(), mdnDimension);

            // set visible property
            setDimensionVisibleProperty(dimTable, mdnDimension);

            return mdnDimension;
        }

        private void setDimensionVisibleProperty(AugmentDimensionTable dimTable, MdnDimension mdnDimension) {
            int invisibleColsCount = 0;
            Set<String> invisibleCols = new HashSet<>();
            for (AugmentDimensionTable.AugmentDimensionCol dimColumn : dimTable.getDimColumns()) {
                if (dimColumn.isInvisible()) {
                    invisibleColsCount++;
                    invisibleCols.add(dimColumn.getAlias());
                }
            }
            if (invisibleColsCount == dimTable.getDimColumns().size()) {
                mdnDimension.setVisible("false");
                return;
            }

            for (MdnAttribute mdnAttribute : mdnDimension.getAttributeWrapper().getMdnAttributes()) {
                if (invisibleCols.contains(mdnAttribute.getName())) {
                    mdnAttribute.setVisible("false");
                }
            }

            if (mdnDimension.getMdnHierarchyWrapper() != null) {
                for (MdnHierarchy mdnHierarchy : mdnDimension.getMdnHierarchyWrapper().getMdnHierarchies()) {
                    boolean isCustomRollup = mdnHierarchy.getMdnLevels().stream()
                            .anyMatch(mdnLevel -> mdnLevel.getWeightAttribute() != null);

                    boolean visible = true;
                    for (int i = 0; visible && i < mdnHierarchy.getMdnLevels().size(); i++) {
                        MdnHierarchy.MdnLevel mdnLevel = mdnHierarchy.getMdnLevels().get(i);

                        if (invisibleCols.contains(mdnLevel.getAttribute())
                                || invisibleCols.contains(mdnLevel.getWeightAttribute())) {
                            if (i <= 1 || isCustomRollup) {
                                mdnHierarchy.setVisible("false");
                            } else {
                                for (int j = i; j < mdnHierarchy.getMdnLevels().size(); j++) {
                                    mdnHierarchy.getMdnLevels().get(j).setVisible("false");
                                }
                            }
                            visible = false;
                        }
                    }
                }
            }
        }

        private void createDimensionHierarchies(AugmentDimensionTable dimTable, MdnDimension mdnDimension) {
            if (dimTable.getHierarchys() == null || dimTable.getHierarchys().size() == 0) {
                return;
            }
            Map<String, AugmentDimensionTable.AugmentDimensionCol> colsMap = createColsMap(dimTable.getDimColumns());
            MdnDimension.MdnHierarchyWrapper mdnHierarchyWrapper = new MdnDimension.MdnHierarchyWrapper();
            List<MdnHierarchy> mdnHierarchies = new ArrayList<>(4);
            mdnHierarchyWrapper.setMdnHierarchies(mdnHierarchies);
            mdnDimension.setMdnHierarchyWrapper(mdnHierarchyWrapper);

            for (AugmentDimensionTable.Hierarchy0 hierarchy0 : dimTable.getHierarchys()) {
                MdnHierarchy mdnHierarchy = new MdnHierarchy();
                mdnHierarchies.add(mdnHierarchy);

                mdnHierarchy.setName(hierarchy0.getName() + "-Hierarchy");
                mdnHierarchy.setHasAll("true");
                List<MdnHierarchy.MdnLevel> mdnLevels = new ArrayList<>(4);
                mdnHierarchy.setMdnLevels(mdnLevels);

                List<String> dimColumns = hierarchy0.getDimColumns();
                List<String> weightColumns = hierarchy0.getWeightColumns();
                for (int i = 0; i < dimColumns.size(); i++) {
                    String hierarchyLevel = dimColumns.get(i);
                    String weightAttribute = weightColumns == null ? null : weightColumns.get(i);

                    String levelName = colsMap.get(hierarchyLevel).getAlias();
                    assert levelName != null;
                    MdnHierarchy.MdnLevel mdnLevel = new MdnHierarchy.MdnLevel(levelName, weightAttribute);
                    mdnLevels.add(mdnLevel);
                }

                createTranslations(hierarchy0.getCustomTranslations(), mdnHierarchy);
            }
        }

        private void createTranslations(List<SemanticDataset.AugmentCustomTranslation> customTranslations, Object mdnObject) {
            if (customTranslations == null || customTranslations.size() == 0) {
                return;
            }
            Consumer<MdnCustomTranslationWrapper> f = null;
            if (mdnObject instanceof MdnDimension) {
                f = ((MdnDimension) mdnObject)::setCustomTranslationWrapper;
            } else if (mdnObject instanceof MdnAttribute) {
                f = ((MdnAttribute) mdnObject)::setCustomTranslationWrapper;
            } else if (mdnObject instanceof MdnMeasure) {
                f = ((MdnMeasure) mdnObject)::setCustomTranslationWrapper;
            } else if (mdnObject instanceof MdnHierarchy) {
                f = ((MdnHierarchy) mdnObject)::setCustomTranslationWrapper;
            } else if (mdnObject instanceof CalculatedMember) {
                f = ((CalculatedMember) mdnObject)::setCustomTranslationWrapper;
            } else {
                return;
            }

            List<MdnCustomTranslation> mdnCustomTranslations = new ArrayList<>();

            for (SemanticDataset.AugmentCustomTranslation customTranslation : customTranslations) {

                String caption = customTranslation.getCaption();
                String description = customTranslation.getDescription();
                if ((caption != null && !"".equals(caption)) || (description != null && !"".equals(description))) {
                    MdnCustomTranslation mdnCustomTranslation = new MdnCustomTranslation(customTranslation.getName());
                    if (caption != null && !"".equals(caption)) {
                        mdnCustomTranslation.setCaption(caption);
                    }
                    if (description != null && !"".equals(description)) {
                        mdnCustomTranslation.setDescription(description);
                    }
                    mdnCustomTranslations.add(mdnCustomTranslation);
                }
            }

            if (mdnCustomTranslations.size() != 0) {
                MdnCustomTranslationWrapper customTranslationWrapper = new MdnCustomTranslationWrapper();
                customTranslationWrapper.setMdnCustomTranslations(mdnCustomTranslations);
                f.accept(customTranslationWrapper);
            }
        }

        private void createDimensionAttributes(AugmentDimensionTable dimTable, MdnDimension mdnDimension) {
            MdnDimension.AttributeWrapper attributeWrapper = new MdnDimension.AttributeWrapper();
            List<MdnAttribute> attributes = new ArrayList<>(8);
            attributeWrapper.setMdnAttributes(attributes);
            mdnDimension.setAttributeWrapper(attributeWrapper);

            // if no hierarchy in these column
            boolean isTimeDimension = dimTable.getType() != null && dimTable.getType().equalsIgnoreCase("time");
            for (AugmentDimensionTable.AugmentDimensionCol dimCol : dimTable.getDimColumns()) {
                MdnAttribute mdnAttribute;
                if (isTimeDimension) {
                    // no hierarchy , but has time dimension
                    mdnAttribute = new MdnAttribute(dimCol.getAlias(), dimCol.getName(), dimCol.getNameColumn(), dimCol.getValueColumn(), getDimAttrLevelType(dimCol.getType()), dimCol.getSubfolder());
                } else {
                    mdnAttribute = new MdnAttribute(dimCol.getAlias(), dimCol.getName(), dimCol.getNameColumn(), dimCol.getValueColumn(), dimCol.getSubfolder());
                }
                List<AugmentDimensionTable.AugmentDimensionCol.AugmentProperty> properties = dimCol.getProperties();
                if (properties != null) {
                    properties.forEach(property -> mdnAttribute.addProperty(property.getName(), property.getAttribute()));
                }
                if (StringUtils.isNotBlank(dimCol.getDefaultMember())) {
                    mdnAttribute.setDefaultMember(dimCol.getDefaultMember());
                }
                createTranslations(dimCol.getCustomTranslations(), mdnAttribute);

                attributes.add(mdnAttribute);
            }
        }

        private Map<String, AugmentDimensionTable.AugmentDimensionCol> createColsMap(List<AugmentDimensionTable.AugmentDimensionCol> dimColumns) {
            Map<String, AugmentDimensionTable.AugmentDimensionCol> colsMap = new HashMap<>(16);
            for (AugmentDimensionTable.AugmentDimensionCol col : dimColumns) {
                colsMap.put(col.getName(), col);
            }
            return colsMap;
        }

        private void createMeasureGroups(SemanticDataset dataset, MdnCube mdnCube) {
            // attention: 2 measure groups can not have the same fact table
            MdnCube.MdnMeasureGroupWrapper mdnMeasureGroupWrapper = new MdnCube.MdnMeasureGroupWrapper();
            List<MdnMeasureGroup> measureGroups = new ArrayList<>(4);
            mdnMeasureGroupWrapper.setMdnMeasureGroups(measureGroups);
            mdnCube.setMdnMeasureGroupWrapper(mdnMeasureGroupWrapper);

            dataset.getModels().forEach(model
                    -> measureGroups.add(createMeasureGroup(model, dataset.getDimTableModelRelations())));
        }

        private void createCubeDimensions(MdnCube mdnCube) {
            MdnCube.MdnDimensionWrapper dimensionWrapper = new MdnCube.MdnDimensionWrapper();
            List<MdnDimension> mdnDimensions = new ArrayList<>(8);

            for (Map.Entry<String, AugmentDimensionTable> entry : dimTablePool.entrySet()) {
                String modelTable = entry.getKey();
                AugmentDimensionTable dimTable = entry.getValue();
                if (dimTable.getDimColumns() == null || dimTable.getDimColumns().isEmpty()) {
                    continue;
                }
                mdnDimensions.add(createCubeDimension(modelTable, dimTable));
            }

            dimensionWrapper.setMdnDimensions(mdnDimensions);
            mdnCube.setMdnDimensionWrapper(dimensionWrapper);
        }

        private MdnMeasureGroup createMeasureGroup(SemanticDataset.AugmentedModel model, List<SemanticDataset.DimTableModelRelation> dimTableModelRelations) {
            MdnMeasureGroup mdnMeasureGroup = new MdnMeasureGroup();
            String factTable = getSchemaAndTable(model.getFactTable())[1];
            mdnMeasureGroup.setTable(factTable + "_" + mapModel2Position.get(model.getModelName()));
            mdnMeasureGroup.setName(model.getModelAlias());

            createDimensionLinks(dimTableModelRelations, mdnMeasureGroup, model.getModelName());
            createMeasures(model, mdnMeasureGroup);

            return mdnMeasureGroup;
        }

        private void createMeasures(SemanticDataset.AugmentedModel model, MdnMeasureGroup mdnMeasureGroup) {
            MdnMeasureGroup.MeasureWrapper measureWrapper = new MdnMeasureGroup.MeasureWrapper();
            List<MdnMeasure> mdnMeasures = new ArrayList<>(4);
            measureWrapper.setMdnMeasures(mdnMeasures);
            mdnMeasureGroup.setMeasureWrapper(measureWrapper);

            for (AugmentMeasure measure : model.getMeasures()) {
                MdnMeasure mdnMeasure;
                String aggregator = getAggregator(measure.getExpression().toLowerCase());
                // Only support min max sum count count-distinct
                if (aggregator == null) {
                    continue;
                }
                boolean constantCol = measure.getDimColumn().equalsIgnoreCase("constant") || measure.getDimColumn().equalsIgnoreCase("1");
                if (aggregator.equalsIgnoreCase("COUNT") && constantCol) {
                    mdnMeasure = new MdnMeasure(measure.getAlias(), null, aggregator, null, measure.getFormatString(), null, null, measure.getSubfolder());
                    if (mdnMeasureGroup.getDefaultMeasure() == null || mdnMeasureGroup.getDefaultMeasure().length() == 0) {
                        mdnMeasureGroup.setDefaultMeasure(measure.getAlias());
                    }
                } else {
                    String[] measureTableAndCol = getTableAndCol(measure.getDimColumn());
                    measureTableAndCol = replaceWithCommonPhysicalTableAlias(measureTableAndCol, model.getModelName());
                    mdnMeasure = new MdnMeasure(measure.getAlias(),
                            measureTableAndCol[0],
                            aggregator, measureTableAndCol[1],
                            measure.getFormatString(), null, null, measure.getSubfolder());
                }
                // set default format string
                if (mdnMeasure.getFormatString() == null || mdnMeasure.getFormatString().length() == 0) {
                    mdnMeasure.setFormatString(getDefaultFormatString(measure));
                }
                // set measure visible
                if (measure.isInvisible()) {
                    mdnMeasure.setVisible("false");
                }

                createTranslations(measure.getCustomTranslations(), mdnMeasure);

                mdnMeasures.add(mdnMeasure);
            }

        }

        private String getDefaultFormatString(AugmentMeasure measure) {
            boolean isInt = measure.getDataType().equalsIgnoreCase("int") || measure.getDataType().equalsIgnoreCase("bigint")
                    || measure.getDataType().equalsIgnoreCase("tinyint");
            switch (measure.getExpression()) {
                case "COUNT":
                case "COUNT_DISTINCT":
                    return "#,###";
                case "SUM":
                case "MIN":
                case "MAX":
                    if (isInt) {
                        return "#,###";
                    } else {
                        return "#,###.00";
                    }
                default:
                    return "";
            }

        }

        private void createDimensionLinks(List<SemanticDataset.DimTableModelRelation> dimTableModelRelations, MdnMeasureGroup mdnMeasureGroup, String modelName) {
            MdnMeasureGroup.MdnDimensionLinkWrapper mdnDimensionLinkWrapper = new MdnMeasureGroup.MdnDimensionLinkWrapper();
            List<MdnMeasureGroup.MdnFactLink> factLinks = new ArrayList<>(8);
            List<MdnMeasureGroup.MdnNoLink> noLinks = new ArrayList<>(8);
            mdnDimensionLinkWrapper.setMdnFactLinks(factLinks);
            mdnDimensionLinkWrapper.setMdnNoLinks(noLinks);
            mdnMeasureGroup.setMdnDimensionLinkWrapper(mdnDimensionLinkWrapper);

            dimTableModelRelations.forEach(dimTableModelRelation -> {
                // now, we not support multiple level m2m
                boolean isM2MFlag = false;
                String relationModelName = dimTableModelRelation.getModelName();
                if (!relationModelName.equals(modelName)) {
                    return;
                }
                List<SemanticDataset.DimTableModelRelation.DimTableRelation> relations = dimTableModelRelation.getTableRelations();
                for (Map.Entry<String, AugmentDimensionTable> entry : dimTablePool.entrySet()) {
                    String modelTable = entry.getKey();
                    AugmentDimensionTable dimTable = entry.getValue();
                    SemanticDataset.DimTableModelRelation.DimTableRelation matchRelation = null;
                    for (SemanticDataset.DimTableModelRelation.DimTableRelation relation : relations) {
                        String relationModelTable = modelName + "." + relation.getTableName();
                        String commonModelTable = mapModelTable2CommonModelTable.get(relationModelTable);
                        if (modelTable.equals(relationModelTable) || modelTable.equals(commonModelTable)) {
                            matchRelation = relation;
                            break;
                        }
                    }
                    // current dim table is not in model relations, so set no link
                    String dimensionCaption = dimTable.getAlias();
                    if (matchRelation == null) {
                        noLinks.add(new MdnMeasureGroup.MdnNoLink(dimensionCaption));
                    } else {
                        factLinks.add(new MdnMeasureGroup.MdnFactLink(dimensionCaption));
                        if (matchRelation.getRelationType() == DimensionLinkType.M2M && !isM2MFlag) {
                            mdnMeasureGroup.setIsM2M("true");
                            String[] factTableAndCol = getTableAndCol(matchRelation.getRelationFactKey());
                            mdnMeasureGroup.setPk(factTableAndCol[1]);
                            mdnMeasureGroup.setBridgeTable(matchRelation.getRelationBridgeTableName());
                            isM2MFlag = true;
                        }
                    }
                }
            });


        }

        private void createNamedSets(SemanticDataset dataset, MdnCube mdnCube) {
            MdnCube.NamedSetWrapper namedSetWrapper = new MdnCube.NamedSetWrapper();
            List<MdnNamedSet> namedSets = new ArrayList<>();
            List<SemanticDataset.NamedSet0> namedSets0 = dataset.getNamedSets();
            if (namedSets0 != null) {
                for (SemanticDataset.NamedSet0 namedSet0 : namedSets0) {
                    MdnFormula formula = new MdnFormula(namedSet0.getExpression());
                    MdnNamedSet namedSet = new MdnNamedSet(namedSet0.getName(), formula, null);
                    if (namedSet0.isInvisible()) {
                        namedSet.setVisible("false");
                    }
                    namedSets.add(namedSet);
                }
                namedSetWrapper.setMdnNamedSets(namedSets);
                mdnCube.setNamedSetWrapper(namedSetWrapper);
            }
        }

        private void createCalculatedMeasures(SemanticDataset dataset, MdnCube mdnCube) {
            MdnCube.CalculatedMemberWrapper calculatedMemberWrapper = new MdnCube.CalculatedMemberWrapper();
            List<CalculatedMember> calculatedMembers = new ArrayList<>(5);
            List<SemanticDataset.CalculateMeasure0> calculateMeasures = dataset.getCalculateMeasures();
            if (calculateMeasures != null) {
                for (SemanticDataset.CalculateMeasure0 calculateMeasure : calculateMeasures) {
                    MdnFormula formula = new MdnFormula(calculateMeasure.getExpression());
                    CalculatedMember.NonEmptyBehaviorMeasureWrapper nonEmptyBehaviorMeasures =
                            createNonEmptyBehaviorMeasureWrapper(calculateMeasure.getNonEmptyBehaviorMeasures());
                    CalculatedMember cm = null;
                    if (calculateMeasure.getSubfolder() == null || calculateMeasure.getSubfolder().equals("")) {
                        cm = new CalculatedMember(
                                calculateMeasure.getName(),
                                "Measures",
                                formula,
                                calculateMeasure.getFormat(),
                                calculateMeasure.getFolder(),
                                null,
                                null,
                                null,
                                nonEmptyBehaviorMeasures);
                    } else {
                        cm = new CalculatedMember(
                                calculateMeasure.getName(),
                                "Measures",
                                formula,
                                calculateMeasure.getFormat(),
                                calculateMeasure.getFolder() + "@" + calculateMeasure.getSubfolder(),
                                null,
                                null,
                                null,
                                nonEmptyBehaviorMeasures);
                    }
                    calculatedMembers.add(cm);
                    // set visible
                    if (calculateMeasure.isInvisible()) {
                        cm.setVisible("false");
                    }
                    createTranslations(calculateMeasure.getCustomTranslations(), cm);
                }
            }
            calculatedMemberWrapper.setCalculatedMembers(calculatedMembers);
            mdnCube.setCalculatedMemberWrapper(calculatedMemberWrapper);
        }

        private String getDimAttrLevelType(Integer levelType) {
            switch (levelType) {
                case 1:
                    return DimAttrLevel.TIME_YEARS;
                case 2:
                    return DimAttrLevel.TIME_QUARTERS;
                case 3:
                    return DimAttrLevel.TIME_MONTHS;
                case 4:
                    return DimAttrLevel.TIME_WEEKS;
                case 5:
                    return DimAttrLevel.TIME_DAYS;
                case 0:
                    return null;
                default:
                    log.warn("unknown attribute level type " + levelType);
                    return null;
            }
        }

        private String getAggregator(String expression) {
            switch (expression) {
                case SupportedAggregator.MIN:
                case SupportedAggregator.MAX:
                case SupportedAggregator.SUM:
                case SupportedAggregator.COUNT:
                    return expression.toLowerCase();
                case SupportedAggregator.COUNT_DISTINCT:
                    return "distinct-count";
                default:
                    log.warn("unsupported aggregator type " + expression);
                    return null;
            }
        }

    }

    private static CalculatedMember.NonEmptyBehaviorMeasureWrapper createNonEmptyBehaviorMeasureWrapper(
            List<CalculatedMemberNonEmptyBehaviorMeasure> nonEmptyBehaviorMeasures) {
        if (Utils.isCollectionEmpty(nonEmptyBehaviorMeasures)) {
            return null;
        }
        return new CalculatedMember.NonEmptyBehaviorMeasureWrapper(
                nonEmptyBehaviorMeasures.stream()
                        .map(nonEmptyBehaviorMeasure ->
                                new CalculatedMember.NonEmptyBehaviorMeasure(nonEmptyBehaviorMeasure.getAlias()))
                        .collect(Collectors.toList()));
    }

    public class SingleModelConverter extends DatasetConverter {

        @Override
        protected void resolveModelDimension(SemanticDataset dataset) {
            for (int i = 0; i < dataset.getModels().size(); i++) {
                SemanticDataset.AugmentedModel model = dataset.getModels().get(i);
                mapModel2Position.put(model.getModelName(), i);
                model.getDimensionTables().forEach(dimTable -> {
                    String modelTable = model.getModelName() + "." + dimTable.getName();
                    // single model has no common table, set the same with origin.
                    String commonModelTable = model.getModelName() + "." + dimTable.getName();
                    dimTablePool.put(modelTable, dimTable);
                    mapModelTable2CommonModelTable.put(modelTable, commonModelTable);
                });
            }
        }
    }

    public class MultipleModelConverter extends DatasetConverter {

        @Override
        protected void resolveModelDimension(SemanticDataset dataset) {
            for (int i = 0; i < dataset.getModels().size(); i++) {
                SemanticDataset.AugmentedModel model = dataset.getModels().get(i);
                mapModel2Position.put(model.getModelName(), i);
                model.getDimensionTables().forEach(dimTable -> {
                    String modelTable = model.getModelName() + "." + dimTable.getName();
                    dimTablePool.put(modelTable, dimTable);
                });
            }
            resolveModelRelations(dataset.getModelRelations());
        }

        // first, build common table candidates from left model
        // second, build map with table and direct relation table
        // third, remove common table follower
        // last, map common table follower/representative to common table representative
        private void resolveModelRelations(List<SemanticDataset.ModelRelation> modelRelations) {
            Set<String> commonTableCandidateSet = new LinkedHashSet<>(16);
            Map<String, List<String>> mapModelTable2DirectRelationTables = new HashMap<>(16);

            modelRelations.forEach(modelRelation -> {
                modelRelation.getRelations().forEach(relation0 -> {
                    String leftModelTable = modelRelation.getModelLeft() + "." + relation0.getLeft();
                    String rightModelTable = modelRelation.getModelRight() + "." + relation0.getRight();
                    commonTableCandidateSet.add(leftModelTable);
                    putOrCreateLinks(mapModelTable2DirectRelationTables, leftModelTable, rightModelTable);
                    putOrCreateLinks(mapModelTable2DirectRelationTables, rightModelTable, leftModelTable);
                });
            });

            commonTableCandidateSet.forEach(commonTableCandidate -> {
                if (dimTablePool.containsKey(commonTableCandidate)) {
                    // contains direct or indirect link tables and self
                    Set<String> linkTables = new HashSet<>();
                    recursiveFindLinkTables(commonTableCandidate, linkTables, mapModelTable2DirectRelationTables);
                    mapModelTable2CommonModelTable.put(commonTableCandidate, commonTableCandidate);
                    linkTables.forEach(linkTable -> {
                        if (!linkTable.equals(commonTableCandidate)) {
                            dimTablePool.remove(linkTable);
                            mapModelTable2CommonModelTable.put(linkTable, commonTableCandidate);
                        }
                    });
                }
            });
        }

        private void recursiveFindLinkTables(String table, Set<String> linkTables, Map<String, List<String>> mapModelTable2DirectRelationTables) {
            linkTables.add(table);
            mapModelTable2DirectRelationTables.get(table).forEach(directLink -> {
                if (!linkTables.contains(directLink)) {
                    recursiveFindLinkTables(directLink, linkTables, mapModelTable2DirectRelationTables);
                }
            });

        }

        private void putOrCreateLinks(Map<String, List<String>> mapModelTable2DirectRelationTables, String modelTable, String linkTable) {
            List<String> links = mapModelTable2DirectRelationTables.get(modelTable);
            if (links == null) {
                links = new ArrayList<>(8);
                links.add(linkTable);
                mapModelTable2DirectRelationTables.put(modelTable, links);
            } else {
                links.add(linkTable);
            }
        }
    }

    interface DimAttrLevel {

        String TIME_YEARS = "TimeYears";
        String TIME_QUARTERS = "TimeQuarters";
        String TIME_MONTHS = "TimeMonths";
        String TIME_WEEKS = "TimeWeeks";
        String TIME_DAYS = "TimeDays";

    }

    interface DimensionLinkType {

        int FACT_LINK = 0;
        int NO_LINK = 1;
        int M2M = 2;

    }

    interface SupportedAggregator {

        String SUM = "sum";
        String MIN = "min";
        String MAX = "max";
        String COUNT = "count";
        String COUNT_DISTINCT = "count_distinct";
    }
}
