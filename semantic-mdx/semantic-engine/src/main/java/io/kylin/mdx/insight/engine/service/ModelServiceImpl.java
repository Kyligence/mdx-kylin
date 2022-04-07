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


package io.kylin.mdx.insight.engine.service;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.meta.SemanticAdapter;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.model.semantic.SemanticModel;
import io.kylin.mdx.insight.core.service.ModelService;
import org.springframework.stereotype.Service;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.NamedDimTable;
import io.kylin.mdx.insight.core.meta.ProjectModel;
import io.kylin.mdx.insight.core.model.generic.ActualTable;
import io.kylin.mdx.insight.core.model.generic.ColumnIdentity;
import io.kylin.mdx.insight.core.model.generic.CubeMeasure;
import io.kylin.mdx.insight.core.model.semantic.DimensionColumn;
import io.kylin.mdx.insight.core.model.semantic.DimensionTable;
import io.kylin.mdx.insight.core.support.SemanticUtils;
import io.kylin.mdx.insight.core.sync.DatasetValidator;
import io.kylin.mdx.insight.core.sync.MetaStore;

import java.util.*;

@Service
public class ModelServiceImpl implements ModelService {
    private final SemanticAdapter semanticAdapter = SemanticAdapter.INSTANCE;

    private final MetaStore metaStore = MetaStore.getInstance();
    @Override
    public List<KylinGenericModel> getCachedGenericModels(String project) {
        List<KylinGenericModel> genericModels = metaStore.getProject2Models().get(project);
        if (genericModels == null || genericModels.size() < 1) {
            //go to underlying datasource fetch generic models
            genericModels = loadGenericModels(project);
            if (Utils.isCollectionEmpty(genericModels)) {
                return Collections.emptyList();
            }
        }
        return genericModels;
    }

    @Override
    public List<KylinGenericModel> loadGenericModels(String project) {
        List<KylinGenericModel> genericModels = semanticAdapter.getNocacheGenericModels(project);
        addModelsToProject(project, genericModels);
        return genericModels;
    }

    @Override
    public void refreshGenericModels(String project, List<KylinGenericModel> genericModels) {
        addModelsToProject(project, genericModels);
    }

    @Override
    public Map<ProjectModel, SemanticModel> getSemanticModelsByProject(String project) {
        List<KylinGenericModel> genericModels = getCachedGenericModels(project);
        if (Utils.isCollectionEmpty(genericModels)) {
            return Collections.emptyMap();
        }
        Map<ProjectModel, SemanticModel> modelMap = new HashMap<>();
        for (KylinGenericModel genericModel : genericModels) {
            ProjectModel projectModel = new ProjectModel(project, genericModel.getModelName());
            SemanticModel semanticModel = buildSemanticModel(projectModel);
            if (semanticModel != null) {
                modelMap.put(projectModel, semanticModel);
            }
        }
        return modelMap;
    }

    @Override
    public SemanticModel getSemanticModel(String project, String model) {
        return buildSemanticModel(project, model);
    }

    @Override
    public SemanticModel buildSemanticModelInternal(String project, String model, KylinGenericModel genericModel) {
        SemanticModel semanticModel = new SemanticModel();
        semanticModel.setProject(project);
        semanticModel.setModelName(model);
        semanticModel.setFactTableAlias(genericModel.getFactTable().getLeft());
        semanticModel.setFactTableSchema(genericModel.getFactTable().getRight().getSchema());
        semanticModel.setJoinTables(genericModel.getRawJoinTableInfo());

        Map<String, DimensionTable> dimTableMap = createDimensionTableMap(genericModel, new HashMap<>(), new HashMap<>());
        semanticModel.setDimensionTables(new ArrayList<>(dimTableMap.values()));

        // remove measure which semantic not support
        List<CubeMeasure> cubeMeasures = genericModel.getCubeMeasures();
        List<CubeMeasure> semanticMeasure = new ArrayList<>();
        for (CubeMeasure cubeMeasure : cubeMeasures) {
            if (cubeMeasure.getExpression().equalsIgnoreCase("TOP_N")
                    || cubeMeasure.getExpression().equalsIgnoreCase("CORR")
                    || cubeMeasure.getExpression().equalsIgnoreCase("PERCENTILE_APPROX")) {
                continue;
            }
            semanticMeasure.add(cubeMeasure);
        }
        semanticModel.setCubeMeasures(semanticMeasure);
        return semanticModel;
    }

    @Override
    public Map<String, DimensionTable> createDimensionTableMap(KylinGenericModel genericModel, Map<DatasetValidator.ModelTable, NamedDimTable> oldTableMap, Map<String, Integer> namedDimTableCountMap) {
        Map<String, ActualTable> tblAlias2ActTbl = genericModel.getTblAlias2ActTbl();
        Set<ColumnIdentity> cubeDimensions = genericModel.getCubeDimensions();
        Map<String, DimensionTable> dimTableMap = new HashMap<>();
        for (ColumnIdentity colIdentity : cubeDimensions) {
            String tableAlias = colIdentity.getTableAlias();
            DimensionColumn dimCol = new DimensionColumn(
                    colIdentity.getColName(),
                    colIdentity.getColAlias(),
                    tableAlias,
                    tblAlias2ActTbl.get(tableAlias),
                    SemanticUtils.getColumnInfo(genericModel, colIdentity)
            );

            DimensionTable dimensionTable = dimTableMap.get(tableAlias);
            if (dimensionTable == null) {
                dimensionTable = new DimensionTable(tableAlias, tblAlias2ActTbl.get(tableAlias).getFormatStr());
                DatasetValidator.ModelTable newModelTable = new DatasetValidator.ModelTable(genericModel.getModelName(), tableAlias, null);
                // 若旧表map中数量小于1，表示为新增数据集，若旧表Map中包含维表，说明不是KE中新增的维表
                if (oldTableMap.size() < 1 || oldTableMap.containsKey(newModelTable)) {
                    dimTableMap.put(tableAlias, dimensionTable);
                } else {
                    dimensionTable.setAlias(tableAlias);
                    int dimTableNum = 0;
                    if (namedDimTableCountMap.containsKey(tableAlias)) {
                        dimTableNum = namedDimTableCountMap.get(tableAlias);
                    }
                    if (dimTableNum > 0) {
                        dimensionTable.setAlias(tableAlias + "_" + (dimTableNum + 1));
                    }
                    dimTableMap.put(tableAlias, dimensionTable);
                    namedDimTableCountMap.put(tableAlias, dimTableNum + 1);
                }
            }
            dimensionTable.addDimensionColumn(dimCol);
        }

        // 增加维表场景：cube设计时维表里维度都未勾选，但度量里却包含该表里的维度
        List<CubeMeasure> measures = genericModel.getCubeMeasures();
        for (CubeMeasure cubeMeasure : measures) {
            ColumnIdentity columnIdentity = cubeMeasure.getColMeasured();
            if (columnIdentity == null || !StringUtils.isNotBlank(columnIdentity.getTableAlias()) || "constant".equalsIgnoreCase(columnIdentity.getTableAlias())) {
                continue;
            }
            String tableName = columnIdentity.getTableAlias();
            DimensionTable dimensionTable = dimTableMap.get(tableName);
            if (dimensionTable != null) {
                continue;
            }
            dimensionTable = new DimensionTable(tableName, tableName, "");
            // 长度小于1，说明非同步任务调用
            if (oldTableMap.size() < 1 || namedDimTableCountMap.size() < 1) {
                dimensionTable.addDimensionColumnFromMeasure();
                dimTableMap.put(tableName, dimensionTable);
                continue;
            }
            String tableAlias = tableName;
            int dimTableNum = 0;
            if (namedDimTableCountMap.containsKey(tableName)) {
                dimTableNum = namedDimTableCountMap.get(tableName);
            }
            // 如果之前的cube中已包含此维表，则对维表改名
            if (dimTableNum > 0) {
                tableAlias = tableName + "_" + (dimTableNum + 1);
            }
            dimensionTable = new DimensionTable(tableName, tableAlias, "");
            dimTableMap.put(tableName, dimensionTable);
            namedDimTableCountMap.put(tableAlias, dimTableNum + 1);
            dimensionTable.addDimensionColumnFromMeasure();
        }
        return dimTableMap;
    }

    @Override
    public List<String> getDimensionTableWithoutFactTableByModel(String project, String model) {
        List<String> dimTableNames = new LinkedList<>();
        SemanticModel semanticModel = getSemanticModel(project, model);
        if (semanticModel == null) {
            return dimTableNames;
        }
        String factTableAlias = semanticModel.getFactTableAlias();
        for (DimensionTable dimTable : semanticModel.getDimensionTables()) {
            if (StringUtils.isNotBlank(factTableAlias) && factTableAlias.equalsIgnoreCase(dimTable.getName())) {
                continue;
            }
            dimTableNames.add(dimTable.getName());
        }
        return dimTableNames;
    }

    @Override
    public List<String> getDimtablesByModel(String project, String model) throws SemanticException {
        return getDimensionTableWithoutFactTableByModel(project, model);
    }

    @Override
    public SemanticModel getSemanticModelDetail(String project, String model) throws SemanticException {
        return getSemanticModel(project, model);
    }

    @Override
    public List<KylinGenericModel> getModelsByProject(String project) throws SemanticException {
        return getCachedGenericModels(project);
    }

    @Override
    public List<String> getCachedProjectNames() {
        List<String> projectNames = new LinkedList<>();
        metaStore.getProject2Models().forEach((name, genericModels) -> projectNames.add(name));
        return projectNames;
    }

    private void addModelsToProject(String project, List<KylinGenericModel> genericModels) {
        if (genericModels != null && genericModels.isEmpty() && metaStore.getNotFoundProjects().contains(project)) {
            return;
        }
        metaStore.setProject2Models(project, genericModels);
    }

    private SemanticModel buildSemanticModel(ProjectModel projectModel) {
        return buildSemanticModel(projectModel.getProject(), projectModel.getModel());
    }

    private SemanticModel buildSemanticModel(String project, String model) {
        KylinGenericModel genericModel = filterGenericModel(project, model);
        if (genericModel == null) {
            return null;
        }
        return buildSemanticModelInternal(project, model, genericModel);
    }

    private KylinGenericModel filterGenericModel(String project, String model) {
        List<KylinGenericModel> genericModels = metaStore.getProject2Models().get(project);
        if (Utils.isCollectionEmpty(genericModels)) {
            /*
             *  There is two circumstances going here
             *  1. the project name is fake
             *  2. the project hasn't loaded its generic models
             */
            genericModels = loadGenericModels(project);
            if (Utils.isCollectionEmpty(genericModels)) {
                return null;
            }
        }
        for (KylinGenericModel genericModel : genericModels) {
            if (model.equalsIgnoreCase(genericModel.getModelName())) {
                return genericModel;
            }
        }
        return null;
    }
}
