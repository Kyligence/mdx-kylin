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
import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.AESWithECBEncryptor;
import org.apache.commons.lang3.StringUtils;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.dao.*;
import io.kylin.mdx.insight.core.entity.*;
import io.kylin.mdx.insight.core.service.DatasetService;
import io.kylin.mdx.insight.core.model.generic.CubeMeasure;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.model.semantic.DatasetStatus;
import io.kylin.mdx.insight.core.model.semantic.DimensionColumn;
import io.kylin.mdx.insight.core.model.semantic.DimensionTable;
import io.kylin.mdx.insight.core.model.semantic.SemanticModel;
import io.kylin.mdx.insight.core.service.ModelService;
import io.kylin.mdx.insight.core.service.UserService;
import io.kylin.mdx.insight.core.support.SemanticUtils;
import io.kylin.mdx.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Condition;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DatasetServiceImpl implements DatasetService {

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
    private UserService userService;

    @Autowired
    private ModelService modelService;

    @Override
    public DatasetEntity selectDatasetById(Integer datasetId) {
        return datasetMapper.selectByPrimaryKey(datasetId);
    }

    @Override
    public List<String> getProjectsRelatedDataset() {
        return datasetMapper.selectAllProjectNames();
    }

    @Override
    public List<DatasetEntity> selectDatasetBySearch(Integer datasetType, Long begin, Long end) {
        return datasetMapper.selectDatasetByTime(datasetType, begin, end);
    }

    @Override
    public List<DatasetEntity> selectDatasetsBySearch(DatasetEntity search) {
        return datasetMapper.select(search);
    }

    @Override
    public List<CommonDimRelation> selectCommonDimRelsByDatasetId(Integer datasetId) {
        return commonDimRelationMapper.selectDimRelations(new CommonDimRelation(datasetId));
    }

    @Override
    public List<CalculateMeasure> selectCMsByDatasetId(Integer datasetId) {
        return calculateMeasureMapper.select(new CalculateMeasure(datasetId));
    }

    @Override
    public List<NamedDimCol> selectDimColsByDatasetId(Integer datasetId) {
        Condition condition = new Condition(NamedDimCol.class);
        Example.Criteria criteria = condition.createCriteria();
        criteria.andEqualTo("datasetId", datasetId);
        condition.setOrderByClause(" model, dim_table, dim_col ASC");
        return namedDimColMapper.selectByExample(condition);
    }

    @Override
    public NamedDimTable selectOneDimTableBySearch(Integer datasetId, String model, String tableAlias) {
        NamedDimTable search = new NamedDimTable(datasetId);
        search.setModel(model);
        search.setDimTableAlias(tableAlias);
        return namedDimTableMapper.selectOne(search);
    }

    @Override
    public NamedDimCol selectOneDimColsBySearch(Integer datasetId, String model, String table, String dimColAlias) {
        NamedDimCol search = new NamedDimCol(datasetId);
        search.setModel(model);
        search.setDimTable(table);
        search.setDimColAlias(dimColAlias);
        return namedDimColMapper.selectOne(search);
    }

    @Override
    public NamedMeasure selectOneMeasureBySearch(Integer datasetId, String model, String measure) {
        NamedMeasure search = new NamedMeasure(datasetId);
        search.setModel(model);
        search.setAlias(measure);
        return namedMeasureMapper.selectOne(search);
    }

    @Override
    public CalculateMeasure selectOneCalcMeasureBySearch(Integer datasetId, String name) {
        CalculateMeasure search = new CalculateMeasure(datasetId);
        search.setName(name);
        return calculateMeasureMapper.selectOne(search);
    }

    @Override
    public NamedSet selectOneNamedSetBySearch(Integer datasetId, String name) {
        NamedSet search = new NamedSet(datasetId);
        search.setName(name);
        return namedSetMapper.selectOne(search);
    }

    @Override
    public List<CustomHierarchy> selectHierarchiesByDatasetId(Integer datasetId) {
        return customHierarchyMapper.selectByDatasetId(new CustomHierarchy(datasetId));
    }

    @Override
    public List<DimTableModelRel> selectDimTblModelRelsByDatasetId(Integer datasetId) {
        return dimTableModelRelMapper.select(new DimTableModelRel(datasetId));
    }

    @Override
    public List<DimTableModelRel> selectDimTblModelRelsBySearch(DimTableModelRel dimTableModelRel) {
        return dimTableModelRelMapper.select(dimTableModelRel);
    }

    @Override
    public List<NamedDimTable> selectDimTablesByDatasetId(Integer datasetId) {
        return namedDimTableMapper.select(new NamedDimTable(datasetId));
    }

    @Override
    public List<NamedMeasure> selectMeasuresByDatasetId(Integer datasetId) {
        Condition condition = new Condition(NamedMeasure.class);
        Example.Criteria criteria = condition.createCriteria();
        criteria.andEqualTo("datasetId", datasetId);
        condition.setOrderByClause(" model, name ASC");
        return namedMeasureMapper.selectByExample(condition);
    }

    @Override
    public List<CalculateMeasure> selectCalculateMeasuresByDatasetId(Integer datasetId) {
        Condition condition = new Condition(CalculateMeasure.class);
        Example.Criteria criteria = condition.createCriteria();
        criteria.andEqualTo("datasetId", datasetId);
        condition.setOrderByClause(" name ASC");
        return calculateMeasureMapper.selectByExample(condition);
    }

    @Override
    public List<NamedSet> selectNamedSetsByDatasetId(Integer datasetId) {
        return namedSetMapper.select(new NamedSet(datasetId));
    }

    @Override
    public DatasetEntity getDataSetEntityByNameAndProjectName(String project, String dataName) {
        Condition condition = new Condition(DatasetEntity.class);
        Example.Criteria criteria = condition.createCriteria();
        criteria.andEqualTo("project", project);
        criteria.andEqualTo("dataset", dataName);
        return datasetMapper.selectOneByExample(condition);
    }

    @Override
    public List<DatasetEntity> getDataSetEntitysByPage(String project, String dataset, int pageNum, int pageSize, String orderByClause) {
        return datasetMapper.selectAllByPage(project, dataset, new RowBounds(pageNum, pageSize), orderByClause);
    }

    @Override
    public List<DatasetEntity> selectDatasetByProjectName(String project) {
        return datasetMapper.selectDatasetByProjectName(new DatasetEntity(project));
    }

    @Override
    public List<NamedDimCol> selectAllDimColByDatasetIds(List<Integer> datasetIds) {
        return namedDimColMapper.selectAllDimColByDatasetIds(datasetIds);
    }

    @Override
    public List<NamedDimCol> selectAllDimColByDatasetIdAndTable(NamedDimCol namedDimCol) {
        return namedDimColMapper.select(namedDimCol);
    }

    @Override
    public List<NamedDimTable> selectAllDimTableByDatasetIds(List<Integer> datasetIds) {
        return namedDimTableMapper.selectAllDimTableByDatasetIds(datasetIds);
    }

    @Override
    public int deleteNamedCol(NamedDimCol namedDimCol) {
        return namedDimColMapper.deleteBySelective(namedDimCol);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void deleteTableLogically(List<NamedDimTable> toDeleteTables) {
        for (NamedDimTable deleteTable : toDeleteTables) {
            validateTableWhenDelete(deleteTable);

            namedDimTableMapper.deleteByPrimaryKey(deleteTable.getId());
            deleteNamedCol(new NamedDimCol(deleteTable.getDatasetId(), deleteTable.getModel(), deleteTable.getDimTable()));
            dimTableModelRelMapper.delete(new DimTableModelRel(deleteTable.getDatasetId(), deleteTable.getModel(), deleteTable.getDimTable()));
        }
    }

    private void validateTableWhenDelete(NamedDimTable toDeleteTable) {
        if (toDeleteTable == null || toDeleteTable.getDatasetId() == null
                || StringUtils.isBlank(toDeleteTable.getModel())
                || StringUtils.isBlank(toDeleteTable.getDimTable())) {
            throw new RuntimeException("Delete dimTable mast have datasetId, model name, table name. But table entity is: " + JSON.toJSONString(toDeleteTable));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void insertTableLogically(List<NamedDimTable> toAddTables, List<NamedDimCol> toAddNamedDimCols) {
        insertNamedTable(toAddTables);
        insertNamedCols(toAddNamedDimCols);
        insertDimTableModelRelation(SemanticUtils.convertToDimTableModelRels(toAddTables));
    }

    @Override
    public int deleteNamedMeasure(Integer id) {
        return namedMeasureMapper.deleteByPrimaryKey(id);
    }

    @Override
    public DatasetEntity getDatasetEntity(String project, String dataset) {
        DatasetEntity search = DatasetEntity.builder().project(project).dataset(dataset).build();
        return datasetMapper.selectOne(search);
    }

    @Override
    public List<DatasetEntity> getDataSetEntitiesBySelectAll(String projectName, Boolean selectAll, List<String> excludes, List<String> includes, String searchName, boolean isBroken) {
        Condition condition = new Condition(DatasetEntity.class);
        Example.Criteria criteria = condition.createCriteria();
        if (isBroken) {
            criteria.andEqualTo("status", "BROKEN");
        }
        if (projectName != null) {
            criteria.andEqualTo("project", projectName);
        }
        if (searchName != null) {
            criteria.andLike("dataset", "%" + searchName + "%");
        }
        List<DatasetEntity> datasetEntities = datasetMapper.selectByExample(condition);
        if (selectAll && !excludes.isEmpty()) {
            return datasetEntities.stream().filter(datasetEntity -> !excludes.contains(datasetEntity.getId().toString())).collect(Collectors.toList());
        } else if (!selectAll && !includes.isEmpty()) {
            return datasetEntities.stream().filter(datasetEntity -> includes.contains(datasetEntity.getId().toString())).collect(Collectors.toList());
        }
        return datasetEntities;
    }

    public List<DatasetEntity> getAllDatasetEntities() {
        Condition condition = new Condition(DatasetEntity.class);

        return datasetMapper.selectByExample(condition);
    }


    @Override
    public int updateDatasetStatusAndBrokenInfo(Integer id, DatasetStatus datasetStatus, String brokenInfo) {
        assert datasetStatus != null && id != null && brokenInfo != null;
        DatasetEntity datasetEntity = new DatasetEntity();
        datasetEntity.setId(id);
        datasetEntity.setStatus(datasetStatus.name());
        datasetEntity.setBrokenMsg(brokenInfo);
        datasetEntity.setModifyTime(Utils.currentTimeStamp());
        return datasetMapper.updateByPrimaryKeySelective(datasetEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void addUserInvisibleThings(String targetUser, NamedDimCol targetNamedDimCol, NamedMeasure targetNamedMeasure,
                                       CalculateMeasure targetCalculateMeasure, NamedSet targetNamedSet) {

        if (targetNamedDimCol != null) {
            EntityExtend namedDimeColExtend = SemanticUtils.addInvisibleUser(targetNamedDimCol.getExtend(), targetUser, DescWrapperExtend.class);
            NamedDimCol updated = new NamedDimCol();
            updated.setId(targetNamedDimCol.getId());
            updated.setExtend(namedDimeColExtend.take());
            namedDimColMapper.updateByPrimaryKeySelective(updated);
        }

        if (targetNamedMeasure != null) {
            EntityExtend namedMeasureExtend = SemanticUtils.addInvisibleUser(targetNamedMeasure.getExtend(), targetUser, DescWrapperExtend.class);
            NamedMeasure updated = new NamedMeasure();
            updated.setId(targetNamedMeasure.getId());
            updated.setExtend(namedMeasureExtend.take());
            namedMeasureMapper.updateByPrimaryKeySelective(updated);
        }

        if (targetCalculateMeasure != null) {
            EntityExtend calcMeasureExtend = SemanticUtils.addInvisibleUser(targetCalculateMeasure.getExtend(), targetUser, DescWrapperExtend.class);
            CalculateMeasure updated = new CalculateMeasure();
            updated.setId(targetCalculateMeasure.getId());
            updated.setExtend(calcMeasureExtend.take());
            calculateMeasureMapper.updateByPrimaryKeySelective(updated);
        }

        if (targetNamedSet != null) {
            EntityExtend namedSetExtend = SemanticUtils.addInvisibleUser(targetNamedSet.getExtend(), targetUser, DescWrapperExtend.class);
            NamedSet updated = new NamedSet();
            updated.setId(targetNamedSet.getId());
            updated.setExtend(namedSetExtend.take());
            namedSetMapper.updateByPrimaryKeySelective(updated);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void removeUserInvisibleThings(String targetUser, NamedDimCol targetNamedDimCol, NamedMeasure targetNamedMeasure,
                                          CalculateMeasure targetCalculateMeasure, NamedSet targetNamedSet) {

        if (targetNamedDimCol != null) {
            EntityExtend namedDimeColExtend = SemanticUtils.removeInvisibleUser(targetNamedDimCol.getExtend(), targetUser, DescWrapperExtend.class);
            NamedDimCol updated = new NamedDimCol();
            updated.setId(targetNamedDimCol.getId());
            updated.setExtend(namedDimeColExtend.take());
            namedDimColMapper.updateByPrimaryKeySelective(updated);
        }

        if (targetNamedMeasure != null) {
            EntityExtend namedMeasureExtend = SemanticUtils.removeInvisibleUser(targetNamedMeasure.getExtend(), targetUser, DescWrapperExtend.class);
            NamedMeasure updated = new NamedMeasure();
            updated.setId(targetNamedMeasure.getId());
            updated.setExtend(namedMeasureExtend.take());
            namedMeasureMapper.updateByPrimaryKeySelective(updated);
        }

        if (targetCalculateMeasure != null) {
            EntityExtend calcMeasureExtend = SemanticUtils.removeInvisibleUser(targetCalculateMeasure.getExtend(), targetUser, DescWrapperExtend.class);
            CalculateMeasure updated = new CalculateMeasure();
            updated.setId(targetCalculateMeasure.getId());
            updated.setExtend(calcMeasureExtend.take());
            calculateMeasureMapper.updateByPrimaryKeySelective(updated);
        }

        if (targetNamedSet != null) {
            EntityExtend namedSetExtend = SemanticUtils.removeInvisibleUser(targetNamedSet.getExtend(), targetUser, DescWrapperExtend.class);
            NamedSet updated = new NamedSet();
            updated.setId(targetNamedSet.getId());
            updated.setExtend(namedSetExtend.take());
            namedSetMapper.updateByPrimaryKeySelective(updated);
        }
    }

    @Override
    public int updateNamedMeasure(Integer id, NamedMeasure update) {
        assert id != null;
        update.setId(id);
        return namedMeasureMapper.updateByPrimaryKeySelective(update);
    }

    @Override
    public Integer insertDatasetWithId(DatasetEntity datasetEntity) {
        assert datasetEntity.getId() != null;
        return datasetMapper.insertSelective(datasetEntity);
    }

    @Override
    public Integer insertDatasetSummary(DatasetEntity datasetEntity) throws SemanticException {
        int r = datasetMapper.insertOneReturnId(datasetEntity);

        if (r > 0 && datasetEntity.getId() != null) {
            return datasetEntity.getId();
        } else {
            String errorMsg = Utils.formatStr("Inserting dataset summary record gets a failure, r:%d, id:%d", r, datasetEntity.getId());
            throw new SemanticException(errorMsg, ErrorCode.DB_OPERATION_ERROR);
        }
    }

    @Override
    public int insertCommonDimModels(List<CommonDimRelation> commonDimRelations) {

        return Utils.isCollectionEmpty(commonDimRelations) ?
                0 : commonDimRelationMapper.insertList(commonDimRelations);
    }

    @Override
    public int insertCalculateMeasures(List<CalculateMeasure> calculateMeasureEntitys) {
        return Utils.isCollectionEmpty(calculateMeasureEntitys) ?
                0 : calculateMeasureMapper.insertList(calculateMeasureEntitys);
    }

    @Override
    public int insertNamedSets(List<NamedSet> namedSets) {
        return Utils.isCollectionEmpty(namedSets) ?
                0 : namedSetMapper.insertList(namedSets);
    }

    @Override
    public int insertNamedCols(List<NamedDimCol> namedDimColEntitys) {
        return Utils.isCollectionEmpty(namedDimColEntitys) ?
                0 : namedDimColMapper.insertList(namedDimColEntitys);
    }

    @Override
    public int insertCustomHierarchies(List<CustomHierarchy> customHierarchyEntitys) {
        return Utils.isCollectionEmpty(customHierarchyEntitys) ?
                0 : customHierarchyMapper.insertList(customHierarchyEntitys);
    }

    @Override
    public int insertNamedMeasure(List<NamedMeasure> namedMeasureEntitys) {
        return Utils.isCollectionEmpty(namedMeasureEntitys) ?
                0 : namedMeasureMapper.insertList(namedMeasureEntitys);
    }

    @Override
    public int insertNamedTable(List<NamedDimTable> namedDimTableEntitys) {
        return Utils.isCollectionEmpty(namedDimTableEntitys) ?
                0 : namedDimTableMapper.insertList(namedDimTableEntitys);
    }

    @Override
    public int insertDimTableModelRelation(List<DimTableModelRel> dimTableModelRelEntitys) {
        return Utils.isCollectionEmpty(dimTableModelRelEntitys) ?
                0 : dimTableModelRelMapper.insertList(dimTableModelRelEntitys);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void deleteOneDataset(Integer datasetId) {
        if (datasetId == null) {
            log.warn("The deleteOneDataset function: arg datasetId can't be null");
            return;
        }

        datasetMapper.deleteByPrimaryKey(datasetId);

        commonDimRelationMapper.delete(new CommonDimRelation(datasetId));

        calculateMeasureMapper.delete(new CalculateMeasure(datasetId));

        namedDimColMapper.delete(new NamedDimCol(datasetId));

        namedDimTableMapper.delete(new NamedDimTable(datasetId));

        customHierarchyMapper.delete(new CustomHierarchy(datasetId));

        namedMeasureMapper.delete(new NamedMeasure(datasetId));

        dimTableModelRelMapper.delete(new DimTableModelRel(datasetId));

        namedSetMapper.delete(new NamedSet(datasetId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void insertAllSingleCubeDatasets(Set<String> projects, Long createTime) throws SemanticException {

        for (String project : projects) {
            insertSingleCubeDatasets(project, createTime);
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void insertSingleCubeDatasets(String project, Long createTime) throws SemanticException {

        List<KylinGenericModel> genericModels = modelService.getCachedGenericModels(project);

        List<String> cubeNames = genericModels.stream().map(KylinGenericModel::getModelName).collect(Collectors.toList());

        for (String model : cubeNames) {
            insertSingleCubeDataset(project, model, createTime);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void insertSingleCubeDataset(String project, String model, Long createTime) throws SemanticException {
        SemanticModel semanticModel = modelService.getSemanticModel(project, model);

        String mdxModelName = getUniqueModelName(project, model);

        DatasetEntity dataset = DatasetEntity.builder()
                .project(project)
                .dataset(mdxModelName)
                .createUser(SemanticConfig.getInstance().getKylinUser())
                .createTime(createTime)
                .modifyTime(createTime)
                .frontVersion("0.2")
                .build();

        Integer mdxDatasetId = insertDatasetSummary(dataset);
        //MDX must have dimension usages
        List<DimTableModelRel> dimTableModelRels = new LinkedList<>();
        semanticModel.getDimensionTables().forEach(dimensionTable ->
                dimTableModelRels.add(new DimTableModelRel(
                        mdxDatasetId, model, dimensionTable.getName(), DimensionUsage.JOINT.ordinal(), "", "")
                )
        );
        insertDimTableModelRelation(dimTableModelRels);
        insertBothComDimAndTableAndDimAndMeasure(model, mdxDatasetId, semanticModel);
    }

    private void insertBothComDimAndTableAndDimAndMeasure(String model, Integer mdxDatasetId, SemanticModel semanticModel) {
        List<CommonDimRelation> commonDimRelations = new ArrayList<>(1);
        commonDimRelations.add(new CommonDimRelation(mdxDatasetId, model, "", ""));


        List<NamedDimTable> namedDimTables = new LinkedList<>();
        List<NamedDimCol> namedDimCols = new LinkedList<>();
        List<NamedMeasure> namedMeasures = new LinkedList<>();
        for (DimensionTable dimensionTable : semanticModel.getDimensionTables()) {
            String tableName = dimensionTable.getName();
            String tableAlias = tableName;
            if (StringUtils.isNotBlank(dimensionTable.getAlias())) {
                tableAlias = dimensionTable.getAlias();
            }
            namedDimTables.add(new NamedDimTable(mdxDatasetId, model, tableName, tableAlias,
                    DimTableType.REGULAR.getLowercase(), dimensionTable.getActualTable(), Utils.concat(SemanticConstants.DOT, semanticModel.getFactTableSchema(), semanticModel.getFactTableAlias())));

            for (DimensionColumn dimCol : dimensionTable.getDimensionColumns()) {
                namedDimCols.add(new NamedDimCol(mdxDatasetId, model, tableName, dimCol.getColumnName(), dimCol.getColumnName(), 0, dimCol.getDataType()));
            }
        }

        for (CubeMeasure cubeMeasure : semanticModel.getCubeMeasures()) {
            namedMeasures.add(new NamedMeasure(mdxDatasetId, model, cubeMeasure.getMeasureName(),
                    cubeMeasure.getMeasureName(), cubeMeasure.getExpression(), cubeMeasure.getDataType(), SemanticUtils.getNormalMeasureArgByColMeasured(cubeMeasure.getColMeasured())));
        }

        insertCommonDimModels(commonDimRelations);
        insertNamedTable(namedDimTables);
        insertNamedCols(namedDimCols);
        insertNamedMeasure(namedMeasures);
    }

    private String getUniqueModelName(String project, String model) {
        // FIXME: be stable to get unique model name
        while (true) {
            DatasetEntity dataset = getDatasetEntity(project, model);
            StringBuffer modelBuffer = new StringBuffer(model);
            if (dataset != null) {
                modelBuffer.append("_1");
            } else {
                return modelBuffer.toString();
            }
        }

    }

    @Override
    public String getUserPwd(String user) throws PwdDecryptException {
        UserInfo userInfo = userService.selectOne(user);
        return Utils.buildBasicAuth(user, AESWithECBEncryptor.decrypt(userInfo.getPassword()));
    }
}
