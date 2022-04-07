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

import static io.kylin.mdx.insight.common.util.Utils.buildBasicAuth;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
public class RoleControllerTest extends BaseControllerTest{

    private static final String CONTROLLER_NAME = "rolecontroller";

    private static final Integer TEST_ROLE_ID = 15;

    private static final String[] ROLE_CONTROLLER_METHODS = {
            "getRoles",
            "selectRoleInfo",
            "updateRoleInfo",
            "deleteRole",
            "insertRole"
    };

    @Autowired
    private MockMvc mvc;

    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/kylinmetadata");
    }

    @Test
    public void getRolesTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/roles?pageNum=0&pageSize=100&RoleName=&containsDesc=true")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json(getRespJSONContent(ROLE_CONTROLLER_METHODS[0]), false));

    }

    @Test
    public void selectRoleInfoTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/role/{roleId}", TEST_ROLE_ID)
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json(getRespJSONContent(ROLE_CONTROLLER_METHODS[1]), false));

    }

    @Test
    @Transactional
    public void updateRoleInfoTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .put("/api/role/{roleId}", TEST_ROLE_ID)
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                        .contentType("application/json")
                        .content(getReqJSONContent(ROLE_CONTROLLER_METHODS[2])))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));

    }

    @Test
    @Transactional
    public void deleteRoleTest() throws Exception {
        String deleteSucResp =
                "{" +
                        "  \"status\" : 0," +
                        "  \"data\" : \"success\" " +
                        "}";
        this.mvc.perform(
                MockMvcRequestBuilders
                        .delete("/api/role/{roleId}", TEST_ROLE_ID)
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json(deleteSucResp, false));

    }

    @Test
    @Transactional
    public void insertRoleTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .post("/api/role")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                        .contentType("application/json")
                        .content(getReqJSONContent(ROLE_CONTROLLER_METHODS[4])))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));
    }


    @Override
    protected String getControllerName() {
        return CONTROLLER_NAME.toLowerCase();
    }

}
