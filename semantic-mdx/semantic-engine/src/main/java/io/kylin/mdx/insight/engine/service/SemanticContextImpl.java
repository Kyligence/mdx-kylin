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

import com.alibaba.fastjson.JSON;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.dao.CalculateMeasureMapper;
import io.kylin.mdx.insight.core.dao.CommonDimRelationMapper;
import io.kylin.mdx.insight.core.dao.CustomHierarchyMapper;
import io.kylin.mdx.insight.core.dao.DatasetMapper;
import io.kylin.mdx.insight.core.dao.DimTableModelRelMapper;
import io.kylin.mdx.insight.core.dao.NamedDimColMapper;
import io.kylin.mdx.insight.core.dao.NamedDimTableMapper;
import io.kylin.mdx.insight.core.dao.NamedMeasureMapper;
import io.kylin.mdx.insight.core.dao.NamedSetMapper;
import io.kylin.mdx.insight.core.dao.RoleInfoMapper;
import io.kylin.mdx.insight.core.entity.CalculateMeasure;
import io.kylin.mdx.insight.core.entity.CalculatedMemberNonEmptyBehaviorMeasure;
import io.kylin.mdx.insight.core.entity.CommonDimRelation;
import io.kylin.mdx.insight.core.entity.CustomHierarchy;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.entity.DatasetType;
import io.kylin.mdx.insight.core.entity.DescWrapperExtend;
import io.kylin.mdx.insight.core.entity.DimTableModelRel;
import io.kylin.mdx.insight.core.entity.DimTableType;
import io.kylin.mdx.insight.core.entity.ModelDimTableHelper;
import io.kylin.mdx.insight.core.entity.NamedDimCol;
import io.kylin.mdx.insight.core.entity.NamedDimTable;
import io.kylin.mdx.insight.core.entity.NamedMeasure;
import io.kylin.mdx.insight.core.entity.NamedSet;
import io.kylin.mdx.insight.core.entity.PropertyAttr;
import io.kylin.mdx.insight.core.entity.RoleInfo;
import io.kylin.mdx.insight.core.entity.RoleType;
import io.kylin.mdx.insight.core.entity.Visibility;
import io.kylin.mdx.insight.core.entity.VisibleAttr;
import io.kylin.mdx.insight.core.meta.ProjectModel;
import io.kylin.mdx.insight.core.model.acl.AclDataset;
import io.kylin.mdx.insight.core.model.semantic.*;
import io.kylin.mdx.insight.core.model.semantic.SemanticDataset.*;
import io.kylin.mdx.insight.core.model.semantic.SemanticDataset.AugmentedModel.AugmentDimensionTable;
import io.kylin.mdx.insight.core.model.semantic.SemanticDataset.AugmentedModel.AugmentDimensionTable.AugmentDimensionCol;
import io.kylin.mdx.insight.core.model.semantic.SemanticDataset.AugmentedModel.AugmentDimensionTable.Hierarchy0;
import io.kylin.mdx.insight.core.model.semantic.SemanticDataset.AugmentedModel.AugmentMeasure;
import io.kylin.mdx.insight.core.service.ModelService;
import io.kylin.mdx.insight.core.service.RoleInfoLoader;
import io.kylin.mdx.insight.core.service.SemanticContext;
import io.kylin.mdx.insight.core.support.AclDatasetApplier;
import io.kylin.mdx.insight.core.support.AclPermission;
import io.kylin.mdx.insight.core.support.Execution;
import io.kylin.mdx.insight.core.support.SemanticUtils;
import io.kylin.mdx.insight.core.sync.MetaStore;
import io.kylin.mdx.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SemanticContextImpl implements SemanticContext {

    private static final Pattern UNION_PATTERN = Pattern.compile("name\"\\:\"([^\"]*?)\"");

    @Autowired
    private DatasetMapper datasetMapper;

    @Autowired
    private CommonDimRelationMapper commonDimRelationMapper;

    @Autowired
    private CalculateMeasureMapper calculateMeasureMapper;

    @Autowired
    private NamedDimColMapper namedDimColMapper;

    @Autowired
    private CustomHierarchyMapper customHierarchyMapper;

    @Autowired
    private NamedMeasureMapper namedMeasureMapper;

    @Autowired
    private DimTableModelRelMapper dimTableModelRelMapper;

    @Autowired
    private NamedDimTableMapper namedDimTableMapper;

    @Autowired
    private NamedSetMapper namedSetMapper;

    @Autowired
    private RoleInfoMapper roleInfoMapper;

    @Autowired
    private AclPermission aclService;

    @Autowired
    private ModelService modelService;

    /**
     * save the modification time when the dataset was last read
     */
    private final Map<DatasetKey, Long> modifyTimeMap = new ConcurrentHashMap<>();

    /**
     * dataset cache, refresh the cache if and only if the dataset in the database has been updated
     */
    private final LoadingCache<DatasetKey, SemanticDataset> datasetCache = CacheBuilder.newBuilder()
            .expireAfterAccess(60, TimeUnit.MINUTES)
            .build(new CacheLoader<DatasetKey, SemanticDataset>() {
                @Override
                public SemanticDataset load(DatasetKey key) {
                    Execution execution = new Execution(
                            "GetOneSemanticDataset[project=%s][dataset=%s][user=%s]",
                            key.getProject(), key.getDatasetName(), key.getUsername());
                    try {
                        return createSemanticDataset(key);
                    } finally {
                        execution.logTimeConsumed();
                    }
                }
            });

    @Override
    public void clearProjectCache(String project, String username) {
        Map<DatasetKey, SemanticDataset> cacheMap = datasetCache.asMap();
        for (DatasetKey datasetKey : cacheMap.keySet()) {
            if (Objects.equals(project, datasetKey.getProject()) && Objects.equals(username, datasetKey.getUsername())) {
                datasetCache.invalidate(datasetKey);
            }
        }
    }

    @Override
    @Nonnull
    public SemanticProject createSemanticProject(String project) {
        return createSemanticProject(null, project);
    }

    @Override
    @Nonnull
    public SemanticProject createSemanticProject(String username, String project) {
        DatasetEntity search = new DatasetEntity(project);
        List<DatasetEntity> datasetEntities = datasetMapper.select(search);
        if (Utils.isCollectionEmpty(datasetEntities)) {
            throw new SemanticException(ErrorCode.NO_AVAILABLE_DATASET, project);
        }
        List<SemanticDataset> datasets = new LinkedList<>();
        for (DatasetEntity datasetEntity : datasetEntities) {
            SemanticDataset semanticDataset = getOrCreateSemanticDataset(username, datasetEntity);
            if (semanticDataset == null) {
                continue;
            }
            datasets.add(semanticDataset);
        }
        if (Utils.isCollectionEmpty(datasets)) {
            throw new SemanticException(ErrorCode.ALL_DATASET_BROKEN, project);
        }
        return new SemanticProject(project, datasets);
    }

    @Override
    @Nullable
    public SemanticDataset getSemanticDataset(String username, String project, String dataset) {
        DatasetKey datasetKey = new DatasetKey(username, project, dataset);
        try {
            return datasetCache.get(datasetKey);
        } catch (ExecutionException e) {
            return null;
        }
    }

    @Nullable
    private SemanticDataset getOrCreateSemanticDataset(String username, DatasetEntity datasetEntity) {
        // 未配置数据集修改时间，也强制失效缓存，并且移除保存的最近修改时间，否则认为这是数据集最新修改时间
        Long modifyTime = datasetEntity.getModifyTime();
        String project = datasetEntity.getProject();
        String dataset = datasetEntity.getDataset();
        // dataset 和 modifyTime 使用相同的主键，因为 Dataset可能被多个用户所使用
        DatasetKey datasetKey = new DatasetKey(username, project, dataset);

        Long lastModifyTime = modifyTimeMap.getOrDefault(datasetKey, 0L);
        if (modifyTime == null || lastModifyTime < modifyTime) {
            datasetCache.invalidate(datasetKey);
            modifyTime = modifyTime == null ? 0L : modifyTime;
        }

        // 更新角色信息时，对缓存进行失效处理
        Long roleLastModifyTime = roleInfoMapper.selectLastModifyTime();
        if (roleLastModifyTime == null || lastModifyTime < roleLastModifyTime) {
            datasetCache.invalidate(datasetKey);
            modifyTime = roleLastModifyTime != null ? roleLastModifyTime : modifyTime;
        }

        try {
            SemanticDataset semanticDataset = datasetCache.get(datasetKey);
            if (semanticDataset.getLastModified() > modifyTime) {
                modifyTime = semanticDataset.getLastModified();
            }
            modifyTimeMap.put(datasetKey, modifyTime);
            return semanticDataset;
        } catch (Exception e) {
            return null;
        }
    }

    @NonNull
    private SemanticDataset createSemanticDataset(DatasetKey datasetKey) {
        String project = datasetKey.getProject();
        String dataset = datasetKey.getDatasetName();
        String username = datasetKey.getUsername();

        DatasetEntity datasetEntity = datasetMapper.selectOne(
                DatasetEntity.builder().project(project).dataset(dataset).build());
        if (datasetEntity == null) {
            String errorMsg = String.format("The project:[%s],dataset:[%s] does not exist, please check it.",
                    project, dataset);
            throw new SemanticException(errorMsg, ErrorCode.DATASET_NOT_EXISTS);
        }

        SemanticDataset semanticDataset = new SemanticDataset(
                project, dataset, datasetEntity.getAccess(),
                SemanticConfig.getInstance().getKylinHost(),
                Integer.valueOf(SemanticConfig.getInstance().getKylinPort()),
                datasetEntity.getCreateTime(), datasetEntity.getModifyTime()
        );
        ModelDimTableHelper modelDimTableHelper = buildModelDimTableRelation(datasetEntity.getId(), semanticDataset);
        if (!validateDatasetModelsEffectInProject(modelDimTableHelper.getModelNameSet(), datasetEntity.getProject())) {
            String errorMsg = String.format("The project:[%s],dataset:[%s] invalid, please check it.",
                    project, dataset);
            throw new SemanticException(errorMsg, ErrorCode.DATASET_INVALID_IN_PROJECT);
        }

        RoleInfoLoader roleInfoLoader = new DefaultRoleInfoLoader(roleInfoMapper);
        buildAugmentModel(datasetEntity, semanticDataset, roleInfoLoader, username, modelDimTableHelper);
        buildCalculateMeasure(datasetEntity, semanticDataset, roleInfoLoader, username);
        buildNamedSet(datasetEntity, semanticDataset, roleInfoLoader, username);
        buildDimTableModelRelation(datasetEntity, semanticDataset);

        // TODO: 基于 KYLIN 列级权限级联
        AclDataset aclDataset = new AclDataset(semanticDataset);
        aclService.preparePermission(project, RoleType.USER.getType(), username, aclDataset);
        AclDatasetApplier.apply(aclDataset, semanticDataset);
        return semanticDataset;
    }

    /**
     * validate whether the models in dataset is all ready in KYLIN. If
     * any one is not ready, the dataset should be not available and don't return it
     */
    private boolean validateDatasetModelsEffectInProject(Set<String> modelNameSet, String project) {
        Map<ProjectModel, SemanticModel> semanticModelMap = modelService.getSemanticModelsByProject(project);
        for (String modelName : modelNameSet) {
            SemanticModel semanticModel = semanticModelMap.get(new ProjectModel(project, modelName));
            if (semanticModel == null) {
                return false;
            }
        }
        return true;
    }

    private void buildDimTableModelRelation(DatasetEntity datasetEntity, SemanticDataset semanticDataset) {
        List<DimTableModelRel> dimTableModelRels = dimTableModelRelMapper.select(new DimTableModelRel(datasetEntity.getId()));

        if (Utils.isCollectionEmpty(dimTableModelRels)) {
            semanticDataset.setDimTableModelRelations(Collections.emptyList());
            return;
        }

        Map<String, List<DimTableModelRel>> modelRelMap = new HashMap<>();
        for (DimTableModelRel modelRel : dimTableModelRels) {
            String modelName = modelRel.getModel();
            Utils.insertValToMap(modelRelMap, modelName, modelRel);
        }

        List<DimTableModelRelation> dimTableModelRelations = new LinkedList<>();
        modelRelMap.forEach((modelName, dimTableModelRelList) -> {
            DimTableModelRelation modelRelation = new DimTableModelRelation(modelName);

            dimTableModelRelList.forEach(dimTblRelation ->
                    modelRelation.addTableRelation(
                            new DimTableModelRelation.DimTableRelation(
                                    dimTblRelation.getDimTable(),
                                    dimTblRelation.getRelation(),
                                    dimTblRelation.getPrimaryDimCol(),
                                    dimTblRelation.getIntermediateDimTable()
                            )
                    )
            );

            dimTableModelRelations.add(modelRelation);
        });

        semanticDataset.setDimTableModelRelations(dimTableModelRelations);
    }

    private void buildCalculateMeasure(DatasetEntity datasetEntity, SemanticDataset semanticDataset, RoleInfoLoader roleInfoLoader, String username) {
        List<CalculateMeasure> calculateMeasures = calculateMeasureMapper.select(new CalculateMeasure(datasetEntity.getId()));

        if (Utils.isCollectionEmpty(calculateMeasures)) {
            semanticDataset.setCalculateMeasures(Collections.emptyList());
            return;
        }

        boolean isAllowByDefault = datasetEntity.toAllowAccessByDefault();
        DatasetEntity.ExtendHelper extendHelper = DatasetEntity.ExtendHelper.restoreModelAlias(datasetEntity);
        for (CalculateMeasure calculateMeasure : calculateMeasures) {
            String calculateMeasureFolder = extendHelper.getModelAlias(calculateMeasure.getMeasureFolder());
            calculateMeasure.setMeasureFolder(calculateMeasureFolder);
            CalculateMeasure0 calculateMeasure0 = new CalculateMeasure0(calculateMeasure);
            if (!hasPermission(roleInfoLoader, isAllowByDefault, username, calculateMeasure)) {
                calculateMeasure0.setInvisible(true);
            }
            addTranslations(calculateMeasure.getTranslation(), calculateMeasure0);
            calculateMeasure0.setNonEmptyBehaviorMeasures(
                    JSON.parseArray(calculateMeasure.getNonEmptyBehaviorMeasures(), CalculatedMemberNonEmptyBehaviorMeasure.class));
            semanticDataset.addCalculateMeasure(calculateMeasure0);
        }
    }

    private void buildNamedSet(DatasetEntity datasetEntity, SemanticDataset semanticDataset, RoleInfoLoader roleInfoLoader, String username) {
        List<NamedSet> namedSetList = namedSetMapper.select(new NamedSet(datasetEntity.getId()));

        if (Utils.isCollectionEmpty(namedSetList)) {
            semanticDataset.setNamedSets(Collections.emptyList());
            return;
        }

        boolean isAllowByDefault = datasetEntity.toAllowAccessByDefault();
        for (NamedSet namedSet : namedSetList) {
            NamedSet0 namedSet0 = new NamedSet0(namedSet);
            if (!hasPermission(roleInfoLoader, isAllowByDefault, username, namedSet)) {
                namedSet0.setInvisible(true);
            }
            semanticDataset.addNamedSet(namedSet0);
        }
    }

    private void buildAugmentModel(DatasetEntity datasetEntity, SemanticDataset semanticDataset, RoleInfoLoader roleInfoLoader, String username,
                                   ModelDimTableHelper modelDimTableHelper) {
        Integer datasetId = datasetEntity.getId();
        boolean isAllowByDefault = datasetEntity.toAllowAccessByDefault();

        List<AugmentedModel> augmentedModels = new LinkedList<>();
        Map<ProjectModel, SemanticModel> semanticModelMap = modelService.getSemanticModelsByProject(datasetEntity.getProject());

        if (semanticModelMap.isEmpty()) {
            semanticDataset.setModels(Collections.emptyList());
            return;
        }

        List<NamedDimCol> namedDimCols = namedDimColMapper.select(new NamedDimCol(datasetId));
        Map<ModelDimTableColKey, NamedDimCol> colMap = SemanticUtils.buildModelDimTableColToDimCol(namedDimCols);

        List<CustomHierarchy> customHierarchies = customHierarchyMapper.selectByDatasetId(new CustomHierarchy(datasetId));
        Map<ModelDimTableKey, List<CustomHierarchy>> hierarchyMap = SemanticUtils.buildModelDimTableToHierarchies(customHierarchies);

        List<NamedMeasure> namedMeasures = namedMeasureMapper.select(new NamedMeasure(datasetId));
        Map<String, List<NamedMeasure>> measureMap = SemanticUtils.buildModelToMeasures(namedMeasures);

        List<NamedDimTable> namedDimTables = namedDimTableMapper.select(new NamedDimTable(datasetId));
        Map<ModelDimTableKey, NamedDimTable> namedDimTableMap = SemanticUtils.buildModelDimTableToNamedTable(namedDimTables);

        DatasetEntity.ExtendHelper datasetExtHelper = DatasetEntity.ExtendHelper.restoreModelAlias(datasetEntity);

        // used for SQL datasetType
        TreeSet<String> dimColAliasSet = new TreeSet<>();
        // adjust order
        Map<String, Integer> modelsOrder = new HashMap<>();
        Map<Integer, AugmentedModel> models = new HashMap<>();
        Matcher m = UNION_PATTERN.matcher(datasetEntity.getExtend());
        int counter = 0;
        while (m.find()) {
            String model = m.group(1);
            modelsOrder.put(model, counter);
            counter++;
        }
        for (Map.Entry<ProjectModel, SemanticModel> entry : semanticModelMap.entrySet()) {
            String modelName = entry.getKey().getModel();
            SemanticModel semanticModel = entry.getValue();

            if (!modelDimTableHelper.containsModel(modelName)) {
                continue;
            }

            AugmentedModel augmentedModel = new AugmentedModel();
            augmentedModel.setModelName(semanticModel.getModelName());
            augmentedModel.setModelAlias(datasetExtHelper.getModelAlias(semanticModel.getModelName()));
            augmentedModel.setLookups(semanticModel.getJoinTables());
            augmentedModel.setFactTable(Utils.concat(SemanticConstants.DOT, semanticModel.getFactTableSchema(), semanticModel.getFactTableAlias()));

            for (DimensionTable dimTable : semanticModel.getDimensionTables()) {
                String tableName = dimTable.getName();

                NamedDimTable namedDimTable = namedDimTableMap.get(new ModelDimTableKey(modelName, tableName));

                AugmentDimensionTable augmentDimTable;
                if (namedDimTable != null) {
                    augmentDimTable = new AugmentDimensionTable();
                    augmentDimTable.setName(namedDimTable.getDimTable());
                    augmentDimTable.setActualTable(namedDimTable.getActualTable());
                    augmentDimTable.setType(namedDimTable.getDimTableType());
                    augmentDimTable.setAlias(Utils.blankToDefaultString(namedDimTable.getDimTableAlias(), namedDimTable.getDimTable()));
                } else if (modelDimTableHelper.displayThisTable(modelName, tableName)) {
                    augmentDimTable = new AugmentDimensionTable();
                    augmentDimTable.setName(tableName);
                    augmentDimTable.setActualTable(dimTable.getActualTable());
                    augmentDimTable.setType(DimTableType.REGULAR.getLowercase());
                    augmentDimTable.setAlias(tableName);
                } else {
                    continue;
                }

                if (namedDimTable != null) {
                    addTranslations(namedDimTable.getTranslation(), augmentDimTable);
                }

                if (dimTable.getDimensionColumns() != null && !dimTable.getDimensionColumns().isEmpty()) {
                    for (DimensionColumn dimCol : dimTable.getDimensionColumns()) {
                        NamedDimCol namedDimCol = colMap.get(new ModelDimTableColKey(modelName, tableName, dimCol.getColumnName()));

                        Integer dimColType;
                        String dimColAlias;
                        String dimColName;
                        String dataType;
                        String dimColDesc = "";
                        String nameColumn = null;
                        String valueColumn = null;
                        String subfolder = null;
                        String defaultMember = null;
                        List<AugmentDimensionCol.AugmentProperty> augmentProperties = null;
                        if (namedDimCol != null) {
                            DescWrapperExtend extend = namedDimCol.buildExtend();
                            dimColName = namedDimCol.getDimCol();
                            dataType = namedDimCol.getDataType();
                            dimColType = namedDimCol.getColType();
                            dimColAlias = namedDimCol.getDimColAlias();
                            dimColDesc = SemanticUtils.getDescFromExtend(extend);
                            nameColumn = namedDimCol.getNameColumn();
                            valueColumn = namedDimCol.getValueColumn();
                            subfolder = namedDimCol.getSubfolder();
                            defaultMember = namedDimCol.getDefaultMember();
                            List<PropertyAttr> properties = SemanticUtils.getPropertiesFromExtend(extend);
                            if (!properties.isEmpty()) {
                                augmentProperties = new ArrayList<>();
                                for (PropertyAttr property : properties) {
                                    augmentProperties.add(new AugmentDimensionCol.AugmentProperty(property.getName(), property.getColAlias()));
                                }
                            }

                        } else if (modelDimTableHelper.displayThisTable(modelName, tableName)) {
                            dimColName = dimCol.getColumnName();
                            dataType = dimCol.getDataType();
                            dimColType = 0;
                            dimColAlias = dimCol.getColumnName();
                        } else {
                            continue;
                        }

                        AugmentDimensionCol augmentDimensionCol = new AugmentDimensionCol(dimColName, dimColType, dataType,
                                dimColAlias, dimColDesc, nameColumn, valueColumn, augmentProperties, subfolder);
                        augmentDimensionCol.setDefaultMember(defaultMember);
                        if (!hasPermissionForDimCol(roleInfoLoader, isAllowByDefault, username, namedDimCol)) {
                            augmentDimensionCol.setInvisible(true);
                        }
                        if (namedDimCol != null) {
                            addTranslations(namedDimCol.getTranslation(), augmentDimensionCol);
                        }
                        augmentDimTable.addAugmentDimensionCol(augmentDimensionCol);
                    }
                }
                augmentedModel.addAugmentDimensionTable(augmentDimTable);

                List<CustomHierarchy> hierarchies = hierarchyMap.get(new ModelDimTableKey(modelName, tableName));
                if (Utils.isCollectionEmpty(hierarchies)) {
                    continue;
                }
                Map<String, Hierarchy0> hierarchy0Map = new HashMap<>();
                for (CustomHierarchy hierarchy : hierarchies) {
                    String name = hierarchy.getName();
                    Hierarchy0 hierarchy0 = hierarchy0Map.get(name);
                    if (hierarchy0 == null) {
                        hierarchy0 = new Hierarchy0(name);
                        hierarchy0.setDesc(hierarchy.getDesc());
                        hierarchy0Map.put(name, hierarchy0);

                        addTranslations(hierarchy.getTranslation(), hierarchy0);
                    }
                    hierarchy0.addCol(hierarchy.getDimCol(), hierarchy.getWeightCol());
                }
                hierarchy0Map.forEach((hierarchyName, hierarchy) -> augmentDimTable.addHierarchy(hierarchy));
            }

            List<NamedMeasure> modelMeasures = measureMap.get(modelName);
            for (NamedMeasure namedMeasure : modelMeasures) {
                AugmentMeasure augmentMeasure = new AugmentMeasure(namedMeasure);
                addTranslations(namedMeasure.getTranslation(), augmentMeasure);
                if (!hasPermission(roleInfoLoader, isAllowByDefault, username, namedMeasure)) {
                    augmentMeasure.setInvisible(true);
                }
                augmentedModel.addAugmentMeasure(augmentMeasure);
            }
            if (modelsOrder.size() != 0) {
                models.put(modelsOrder.get(augmentedModel.getModelName()), augmentedModel);
            } else {
                augmentedModels.add(augmentedModel);
            }
        }
        for (int i = 0; i < models.size(); i++) {
            augmentedModels.add(models.get(i));
        }
        semanticDataset.setModels(augmentedModels);
    }

    private ModelDimTableHelper buildModelDimTableRelation(Integer datasetId, SemanticDataset semanticDataset) {
        List<CommonDimRelation> commonDimRelations = commonDimRelationMapper.selectDimRelations(new CommonDimRelation(datasetId));

        ModelDimTableHelper modelDimTableHelper = new ModelDimTableHelper();

        if (Utils.isCollectionEmpty(commonDimRelations)) {
            semanticDataset.setModelRelations(Collections.emptyList());
            return modelDimTableHelper;
        }

        List<SemanticDataset.ModelRelation> modelRelations = new LinkedList<>();
        for (CommonDimRelation commonDimRel : commonDimRelations) {

            Map<String, String> split = Utils.createSplitMap(commonDimRel.getRelation());

            List<SemanticDataset.ModelRelation.Relation0> relation0s = new LinkedList<>();
            for (Map.Entry<String, String> entry : split.entrySet()) {
                modelDimTableHelper.addIgnoreItem(commonDimRel.getModelRelated(), entry.getValue());
                relation0s.add(new SemanticDataset.ModelRelation.Relation0(entry.getKey(), entry.getValue()));
            }

            modelDimTableHelper.addModelName(commonDimRel);
            modelRelations.add(new SemanticDataset.ModelRelation(commonDimRel.getModel(), commonDimRel.getModelRelated(), relation0s));
        }

        semanticDataset.setModelRelations(modelRelations);

        return modelDimTableHelper;
    }

    private void addTranslations(String translationJsonStr, Object argumentObject) {
        Consumer<AugmentCustomTranslation> f;
        if (argumentObject instanceof AugmentDimensionTable) {
            f = ((AugmentDimensionTable) argumentObject)::addCustomTranslation;
        } else if (argumentObject instanceof AugmentDimensionCol) {
            f = ((AugmentDimensionCol) argumentObject)::addCustomTranslation;
        } else if (argumentObject instanceof AugmentMeasure) {
            f = ((AugmentMeasure) argumentObject)::addCustomTranslation;
        } else if (argumentObject instanceof Hierarchy0) {
            f = ((Hierarchy0) argumentObject)::addCustomTranslation;
        } else if (argumentObject instanceof CalculateMeasure0) {
            f = ((CalculateMeasure0) argumentObject)::addCustomTranslation;
        } else {
            return;
        }
        for (Map.Entry<String, String> translation : SemanticUtils.buildTranslation(translationJsonStr).entrySet()) {
            AugmentCustomTranslation augmentCustomTranslation = new AugmentCustomTranslation(translation.getKey());
            augmentCustomTranslation.setCaption(translation.getValue());
            f.accept(augmentCustomTranslation);
        }
    }

    private boolean hasPermissionForDimCol(RoleInfoLoader roleInfoLoader, boolean isAllowByDefault, String username,
                                           NamedDimCol namedDimCol) {
        if (StringUtils.isBlank(username) || namedDimCol == null) {
            return true;
        }
        if (!namedDimCol.getVisibleFlag()) {
            return false;
        }
        return isVisible(namedDimCol, isAllowByDefault, username, roleInfoLoader);
    }

    private boolean hasPermission(RoleInfoLoader roleInfoLoader, boolean isAllowByDefault, String username,
                                  Visibility visibility) {
        if (StringUtils.isBlank(username)) {
            return true;
        }
        if (visibility == null || !visibility.getVisibleFlag()) {
            return false;
        }
        return isVisible(visibility, isAllowByDefault, username, roleInfoLoader);
    }

    @Override
    public boolean isVisible(Visibility visibility, boolean isAllowByDefault, String username,
                             RoleInfoLoader roleInfoLoader) {
        List<VisibleAttr> visibleAttrs;
        if (isAllowByDefault) {
            visibleAttrs = SemanticUtils.getInvisibleFromExtend(visibility.buildExtend());
        } else {
            visibleAttrs = SemanticUtils.getVisibleFromExtend(visibility.buildExtend());
        }
        for (VisibleAttr visibleAttr : visibleAttrs) {
            if (RoleType.USER.getType().equals(visibleAttr.getType()) &&
                    username.equalsIgnoreCase(visibleAttr.getName())) {
                return !isAllowByDefault;
            }
            // for group type, check whether contains this user.
            if (RoleType.GROUP.getType().equals(visibleAttr.getType())) {
                List<String> users = MetaStore.getInstance().getUsersByGroup(visibleAttr.getName());
                for (String user : users) {
                    if (username.equalsIgnoreCase(user)) {
                        return !isAllowByDefault;
                    }
                }
            }
            // for role type, check whether contains this user
            if (RoleType.ROLE.getType().equals(visibleAttr.getType())) {
                RoleInfo roleInfo = new RoleInfo(visibleAttr.getName());
                List<VisibleAttr> roleAttrs = roleInfoLoader.load(roleInfo);
                if (!Utils.isCollectionEmpty(roleAttrs)) {
                    if (isMatchForRoleType(roleAttrs, username)) {
                        return !isAllowByDefault;
                    }
                }
            }
        }
        // 白名单模式需要检查一下默认角色，黑名单模式直接结束
        if (isAllowByDefault) {
            return true;
        }
        return isVisibleForDefaultRole(username, roleInfoLoader);
    }

    private static boolean isVisibleForDefaultRole(String username, RoleInfoLoader roleInfoLoader) {
        // check whether default role contains this user
        RoleInfo roleInfo = new RoleInfo(SemanticConstants.DEFAULT_ROLE);
        List<VisibleAttr> roleAttrs = roleInfoLoader.load(roleInfo);
        if (roleAttrs == null) {
            return false;
        }
        if (!Utils.isCollectionEmpty(roleAttrs)) {
            return isMatchForRoleType(roleAttrs, username);
        }
        return false;
    }

    private static boolean isMatchForRoleType(List<VisibleAttr> roleAttrs, String username) {
        for (VisibleAttr roleAttr : roleAttrs) {
            if (RoleType.USER.getType().equals(roleAttr.getType()) &&
                    username.equalsIgnoreCase(roleAttr.getName())) {
                return true;
            }
            if (RoleType.GROUP.getType().equals(roleAttr.getType())) {
                String groupName = roleAttr.getName();
                List<String> users = MetaStore.getInstance().getUsersByGroup(groupName);
                for (String user : users) {
                    if (username.equalsIgnoreCase(user)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static class DefaultRoleInfoLoader implements RoleInfoLoader {

        private final RoleInfoMapper roleInfoMapper;

        private final Map<RoleInfo, List<VisibleAttr>> cache = new HashMap<>();

        public DefaultRoleInfoLoader(RoleInfoMapper roleInfoMapper) {
            this.roleInfoMapper = roleInfoMapper;
        }

        @Override
        public List<VisibleAttr> load(RoleInfo roleInfo) {
            if (cache.containsKey(roleInfo)) {
                return cache.get(roleInfo);
            }
            RoleInfo result = roleInfoMapper.selectOne(roleInfo);
            List<VisibleAttr> attrs = null;
            if (result != null) {
                attrs = result.extractVisibleFromExtend();
            }
            cache.put(roleInfo, attrs);
            return attrs;
        }

    }

    @Data
    @AllArgsConstructor
    public static class DatasetKey {

        private String username;

        private String project;

        private String datasetName;

    }

}
