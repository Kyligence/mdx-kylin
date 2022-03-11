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

import com.alibaba.fastjson.JSONArray;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.entity.DatasetType;
import io.kylin.mdx.insight.engine.bean.SimpleSchema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.*;

@Data
@NoArgsConstructor
public class DatasetDTO {

    private Integer datasetId;

    @NotBlank
    private String project;

    @NotBlank
    @JsonProperty("dataset_name")
    @Length(max = 100)
    private String datasetName;

    private Integer access;

    private String canvas;

    @JsonProperty("front_v")
    private String frontVersion;

    @NotNull
    @JsonProperty("model_relations")
    private List<CommonDimModelDTO> modelRelations;

    @NotNull
    @Valid
    private List<SemanticModelDTO> models;

    @NotNull
    @JsonProperty("calculate_measures")
    @Valid
    private List<CalculationMeasureDTO> calculateMeasures;

    @NotNull
    @JsonProperty("named_sets")
    @Valid
    private List<NamedSetDTO> namedSets;

    @NotNull
    @JsonProperty("dim_table_model_relations")
    private List<DimTableModelRelationDTO> dimTableModelRelations;

    @JsonProperty("translation_types")
    private List<String> translationTypes;

    public DatasetDTO(DatasetEntity datasetEntity) throws SemanticException {
        this.datasetId = datasetEntity.getId();
        this.project = datasetEntity.getProject();
        this.datasetName = datasetEntity.getDataset();
        this.access = datasetEntity.getAccess();
        this.canvas = datasetEntity.getCanvas();
        this.frontVersion = datasetEntity.getFrontVersion();
        this.translationTypes = JSONArray.parseArray(datasetEntity.getTranslationTypes(), String.class);
        this.modelRelations = new ArrayList<>();
        this.calculateMeasures = new ArrayList<>();
        this.namedSets = new ArrayList<>();
        this.dimTableModelRelations = new ArrayList<>();
    }

    public DatasetDTO(DatasetDTO copyDatasetDTO) {
        this.datasetId = copyDatasetDTO.datasetId;
        this.project = copyDatasetDTO.project;
        this.datasetName = copyDatasetDTO.datasetName;
        this.access = copyDatasetDTO.access;
        this.canvas = copyDatasetDTO.canvas;
        this.modelRelations = copyDatasetDTO.modelRelations;
        this.models = copyDatasetDTO.models;
        this.calculateMeasures = copyDatasetDTO.calculateMeasures;
        this.namedSets = copyDatasetDTO.namedSets;
        this.dimTableModelRelations = copyDatasetDTO.dimTableModelRelations;
        this.frontVersion = copyDatasetDTO.frontVersion;
        this.translationTypes = copyDatasetDTO.translationTypes;
    }

    public void addSemanticModelDTO(SemanticModelDTO semanticModelDTO) {
        if (models == null) {
            models = new LinkedList<>();
        }
        models.add(semanticModelDTO);
    }

    private void populate(HierarchyDTO hierarchyDTO, Set<SimpleSchema.Hierarchy> hierarchies) {
        if (hierarchyDTO == null || hierarchies == null) {
            return;
        }

        SimpleSchema.Hierarchy hierarchy = new SimpleSchema.Hierarchy();
        hierarchy.setName(hierarchyDTO.getName() + "-Hierarchy");
        Set<String> tableColAliases2 = new HashSet<>();
        hierarchy.setTableColAliases(tableColAliases2);
        List<String> dimCols = hierarchyDTO.getDimCols();
        if (dimCols != null && !dimCols.isEmpty()) {
            tableColAliases2.addAll(dimCols);
        }

        hierarchies.add(hierarchy);
    }

    private void populate(DimensionTableDTO dimensionTableDTO, Set<SimpleSchema.DimensionTable> dimensionTables, String modelName) {
        if (dimensionTableDTO == null || dimensionTables == null) {
            return;
        }

        SimpleSchema.DimensionTable dimensionTable = new SimpleSchema.DimensionTable();
        dimensionTable.setAlias(dimensionTableDTO.getAlias());
        dimensionTable.setType(dimensionTableDTO.getType());
        dimensionTable.setModel(modelName);
        Set<SimpleSchema.DimensionCol> tableColAliases = new HashSet<>();
        dimensionTable.setTableColAliases(tableColAliases);
        Set<SimpleSchema.Hierarchy> hierarchies = new HashSet<>();
        dimensionTable.setHierarchies(hierarchies);

        for (DimensionColDTO dimensionColDTO : dimensionTableDTO.getDimCols()) {
            SimpleSchema.DimensionCol dimensionCol = new SimpleSchema.DimensionCol();
            dimensionCol.setTableColAlias(dimensionColDTO.getAlias());
            dimensionCol.setType(dimensionColDTO.getType());
            tableColAliases.add(dimensionCol);
        }

        List<HierarchyDTO> hierarchys = dimensionTableDTO.getHierarchys();
        if (hierarchys != null && !hierarchys.isEmpty()) {
            for (HierarchyDTO hierarchyDTO : hierarchys) {
                populate(hierarchyDTO, hierarchies);
            }
        }

        dimensionTables.add(dimensionTable);
    }

    private void populate(SemanticModelDTO model, Set<String> measureAliases, Set<SimpleSchema.DimensionTable> dimensionTables) {
        if (model == null || measureAliases == null || dimensionTables == null) {
            return;
        }

        for (MeasureDTO measureDTO : model.getMeasures()) {
            measureAliases.add(measureDTO.getAlias());
        }

        List<DimensionTableDTO> dimensionTableDTOs = model.getDimensionTables();
        if (dimensionTableDTOs == null || dimensionTableDTOs.isEmpty()) {
            return;
        }

        for (DimensionTableDTO dimensionTableDTO : dimensionTableDTOs) {
            populate(dimensionTableDTO, dimensionTables, model.getModelName());
        }
    }

    public SimpleSchema makeSimpleSchema() {
        SimpleSchema ss = new SimpleSchema();
        Set<String> measureAliases = new HashSet<>();
        ss.setMeasureAliases(measureAliases);
        Set<String> calcMeasureNames = new HashSet<>();
        ss.setCalcMeasureNames(calcMeasureNames);
        Set<SimpleSchema.NamedSet> namedSets2 = new HashSet<>();
        ss.setNamedSets(namedSets2);
        Set<SimpleSchema.DimensionTable> dimensionTables = new HashSet<>();
        ss.setDimensionTables(dimensionTables);

        for (SemanticModelDTO model : models) {
            populate(model, measureAliases, dimensionTables);
        }

        for (CalculationMeasureDTO calculationMeasureDTO : calculateMeasures) {
            calcMeasureNames.add(calculationMeasureDTO.getName());
        }

        for (NamedSetDTO namedSetDTO : namedSets) {
            SimpleSchema.NamedSet ns = new SimpleSchema.NamedSet();
            ns.setName(namedSetDTO.getName());
            ns.setExpression(namedSetDTO.getExpression());
            ns.setLocation(namedSetDTO.getLocation());
            namedSets2.add(ns);
        }

        return ss;
    }

}
