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
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.dao.CommonDimRelationMapper;
import io.kylin.mdx.insight.core.entity.CommonDimRelation;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.model.semantic.DatasetStatus;
import io.kylin.mdx.insight.core.service.BrokenService;
import io.kylin.mdx.insight.core.service.DatasetService;
import io.kylin.mdx.insight.core.sync.DatasetBrokenInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class BrokenServiceImpl implements BrokenService {

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private CommonDimRelationMapper commonDimRelationMapper;

    /**
     * if KYLIN's cube is deleted, the dataset using this cube need to set broken status
     *
     * @param project          cubes belong to project
     * @param deletedCubeNames the cube name list disable or deleted in KYLIN
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void setDatasetsBroken(String project, Set<String> deletedCubeNames) {
        List<CommonDimRelation> commonDimRelations = commonDimRelationMapper.selectDimRelationsWithProject(project, deletedCubeNames);

        Map<Integer, Set<String>> modelToDeletedCubesMap = new HashMap<>();

        for (CommonDimRelation comDimRel : commonDimRelations) {
            Set<String> cubeNames = modelToDeletedCubesMap.computeIfAbsent(comDimRel.getDatasetId(), id -> new HashSet<>());

            if (deletedCubeNames.contains(comDimRel.getModel())) {
                cubeNames.add(comDimRel.getModel());
            }

            if (deletedCubeNames.contains(comDimRel.getModelRelated())) {
                cubeNames.add(comDimRel.getModelRelated());
            }
        }

        modelToDeletedCubesMap.forEach(this::setOneDatasetBroken);
    }

    @Override
    public void setOneDatasetBroken(Integer datasetId, Set<String> brokenCubesInDataset) {
        DatasetBrokenInfo datasetBrokenInfo = new DatasetBrokenInfo();
        datasetBrokenInfo.setBrokenModelList(brokenCubesInDataset);

        setOneDatasetBroken(datasetId, datasetBrokenInfo);
    }
    @Override
    public void setOneDatasetBroken(Integer datasetId, DatasetBrokenInfo brokenInfo) {

        String brokenInfoStr = JSON.toJSONString(brokenInfo);
        datasetService.updateDatasetStatusAndBrokenInfo(datasetId, DatasetStatus.BROKEN, brokenInfoStr);

        log.info("Dataset status update, id:{}, status:{}, brokenInfo:{}", datasetId, DatasetStatus.BROKEN.name(), brokenInfoStr);
    }
    @Override
    public void recoverOneDatasetNormal(Integer datasetId) {
        datasetService.updateDatasetStatusAndBrokenInfo(datasetId, DatasetStatus.NORMAL, "");
    }

    @Override
    public void tryRecoverDataset(String projectName, Set<String> cubeNames) {

        DatasetEntity search = new DatasetEntity();
        search.setProject(projectName);
        search.setStatus(DatasetStatus.BROKEN.name());
        List<DatasetEntity> brokenDatasets = datasetService.selectDatasetsBySearch(search);

        for (DatasetEntity brokenDataset : brokenDatasets) {
            if (brokenDataset.getBrokenMsg() == null || "".equals(brokenDataset.getBrokenMsg())) {
                continue;
            }
            DatasetBrokenInfo datasetBrokenInfo = JSON.parseObject(brokenDataset.getBrokenMsg(), DatasetBrokenInfo.class);

            if (datasetBrokenInfo.getModels() != null) {
                List<String> brokenModels = datasetBrokenInfo.extractBrokenModels();

                Set<String> unRecoverModels = new HashSet<>();
                for (String brokenModel : brokenModels) {
                    if (!cubeNames.contains(brokenModel)) {
                        unRecoverModels.add(brokenModel);
                    }
                }

                if (Utils.isCollectionEmpty(unRecoverModels)) {
                    recoverOneDatasetNormal(brokenDataset.getId());
                    log.info("recover dataset. project:{}, dataset_name:{}", brokenDataset.getProject(), brokenDataset.getDataset());
                } else {
                    DatasetBrokenInfo revisionBrokenInfo = new DatasetBrokenInfo();
                    revisionBrokenInfo.setBrokenModelList(unRecoverModels);
                    setOneDatasetBroken(brokenDataset.getId(), revisionBrokenInfo);
                }
            }
        }
    }
}
