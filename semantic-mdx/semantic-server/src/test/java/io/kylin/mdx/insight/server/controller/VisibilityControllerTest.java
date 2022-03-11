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

import com.google.common.collect.Sets;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.manager.ProjectManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import static io.kylin.mdx.insight.common.util.Utils.buildBasicAuth;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
public class VisibilityControllerTest extends BaseControllerTest {

    private static final String project = "mdx_automation_test";

    private static final String CONTROLLER_NAME = "VisibilityController";

    private static final String[] VISIBILITY_CONTROLLER_METHODS = {
            "addUserInvisibility",
            "addUserToRole"
    };

    @Autowired
    private MockMvc mvc;

    @Autowired
    @MockBean
    private ProjectManager projectManager;

    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/kylinmetadata");
    }

    @Before
    public void before() throws SemanticException {
        when(projectManager.getActualProjectSet(any())).thenReturn(Sets.newHashSet(project));
    }

    @Test
    @Transactional
    public void testAddInvisibility() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .post("/api/dataset/user/invisibility")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                        .contentType("application/json")
                        .content(getReqJSONContent(VISIBILITY_CONTROLLER_METHODS[0]))
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json("{}", false));

    }

    @Test
    @Transactional
    public void testRemoveInvisibility() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .delete("/api/dataset/user/invisibility")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                        .contentType("application/json")
                        .content(getReqJSONContent(VISIBILITY_CONTROLLER_METHODS[0]))
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json("{}", false));

    }

    @Test
    @Transactional
    public void testAddUserToRole() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .post("/api/role/user/visibility/15")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                        .contentType("application/json")
                        .content(getReqJSONContent(VISIBILITY_CONTROLLER_METHODS[1]))
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json("{}", false));

    }

    @Test
    @Transactional
    public void testDeleteUserFromRole() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .delete("/api/role/user/visibility/15")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                        .contentType("application/json")
                        .content(getReqJSONContent(VISIBILITY_CONTROLLER_METHODS[1]))
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json("{}", false));

    }

    @Override
    protected String getControllerName() {
        return CONTROLLER_NAME.toLowerCase();
    }

}
