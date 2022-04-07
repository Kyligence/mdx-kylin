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

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.CalculateMeasure;
import io.kylin.mdx.insight.core.entity.CalculatedMemberNonEmptyBehaviorMeasure;
import io.kylin.mdx.insight.core.entity.CommonDimRelation;
import io.kylin.mdx.insight.core.entity.CustomHierarchy;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.entity.DescWrapperExtend;
import io.kylin.mdx.insight.core.entity.DimTableModelRel;
import io.kylin.mdx.insight.core.entity.DimTableType;
import io.kylin.mdx.insight.core.entity.ModelDimTableHelper;
import io.kylin.mdx.insight.core.entity.NamedDimCol;
import io.kylin.mdx.insight.core.entity.NamedDimTable;
import io.kylin.mdx.insight.core.entity.NamedMeasure;
import io.kylin.mdx.insight.core.entity.PropertyAttr;
import io.kylin.mdx.insight.core.model.generic.ColumnIdentity;
import io.kylin.mdx.insight.core.model.generic.CubeMeasure;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.model.semantic.DimensionColumn;
import io.kylin.mdx.insight.core.model.semantic.DimensionTable;
import io.kylin.mdx.insight.core.model.semantic.ModelColumnIdentity;
import io.kylin.mdx.insight.core.model.semantic.ModelDimRelationType;
import io.kylin.mdx.insight.core.service.DatasetService;
import io.kylin.mdx.insight.core.service.ModelService;
import io.kylin.mdx.insight.core.support.SemanticUtils;
import io.kylin.mdx.insight.core.sync.DatasetValidateResult.DatasetValidateType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j(topic = "dataset.validator")
public class DatasetValidator {

    private static final Pattern MEASURE_PATTERN = Pattern.compile("\\[Measures]\\.\\[(?<MeasureName>[\\p{IsHan}\\w\\s\\-%()（）?？]+)]");

    private final List<KylinGenericModel> noCacheGenericModels;

    private final DatasetEntity dataset;

    private final DatasetService datasetService;

    private final ModelService modelService;

    public DatasetValidator(DatasetEntity dataset, DatasetService datasetService, List<KylinGenericModel> noCacheGenericModels, ModelService modelService) {
        this.dataset = dataset;
        this.datasetService = datasetService;
        this.noCacheGenericModels = noCacheGenericModels;
        this.modelService = modelService;
    }

    public DatasetValidateResult validate() {
        DatasetValidateResult validateResult = new DatasetValidateResult();
        Integer id = dataset.getId();

        Map<String, KylinGenericModel> canonicalNameToModelMap = noCacheGenericModels.stream()
                .collect(Collectors.toMap(KylinGenericModel::getModelName, model -> model));

        /*
         * 1. validate whether models used in dataset are effective in KYLIN
         */
        List<CommonDimRelation> commonDimRelations = datasetService.selectCommonDimRelsByDatasetId(id);
        Set<String> modelUsedSet = SemanticUtils.extractModelNames(commonDimRelations);

        for (String modelName : modelUsedSet) {
            KylinGenericModel genericModel = canonicalNameToModelMap.get(modelName);
            if (genericModel == null) {
                validateResult.setDatasetValidateType(DatasetValidateType.BROKEN);
                validateResult.addBrokenModelName(modelName);
                break;
            }
        }
        if (validateResult.getDatasetValidateType() == DatasetValidateType.BROKEN) {
            return validateResult;
        }

        if (log.isDebugEnabled()) {
            log.debug("Dataset verify: models used in dataset are ok. ");
        }

        Map<String, Map<String, DimensionTable>> modelToDimensionTableMap = new HashMap<>();

        List<NamedDimTable> namedDimTables = datasetService.selectDimTablesByDatasetId(id);
        Map<ModelTable, NamedDimTable> oldTableMap = new HashMap<>();
        namedDimTables.forEach(namedDimTable -> oldTableMap.put(new ModelTable(namedDimTable), namedDimTable));
        Map<String, Integer> namedDimTableCountMap = new HashMap<>();
        for (NamedDimTable namedDimTable : namedDimTables) {
            if (namedDimTableCountMap.containsKey(namedDimTable.getDimTable())) {
                namedDimTableCountMap.put(namedDimTable.getDimTable(), namedDimTableCountMap.get(namedDimTable.getDimTable()) + 1);
            } else {
                namedDimTableCountMap.put(namedDimTable.getDimTable(), 1);
            }
        }
        for (String modelName : modelUsedSet) {
            KylinGenericModel genericModel = canonicalNameToModelMap.get(modelName);
            if (genericModel == null) {
                continue;
            }
            modelToDimensionTableMap.put(modelName, modelService.createDimensionTableMap(genericModel, oldTableMap, namedDimTableCountMap));
        }

        /*
         * 2. validate dimension table used in dataset are effective
         */
        // 2.1 validate common table
        Map<String, Set<String>> modelToTableAliases = new HashMap<>();
        ModelDimTableHelper modelDimTableHelper = new ModelDimTableHelper();
        for (CommonDimRelation comDimRel : commonDimRelations) {
            if (StringUtils.isBlank(comDimRel.getModelRelated())) {
                continue;
            }

            Map<String, String> splitMap = Utils.createSplitMap(comDimRel.getRelation());

            Set<String> modelTableAliases = modelToTableAliases.computeIfAbsent(comDimRel.getModel(), name -> new HashSet<>());
            Set<String> modelRelatedTableAliases = modelToTableAliases.computeIfAbsent(comDimRel.getModelRelated(), name -> new HashSet<>());
            splitMap.forEach((k, v) -> {
                modelTableAliases.add(k);
                modelRelatedTableAliases.add(v);

                modelDimTableHelper.addIgnoreItem(comDimRel.getModelRelated(), v);
            });
        }
        for (Map.Entry<String, Set<String>> entry : modelToTableAliases.entrySet()) {
            String modelName = entry.getKey();
            Set<String> tableAliases = entry.getValue();

            Map<String, DimensionTable> dimensionTableMap = modelToDimensionTableMap.get(modelName);
            if (dimensionTableMap == null) {
                continue;
            }
            for (String tableAlias : tableAliases) {
                if (dimensionTableMap.get(tableAlias) == null) {
                    validateResult.setDatasetValidateType(DatasetValidateType.BROKEN);
                    validateResult.addCommonTableBroken(modelName, tableAlias);
                }
            }
        }

        // 2.2 validate dimension table relation
        DimTableModelRel search = new DimTableModelRel();
        search.setDatasetId(id);
        search.setRelation(ModelDimRelationType.MANY_TO_MANY.ordinal());
        List<DimTableModelRel> dimTableModelManyToManyRels = datasetService.selectDimTblModelRelsBySearch(search);
        for (DimTableModelRel dimTableModelRel : dimTableModelManyToManyRels) {
            String bridgeDimTable = dimTableModelRel.getIntermediateDimTable();
            Map<String, DimensionTable> dimensionTableMap = modelToDimensionTableMap.get(dimTableModelRel.getModel());
            if (dimensionTableMap == null) {
                continue;
            }
            Set<String> tableAliases = dimensionTableMap.keySet();
            if (!tableAliases.contains(bridgeDimTable)) {
                validateResult.setDatasetValidateType(DatasetValidateType.BROKEN);
                validateResult.addBridgeDimTableBroken(dimTableModelRel.getModel(), bridgeDimTable);
            }
        }


        /*
         * validate whether dimension column in hierarchy is valid in KE
         */
        Set<ModelColumnIdentity> canonicalDimensions = new HashSet<>();
        for (String modelName : modelUsedSet) {
            KylinGenericModel genericModel = canonicalNameToModelMap.get(modelName);
            if (genericModel == null) {
                continue;
            }
            canonicalDimensions.addAll(
                    genericModel.getCubeDimensions().stream()
                            .map(cubeDimension -> new ModelColumnIdentity(modelName, cubeDimension))
                            .collect(Collectors.toSet())
            );
        }

        List<CustomHierarchy> customHierarchies = datasetService.selectHierarchiesByDatasetId(id);
        for (CustomHierarchy customHierarchy : customHierarchies) {
            int brokenStatus = 0;
            ColumnIdentity verifyColIden = new ColumnIdentity(customHierarchy.getDimTable(), customHierarchy.getDimCol());
            if (!canonicalDimensions.contains(new ModelColumnIdentity(customHierarchy.getModel(), verifyColIden))) {
                brokenStatus |= 0x1;
            }

            String weightCol = customHierarchy.getWeightCol();
            if (weightCol == null) {
                if (brokenStatus > 0) {
                    validateResult.setDatasetValidateType(DatasetValidateType.BROKEN);
                    validateResult.addBrokenHierarchy(customHierarchy, brokenStatus);
                }
                continue;
            }

            verifyColIden = new ColumnIdentity(customHierarchy.getDimTable(), customHierarchy.getWeightCol());
            if (!canonicalDimensions.contains(new ModelColumnIdentity(customHierarchy.getModel(), verifyColIden))) {
                brokenStatus |= 0x10;
            }

            if (brokenStatus > 0) {
                validateResult.setDatasetValidateType(DatasetValidateType.BROKEN);
                validateResult.addBrokenHierarchy(customHierarchy, brokenStatus);
            }
        }

        /*
         * validate whether dimension column in name column is valid in KE
         */
        List<NamedDimCol> namedDimCols = datasetService.selectDimColsByDatasetId(id);
        for (NamedDimCol namedDimCol : namedDimCols) {
            String nameCol = namedDimCol.getNameColumn();
            if (nameCol != null) {
                ColumnIdentity verifyNameColumnIden = new ColumnIdentity(namedDimCol.getDimTable(), namedDimCol.getNameColumn());
                if (!canonicalDimensions.contains(new ModelColumnIdentity(namedDimCol.getModel(), verifyNameColumnIden))) {
                    validateResult.setDatasetValidateType(DatasetValidateType.BROKEN);
                    validateResult.addBrokenNameColumn(namedDimCol);
                }
            }
        }


        /*
         * validate whether dimension column in value column is valid in KE
         */
        for (NamedDimCol namedDimCol : namedDimCols) {
            String valueCol = namedDimCol.getValueColumn();
            if (valueCol != null) {
                ColumnIdentity verifyValueColumnIden = new ColumnIdentity(namedDimCol.getDimTable(), namedDimCol.getValueColumn());
                if (!canonicalDimensions.contains(new ModelColumnIdentity(namedDimCol.getModel(), verifyValueColumnIden))) {
                    validateResult.setDatasetValidateType(DatasetValidateType.BROKEN);
                    validateResult.addBrokenValueColumn(namedDimCol);
                }
            }
        }

        /*
         * validate whether dimension column in properties is valid in KE
         */
        for (NamedDimCol namedDimCol : namedDimCols) {
            ColumnIdentity verifyColIden = new ColumnIdentity(namedDimCol.getDimTable(), namedDimCol.getDimCol());
            DescWrapperExtend descWrapperExtend = namedDimCol.buildExtend();
            List<PropertyAttr> properties = SemanticUtils.getPropertiesFromExtend(descWrapperExtend);
            if (properties == null) {
                continue;
            }
            if (!canonicalDimensions.contains(new ModelColumnIdentity(namedDimCol.getModel(), verifyColIden))) {
                for (PropertyAttr propertyAttr : properties) {
                    if (propertyAttr == null) {
                        continue;
                    }
                    String propertyCol = propertyAttr.getColName();
                    validateResult.setDatasetValidateType(DatasetValidateType.BROKEN);
                    validateResult.addBrokenProperties(namedDimCol, propertyCol);
                }
                continue;
            }
            for (PropertyAttr propertyAttr : properties) {
                if (propertyAttr == null) {
                    continue;
                }
                String propertyCol = propertyAttr.getColName();
                ColumnIdentity verifyPropertyIden = new ColumnIdentity(namedDimCol.getDimTable(), propertyCol);
                if (!canonicalDimensions.contains(new ModelColumnIdentity(namedDimCol.getModel(), verifyPropertyIden))) {
                    validateResult.setDatasetValidateType(DatasetValidateType.BROKEN);
                    validateResult.addBrokenProperties(namedDimCol, propertyCol);
                }
            }
        }

        /*
         * validate whether fact key is effective
         */
        for (DimTableModelRel dimTableModelRel : dimTableModelManyToManyRels) {
            String primaryDimColWithTable = dimTableModelRel.getPrimaryDimCol();

            ColumnIdentity primaryKey = new ColumnIdentity(primaryDimColWithTable);
            if (!canonicalDimensions.contains(new ModelColumnIdentity(dimTableModelRel.getModel(), primaryKey))) {
                validateResult.setDatasetValidateType(DatasetValidateType.BROKEN);
                validateResult.addBrokenManyToManyKey(dimTableModelRel.getModel(), primaryKey);
            }
        }

        if (validateResult.getDatasetValidateType() == DatasetValidateType.BROKEN) {
            log.info("[Verify dataset] dataset broken. project:{}, dataset:{}, info:{}",
                    dataset.getProject(), dataset.getDataset(), JSON.toJSONString(validateResult.getBrokenInfo()));
        }


        /*
         * validate whether dimension tables are added or deleted in KE
         */

        //for new table to add
        List<NamedDimTable> toAddNamedDimTables = new LinkedList<>();
        List<NamedDimCol> toAddNamedDimCols = new LinkedList<>();
        for (String modelName : modelUsedSet) {
            KylinGenericModel genericModel = canonicalNameToModelMap.get(modelName);
            Map<String, DimensionTable> dimensionTableMap = modelToDimensionTableMap.get(modelName);
            if (dimensionTableMap == null) {
                continue;
            }

            for (String table : dimensionTableMap.keySet()) {
                if (modelDimTableHelper.displayThisTable(modelName, table)) {
                    continue;
                }

                ModelTable modelTable = new ModelTable(modelName, table, dimensionTableMap.get(table).getAlias());
                if (!oldTableMap.containsKey(modelTable)) {
                    DimensionTable dimensionTable = dimensionTableMap.get(table);
                    toAddNamedDimTables.add(new NamedDimTable(id, modelTable.getModel(), modelTable.getTable(), Utils.blankToDefaultString(modelTable.getTableAlias(), modelTable.getTable()),
                            DimTableType.REGULAR.getLowercase(), dimensionTable.getActualTable(), genericModel.getFactTable().getRight().getFormatStr()));

                    if (dimensionTable.getDimensionColumns() == null || dimensionTable.getDimensionColumns().size() < 1) {
                        continue;
                    }
                    for (DimensionColumn dimCol : dimensionTable.getDimensionColumns()) {
                        toAddNamedDimCols.add(new NamedDimCol(id, modelTable.getModel(), modelTable.getTable(),
                                dimCol.getColumnName(), dimCol.getColumnName(), 0, dimCol.getDataType()));
                    }
                }
            }
        }


        if (!Utils.isCollectionEmpty(toAddNamedDimTables)) {
            datasetService.insertTableLogically(toAddNamedDimTables, toAddNamedDimCols);

            log.info("[Verify dataset] Datasource has new tables add. Dataset:{},project:{},table:{},cols:{}", dataset.getDataset(), dataset.getProject(),
                    JSON.toJSONString(toAddNamedDimTables), JSON.toJSONString(toAddNamedDimCols));

            if (!DatasetValidateType.BROKEN.equals(validateResult.getDatasetValidateType())) {
                validateResult.setDatasetValidateType(DatasetValidateType.SELF_FIX);
            }
        }

        //for miss table to delete
        List<NamedDimTable> toDeleteTables = new LinkedList<>();
        for (ModelTable modelTable : oldTableMap.keySet()) {
            Map<String, DimensionTable> dimensionTableMap = modelToDimensionTableMap.get(modelTable.getModel());
            if (dimensionTableMap == null) {
                continue;
            }
            if (dimensionTableMap.get(modelTable.getTable()) == null) {
                toDeleteTables.add(oldTableMap.get(modelTable));
            }
        }
        if (!Utils.isCollectionEmpty(toDeleteTables)) {
            datasetService.deleteTableLogically(toDeleteTables);

            log.info("[Verify dataset] Datasource has tables delete. Dataset:{},project:{},table:{}", dataset.getDataset(), dataset.getProject(), JSON.toJSONString(toDeleteTables));
            if (!DatasetValidateType.BROKEN.equals(validateResult.getDatasetValidateType())) {
                validateResult.setDatasetValidateType(DatasetValidateType.SELF_FIX);
            }
        }


        /*
         * validate whether all named dimension columns in semantic is valid in KYLIN/
         */
        Set<ModelColumnIdentity> oldNamedDimColSet = namedDimCols.stream()
                .map(col -> new ModelColumnIdentity(col.getModel(), new ColumnIdentity(col.getDimTable(), col.getDimCol())))
                .collect(Collectors.toSet());

        Set<ModelColumnIdentity> canonicalUsedTableDimensions = new HashSet<>();
        for (ModelColumnIdentity modelColumnIden : canonicalDimensions) {
            String modelName = modelColumnIden.getModelName();
            String tableAlias = modelColumnIden.getColumnIdentity().getTableAlias();
            if (oldTableMap.containsKey(new ModelTable(modelName, tableAlias, null))) {
                canonicalUsedTableDimensions.add(modelColumnIden);
            }
        }
        ImmutableSet<ModelColumnIdentity> dimensionsAdded = Sets.difference(canonicalUsedTableDimensions, oldNamedDimColSet).immutableCopy();
        ImmutableSet<ModelColumnIdentity> dimensionsDeleted = Sets.difference(oldNamedDimColSet, canonicalUsedTableDimensions).immutableCopy();

        if (!Utils.isCollectionEmpty(dimensionsAdded)) {
            List<NamedDimCol> newNamedDimCols = dimensionsAdded.stream()
                    .map(dim -> new NamedDimCol(id, dim, SemanticUtils.getColumnInfo(canonicalNameToModelMap.get(dim.getModelName()), dim.getColumnIdentity())))
                    .collect(Collectors.toList());

            datasetService.insertNamedCols(newNamedDimCols);
            log.info("[Verify dataset] Datasource has new dimensions add. Dataset:{},project:{},dimensions:{}", dataset.getDataset(), dataset.getProject(), JSON.toJSONString(newNamedDimCols));

            if (!DatasetValidateType.BROKEN.equals(validateResult.getDatasetValidateType())) {
                validateResult.setDatasetValidateType(DatasetValidateType.SELF_FIX);
            }
        }

        if (!Utils.isCollectionEmpty(dimensionsDeleted)) {
            for (ModelColumnIdentity modelColIden : dimensionsDeleted) {
                NamedDimCol deleteNamedDimCol = new NamedDimCol();
                deleteNamedDimCol.setDatasetId(id);
                deleteNamedDimCol.setModel(modelColIden.getModelName());
                deleteNamedDimCol.setDimTable(modelColIden.getColumnIdentity().getTableAlias());
                deleteNamedDimCol.setDimCol(modelColIden.getColumnIdentity().getColName());
                datasetService.deleteNamedCol(deleteNamedDimCol);
            }
            log.info("[Verify dataset] Datasource has dimensions to delete. Dataset:{},project:{},dimensions:{}", dataset.getDataset(), dataset.getProject(), JSON.toJSONString(dimensionsDeleted));

            if (!DatasetValidateType.BROKEN.equals(validateResult.getDatasetValidateType())) {
                validateResult.setDatasetValidateType(DatasetValidateType.SELF_FIX);
            }
        }


        /*
         * validate whether all named measure columns in semantic is valid in KYLIN
         */
        Map<ModelMeasure, CubeMeasure> canonicalCubeMeasureMap = new HashMap<>();
        for (String modelName : modelUsedSet) {
            KylinGenericModel genericModel = canonicalNameToModelMap.get(modelName);
            if (genericModel == null) {
                continue;
            }
            genericModel.getCubeMeasures().forEach(cubeMeasure -> canonicalCubeMeasureMap.put(new ModelMeasure(modelName, cubeMeasure.getMeasureName()), cubeMeasure));
        }

        List<NamedMeasure> namedMeasures = datasetService.selectMeasuresByDatasetId(id);
        Map<ModelMeasure, NamedMeasure> oldNamedMeasureMap = namedMeasures.stream().collect(Collectors.toMap(ModelMeasure::new, measure -> measure));

        // for new measure to add
        List<NamedMeasure> toAddMeasures = new LinkedList<>();
        for (ModelMeasure modelMeasure : canonicalCubeMeasureMap.keySet()) {
            NamedMeasure namedMeasure = oldNamedMeasureMap.get(modelMeasure);
            if (namedMeasure == null) {
                CubeMeasure cubeMeasure = canonicalCubeMeasureMap.get(modelMeasure);
                if (cubeMeasure.getExpression().equalsIgnoreCase("TOP_N")
                        || cubeMeasure.getExpression().equalsIgnoreCase("CORR")
                        || cubeMeasure.getExpression().equalsIgnoreCase("PERCENTILE_APPROX")) {
                    continue;
                }
                toAddMeasures.add(new NamedMeasure(id, modelMeasure.getModel(), modelMeasure.getMeasure(), modelMeasure.getMeasure() + "_" + modelMeasure.getModel(),
                        cubeMeasure.getExpression(), cubeMeasure.getDataType(), SemanticUtils.getNormalMeasureArgByColMeasured(cubeMeasure.getColMeasured())));

            }
        }
        if (!Utils.isCollectionEmpty(toAddMeasures)) {
            log.info("[Verify dataset] Datasource has new measures to add. Dataset:{},project:{},measures:{}", dataset.getDataset(), dataset.getProject(), JSON.toJSONString(toAddMeasures));
            datasetService.insertNamedMeasure(toAddMeasures);
            if (!DatasetValidateType.BROKEN.equals(validateResult.getDatasetValidateType())) {
                validateResult.setDatasetValidateType(DatasetValidateType.SELF_FIX);
            }
        }

        // for same measure, check whether the expression is same
        for (ModelMeasure modelMeasure : canonicalCubeMeasureMap.keySet()) {
            NamedMeasure oldNamedMeasure = oldNamedMeasureMap.get(modelMeasure);
            if (oldNamedMeasure != null) {
                CubeMeasure newCubeMeasure = canonicalCubeMeasureMap.get(modelMeasure);
                if (!oldNamedMeasure.getExpression().equalsIgnoreCase(newCubeMeasure.getExpression())
                        || !oldNamedMeasure.getDimColumn().equalsIgnoreCase(SemanticUtils.getNormalMeasureArgByColMeasured(newCubeMeasure.getColMeasured()))) {
                    if (newCubeMeasure.getExpression().equalsIgnoreCase("TOP_N")
                            || newCubeMeasure.getExpression().equalsIgnoreCase("CORR")
                            || newCubeMeasure.getExpression().equalsIgnoreCase("PERCENTILE_APPROX")) {
                        datasetService.deleteNamedMeasure(oldNamedMeasure.getId());
                        continue;
                    }
                    NamedMeasure update = new NamedMeasure(oldNamedMeasure.getDatasetId());
                    update.setExpression(newCubeMeasure.getExpression());
                    update.setDataType(newCubeMeasure.getDataType());
                    update.setDimColumn(SemanticUtils.getNormalMeasureArgByColMeasured(newCubeMeasure.getColMeasured()));
                    datasetService.updateNamedMeasure(oldNamedMeasure.getId(), update);
                    log.info("[Verify dataset] Datasource has same measure to change. Dataset:{}, project:{}, measure:{}, expression:{}, arg col:{}",
                            dataset.getDataset(), dataset.getProject(), oldNamedMeasure.getName(), oldNamedMeasure.getExpression(), oldNamedMeasure.getDimColumn());
                }
            }
        }


        // Delete missing measures.
        List<NamedMeasure> toDeleteMeasures = new LinkedList<>();
        for (Map.Entry<ModelMeasure, NamedMeasure> oldMeasure : oldNamedMeasureMap.entrySet()) {
            if (!canonicalCubeMeasureMap.containsKey(oldMeasure.getKey())) {
                toDeleteMeasures.add(oldMeasure.getValue());
            }
        }
        if (!Utils.isCollectionEmpty(toDeleteMeasures)) {
            for (NamedMeasure measure : toDeleteMeasures) {
                datasetService.deleteNamedMeasure(measure.getId());
            }

            log.info("[Verify dataset] Datasource has measures to delete. Dataset:{},project:{},measures:{}", dataset.getDataset(), dataset.getProject(), JSON.toJSONString(toDeleteMeasures));
            if (!DatasetValidateType.BROKEN.equals(validateResult.getDatasetValidateType())) {
                validateResult.setDatasetValidateType(DatasetValidateType.SELF_FIX);
            }
        }

        /*
         * validate whether all measures in CM expressions are valid in KYLIN
         */

        // Get the newest status of base measures and calculated measures.
        Set<String> measureNames = datasetService.selectMeasuresByDatasetId(id).stream()
                .map(namedMeasure -> namedMeasure.getAlias().toLowerCase())
                .collect(Collectors.toSet());
        List<CalculateMeasure> calculatedMeasures = datasetService.selectCalculateMeasuresByDatasetId(id);
        calculatedMeasures.stream()
                .map(calculateMeasure -> calculateMeasure.getName().toLowerCase())
                .forEach(measureNames::add);
        for (CalculateMeasure calculatedMeasure : calculatedMeasures) {
            String calculatedMeasureExpression = calculatedMeasure.getExpression();

            // Extract base measure aliases from the calculated measure expression.
            Matcher expressionMatcher = MEASURE_PATTERN.matcher(calculatedMeasureExpression);

            // Check whether the extracted base measure aliases are valid.
            while (expressionMatcher.find()) {
                String nestedMeasureName = expressionMatcher.group("MeasureName").toLowerCase();
                if (!measureNames.contains(nestedMeasureName)) {
                    validateResult.setDatasetValidateType(DatasetValidateType.BROKEN);
                }
            }
        }

        for (CalculateMeasure calculatedMeasure : calculatedMeasures) {
            List<CalculatedMemberNonEmptyBehaviorMeasure> nonEmptyBehaviorMeasures = JSON.parseArray(
                    calculatedMeasure.getNonEmptyBehaviorMeasures(),
                    CalculatedMemberNonEmptyBehaviorMeasure.class);
            if (nonEmptyBehaviorMeasures == null) {
                continue;
            }

            List<ModelMeasure> nonEmptyBehaviorModelMeasures = nonEmptyBehaviorMeasures.stream()
                    .map(nonEmptyBehaviorMeasure ->
                            new ModelMeasure(nonEmptyBehaviorMeasure.getModel(), nonEmptyBehaviorMeasure.getName()))
                    .collect(Collectors.toList());

            for (ModelMeasure nonEmptyBehaviorModelMeasure : nonEmptyBehaviorModelMeasures) {
                if (!canonicalCubeMeasureMap.containsKey(nonEmptyBehaviorModelMeasure)) {
                    log.info("[Verify dataset] Invalid base measure \"{}\" found in the non-empty behavior list of CM \"{}\".",
                            nonEmptyBehaviorModelMeasure, calculatedMeasure.getName());
                    validateResult.setDatasetValidateType(DatasetValidateType.BROKEN);
                }
            }
        }

        if (validateResult.getDatasetValidateType() == null) {
            validateResult.setDatasetValidateType(DatasetValidateType.NORMAL);
        }

        return validateResult;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class ModelMeasure {

        private String model;

        private String measure;

        public ModelMeasure(NamedMeasure namedMeasure) {
            this.model = namedMeasure.getModel();
            this.measure = namedMeasure.getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ModelMeasure)) {
                return false;
            }
            ModelMeasure that = (ModelMeasure) o;
            return getModel().equals(that.getModel()) &&
                    getMeasure().equals(that.getMeasure());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getModel(), getMeasure());
        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelTable {

        private String model;

        private String table;

        private String tableAlias;

        public ModelTable(NamedDimTable table) {
            this.model = table.getModel();
            this.table = table.getDimTable();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ModelTable)) {
                return false;
            }
            ModelTable that = (ModelTable) o;
            return getModel().equals(that.getModel()) &&
                    getTable().equals(that.getTable());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getModel(), getTable());
        }

    }

}
