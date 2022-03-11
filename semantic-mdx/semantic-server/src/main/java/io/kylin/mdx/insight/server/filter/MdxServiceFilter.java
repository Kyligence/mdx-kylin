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


package io.kylin.mdx.insight.server.filter;

import org.apache.commons.lang3.StringUtils;
import io.kylin.mdx.ErrorCode;
import io.kylin.mdx.ExceptionUtils;
import io.kylin.mdx.core.MdxException;
import io.kylin.mdx.rolap.cache.CacheManager;
import io.kylin.mdx.web.support.MdxAuthenticator;
import io.kylin.mdx.web.util.ErrorHandler;
import io.kylin.mdx.web.xmla.XmlaDatasourceManager;
import lombok.SneakyThrows;
import mondrian.olap.MondrianCacheControl;
import mondrian.util.ByteString;
import mondrian.xmla.XmlaRequestContext;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MdxServiceFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MdxServiceFilter.class);

    private static final Pattern URI_PATTERN = Pattern.compile("/mdx/xmla(_server)?/(.+)");

    @SneakyThrows
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        XmlaRequestContext context = Objects.requireNonNull(XmlaRequestContext.getContext(), "No Context!");
        try {
            Pair<Boolean, String> projectPair = getProjectContext(request.getRequestURI());
            Triple<String, Boolean, Boolean> triple = checkClearFlag(projectPair.getRight());
            String projectName = triple.getLeft();
            if (triple.getMiddle()) {
                MdxAuthenticator.authenticate(projectName, request, response);
                clearCache(projectName, triple.getRight(), response);
                return;
            }
            context.fromGateway = projectPair.getLeft();
            context.currentProject = projectName;
            context.currentUser = MdxAuthenticator.authenticate(projectName, request, response);
            filterChain.doFilter(request, response);
        } catch (MdxException me) {
            if (ErrorCode.MISSING_AUTH_INFO.equals(me.getErrorCode())) {
                LOGGER.warn(ExceptionUtils.getFormattedErrorMsg("please add auth info in your request.", ErrorCode.MISSING_AUTH_INFO));
            } else {
                LOGGER.error("internal error", me);
            }
            ErrorHandler.handleXmlaFault(request, response, me);
        } catch (Exception e) {
            if (e.getMessage() == null || !e.getMessage().contains("No enum constant mondrian.xmla.RowsetDefinition.DBSCHEMA_SCHEMATA")) {
                LOGGER.error("internal error", e);
            }
            ErrorHandler.handleXmlaFault(request, response, e);
        }
    }

    public static Pair<Boolean, String> getProjectContext(String contextPath) throws MdxException {
        Matcher uriMatcher = URI_PATTERN.matcher(contextPath);
        String projectContext = null;
        if (uriMatcher.find()) {
            projectContext = uriMatcher.group(2);
        }
        if (StringUtils.isBlank(projectContext)) {
            throw new MdxException("Please add parameter project in your url.", ErrorCode.UNSUPPORTED_XMLA_REQUEST_PATH);
        }
        boolean fromGateway = StringUtils.isNotBlank(uriMatcher.group(1));
        return new ImmutablePair<>(fromGateway, projectContext);
    }

    public static Triple<String, Boolean, Boolean> checkClearFlag(String projectContext) {
        String project = "";
        boolean clearCache = false;
        boolean deprecateFlag = false;
        int clearCacheFlagIdx = projectContext.indexOf("clearCache");
        int deprecateCacheFlagIdx = projectContext.indexOf("/clearCache");
        if (deprecateCacheFlagIdx != -1 && "".equals(projectContext.substring(0, deprecateCacheFlagIdx))) {
            // etc "/mdx/xmla//clearCache"
            clearCache = true;
            deprecateFlag = true;
        } else if (deprecateCacheFlagIdx != -1 && !"".equals(projectContext.substring(0, deprecateCacheFlagIdx))) {
            // etc "/mdx/xmla/learn_kylin/clearCache"
            clearCache = true;
            project = projectContext.substring(0, deprecateCacheFlagIdx);
        } else if (clearCacheFlagIdx != -1) {
            // etc "/mdx/xmla/clearCache"
            clearCache = true;
        } else {
            // etc "/mdx/xmla/learn_kylin"
            project = projectContext;
        }
        return new ImmutableTriple<>(project, clearCache, deprecateFlag);
    }

    public static void clearCache(String project, boolean deprecateFlag, HttpServletResponse response) throws IOException {
        if (StringUtils.isNotBlank(project)) {
            Pair<List<String>, List<ByteString>> schemaCacheKeys =
                    MondrianCacheControl.clearProjectCache(project);
            if (schemaCacheKeys != null) {
                for (int i = 0; i < schemaCacheKeys.getLeft().size(); i++) {
                    CacheManager.getCacheManager().expireAllForOneSchema(
                            schemaCacheKeys.getLeft().get(i), schemaCacheKeys.getRight().get(i));
                }
            }
            XmlaDatasourceManager.getInstance().clearSchemaHashCodes(project);
            response.getWriter().println(project + " cache has been cleared.");
        } else {
            MondrianCacheControl.clearCache();
            CacheManager.getCacheManager().expireAllForAllSchemas();
            XmlaDatasourceManager.getInstance().clearSchemaHashCodes();
            if (deprecateFlag) {
                response.getWriter().println(" Cache has been cleared. \n" +
                        "The API will be abandoned on December 31, 2020. " +
                        "Please use GET http://host:port/mdx/xmla/clearCache to clear the cache of the MDX server.");
            } else {
                response.getWriter().println("Cache has been cleared.");
            }
        }
    }

}
