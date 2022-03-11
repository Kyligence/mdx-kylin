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
import io.kylin.mdx.insight.common.util.DatasourceUtils;
import io.kylin.mdx.ErrorCode;
import mondrian.olap.MondrianServer;
import mondrian.olap.Util;
import mondrian.server.RepositoryContentFinder;
import mondrian.server.UrlRepositoryContentFinder;
import mondrian.spi.CatalogLocator;
import mondrian.spi.impl.ServletContextCatalogLocator;
import mondrian.xmla.XmlaServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class MondrianServerBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MondrianServerBuilder.class);

    private final ServletConfig servletConfig;

    private String username;

    private String project;

    private String delegate;

    public MondrianServerBuilder(ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
    }

    public MondrianServerBuilder username(String username) {
        this.username = username;
        return this;
    }

    public MondrianServerBuilder project(String project) {
        this.project = project;
        return this;
    }

    public MondrianServerBuilder delegate(String delegate) {
        this.delegate = delegate;
        return this;
    }

    public MondrianServer build() {
        CatalogLocator catalogLocator = makeCatalogLocator(servletConfig);
        String dataSources = makeDataSourcesUrl(servletConfig, username, project, delegate);
        RepositoryContentFinder contentFinder = makeContentFinder(dataSources);
        return MondrianServer.createWithRepository(contentFinder, catalogLocator);
    }

    /**
     * Creates a callback for reading the repository. Derived classes may
     * override.
     *
     * @param dataSources Data sources
     * @return Callback for reading repository
     */
    protected RepositoryContentFinder makeContentFinder(String dataSources) {
        return new UrlRepositoryContentFinder(dataSources);
    }

    /**
     * Make catalog locator.  Derived classes can roll their own.
     *
     * @param servletConfig Servlet configuration info
     * @return Catalog locator
     */
    protected CatalogLocator makeCatalogLocator(ServletConfig servletConfig) {
        ServletContext servletContext = servletConfig.getServletContext();
        return new ServletContextCatalogLocator(servletContext);
    }

    /**
     * Creates the URL where the data sources file is to be found.
     *
     * <p>Derived classes can roll their own.
     *
     * <p>If there is an initParameter called "DataSourcesConfig"
     * get its value, replace any "${key}" content with "value" where
     * "key/value" are System properties, and try to create a URL
     * instance out of it. If that fails, then assume its a
     * real filepath and if the file exists then create a URL from it
     * (but only if the file exists).
     * If there is no initParameter with that name, then attempt to
     * find the file called "datasources.xml"  under "WEB-INF/"
     * and if it exists, use it.
     *
     * @param servletConfig Servlet config
     * @return URL where data sources are to be found
     */
    protected String makeDataSourcesUrl(ServletConfig servletConfig, String currentUser, String project, String delegate) {
        String paramValue = servletConfig.getInitParameter(XmlaServlet.PARAM_DATASOURCES_CONFIG);

        // if false, then do not throw exception if the file/url can not be found
        boolean optional = XmlaServlet.getBooleanInitParameter(servletConfig, XmlaServlet.PARAM_OPTIONAL_DATASOURCE_CONFIG);

        URL dataSourcesConfigUrl = null;
        try {
            if (paramValue == null) {
                // fallback to default
                String defaultDS = DatasourceUtils.getDatasourcePath("WEB-INF", currentUser, project, delegate);
                ServletContext servletContext = servletConfig.getServletContext();
                File realPath = new File(servletContext.getRealPath(defaultDS));
                if (realPath.exists()) {
                    // only if it exists
                    dataSourcesConfigUrl = realPath.toURL();
                    return dataSourcesConfigUrl.toString();
                } else {
                    throw new SemanticException(ErrorCode.DATASOURCE_FILE_NOT_FOUND, defaultDS);
                }
            } else if (paramValue.startsWith("inline:")) {
                return paramValue;
            } else {
                throw Util.newError("invalid datasource config : '" + paramValue + "'");
            }
        } catch (MalformedURLException mue) {
            throw Util.newError(mue, "invalid URL path '" + paramValue + "'");
        }
    }

}
