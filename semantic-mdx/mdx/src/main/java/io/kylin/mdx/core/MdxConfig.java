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


package io.kylin.mdx.core;

import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.util.NetworkUtils;
import org.apache.commons.lang3.StringUtils;

import static io.kylin.mdx.insight.common.constants.SystemConstants.LOCAL_HOST;
import static io.kylin.mdx.insight.common.constants.SystemConstants.LOCAL_IP;

public class MdxConfig {

    private static final MdxConfig MDX_CONFIG = new MdxConfig();

    private final SemanticConfig config;

    public static MdxConfig getInstance() {
        return MDX_CONFIG;
    }

    private MdxConfig() {
        this.config = SemanticConfig.getInstance();
    }

    public String getKylinHost() {
        return config.getKylinHost();
    }

    public String getKylinPort() {
        return config.getKylinPort();
    }

    public String getKeProtocol() {
        return config.getKylinProtocol();
    }

    public String getMdxHost() {
        return config.getMdxHost();
    }

    public String getMdxPort() {
        return config.getMdxPort();
    }

    public String getCluster() {
        String hostIp = config.getMdxHost();
        if (StringUtils.isBlank(hostIp) || LOCAL_HOST.equals(hostIp) || LOCAL_IP.equals(hostIp)) {
            hostIp = NetworkUtils.getLocalIP();
        }
        return hostIp + ":" + config.getMdxPort();
    }

    public boolean isOptimizeMdxEnable() {
        return config.getBooleanValue("insight.mdx.optimize-enabled", true);
    }

    public boolean isNeedCalculateTotal() {
        return config.getBooleanValue("insight.mdx.calculate-total-need", true);
    }

    public boolean isEnableSortSqlResult() {
        return config.getBooleanValue("insight.mdx.sql.orderby.enable", true);
    }

    public boolean isCreateSchemaFromDataSet() {
        return config.getBooleanValue("insight.mdx.schema.create-from-dataset", true);
    }

    public boolean isUpperAdminName() {
        return config.getBooleanValue("insight.mdx.upper-admin-name", true);
    }

    public boolean isUpperUserName() {
        return config.getBooleanValue("insight.mdx.upper-user-name", true);
    }

    public boolean isEnableSqlEngineHint() {
        return config.getBooleanValue("insight.mdx.sql.calcite-engine-hint.enable", false);
    }

    // for develop, 不建议在生产环境使用

    public boolean isStressTestMode() {
        return config.getBooleanValue("insight.mdx.mode.is-stress-test", false);
    }

    public boolean isDisableRefreshSchema() {
        return config.getBooleanValue("insight.mdx.schema.refresh.disable", false);
    }

    // for gateway, MDX Server 不使用

    public boolean isJsonAutoArray() {
        return config.getBooleanValue("insight.mdx.gateway.json.auto-array", true);
    }

    public boolean isEnableJsonTransfer() {
        return config.getBooleanValue("insight.mdx.gateway.transfer.json.enable", false);
    }

    public int getJsonTransferThreshold() {
        return config.getIntValue("insight.mdx.gateway.transfer.json.threshold", 10000);
    }

}
