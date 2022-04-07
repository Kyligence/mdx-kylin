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

import io.kylin.mdx.insight.common.util.JacksonSerDeUtils;
import io.kylin.mdx.insight.common.util.Utils;


import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.server.bean.dto.DatasetImportDetailsDTO;
import io.kylin.mdx.insight.server.bean.dto.FormatSampleDTO;
import io.kylin.mdx.insight.server.bean.dto.ProjectDatasetDTO;
import io.kylin.mdx.insight.server.service.BatchDatasetService;
import io.kylin.mdx.insight.server.bean.dto.DatasetDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetImportRequestDTO;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.Cookie;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;

import static io.kylin.mdx.insight.common.util.Utils.buildBasicAuth;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
public class DatasetControllerTest extends BaseControllerTest {

    private static final String CONTROLLER_NAME = "DatasetController";

    private static final Integer TEST_DATASET_ID = 4;

    private static final String[] DATASET_CONTROLLER_METHODS = {
            "getAllProjects",
            "getDatasetsByPage",
            "existDataset",
            "deleteOneDataset",
            "updateOneDataset",
            "getOneDataset",
            "insertOneDataset",
            "packageRequest",
            "MDX_AdventureWorks_20210916114142",
            "insertOneDataset2",
    };

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private MockHttpServletResponse mockHttpServletResponse;

    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/kylinmetadata");
    }

    @AfterClass
    public static void end() {
        File file = new File("/zip/test.zip");
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void getAllProjects() throws Exception {
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/projects")
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json(getRespJSONContent(DATASET_CONTROLLER_METHODS[0]), false));
    }

    @Test
    public void getAllProjectsByCookie() throws Exception {
        String currentCookie = Utils.encodeTxt(20, "ADMIN");
        Cookie cookie = new Cookie("mdx_session",
                currentCookie);
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/projects")
                                .cookie(cookie)
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json(getRespJSONContent(DATASET_CONTROLLER_METHODS[0]), false));
    }

    @Test
    public void addSessionCookieTest() {
        authService.addSessionCookie("ADMIN", "KYLIN", mockHttpServletResponse);
    }

    @Test
    public void removeSessionCookieTest() {
        String currentCookie = Utils.encodeTxt(20, "ADMIN");
        Cookie cookie = new Cookie("mdx_session",
                currentCookie);
        mockHttpServletResponse.addCookie(cookie);
        authService.removeSessionCookie(mockHttpServletResponse);
    }


    @Test
    public void getDatasetsByPageTest() throws Exception {
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/datasets?pageNum=0&pageSize=10")
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));

    }

    @Test
    public void getDatasetsByPageAndDatasetNameTest() throws Exception {
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/datasets?pageNum=0&pageSize=10&projectName=learn_kylin&datasetName=test&orderBy=status,dataset&direction=desc,desc")
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));
    }


    @Test
    public void exportDatasetZipPackageTest() throws Exception {
        String currentCookie = Utils.encodeTxt(20, "ADMIN");
        Cookie cookie = new Cookie("mdx_session", currentCookie);
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/datasets/package")
                                .cookie(cookie)
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                                .contentType("application/json")
                                .content(getReqJSONContent(DATASET_CONTROLLER_METHODS[7])))
                .andExpect(status().isOk());
    }

    @Test
    public void getDatasetBrokenTest() throws Exception {
        String currentCookie = Utils.encodeTxt(20, "ADMIN");
        Cookie cookie = new Cookie("mdx_session", currentCookie);
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/datasets/validation/broken?projectName=AdventureWorks")
                                .cookie(cookie)
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                                .contentType("application/json")
                                .content(getReqJSONContent(DATASET_CONTROLLER_METHODS[7])))
                .andExpect(status().isOk());
    }


    @Test
    public void downloadPackage() throws Exception {
        String token = String.valueOf(System.currentTimeMillis());
        String currentCookie = Utils.encodeTxt(20, "ADMIN");
        Cookie cookie = new Cookie("mdx_session", currentCookie);
        DatasetDTO datasetDTO = JacksonSerDeUtils.readString(getReqJSONContent(DATASET_CONTROLLER_METHODS[6]), DatasetDTO.class);
        DatasetController.exportMap.put(token, Arrays.asList(datasetDTO));

        this.mvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/datasets/package/" + token + "?projectName=11")
                                .cookie(cookie)
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                                .contentType("application/json"))
                .andExpect(status().isOk());
    }


    @Test
    public void uploadDatasetTest2() throws Exception {
        String currentCookie = Utils.encodeTxt(20, "ADMIN");
        Cookie cookie = new Cookie("mdx_session", currentCookie);
        MockMultipartFile jsonFile = new MockMultipartFile("file", "file", "application/zip", getZipStream(DATASET_CONTROLLER_METHODS[8]));
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .multipart("/api/datasets?type=import&projectName=mdx_automation_test")
                                .file(jsonFile)
                                .cookie(cookie)
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                                .characterEncoding("UTF-8"))
                .andExpect(status().isOk());
    }

    @Test
    public void importDatasetTest() throws Exception {
        String token = String.valueOf(System.currentTimeMillis());
        String currentCookie = Utils.encodeTxt(20, "ADMIN");
        Cookie cookie = new Cookie("mdx_session", currentCookie);
        DatasetDTO datasetDTO = JacksonSerDeUtils.readString(getReqJSONContent(DATASET_CONTROLLER_METHODS[6]), DatasetDTO.class);
        DatasetDTO datasetDTO2 = JacksonSerDeUtils.readString(getReqJSONContent(DATASET_CONTROLLER_METHODS[9]), DatasetDTO.class);
        DatasetImportDetailsDTO datasetImportDetailsDTO = new DatasetImportDetailsDTO("1", "test-1", true, "ADD_NEW");
        DatasetImportDetailsDTO datasetImportDetailsDTO2 = new DatasetImportDetailsDTO("2", "test-1", false, "OVERRIDE");
        DatasetImportRequestDTO datasetImportRequestDTO = new DatasetImportRequestDTO(token, "test_1", Arrays.asList(datasetImportDetailsDTO, datasetImportDetailsDTO2));
        DatasetController.importToken.add(token);
        Pair<DatasetDTO, DatasetDTO> pair = Pair.of(null, datasetDTO);
        Pair<DatasetDTO, DatasetDTO> pairOverride = Pair.of(datasetDTO2, datasetDTO2);

        BatchDatasetService.importDatasetMap.put("1", pair);
        BatchDatasetService.importDatasetMap.put("2", pairOverride);

        this.mvc.perform(
                        MockMvcRequestBuilders
                                .put("/api/datasets?type=import")
                                .cookie(cookie)
                                .contentType("application/json")
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                                .characterEncoding("UTF-8")
                                .content(JacksonSerDeUtils.writeJsonAsByte(datasetImportRequestDTO)))
                .andExpect(status().isOk());
    }


    @Test
    public void existDatasetTest() throws Exception {
        String doExistRespJSON =
                "{" +
                        "  \"status\" : 0," +
                        "  \"data\" : true" +
                        "}";
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/dataset/{project}/{dataset}/{type}", "mdx_automation_test", "snowflake_dataset", "MDX")
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json(doExistRespJSON, false));

        String notExistRespJSON =
                "{" +
                        "  \"status\" : 0," +
                        "  \"data\" : false" +
                        "}";
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/dataset/{project}/{dataset}/{type}", "learn_kylin", "dataset1", "SQL")
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json(notExistRespJSON, false));

    }

    @Test
    @Transactional
    public void deleteOneDatasetTest() throws Exception {
        String deleteSucResp =
                "{" +
                        "  \"status\" : 0," +
                        "  \"data\" : \"success\" " +
                        "}";
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .delete("/api/dataset/{datasetId}", TEST_DATASET_ID)
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json(deleteSucResp, false));
    }

    @Test
    @Transactional
    public void updateOneDatasetTest() throws Exception {
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .put("/api/dataset/{datasetId}", TEST_DATASET_ID)
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                                .contentType("application/json")
                                .content(getReqJSONContent(DATASET_CONTROLLER_METHODS[6])))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));
    }

    @Test
    public void getOneDatasetTest() throws Exception {
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/dataset/{datasetId}", TEST_DATASET_ID)
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                                .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE))
                .andExpect(content().json("{}", false));
    }

    @Test
    @Transactional
    public void insertOneDatasetTest() throws Exception {
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/dataset?createType=new")
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                                .contentType("application/json")
                                .content(getReqJSONContent(DATASET_CONTROLLER_METHODS[6])))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));
    }

    @Test
    @Transactional
    public void importOneDatasetTest() throws Exception {
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/dataset/import")
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                                .contentType("application/json")
                                .content(getReqJSONContent(DATASET_CONTROLLER_METHODS[6])))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));
    }

    @Test
    @Transactional
    public void updateDatasetTest() throws Exception {
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .put("/api/dataset")
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                                .contentType("application/json")
                                .content(getReqJSONContent(DATASET_CONTROLLER_METHODS[6])))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));
    }

    @Test
    public void exportOneDatasetTest() throws Exception {
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/dataset?datasetType=MDX&project=mdx_automation_test&datasetName=snowflake_dataset")
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));
    }

    @Test
    public void exportOneDataset2Test() throws Exception {
        ProjectDatasetDTO projectDatasetDTO = new ProjectDatasetDTO();
        projectDatasetDTO.setProject("mdx_automation_test");
        projectDatasetDTO.setDatasetName("snowflake_dataset");
        InputStream fileInputStream = new ByteArrayInputStream(JacksonSerDeUtils.writeJsonAsByte(projectDatasetDTO));
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/dataset/export/MDX")
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                                .contentType("application/json")
                                .content(IOUtils.toString(fileInputStream)))
                .andExpect(status().isOk());
    }


    @Test
    public void getDatasetTest() throws Exception {
        this.mvc.perform(
                        MockMvcRequestBuilders
                                .get("/api/datasets/{type}/{project}/", "MDX", "mdx_automation_test")
                                .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(DEFAULT_CONTENT_TYPE));

    }
    @Test
    public void formatPreviewTest() throws Exception {
        FormatSampleDTO formatSampleDTO = new FormatSampleDTO();
        formatSampleDTO.setFormat("USD");
        formatSampleDTO.setValue(0.5);
        InputStream fileInputStream = new ByteArrayInputStream(JacksonSerDeUtils.writeJsonAsByte(formatSampleDTO));
        this.mvc.perform(
                MockMvcRequestBuilders
                        .post("/api/dataset/format/preview")
                        .header(SemanticConstants.BASIC_AUTH_HEADER_KEY, buildBasicAuth("ADMIN", "KYLIN"))
                        .contentType("application/json")
                        .content(IOUtils.toString(fileInputStream)))
                .andExpect(status().isOk());


    }

    @Override
    protected String getControllerName() {
        return CONTROLLER_NAME.toLowerCase();
    }
}
