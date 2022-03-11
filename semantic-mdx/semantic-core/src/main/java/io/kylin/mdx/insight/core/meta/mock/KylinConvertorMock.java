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


package io.kylin.mdx.insight.core.meta.mock;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.http.Response;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.core.meta.KylinConvertor;
import org.apache.http.HttpStatus;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static io.kylin.mdx.insight.core.meta.mock.MockTestLoader.getContentByInputStream;

public class KylinConvertorMock extends KylinConvertor {

    private final static String project = "EasyJet";

    @Override
    public Set<String> getActualProjectSet(ConnectionInfo connInfo) throws SemanticException {
        return new HashSet<>(Collections.singletonList(project));
    }

    @Override
    protected String getAccessResponse() {
        return "{\n" +
                "    \"code\": \"000\",\n" +
                "    \"data\": \"GLOBAL_ADMIN\",\n" +
                "    \"msg\": \"\"\n" +
                "}";
    }

    @Override
    protected String getCubeNames(String projectName) {
        String path = new StringBuilder()
                .append(getKeJsonPath())
                .append("/")
                .append(projectName).append("_cubelist")
                .append(".json")
                .toString();
        return getContentByInputStream(path);
    }

    @Override
    protected String getModelDescByHttp(String modelName, String projectName) {
        String path = new StringBuilder()
                .append(getKeJsonPath())
                .append("/")
                .append(projectName).append("_model_").append(modelName)
                .append(".json")
                .toString();
        return getContentByInputStream(path);
    }

    @Override
    protected String getColumnDescByHttp(String projectName) {
        String path = new StringBuilder()
                .append(getKeJsonPath())
                .append("/")
                .append(projectName).append("_tablecolumns")
                .append(".json")
                .toString();
        return getContentByInputStream(path);
    }

    @Override
    protected String getCubeDescByHttp(String cubeName, String projectName) {
        String path = new StringBuilder()
                .append(getKeJsonPath())
                .append("/")
                .append(projectName).append("_cube_").append(cubeName)
                .append(".json")
                .toString();
        return getContentByInputStream(path);
    }

    @Override
    protected String getProjectList(String projectName) {
        String path = new StringBuilder()
                .append(getKeJsonPath())
                .append("/")
                .append("mdx_automation_test").append("_projectlist")
                .append(".json")
                .toString();
        return getContentByInputStream(path);
    }

    @Override
    public Response authentication(String basicAuth) throws SemanticException {
        Response response = new Response();
        response.setHttpStatus(HttpStatus.SC_OK);
        response.setContent("");
        return response;
    }

    @Override
    protected Response getLicenseResponse() {
        Response response = new Response();
        response.setHttpStatus(HttpStatus.SC_OK);
        response.setContent(getContentByInputStream("/json/license.json"));
        return response;
    }

    private String getKeJsonPath() {
        return System.getProperty("kylin.json.path");
    }

}
