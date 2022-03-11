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


package io.kylin.mdx.insight.core.sync;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.dao.UserInfoMapper;
import io.kylin.mdx.insight.core.manager.ProjectManager;
import io.kylin.mdx.insight.core.meta.mock.MockTestLoader;
import io.kylin.mdx.insight.core.service.*;
import io.kylin.mdx.insight.server.SemanticLauncher;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(classes = SemanticLauncher.class)
@RunWith(SpringRunner.class)
public class MetaSyncTest extends BaseEnvSetting {

    private final static String project = "EasyJet";

    @Autowired
    @SpyBean
    private DatasetService datasetService;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private BrokenService brokenService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private InitService initService;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private ModelService modelService;

    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/datasetvalidation/kylin");
        MockTestLoader.put(new MockTestLoader.Full("/kylin/api/tables?project=EasyJet&ext=true"), "/json/kylinmetadata/easy_jet_get_tables.json");
    }

    @Before
    public void before() throws SemanticException {
        projectManager.initLoadProjectList();
        MetaStore metaStore = MetaStore.getInstance();

        // clear cube cache because the other test case may add unexpected cube into cache to cause this test case
        // have unexpected result
        // we should be careful about the static class in the same project test units
        metaStore.clearProjectModels();
        modelService.loadGenericModels(project);
    }

    @Test
    public void test() {

    }
}
