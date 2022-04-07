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


package io.kylin.mdx.insight.server.controller;

import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.entity.DatasetType;
import io.kylin.mdx.insight.core.manager.ProjectManager;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.core.service.DatasetService;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.service.ModelService;
import io.kylin.mdx.insight.core.sync.DatasetEventObject;
import io.kylin.mdx.insight.engine.manager.SyncManager;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.support.Permission;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Validated
@RestController
@RequestMapping("api")
public class AutoCreateController {

    @Autowired
    private ModelService modelService;

    @Autowired
    private SyncManager syncManager;

    private final static String RANGE = "all";

    private final DatasetService datasetService;

    private final ProjectManager projectManager;

    private final AuthService authService;

    @Autowired
    public AutoCreateController(DatasetService datasetService, ProjectManager projectManager, AuthService authService, ModelService modelService) {
        this.datasetService = datasetService;
        this.projectManager = projectManager;
        this.authService = authService;
        this.modelService = modelService;

    }

    /**
     * 自动创建单Cube的数据集
     */
    @PostMapping("/single/cube")
    @Permission
    public Response<String> createSingleCubeDatasets(
            @RequestParam(value = "range", required = false) String range,
            @RequestParam(value = "projectName", required = false) String projectName,
            @RequestParam(value = "cubeName", required = false) String cubeName) throws SemanticException {

        log.info("user:{} insert dataset, range:{}, projectName:{}, cubeName:{}", authService.getCurrentUser(), range, projectName, cubeName);

        Set<String> projects = projectManager.getAllProject();
        long createTime = Utils.currentTimeStamp();
        boolean isEffective = false;

        if (RANGE.equalsIgnoreCase(range)) {
            datasetService.insertAllSingleCubeDatasets(projects, createTime);
            isEffective = true;
        } else if (StringUtils.isNotBlank(projectName) && projects.contains(projectName) && StringUtils.isBlank(cubeName)) {
            datasetService.insertSingleCubeDatasets(projectName, createTime);
            isEffective = true;
        } else if (StringUtils.isNotBlank(projectName) && projects.contains(projectName) && StringUtils.isNotBlank(cubeName)) {

            Set<String> cubeNames = modelService.getCachedGenericModels(projectName)
                    .stream()
                    .map(KylinGenericModel::getModelName)
                    .collect(Collectors.toSet());

            if (cubeNames.contains(cubeName)) {
                datasetService.insertSingleCubeDataset(projectName, cubeName, createTime);
                isEffective = true;
            }
        }

        if (!isEffective) {
            return new Response<String>(Response.Status.FAIL)
                    .data("Something wrong! Please check whether url parameters are correct.");
        }

        //notify studio
        List<DatasetEntity> notifyDatasetEntities = datasetService.selectDatasetBySearch(DatasetType.SQL.ordinal(), createTime, null);

        for (DatasetEntity notifyDataset : notifyDatasetEntities) {
            syncManager.asyncNotify(
                    new DatasetEventObject(
                            new DatasetEventObject.DatasetChangedSource(notifyDataset.getProject(), notifyDataset.getDataset()),
                            DatasetEventObject.DatasetEventType.DATASET_NEWED)
            );
        }


        return new Response<String>(Response.Status.SUCCESS)
                .data(SemanticConstants.RESP_SUC);
    }

}
