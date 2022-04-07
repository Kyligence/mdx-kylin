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


package io.kylin.mdx.insight.common;

import io.kylin.mdx.insight.common.constants.ConfigConstants;
import io.kylin.mdx.insight.common.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;

/**
 * 配置文件加载：
 * #  取默认配置文件: insight.override.properties > insight.properties
 * #  取 propFile + ".override" 覆盖配置
 */
@Slf4j
public class SemanticConfig extends SemanticConfigBase {
    private static class SemanticConfigHolder {
        private static final SemanticConfig INSTANCE = new SemanticConfig();
    }


    private static final String HTTP_LITERAL = "http";

    private static final String HTTPS_LITERAL = "https";

    public static SemanticConfig getInstance() {
        return SemanticConfigHolder.INSTANCE;
    }

    private SemanticConfig() {
        loadConfig();
        initConfig();
    }

    public String getClustersInfo() {
        return getOptional("insight.mdx.cluster.nodes", "");
    }

    public void setClustersInfo(String clustersInfo) {
        getProperties().put("insight.mdx.cluster.nodes", clustersInfo);
    }

    public boolean isConvertorMock() {
        return getBooleanValue("converter.mock", false);
    }

    public boolean isSyncOnStartupEnable() {
        return getBooleanValue("insight.semantic.startup.sync.enable", true);
    }

    public boolean isClearOnStartupEnable() {
        return getBooleanValue("insight.semantic.startup.clear.enable", true);
    }

    public boolean isUpperAdminName() {
        return getBooleanValue("insight.mdx.upper-admin-name", true);
    }

    public boolean isUpperUserName() {
        return getBooleanValue("insight.mdx.upper-user-name", true);
    }

    public String getKylinHost() {
        return getOptional("insight.kylin.host", "localhost");
    }

    public String getKylinPort() {
        return getOptional("insight.kylin.port", "7070");
    }

    public DatasourceTypeEnum getDatasourceType() {
        return DatasourceTypeEnum.KYLIN;
    }

    public String getProjectPageSize() {
        return getOptional("insight.semantic.meta.sync.project-page-size", "1000");
    }

    public String getModelPageSize() {
        return getOptional("insight.semantic.meta.sync.model-page-size", "1000");
    }

    public String getUserPageSize() {
        return getOptional("insight.semantic.meta.sync.user-page-size", "100000");
    }

    public String getTablePageSize() {
        return getOptional("insight.semantic.meta.sync.table-page-size", "10000");
    }

    public boolean isModelVersionVerifyEnable() {
        return getBooleanValue("insight.semantic.model.version.verify", false);
    }

    public int getMdxQueryHousekeepMaxRows() {
        return getIntValue("insight.semantic.meta.keep.mdx-query.max.rows", 1000000);
    }

    public int getMdxQueryQueueSize() {
        return getIntValue("insight.semantic.meta.keep.mdx-query.queue.size", 1000);
    }

    public String getTimeZone() {
        return getOptional("insight.semantic.time-zone", "GMT+8:00");
    }

    public boolean isDatasetVerifyEnable() {
        return getBooleanValue("insight.dataset.verify.enable", true);
    }

    public int getDatasetVerifyInterval() {
        return getIntValue("insight.dataset.verify.interval.count", 15);
    }

    public boolean isDatasetAccessByDefault() {
        return getBooleanValue(SemanticConstants.DATASET_ALLOW_ACCESS_BY_DEFAULT, false);
    }

    public int getCookieAge() {
        return getIntValue("insight.semantic.cookie-age", 86400);
    }

    public String getKylinProtocol() {
        return getBooleanValue("insight.kylin.ssl", false) ? HTTPS_LITERAL : HTTP_LITERAL;
    }

    public String getMdxProtocol() {
        return getBooleanValue("insight.semantic.ssl.enabled", false) ? HTTPS_LITERAL : HTTP_LITERAL;
    }

    public String getMdxHost() {
        return getOptional("insight.host", "127.0.0.1");
    }

    public String getMdxPort() {
        return getOptional("insight.semantic.port", "7080");
    }

    public boolean isWhetherCheckDatasetConnect() {
        return getBooleanValue("insight.semantic.checkout.dataset.connect", true);
    }

    public int getConnectTimeout() {
        return getIntValue("insight.semantic.connect.timeout", 5000);
    }

    public int getSocketTimeout() {
        return getIntValue("insight.semantic.socket.timeout", 10000);
    }

    public int getConnectionRequestTimeout() {
        return getIntValue("insight.semantic.connection.request.timeout", 8000);
    }

    public int getConnectionMaxTotal() {
        return getIntValue("insight.semantic.connection.max-total", 200);
    }

    public boolean getConnectionManagerShared() {
        return getBooleanValue("insight.semantic.connection.manager-shared", true);
    }

    public boolean isEnableSyncSegment() {
        return getBooleanValue("insight.semantic.segment.sync.enable", true);
    }

    public boolean isEnableQueryApi() {
        return getBooleanValue("insight.mdx.query-api.enable", false);
    }

    public boolean isGrantVisibilityToCreator() {
        return getBooleanValue("insight.mdx.grant-visibility-to-creator", true);
    }

    public String getDatabaseType() {
        return getOptional("insight.database.type", "mysql");
    }

    public String getDatabaseIp() {
        return getOptional("insight.database.ip", "localhost");
    }

    public String getDatabasePort() {
        return getOptional("insight.database.port", "3306");
    }

    public String getDatabaseName() {
        return getOptional("insight.database.name", "mdx");
    }

    public String getPostgresqlSchema() {
        return getOptional("insight.database.postgres-schema", "public");
    }

    public int getMondrianServerSize() {
        return getIntValue("insight.mdx.mondrian.server.maxSize", 50);
    }

    public long getQueryTimeout() {
        return getLongValue("insight.mdx.mondrian.rolap.queryTimeout", 300L);
    }

    public int getLogHttpDebugTime() {
        return getIntValue("insight.semantic.log.http.debug", 0);
    }

    public int getLogHttpInfoTime() {
        return getIntValue("insight.semantic.log.http.info", 50);
    }

    public int getLogHttpWarnTime() {
        return getIntValue("insight.semantic.log.http.warn", 1000);
    }

    public int getLogHttpErrorTime() {
        return getIntValue("insight.semantic.log.http.error", 10000);
    }

    public String getSessionName() {
        boolean optimize = getBooleanValue("insight.semantic.cookie-optimize", true);
        if (isConvertorMock() || !optimize) {
            return "mdx_session";
        }

        String databaseType = getDatabaseType();
        String sessionBase;
        if ("pg".equalsIgnoreCase(databaseType)) {
            sessionBase = databaseType + getDatabaseIp() + getDatabasePort() + getDatabaseName() + getPostgresqlSchema();
        } else {
            sessionBase = databaseType + getDatabaseIp() + getDatabasePort() + getDatabaseName();
        }

        return "mdx_session_" + Base64.encodeBase64String(sessionBase.getBytes(StandardCharsets.UTF_8))
                .replace("=", "")
                .replace("/", "")
                .replace("+", "")
                .trim();
    }

    public boolean isGetHeapDump() {
        return getBooleanValue("insight.semantic.get.heap.dump", false);
    }

    /**
     * 是否禁止异步访问 http
     */
    public boolean isDisableAsyncHttpCall() {
        return getBooleanValue("insight.semantic.http.async.disable", false);
    }

    public boolean isEnableCheckReject() {
        return getBooleanValue("insight.semantic.reject.enable", false);
    }

    public String getRejectRedirectAddress() {
        return getOptional("insight.semantic.reject.redirect-address", "");
    }

    public long getDimensionHighCardinalitySize() {
        return getLongValue("insight.semantic.reject.dimension.cardinality", 100000);
    }

    public String getContextPath() {
        return Utils.endWithSlash(getOptional("insight.semantic.context-path", "/"));
    }

    public String getMdxServletPath() {
        return Utils.endWithSlash(getOptional("insight.mdx.servlet-path", "/mdx/xmla/"));
    }

    public String getMdxGatewayPath() {
        return Utils.endWithSlash(getOptional("insight.mdx.gateway-path", "/mdx/xmla_server/"));
    }

    public String getDiscoverCatalogUrl(String project) {
        return getMdxProtocol() + "://" + getMdxHost() + ":" + getMdxPort() + getContextPath()
                + Utils.startWithoutSlash(getMdxServletPath()) + project;
    }

    public String getClearCacheUrl(String project) {
        return getDiscoverCatalogUrl(project) + "/clearCache";
    }

    public boolean isZookeeperEnable() {
        return getBooleanValue("insight.semantic.zookeeper.enable", false);
    }

    public String getZookeeperAddress() {
        return getOptional("insight.semantic.zookeeper.address", "localhost:2181");
    }

    public String getZookeeperNodePath() {
        return getOptional("insight.semantic.zookeeper.node.path", "/mdx");
    }

    public int getZookeeperTimeout() {
        return getIntValue("insight.semantic.zookeeper.session.timeout", 30000);
    }

    public boolean isKylinAutoHeaderEnable() {
        return getBooleanValue("insight.kylin.auto.header.enable", true);
    }

    public String isKylinOnlyNormalDimEnable() {
        return getOptional("insight.kylin.only.normal.dim.enable", "false");
    }

    public boolean isEnableQueryWithExecuteAs() {
        return getBooleanValue("insight.semantic.query-with-execute-as", false);
    }

    public boolean isEnableCompressResult() {
        return getBooleanValue("insight.semantic.compress-result", true);
    }

    public boolean isEnableCompactResult() {
        return getBooleanValue("insight.semantic.compact-result", false);
    }

    public boolean isEnableAAD() {
        return getBooleanValue(ConfigConstants.IS_ENABLE_AAD, false);
    }

    public boolean isEnableAADInternalRedirect() {
        return getBooleanValue(ConfigConstants.IS_AAD_INTERNAL_REDIRECT, false);
    }

    public String getAADAuthenticationCallbackUrl() {
        return getOptional(ConfigConstants.AAD_AUTHENTICATION_CALLBACK_URL, "");
    }

    public String getAADAuthenticationCodeUrl() {
        return getOptional(ConfigConstants.AAD_AUTHENTICATION_CODE_URL, "");
    }

    public String getAADLoginUrl() {
        return getContextPath() + getOptional(ConfigConstants.AAD_LOGIN_URL, "oauth2/authorization/azure");
    }

    public String getAADLogoutUrl() {
        return getOptional(ConfigConstants.AAD_LOGOUT_URL, getAADServerUrl() + "common/oauth2/logout");
    }

    public String getAADServerUrl() {
        return getOptional(ConfigConstants.AAD_SERVER_URL, "https://login.microsoftonline.com/");
    }

    public String getAADTenantId() {
        return getOptional("azure.activedirectory.tenant-id", "");
    }

    public String getAADClientId() {
        return getOptional("azure.activedirectory.client-id", "");
    }

    public String getAADClientSecret() {
        return getOptional("azure.activedirectory.client-secret", "");
    }

    public String getAADRedirectUriTemplate() {
        return getOptional(ConfigConstants.REDIRECT_URI_TEMPLATE, "");
    }

    /**
     * Whether to enable support for HIERARCHY_VISIBILITY and MEASURE_VISIBILITY restrictions.
     */
    public boolean isRowsetVisibilitiesSupported() {
        return getBooleanValue("insight.mdx.xmla.support-rowset-visibilities", true);
    }

    public String getProjectRowsetVisibilities() {
        return getOptional("insight.mdx.xmla.project-rowset-visibilities", "*");
    }

    /**
     * Whether to force return default-valued FormatString tags in XML/A responses.
     */
    public boolean isFormatStringDefaultValueForceReturned() {
        return getBooleanValue("insight.mdx.xmla.format-string.default.force-returned", false);
    }

    /**
     * The default value of <i>FormatString</i> tag. The default value of this tag is an empty string since 1.3.1,
     * was "regular" before 1.3.1.
     */
    public String formatStringDefaultValue() {
        return getOptional("insight.mdx.xmla.format-string.default", "");
    }

    public String getUploadFileMaxSize() {
        return getOptional("insight.mdx.upload.max-file-size", "20MB");
    }

    /**
     * Whether to log jstack and jmap info
     */
    public boolean isLogJavaInfoEnable() {
        return getBooleanValue("insight.semantic.log.java.info.enable", true);
    }

    public int getMetaSyncInterval() {
        return getIntValue("insight.semantic.meta-sync.interval", 30);
    }

    public String getExportFileMaxSize() {
        return getOptional("insight.dataset.export-file-limit", "10MB");
    }

    /**
     * Micrometer integration Prometheus
     */
    public String getManagementWebBasePath(){
        return getOptional("insight.management.endpoints.web.base-path", "/actuator");
    }

    public boolean isPostgre() {
        return "pg".equalsIgnoreCase(getDatabaseType()) || "postgresql".equalsIgnoreCase(getDatabaseType()) || "postgres".equalsIgnoreCase(getDatabaseType());
    }

    public int getMdxQueryJobRunPeriodTime() {
        return getIntValue("insight.semantic.startup.query-house-keep.period", 3600);
    }

    public String getSecretKey() {
        return getOptional("insight.semantic.secret-key", "3500d18495a54c54b9a3d56641a8a521");
    }
}
