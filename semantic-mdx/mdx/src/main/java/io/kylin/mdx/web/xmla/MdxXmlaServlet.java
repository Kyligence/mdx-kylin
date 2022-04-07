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


package io.kylin.mdx.web.xmla;

import com.google.common.net.HttpHeaders;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.util.NetworkUtils;
import org.apache.commons.lang3.StringUtils;
import io.kylin.mdx.insight.common.util.UUIDUtils;
import io.kylin.mdx.insight.core.entity.MdxQuery;
import io.kylin.mdx.insight.core.entity.SqlQuery;
import io.kylin.mdx.insight.core.support.MdxQueryMetrics;
import io.kylin.mdx.insight.core.support.SemanticFacade;
import io.kylin.mdx.insight.core.sync.MetaStore;
import io.kylin.mdx.insight.core.sync.QueryLogPersistence;
import io.kylin.mdx.insight.core.sync.UserSyncHolder;
import io.kylin.mdx.core.MdxConfig;
import io.kylin.mdx.web.support.HttpRequestWrapper;
import io.kylin.mdx.web.support.MdxAuthenticator;
import io.micrometer.core.instrument.Timer;
import lombok.SneakyThrows;
import mondrian.olap.MondrianProperties;
import mondrian.olap.MondrianServer;
import mondrian.util.Pair;
import mondrian.xmla.XmlaRequestContext;
import mondrian.xmla.context.ConnectionFactory;
import mondrian.xmla.impl.DefaultXmlaServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MdxXmlaServlet extends DefaultXmlaServlet {

    private static final Logger logger = LoggerFactory.getLogger(MdxXmlaServlet.class);

    private static final Timer mdxQueryTimer = MdxQueryMetrics.mdxQueryRequestTimer();

    private static final MdxConfig MDX_CONFIG = MdxConfig.getInstance();

    @Override
    protected synchronized ConnectionFactory createConnectionFactory(ServletConfig servletConfig) {
        XmlaRequestContext context = XmlaRequestContext.getContext();
        String username = context.getLoginUser();
        String project = context.currentProject;
        String delegate = context.delegateUser;
        String cacheKey = username + "_" + project;
        if (delegate != null) {
            cacheKey = delegate + "_" + cacheKey;
        }
        MondrianServer server = MondrianServerManager.getIfPresent(cacheKey);
        if (server == null) {
            server = new MondrianServerBuilder(servletConfig)
                    .project(project)
                    .username(username)
                    .delegate(delegate)
                    .build();
            MondrianServerManager.putMondrianServer(cacheKey, server);
        }
        return (ConnectionFactory) server;
    }

    @Override
    public void destroy() {
        super.destroy();
        Map<String, MondrianServer> serverMap = MondrianServerManager.asMap();
        for (Map.Entry<String, MondrianServer> entry : serverMap.entrySet()) {
            entry.getValue().shutdown();
        }
    }

    @SneakyThrows
    @Override
    public void process(HttpServletRequest request, HttpServletResponse response) {
        XmlaRequestContext context = XmlaRequestContext.getContext();
        try {
            HttpRequestWrapper requestWrapper = HttpRequestWrapper.wrap(request);

            prepareMondrianSchema(requestWrapper, context);
            setMdxContext(requestWrapper, context);
            printConnectInfo(requestWrapper, context);

            prepareServletResponse(context, response);
            super.process(requestWrapper, response);
        } finally {
            logStatistics(context);
        }
    }

    private static void prepareServletResponse(XmlaRequestContext mdxContext, HttpServletResponse response) {
        // set header : Mdx-Query-Id and Mdx-Execute-Node
        if (mdxContext.runningStatistics.queryID != null) {
            response.addHeader("Mdx-Query-Id", mdxContext.runningStatistics.queryID);
            // 兼容旧版 gateway
            if (mdxContext.fromGateway) {
                response.addHeader("queryId", mdxContext.runningStatistics.queryID);
            }
        }
        String mdxHost = MDX_CONFIG.getMdxHost();
        if (StringUtils.isNotBlank(mdxHost)) {
            if (SemanticConstants.LOCAL_IP.equals(mdxHost) || SemanticConstants.LOCAL_HOST.equals(mdxHost)) {
                mdxHost = NetworkUtils.getLocalIP();
            }
            response.addHeader("Mdx-Execute-Node", mdxHost + ":" + MDX_CONFIG.getMdxPort());
        }
        // set query start time
        mdxContext.runningStatistics.start = System.currentTimeMillis();
    }

    private static void setMdxContext(HttpRequestWrapper request, XmlaRequestContext mdxContext) {
        mdxContext.mdxOptimizer = new DefaultMdxOptimizer();
        mdxContext.mdxRewriter = new DefaultMdxRewriter();
        mdxContext.mdxRejecter = new DefaultMdxRejecter();
        mdxContext.excelClient = request.getBody().contains("DbpropMsmdMDXCompatibility");
        setXmlaRequestProperties(request, mdxContext);
    }

    private static void logStatistics(XmlaRequestContext mdxContext) {
        boolean gateway = mdxContext.fromGateway;
        if (mdxContext.redirectMdx != null) {
            String mdxQuery = mdxContext.mdxQuery;
            if (mdxQuery == null) {
                mdxQuery = "cancel";
            }
            logger.info("mdx query '{}' send redirect to {}", mdxQuery, mdxContext.redirectMdx);
        } else if (mdxContext.mdxQuery != null) {
            long end = System.currentTimeMillis();
            mdxContext.runningStatistics.mdxRunTotalTime = end - mdxContext.runningStatistics.start;
            mdxQueryTimer.record(Duration.ofMillis(mdxContext.runningStatistics.mdxRunTotalTime));
            long queryTimeout = MondrianProperties.instance().QueryTimeout.get() * 1000L;
            if (queryTimeout != 0 && queryTimeout < 60000) {
                queryTimeout = 60000;
            }
            if (mdxContext.runningStatistics.mdxRunTotalTime > queryTimeout) {
                mdxContext.runningStatistics.timeout = true;
            }
            if (gateway) {
                long start = mdxContext.runningStatistics.marshallSoapMessageTimeStart;
                mdxContext.runningStatistics.marshallSoapMessageTime = end - start;
                mdxContext.runningStatistics.gatewayUsed = true;
            }
            String runningReport = mdxContext.runningStatistics.getReportString();
            if (runningReport != null) {
                logger.info(runningReport);
                if (QueryLogPersistence.INSTANCE != null) {
                    Map<String, Object> mapMdxQuery = mdxContext.runningStatistics.getMdxQuery();
                    mapMdxQuery.put("gateway", gateway);
                    mapMdxQuery.put("node", MdxConfig.getInstance().getCluster());
                    MdxQuery mdxQuery = new MdxQuery(mapMdxQuery);
                    // 对类型为 PowerBI Desktop 的应用类型，统一保存为 PowerBI
                    if (XmlaRequestContext.ClientType.POWERBI_DESKTOP.equals(mdxQuery.getApplication())) {
                        mdxQuery.setApplication(XmlaRequestContext.ClientType.POWERBI);
                    }
                    QueryLogPersistence.INSTANCE.asyncNotify(mdxQuery);
                    List<Map<String, Object>> sqlQueryList = mdxContext.runningStatistics.getSqlQueryList();
                    for (Map<String, Object> mapSqlQuery : sqlQueryList) {
                        SqlQuery sqlQuery = new SqlQuery(mapSqlQuery);
                        QueryLogPersistence.INSTANCE.asyncNotify(sqlQuery);
                    }
                    logger.info("mdx query persistence: {}, sql query persisted size: {}",
                            mdxQuery.getMdxQueryId(), sqlQueryList.size());
                }
            }
        }
    }

    private static void prepareMondrianSchema(HttpRequestWrapper requestWrapper, XmlaRequestContext mdxContext) {
        String username = mdxContext.getLoginUser();
        String delegate = mdxContext.delegateUser;
        String project = mdxContext.currentProject;

        boolean forceRefresh = MetaStore.getInstance().isForceRefreshSchema(project, mdxContext.getQueryUser());
        if (forceRefresh) {
            SemanticFacade.INSTANCE.clearProjectCache(project, mdxContext.getQueryUser());
        }

        if (MdxConfig.getInstance().isDisableRefreshSchema()) {
            logger.info("disable refresh schema, so {}:{} not refresh", mdxContext.getQueryUser(), project);
            return;
        }
        String password = MdxAuthenticator.getPassword(new Pair<>(username, project));
        String excludedProject = null;
        boolean shouldRefreshSchema = shouldRefreshSchema(requestWrapper.getBody()) || forceRefresh;
        XmlaDatasourceManager manager = XmlaDatasourceManager.getInstance();
        if (mdxContext.invalidPassword
                || shouldRefreshSchema
                || !manager.isSchemaExist(username, project, delegate)) {
            if (!mdxContext.notLogRequest) {
                logger.info("begin init datasource, username={}, project={}", username, project);
                XmlaDatasource datasource = manager.newDatasource(username, password, project, delegate, forceRefresh);
                // 对于抛出无可用数据集 MdxException，不处理直接上抛
                datasource.initDatasource();
                mdxContext.hasRefreshedSchema = true;
                excludedProject = project;
            }
        }
        handleDatasetChange(username, password, delegate, excludedProject);
    }

    private static boolean shouldRefreshSchema(String xmlaBody) {
        // SSAS and xmla-connect
        return (xmlaBody.contains("<RequestType>DISCOVER_PROPERTIES</RequestType>")
                && xmlaBody.contains("<PropertyName>Catalog</PropertyName>"))
                || xmlaBody.contains("DBSCHEMA_CATALOGS");
    }

    private static void handleDatasetChange(String username, String password, String delegate, String excluded) {
        Set<String> projects = UserSyncHolder.INSTANCE.getChangedProjectsByUser(username);
        projects.remove(excluded);
        for (String project : projects) {
            try {
                XmlaDatasourceManager manager = XmlaDatasourceManager.getInstance();
                XmlaDatasource datasource = manager.newDatasource(username, password, project, delegate, false);
                datasource.initDatasource();
                logger.info("Event dataset-change-handler: mondrian refresh dataset of project:[{}] successfully.", project);
            } catch (Exception e) {
                // 忽略其他 project 异常, 例如: 无可用数据集
                logger.error("Event dataset-change-handler: mondrian refresh dataset of project:[{}] error.", project, e);
            }
        }
    }

    private static void setXmlaRequestProperties(HttpServletRequest request, XmlaRequestContext mdxContext) {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        // Apache-HttpClient match MicroStrategy Web on KC
        if (userAgent == null || userAgent.contentEquals("") || userAgent.startsWith("Apache-HttpClient")) {
            userAgent = "MicroStrategy";
        }
        // set client type
        mdxContext.clientType = XmlaRequestContext.ClientType.fromUserAgent(userAgent);

        // set can optimize mdx and calculate total or not.
        String enableOptimizeMdx = request.getParameter("enableOptimizeMdx");
        String needCalculateTotal = request.getParameter("needCalculateTotal");
        enableOptimizeMdx = enableOptimizeMdx != null ? enableOptimizeMdx
                : String.valueOf(MDX_CONFIG.isOptimizeMdxEnable());
        needCalculateTotal = needCalculateTotal != null ? needCalculateTotal
                : String.valueOf(MDX_CONFIG.isNeedCalculateTotal());
        mdxContext.setParameter(XmlaRequestContext.Parameter.ENABLE_OPTIMIZE_MDX, enableOptimizeMdx);
        mdxContext.setParameter(XmlaRequestContext.Parameter.NEED_CALCULATE_TOTAL, needCalculateTotal);

        // set use other query engine or mondrian engine.
        String useMondrianEngine = request.getParameter(XmlaRequestContext.Parameter.USE_MONDRIAN_ENGINE);
        if (StringUtils.isNotBlank(useMondrianEngine) && Boolean.TRUE.equals(Boolean.valueOf(useMondrianEngine))) {
            mdxContext.useMondrian = true;
        } else {
            String executorClass = MondrianProperties.instance().RolapExecutorClass.get();
            if (executorClass == null || executorClass.equals("native")) {
                mdxContext.useMondrian = true;
            } else {
                mdxContext.executorClass = executorClass;
            }
        }

        // set compact result (not compress)
        String compactResult = request.getParameter(XmlaRequestContext.Parameter.NEED_COMPACT_RESULT);
        if (StringUtils.isNotBlank(compactResult)) {
            mdxContext.compactResult = Boolean.parseBoolean(compactResult);
        } else {
            mdxContext.compactResult = SemanticConfig.getInstance().isEnableCompactResult();
        }

        // set enable debug mode
        String debugMode = request.getParameter(XmlaRequestContext.Parameter.ENABLE_DEBUG_MODE);
        if (StringUtils.isNotBlank(debugMode)) {
            mdxContext.debugMode = Boolean.parseBoolean(debugMode);
        }
    }

    private static void printConnectInfo(HttpRequestWrapper request, XmlaRequestContext mdxContext) {
        String queryString = request.getQueryString();
        String authInfo = mdxContext.getLoginUser();
        if (mdxContext.delegateUser != null) {
            authInfo = authInfo + ":" + mdxContext.delegateUser;
        }
        if (queryString != null) {
            queryString = queryString.replaceAll("password=[^&]+", "password=******");
        }
        String propertyName = null;
        String requestBody = request.getBody();
        if (requestBody.contains("</Discover>")) {
            int requestTypeBeginId = requestBody.indexOf("<RequestType>");
            int requestTypeEndId = requestBody.indexOf("</RequestType>");
            String requestType = "Discover";
            if (requestTypeBeginId != -1 && requestTypeEndId != -1) {
                propertyName = requestBody.substring(requestTypeBeginId + 13, requestTypeEndId);
            }
            if (!mdxContext.notLogRequest) {
                logger.info("{} {} {} {} requestType:{} Property:{}", request.getHeader(HttpHeaders.USER_AGENT),
                        request.getRequestURL(), queryString, authInfo, requestType, propertyName);
            }
        } else if (requestBody.contains("</Execute>")) {
            String requestType = "Execute";
            if (requestBody.contains("EndSession")) {
                propertyName = "EndSession";
            } else if (requestBody.contains("BeginGetSessionToken")) {
                propertyName = "BeginGetSessionToken";
            } else if (requestBody.contains("BeginSession")) {
                propertyName = "BeginSession";
            } else if (requestBody.contains("REFRESH CUBE")) {
                propertyName = "REFRESH CUBE";
            }
            if (propertyName == null) {
                propertyName = "Execute Query";
            }
            if (StringUtils.isBlank(mdxContext.runningStatistics.queryID)) {
                mdxContext.runningStatistics.queryID = UUIDUtils.randomUUID();
            }
            logger.info("{} {} {} {} requestType:{} Property:{}", request.getHeader(HttpHeaders.USER_AGENT),
                    request.getRequestURL(), queryString, authInfo, requestType, propertyName);
        } else {
            logger.info("{} {} {} {}", request.getHeader(HttpHeaders.USER_AGENT),
                    request.getRequestURL(), queryString, authInfo);
        }
    }

}
