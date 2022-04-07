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

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.AESWithECBEncryptor;
import io.kylin.mdx.insight.common.util.JacksonSerDeUtils;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.core.MdxConfig;
import io.kylin.mdx.core.datasource.MdCatalog;
import io.kylin.mdx.core.datasource.MdDatasource;
import io.kylin.mdx.core.mondrian.MdnSchema;
import io.kylin.mdx.core.mondrian.MdnSchemaSet;
import io.kylin.mdx.core.service.ModelManager;
import mondrian.olap.Util;
import org.apache.commons.collections.CollectionUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.kylin.mdx.insight.common.util.DatasourceUtils.getDatasourceName;
import static io.kylin.mdx.insight.common.util.DatasourceUtils.getDatasourcePath;
import static io.kylin.mdx.insight.common.util.DatasourceUtils.getSchemaPath;

/**
 * XMLA 数据源: datasource -> catalogs
 * 普通模式:
 * #  username + project + "datasources.xml"
 * #  ->  username + project + catalog + ".xml"
 * 委任模式:
 * #  delegate + username + project + "datasources.xml"
 * #  ->  username + project + catalog + ".xml"
 * 两种模式的 schema(catalog) 名字会出现重复
 */
public class XmlaDatasource {

    private static final String SCHEMA_DIR = "/WEB-INF/schema/";

    /**
     * 保存 username(delegate) + project -> last modified time
     */
    private static final Map<String, Long> ACL_UPDATES = new ConcurrentHashMap<>();

    private final MdxConfig config = MdxConfig.getInstance();

    private final String rootPath;

    private final String username;

    private final String password;

    private final String delegate;

    private final String project;

    private final boolean force;

    public XmlaDatasource(String rootPath, String username, String password, String project,
                          String delegate, boolean forceRefresh) {
        this.rootPath = rootPath;
        this.username = username;
        this.password = password;
        this.project = project;
        this.delegate = delegate;
        this.force = forceRefresh;
        createSchemaDir();
    }

    public void initDatasource() {
        initDatasource(loadMdnSchemas());
    }

    public void initDatasource(MdnSchemaSet mdnSchemas) {
        if (CollectionUtils.isNotEmpty(mdnSchemas.getMdnSchemas())) {
            createDatasource(mdnSchemas);
            createMdnSchemas(mdnSchemas);
        }
    }

    private MdnSchemaSet loadMdnSchemas() {
        ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .user(username)
                .password(password)
                .project(project)
                .delegate(delegate)
                .build();
        if (MdxConfig.getInstance().isCreateSchemaFromDataSet()) {
            return new ModelManager().buildMondrianSchemaFromDataSet(connectionInfo);
        } else {
            return new ModelManager().buildMondrianSchemaByKylin(connectionInfo);
        }
    }

    private void createSchemaDir() {
        File file = new File(getSchemaDir());
        if (!file.exists()) {
            synchronized (XmlaDatasource.class) {
                if (!file.exists() && !file.mkdirs()) {
                    throw new SemanticException("Can't create directory:" + file.getAbsolutePath());
                }
            }
        }
    }

    private void createMdnSchemas(MdnSchemaSet schemaSet) {
        for (MdnSchema schema : schemaSet.getMdnSchemas()) {
            String schemaFilePath = getSchemaPath(getSchemaDir(), username, project, schema.getName(), delegate);

            String content = JacksonSerDeUtils.writeXmlAsString(schema);
            if (content == null) {
                throw new SemanticException("Can't serialize schema:" + schema.getName());
            }

            byte[] schemaHashCode = Util.digestMd5(content);
            XmlaDatasourceManager.getInstance().checkEqualsAndWrite(project, schemaFilePath, schemaHashCode,
                    content, "Can't create schema:%s", schema.getName());
        }
    }

    private void createDatasource(MdnSchemaSet schemaSet) {
        String datasourceName = getDatasourceName(username, project, delegate);
        String datasourceInfo = buildDatasourceInfo();
        List<MdCatalog> catalogs = getMdCatalogs(schemaSet.getMdnSchemas());
        long lastModified = getLastModified(schemaSet);
        MdDatasource mdDatasource = new MdDatasource(datasourceName, datasourceInfo, catalogs, lastModified);

        String datasourceFile = getDatasourcePath(getDatasourceDir(), username, project, delegate);
        String content = mdDatasource.toString();
        byte[] datasourceHashCode = Util.digestMd5(content);
        XmlaDatasourceManager.getInstance().checkEqualsAndWrite(project, datasourceFile, datasourceHashCode,
                content, "Can't create datasource:%s", datasourceName);
    }

    private String buildDatasourceInfo() {
        StringBuilder builder = new StringBuilder("Provider=mondrian;UseContentChecksum=true;Jdbc=jdbc:kylin://")
                .append(config.getKylinHost()).append(":").append(config.getKylinPort()).append("/").append(project)
                .append(";JdbcDrivers=org.apache.kylin.jdbc.Driver")
                .append(";JdbcUser=").append(username)
                .append(";JdbcPassword=").append(AESWithECBEncryptor.encrypt(password));
        if (delegate != null) {
            builder.append(";JdbcDelegate=").append(delegate);
        }
        if ("https".equals(config.getKeProtocol())) {
            builder.append(";ssl=true");
        }
        return builder.toString();
    }

    private List<MdCatalog> getMdCatalogs(List<MdnSchema> schemas) {
        List<MdCatalog> mdCatalogs = new LinkedList<>();
        for (MdnSchema schema : schemas) {
            String catalogName = schema.getName();
            String catalogPath = getSchemaPath(SCHEMA_DIR, username, project, catalogName, delegate);
            MdCatalog mdCatalog = new MdCatalog(catalogName, catalogPath);
            mdCatalogs.add(mdCatalog);
        }
        return mdCatalogs;
    }

    /**
     * 取 数据集中最大的更新时间 和 最后一次 force 时间戳
     *
     * @param schemaSet 数据集合
     * @return 时间戳
     */
    private synchronized long getLastModified(MdnSchemaSet schemaSet) {
        String key = (delegate != null ? delegate : username) + "_" + project;
        long lastModified = schemaSet.getLastModified() * 1000;
        if (force) {
            long currentTime = System.currentTimeMillis();
            ACL_UPDATES.put(key, currentTime);
            lastModified = Math.max(lastModified, currentTime);
        } else {
            if (ACL_UPDATES.containsKey(key)) {
                lastModified = Math.max(lastModified, ACL_UPDATES.get(key));
            }
        }
        return lastModified;
    }

    private String getDatasourceDir() {
        return this.rootPath + "/";
    }

    private String getSchemaDir() {
        return this.rootPath + "/schema/";
    }

}
