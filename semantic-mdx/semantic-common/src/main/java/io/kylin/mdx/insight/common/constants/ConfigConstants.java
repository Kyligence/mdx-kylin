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


package io.kylin.mdx.insight.common.constants;

public interface ConfigConstants {

    // KYLIN 配置信息

    String KYLIN_HOST = "insight.kylin.host";

    String KYLIN_PORT = "insight.kylin.port";

    String KYLIN_USERNAME = "insight.kylin.username";

    String KYLIN_PASSWORD = "insight.kylin.password";

    // 数据库配置

    String DATABASE_PASSWORD = "insight.database.password";

    // Dataset 配置信息

    String DATASET_ALLOW_ACCESS_BY_DEFAULT = "insight.dataset.allow-access-by-default";

    String DATASET_ALLOW_MAX_SIZE_EXPORT_FILE = "insight.dataset.export-file-limit";

    // AAD settings information

    String IS_ENABLE_AAD = "insight.semantic.enable.aad";

    String IS_AAD_INTERNAL_REDIRECT = "insight.semantic.aad.internal.redirect.enable";

    String AAD_SERVER_URL = "aad.server.url";

    String AAD_LOGIN_URL = "aad.login.url";

    String AAD_LOGOUT_URL = "aad.logout.url";

    String AAD_AUTHENTICATION_CALLBACK_URL = "aad.login.callback-url";

    String AAD_AUTHENTICATION_CODE_URL = "aad.login.code-url";

    String TENANT_ID = "azure.activedirectory.tenant-id";

    String CLIENT_ID = "azure.activedirectory.client-id";

    String CLIENT_SECRET = "azure.activedirectory.client-secret";

    String REDIRECT_URI_TEMPLATE = "azure.activedirectory.redirect-uri-template";


}
