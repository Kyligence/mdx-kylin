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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static io.kylin.mdx.insight.common.util.Utils.buildBasicAuth;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
public class ModelControllerTest extends BaseControllerTest {

    private static final String CONTROLLER_NAME = "ModelController";

    private static final String PROJECT = "mdx_automation_test";

    private static final String MODEL = "snowflake_inner_hierarcy_cube";

    private static final String[] MODEL_CONTROLLER_METHODS = {
            "getModelsByProject",
            "getDimtablesByModel",
            "getSemanticModel"
    };

    @Autowired
    private MockMvc mvc;

    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/kylinmetadata");
    }

    @Before
    public void loadProjectModels() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/system/refresh/{project}", PROJECT)
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json("{\"status\" : 0,\"data\" : \"success\"}", false));
    }

    @Test
    public void health() throws Exception {
        this.mvc.perform(MockMvcRequestBuilders.get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json("{\"status\" : 0,\"data\" : \"health\"}", false));
    }

    @Test
    public void getModelsByProjectTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/model/list/{project}", PROJECT)
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json(getRespJSONContent(MODEL_CONTROLLER_METHODS[0]), false));
    }

    @Test
    public void getDimtablesByModelTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/dimtable/{project}/{model}", PROJECT, MODEL)
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json(getRespJSONContent(MODEL_CONTROLLER_METHODS[1]), false));
    }

    @Test
    public void getSemanticModelTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/model/detail/{project}/{model}", PROJECT, MODEL)
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json(getRespJSONContent(MODEL_CONTROLLER_METHODS[2]), false));
    }

    @Override
    protected String getControllerName() {
        return CONTROLLER_NAME.toLowerCase();
    }
}
