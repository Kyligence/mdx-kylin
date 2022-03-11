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


package io.kylin.mdx.insight.core.service;

import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.entity.*;
import io.kylin.mdx.insight.core.model.semantic.DatasetStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

public interface DatasetService {
    List<String> getProjectsRelatedDataset();

    DatasetEntity selectDatasetById(Integer datasetId);

    DatasetEntity getDataSetEntityByNameAndProjectName(String project, String dataName);

    List<DatasetEntity> getDataSetEntitysByPage(String project, String dataset, int pageNum, int pageSize, String orderByClause);

    List<DatasetEntity> selectDatasetByProjectName(String project);

    List<DatasetEntity> selectDatasetBySearch(Integer datasetType, Long begin, Long end);

    List<CommonDimRelation> selectCommonDimRelsByDatasetId(Integer datasetId);

    List<NamedDimTable> selectAllDimTableByDatasetIds(List<Integer> datasetIds);

    int deleteNamedCol(NamedDimCol namedDimCol);

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    void deleteTableLogically(List<NamedDimTable> toDeleteTables);

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    void insertTableLogically(List<NamedDimTable> toAddTables, List<NamedDimCol> toAddNamedDimCols);

    int deleteNamedMeasure(Integer id);

    DatasetEntity getDatasetEntity(String project, String dataset);

    List<DatasetEntity> getDataSetEntitiesBySelectAll(String projectName, Boolean selectAll, List<String> excludes, List<String> includes, String searchName, boolean isBroken);

    int updateDatasetStatusAndBrokenInfo(Integer id, DatasetStatus datasetStatus, String brokenInfo);

    List<NamedDimTable> selectDimTablesByDatasetId(Integer datasetId);

    List<NamedDimCol> selectAllDimColByDatasetIds(List<Integer> datasetIds);

    List<NamedDimCol> selectAllDimColByDatasetIdAndTable(NamedDimCol namedDimCol);

    List<DatasetEntity> selectDatasetsBySearch(DatasetEntity search);

    List<DimTableModelRel> selectDimTblModelRelsBySearch(DimTableModelRel dimTableModelRel);

    List<NamedDimCol> selectDimColsByDatasetId(Integer datasetId);

    NamedDimTable selectOneDimTableBySearch(Integer datasetId, String model, String tableAlias);

    NamedDimCol selectOneDimColsBySearch(Integer datasetId, String model, String table, String dimColAlias);

    NamedMeasure selectOneMeasureBySearch(Integer datasetId, String model, String measure);

    CalculateMeasure selectOneCalcMeasureBySearch(Integer datasetId, String name);

    NamedSet selectOneNamedSetBySearch(Integer datasetId, String name);

    List<CustomHierarchy> selectHierarchiesByDatasetId(Integer datasetId);

    List<NamedMeasure> selectMeasuresByDatasetId(Integer datasetId);

    List<CalculateMeasure> selectCMsByDatasetId(Integer datasetId);

    List<CalculateMeasure> selectCalculateMeasuresByDatasetId(Integer datasetId);

    List<NamedSet> selectNamedSetsByDatasetId(Integer datasetId);

    List<DimTableModelRel> selectDimTblModelRelsByDatasetId(Integer datasetId);

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    void addUserInvisibleThings(String targetUser, NamedDimCol targetNamedDimCol, NamedMeasure targetNamedMeasure,
                                CalculateMeasure targetCalculateMeasure, NamedSet targetNamedSet);

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    void removeUserInvisibleThings(String targetUser, NamedDimCol targetNamedDimCol, NamedMeasure targetNamedMeasure,
                                   CalculateMeasure targetCalculateMeasure, NamedSet targetNamedSet);

    int updateNamedMeasure(Integer id, NamedMeasure update);

    Integer insertDatasetWithId(DatasetEntity datasetEntity);

    Integer insertDatasetSummary(DatasetEntity datasetEntity) throws SemanticException;

    int insertCommonDimModels(List<CommonDimRelation> commonDimRelations);

    int insertCalculateMeasures(List<CalculateMeasure> calculateMeasureEntitys);

    int insertNamedSets(List<NamedSet> namedSets);

    int insertNamedCols(List<NamedDimCol> namedDimColEntitys);

    int insertCustomHierarchies(List<CustomHierarchy> customHierarchyEntitys);

    int insertNamedMeasure(List<NamedMeasure> namedMeasureEntitys);

    int insertNamedTable(List<NamedDimTable> namedDimTableEntitys);

    int insertDimTableModelRelation(List<DimTableModelRel> dimTableModelRelEntitys);

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    void deleteOneDataset(Integer datasetId);

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    void insertAllSingleCubeDatasets(Set<String> projects, Long createTime) throws SemanticException;

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    void insertSingleCubeDatasets(String project, Long createTime) throws SemanticException;

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    void insertSingleCubeDataset(String project, String model, Long createTime) throws SemanticException;

    String getUserPwd(String user) throws PwdDecryptException;
}
