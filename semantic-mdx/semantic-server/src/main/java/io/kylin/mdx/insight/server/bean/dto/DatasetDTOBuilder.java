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


package io.kylin.mdx.insight.server.bean.dto;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.CalculateMeasure;
import io.kylin.mdx.insight.core.entity.CommonDimRelation;
import io.kylin.mdx.insight.core.entity.CustomHierarchy;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.entity.DimTableModelRel;
import io.kylin.mdx.insight.core.entity.NamedDimCol;
import io.kylin.mdx.insight.core.entity.NamedDimTable;
import io.kylin.mdx.insight.core.entity.NamedMeasure;
import io.kylin.mdx.insight.core.entity.NamedSet;
import io.kylin.mdx.insight.core.model.semantic.ModelDimTableKey;
import io.kylin.mdx.insight.core.support.SemanticUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DatasetDTOBuilder {

    private final DatasetDTO datasetDTO;

    private final TreeSet<String> modelUsed = new TreeSet<>();

    public DatasetDTOBuilder(DatasetEntity datasetEntity) throws SemanticException {
        this.datasetDTO = new DatasetDTO(datasetEntity);
    }

    public DatasetDTOBuilder withCommonDimRelations(List<CommonDimRelation> commonDimRelations) {
        if (Utils.isNotEmpty(commonDimRelations)) {
            datasetDTO.setModelRelations(commonDimRelations
                    .stream()
                    .map(commonDimRelation -> {
                        if (StringUtils.isNotBlank(commonDimRelation.getModel())) {
                            modelUsed.add(commonDimRelation.getModel());
                        }
                        if (StringUtils.isNotBlank(commonDimRelation.getModelRelated())) {
                            modelUsed.add(commonDimRelation.getModelRelated());
                        }
                        return new CommonDimModelDTO(commonDimRelation);
                    })
                    .collect(Collectors.toList())
            );
        }
        return this;
    }

    public DatasetDTOBuilder withModelDetails(DatasetEntity datasetEntity, List<NamedDimTable> namedDimTables, List<NamedDimCol> namedDimCols,
                                              List<CustomHierarchy> customHierarchies, List<NamedMeasure> namedMeasures) throws SemanticException {

        Map<String, List<NamedDimTable>> namedTablesMap = SemanticUtils.buildModelToNamedTables(namedDimTables);
        Map<ModelDimTableKey, List<CustomHierarchy>> customHierarchyMap = SemanticUtils.buildModelDimTableToHierarchies(customHierarchies);
        Map<ModelDimTableKey, List<NamedDimCol>> namedDimColsMap = SemanticUtils.buildModelDimTableToDimCols(namedDimCols);
        Map<String, List<NamedMeasure>> namedMeasureMap = SemanticUtils.buildModelToMeasures(namedMeasures);
        DatasetEntity.ExtendHelper datasetExtHelper = DatasetEntity.ExtendHelper.restoreModelAlias(datasetEntity);

        LinkedHashSet<String> models = getModelsAsInsertFromExtend(datasetEntity);
        Set<String> chosenModels = models.size() == 0 ? modelUsed : models;
        for (String modelName : chosenModels) {
            List<NamedDimTable> namedDimTableList = namedTablesMap.get(modelName);

            if (namedDimTableList == null) {
                namedDimTableList = Collections.emptyList();
            }

            SemanticModelDTO semanticModelDTO = new SemanticModelDTO(modelName);
            semanticModelDTO.setModelAlias(datasetExtHelper.getModelAlias(modelName));

            if (!Utils.isCollectionEmpty(namedDimTableList)) {
                semanticModelDTO.setFactTable(namedDimTableList.get(0).getFactTable());
            }
            for (NamedDimTable namedDimTable : namedDimTableList) {

                DimensionTableDTO dimTableDTO = new DimensionTableDTO(namedDimTable);

                ModelDimTableKey modelDimTableKey = new ModelDimTableKey(modelName, namedDimTable.getDimTable());
                List<NamedDimCol> namedDimColList = namedDimColsMap.get(modelDimTableKey);

                if (!Utils.isCollectionEmpty(namedDimColList)) {
                    for (NamedDimCol namedDimCol : namedDimColList) {
                        dimTableDTO.addDimensionCol(new DimensionColDTO(namedDimCol));
                    }
                }

                List<CustomHierarchy> customHierarchyList = customHierarchyMap.get(modelDimTableKey);
                if (Utils.isCollectionEmpty(customHierarchyList)) {
                    dimTableDTO.setHierarchys(Collections.emptyList());
                } else {
                    customHierarchyList.stream()
                            .collect(Collectors.groupingBy(CustomHierarchy::getName))
                            .forEach((hierarchyName, cHierarchies) ->
                                    dimTableDTO.addHierarchy(new HierarchyDTO(hierarchyName, cHierarchies))
                            );

                }
                semanticModelDTO.addDimensionTableDTO(dimTableDTO);
            }
            if (semanticModelDTO.getDimensionTables() == null) {
                semanticModelDTO.setDimensionTables(Collections.emptyList());
            }
            List<NamedMeasure> namedMeasureList = namedMeasureMap.get(modelName);
            if (namedMeasureList != null) {
                for (NamedMeasure namedMeasure : namedMeasureList) {
                    semanticModelDTO.addMeasureDTO(new MeasureDTO(namedMeasure));
                }
            }
            if (semanticModelDTO.getMeasures() == null) {
                semanticModelDTO.setMeasures(Collections.emptyList());
            }
            datasetDTO.addSemanticModelDTO(semanticModelDTO);
        }
        return this;
    }

    public LinkedHashSet<String> getModelsAsInsertFromExtend(DatasetEntity datasetEntity) {
        String extend = datasetEntity.getExtend();
        LinkedHashSet<String> models = new LinkedHashSet<>();
        final Pattern unionPattern_1 = Pattern.compile("name\":\"([^\"]*?)\"");
        Matcher m = unionPattern_1.matcher(extend);
        while (m.find()) {
            models.add(m.group(1));
        }
        return models;
    }

    public DatasetDTOBuilder calculateMeasures(List<CalculateMeasure> calculateMeasures) {
        if (Utils.isNotEmpty(calculateMeasures)) {
            datasetDTO.setCalculateMeasures(
                    calculateMeasures
                            .stream()
                            .map(CalculationMeasureDTO::new)
                            .collect(Collectors.toList())
            );
        }
        return this;
    }

    public DatasetDTOBuilder namedSets(List<NamedSet> namedSets) {
        if (Utils.isNotEmpty(namedSets)) {
            datasetDTO.setNamedSets(
                    namedSets
                            .stream()
                            .map(NamedSetDTO::new)
                            .collect(Collectors.toList())
            );
        }
        return this;
    }

    public DatasetDTOBuilder dimTableModelRels(List<DimTableModelRel> dimTableModelRels) {
        if (Utils.isNotEmpty(dimTableModelRels)) {
            Map<String, List<DimTableModelRel>> modelRelMap = new HashMap<>();
            dimTableModelRels.forEach(
                    modelRel -> Utils.insertValToMap(modelRelMap, modelRel.getModel(), modelRel)
            );
            List<DimTableModelRelationDTO> modelRelationDTOs = new LinkedList<>();
            modelRelMap.forEach(
                    (modelName, modelRelations) ->
                            modelRelationDTOs.add(new DimTableModelRelationDTO(modelName, modelRelations))
            );
            datasetDTO.setDimTableModelRelations(modelRelationDTOs);
        }
        return this;
    }

    public DatasetDTO build() {
        return this.datasetDTO;
    }

}
