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


package io.kylin.mdx.insight.core.meta;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.http.HttpUri;
import io.kylin.mdx.insight.common.http.HttpUtil;
import io.kylin.mdx.insight.common.http.Response;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import static io.kylin.mdx.insight.common.http.HttpUri.GET_KYLIN_PROJECTS_URI;
import static io.kylin.mdx.insight.common.http.HttpUri.GET_KYLIN_ACCESS_INFO;

@Slf4j(topic = "http.call")
public abstract class AbstractConvertor<T> implements IConvertor<T>, IHttpCall {

    private static final int HTTP_LOG_DEBUG = SemanticConfig.getInstance().getLogHttpDebugTime();
    private static final int HTTP_LOG_INFO = SemanticConfig.getInstance().getLogHttpInfoTime();
    private static final int HTTP_LOG_WARN = SemanticConfig.getInstance().getLogHttpWarnTime();
    private static final int HTTP_LOG_ERROR = SemanticConfig.getInstance().getLogHttpErrorTime();

    @Override
    public Response authentication(String basicAuth) throws SemanticException {
        return HttpUtil.httpPostWithResponse(HttpUri.USER_AUTHENTICATION, basicAuth.getBytes(StandardCharsets.UTF_8), "", false);
    }

    @Override
    public Response getLicense() throws SemanticException {
        return getLicenseResponse();
    }

    @Override
    public Set<String> getActualProjectSet(ConnectionInfo connInfo) throws SemanticException {
        byte[] auth = Utils.encodeAuthentication(connInfo.getUser(), connInfo.getPassword());

        String content;
        if (SemanticConfig.getInstance().isConvertorMock()) {
            content = getProjectList(connInfo.getProject());
        } else {
            content = doHttpCall(GET_KYLIN_PROJECTS_URI, auth);
        }

        JSONObject jsonObject = JSONObject.parseObject(content);

        if (!SemanticConstants.STATUS_SUC.equals(jsonObject.getString(SemanticConstants.STATUS_KEY))) {
            log.warn("To get project lists has exception, {}", content);
            throw new SemanticException("To get project lists has exception", ErrorCode.FETCH_KYLIN_PROJECT_LIST_ERROR);
        }

        Set<String> projects = new TreeSet<>();
        JSONArray projectJsonArrs;
        projectJsonArrs = jsonObject.getJSONObject(SemanticConstants.SUC_DATA).getJSONArray("projects");
        for (int i = 0; i < projectJsonArrs.size(); i++) {
            JSONObject project = projectJsonArrs.getJSONObject(i);
            projects.add(project.getString("name"));
        }

        return projects;
    }

    @Override
    public String getAccessInfo(ConnectionInfo connInfo) throws SemanticException {
        byte[] auth = Utils.encodeAuthentication(connInfo.getUser(), connInfo.getPassword());

        String content;
        if (SemanticConfig.getInstance().isConvertorMock()) {
            content = getAccessResponse();
        } else {
            String accessInfo = GET_KYLIN_ACCESS_INFO.replace("{project}", connInfo.getProject());
            content = doHttpCall(accessInfo, auth);
        }

        JSONObject jsonObject = JSONObject.parseObject(content);

        if (!SemanticConstants.STATUS_SUC.equals(jsonObject.getString(SemanticConstants.STATUS_KEY))) {
            log.warn("To get project access information has exception, {}", content);
            throw new SemanticException("To get project access information has exception", ErrorCode.FETCH_KYLIN_ACCESS_INFO_ERROR);
        }
        return jsonObject.getString(SemanticConstants.SUC_DATA);
    }

    @Override
    public String doHttpCall(String url, byte[] auth) {
        long start = System.currentTimeMillis();
        CloseableHttpResponse response = null;
        try {
            response = HttpUtil.httpGet(url, auth);
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SemanticException("The http calling invoke exception.", e);
        } finally {
            int status = response == null ? 500 : response.getStatusLine().getStatusCode();
            long time = System.currentTimeMillis() - start;
            if (time >= HTTP_LOG_ERROR) {
                log.error("Http call. status:{}, url:{}, time:{}", status, url, time);
            } else if (time >= HTTP_LOG_WARN) {
                log.warn("Http call. status:{}, url:{}, time:{}", status, url, time);
            } else if (time >= HTTP_LOG_INFO) {
                log.info("Http call. status:{}, url:{}, time:{}", status, url, time);
            } else if (time >= HTTP_LOG_DEBUG) {
                log.debug("Http call. status:{}, url:{}, time:{}", status, url, time);
            }
            IOUtils.closeQuietly(response);
        }
    }

    @Override
    public Response getProfile(ConnectionInfo connectionInfo) throws SemanticException {
        if (SemanticConfig.getInstance().isConvertorMock()) {
            return getProfileResponse();
        } else {
            return null;
        }
    }

    protected String getProjectList(String project) {
        return null;
    }

    protected Response getLicenseResponse() {
        return null;
    }

    protected String getAccessResponse() {
        return null;
    }

    protected Response getProfileResponse() {
        return null;
    }

    public static void validateRespSuccess(JSONObject respJSON) throws SemanticException {
        if (SemanticConstants.STATUS_SUC.equals(respJSON.getString(SemanticConstants.STATUS_KEY))) {
            return;
        }
        String msg = respJSON.getString("msg");
        if (StringUtils.isBlank(msg)) {
            msg = respJSON.getString("stacktrace");
        }
        log.error("The http response get code != 000, info:{}", msg);
        throw new SemanticException(msg);
    }

}
