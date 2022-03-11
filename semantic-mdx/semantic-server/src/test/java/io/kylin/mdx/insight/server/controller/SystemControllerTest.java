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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import static io.kylin.mdx.insight.common.constants.ConfigConstants.DATASET_ALLOW_ACCESS_BY_DEFAULT;
import static io.kylin.mdx.insight.common.util.Utils.buildBasicAuth;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
public class SystemControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/kylinmetadata");
        System.setProperty("insight.semantic.enable.aad", "true");
        System.setProperty("azure.activedirectory.client-secret", "test");
    }

    @AfterClass
    public static void clear() {
        System.clearProperty("insight.semantic.enable.aad");
        System.clearProperty("azure.activedirectory.client-secret");
    }

    private static final String CONTROLLER_NAME = "systemcontroller";

    private static final String[] SYSTEM_CONTROLLER_METHODS = {
            "getConfigurations",
            "getConfigurationsByKey"
    };

    @Test
    public void refreshProjectModelsTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/system/refresh/mdx_automation_test")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk());
    }

    @Test
    public void syncKeMetaDataTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/system/sync")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")));
    }

    @Test
    @Transactional
    public void updateConfigurationsTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .put("/api/system/configurations")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                        .contentType("application/json")
                        .content("{\n" +
                                "\t\"insight.kylin.username\": \"ADMIN\",\n" +
                                "\t\"insight.kylin.password\":\"S1lMSU4=\"\n" +
                                "}"));
    }

    @Test
    public void syncJobTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .post("/api/system/sync/jobs")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")));
    }

    @Test
    public void getConfigurationsTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/system/configurations")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json(getRespJSONContent(SYSTEM_CONTROLLER_METHODS[0]), false));
    }

    @Test
    public void getConfigurationsByKeyTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/system/configurations?key=" + DATASET_ALLOW_ACCESS_BY_DEFAULT)
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json(getRespJSONContent(SYSTEM_CONTROLLER_METHODS[1]), false));
    }

    @Test
    public void syncLicenseTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .post("/api/system/sync/license")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));
    }

    @Test
    public void checkHealthTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/system/health")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));
    }

    @Test
    public void getCardinalityTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/system/metadata/cardinality?project=learn_kylin")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));
    }

    @Test
    public void checkLoadTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/system/load")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));
    }

    @Test
    public void getAADInfoTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/system/aadInfo")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));
    }

    @Test
    public void getAADSettingsTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/system/aad-settings")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));
    }

    @Override
    protected String getControllerName() {
        return CONTROLLER_NAME.toLowerCase();
    }
}
