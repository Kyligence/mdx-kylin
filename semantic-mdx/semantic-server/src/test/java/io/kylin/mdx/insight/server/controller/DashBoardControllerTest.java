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

import static io.kylin.mdx.insight.common.util.Utils.buildBasicAuth;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
public class DashBoardControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    private final String content = "{\n" +
            "  \"axis\": [1608011590110, 1608112517653],\n" +
            "  \"datasetNames\": [\"1\", \"2\"]\n" +
            "}";

    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/kylinmetadata");
    }

    @Test
    public void getBasicStatisticsTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/statistics/basic?startTime=1608011590110&endTime=1608021230176&projectName=learn_kylin")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
        ).andExpect(status().isOk());
    }

    @Test
    public void getTrendStatisticsTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .post("/api/statistics/trend?projectName=learn_kylin&datasetNames=1,2")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                        .contentType("application/json")
                        .content(content)
        ).andExpect(status().isOk());
    }

    @Test
    public void getQueryCostStatisticsTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .post("/api/statistics/query-cost?projectName=learn_kylin")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                        .contentType("application/json")
                        .content(content)
        ).andExpect(status().isOk());
    }

    @Test
    public void getRankingStatisticsTest() throws Exception {
        this.mvc.perform(
                MockMvcRequestBuilders
                        .get("/api/statistics/ranking?startTime=1608011590110&endTime=1608021230176&projectName=learn_kylin&count=2&direction=DESC")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
        ).andExpect(status().isOk());

    }

    @Override
    protected String getControllerName() {
        return null;
    }
}
