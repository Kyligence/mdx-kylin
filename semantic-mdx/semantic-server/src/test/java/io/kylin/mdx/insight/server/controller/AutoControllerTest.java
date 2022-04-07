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

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.core.service.DatasetService;
import io.kylin.mdx.insight.core.manager.ProjectManager;
import io.kylin.mdx.insight.core.service.ModelService;
import io.kylin.mdx.insight.engine.manager.ProjectManagerImpl;
import io.kylin.mdx.insight.server.bean.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class AutoControllerTest extends BaseEnvSetting {

    private final static String project = "EasyJet";

    private final static String RANGE_ALL = "all";

    private final static String CUBE_NAME = "EJ_Cube1";

    @Mock
    private DatasetService datasetService;

    @Mock
    private AuthService authService;

    @Mock
    private ModelService modelService;

    private ProjectManager projectManager;


    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/datasetvalidation/kylin");
    }

    @Before
    public void before() throws SemanticException {
        projectManager = new ProjectManagerImpl();
        projectManager.initLoadProjectList();
    }

    @Test
    public void test() throws SemanticException {
        KylinGenericModel kylinGenericModel = new KylinGenericModel();
        kylinGenericModel.setModelName("EJ_Cube1");
        doNothing().when(datasetService).insertAllSingleCubeDatasets(any(), any());
        doNothing().when(datasetService).insertSingleCubeDatasets(any(), any());
        doNothing().when(datasetService).insertSingleCubeDataset(anyString(), any(), any());
        doReturn(Collections.emptyList()).when(datasetService).selectDatasetBySearch(any(), any(), any());
        doReturn(Collections.singletonList(kylinGenericModel)).when(modelService).getCachedGenericModels(any());

        AutoCreateController autoCreateController = new AutoCreateController(datasetService, projectManager, authService, modelService);
        Response<String> response = autoCreateController.createSingleCubeDatasets(RANGE_ALL, null, null);
        Assert.assertEquals(Response.Status.SUCCESS.ordinal(), (int) response.getStatus());

        Response<String> response2 = autoCreateController.createSingleCubeDatasets(null, project, null);
        Assert.assertEquals(Response.Status.SUCCESS.ordinal(), (int) response2.getStatus());

        Response<String> response3 = autoCreateController.createSingleCubeDatasets(null, project, CUBE_NAME);
        Assert.assertEquals(Response.Status.SUCCESS.ordinal(), (int) response3.getStatus());
    }

}

