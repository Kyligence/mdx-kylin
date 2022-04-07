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


package io.kylin.mdx.core.datasource;

import java.util.List;

public class MdDatasource {

    private String dataSourceName;

    private String dataSourceInfo;

    private long lastModifiedTime;

    private List<MdCatalog> catalogs;

    public MdDatasource(String dataSourceName, String dataSourceInfo, List<MdCatalog> catalogs, long lastModifiedTime) {
        this.dataSourceName = dataSourceName;
        this.dataSourceInfo = dataSourceInfo;
        this.catalogs = catalogs;
        this.lastModifiedTime = lastModifiedTime;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<DataSources>\n");
        sb.append("\t<DataSource>\n");
        sb.append("\t\t<DataSourceName>").append(this.dataSourceName).append("</DataSourceName>\n");
        sb.append("\t\t<DataSourceDescription>Kylin xmla datasource, last modified time is ").append(lastModifiedTime).append("</DataSourceDescription>\n");
        sb.append("\t\t<URL>http://lcoalhost:8080/mdx/xmla</URL>\n");
        sb.append("\t\t<DataSourceInfo>").append(this.dataSourceInfo).append("</DataSourceInfo>\n");
        sb.append("\t\t<ProviderName>Mondrian</ProviderName>\n");
        sb.append("\t\t<ProviderType>MDP</ProviderType>\n");
        sb.append("\t\t<AuthenticationMode>Unauthenticated</AuthenticationMode>\n");
        sb.append("\t\t<Catalogs>\n");
        for (MdCatalog catalog : this.catalogs) {
            sb.append(catalog.toString());
            sb.append("\n");
        }
        sb.append("\t\t</Catalogs>\n");
        sb.append("\t</DataSource>\n");
        sb.append("</DataSources>");
        return sb.toString();
    }

}
