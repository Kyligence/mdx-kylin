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

import com.google.common.collect.Sets;
import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.DatasetType;
import io.kylin.mdx.insight.server.SemanticLauncher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@SpringBootTest(classes = SemanticLauncher.class)
@RunWith(SpringRunner.class)
public class DatasetServiceTest extends BaseEnvSetting {

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private ModelService modelService;

    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/kylinmetadata");
    }

    @Before
    public void loadModels() throws SemanticException {
        modelService.loadGenericModels("mdx_automation_test");
    }

    @Test
    @Transactional
    public void insertAllSingleCubeDatasetsTest() throws SemanticException {
        Set<String> projects = Sets.newHashSet("mdx_automation_test");
        datasetService.insertAllSingleCubeDatasets(projects, Utils.currentTimeStamp());
    }

    @Test
    public void datasetType() {
        Assert.assertEquals("MDX", DatasetType.ordinalToLiteral(0));
        Assert.assertEquals("SQL", DatasetType.ordinalToLiteral(1));
    }

}
