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


package io.kylin.mdx.insight.core.support;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.model.semantic.SemanticDataset;
import io.kylin.mdx.insight.core.model.semantic.SemanticProject;
import io.kylin.mdx.insight.core.service.SemanticContext;
import io.kylin.mdx.insight.server.SemanticLauncher;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = SemanticLauncher.class)
@RunWith(SpringRunner.class)
public class SemanticFacadeTest extends BaseEnvSetting {

    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/kylinmetadata");
    }

    @Test
    public void facadeTest() throws SemanticException {
        SemanticProject semanticProject = SemanticFacade.INSTANCE.getSemanticProject("mdx_automation_test");

        Assert.assertNotNull(semanticProject);
        Assert.assertEquals("mdx_automation_test", semanticProject.getProject());
        Assert.assertTrue(semanticProject.getSemanticDatasets().size() > 0);

        SemanticDataset semanticDataset = semanticProject.getSemanticDatasets().get(0);
        Assert.assertEquals("MDX", semanticDataset.getType());
        Assert.assertEquals("snowflake_dataset", semanticDataset.getDatasetName());
        Assert.assertEquals(1, semanticDataset.getModelRelations().size());

        SemanticContext semanticCtx = SpringHolder.getBean(SemanticContext.class);
        semanticCtx.clearProjectCache("mdx_automation_test", null);

        SemanticFacade.INSTANCE.clearProjectCache("mdx_automation_test", "ADMIN");
        try {
            SemanticFacade.INSTANCE.getSemanticProjectByUser("mdx_automation_test", "ADMIN");
        } catch (SemanticException e) {
        }
    }

}
