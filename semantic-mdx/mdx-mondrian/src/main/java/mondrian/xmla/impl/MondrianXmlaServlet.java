/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2011-2012 Pentaho
// All Rights Reserved.
*/
package mondrian.xmla.impl;

import mondrian.olap.MondrianServer;
import mondrian.olap.Util;
import mondrian.server.RepositoryContentFinder;
import mondrian.server.UrlRepositoryContentFinder;
import mondrian.spi.CatalogLocator;
import mondrian.spi.impl.ServletContextCatalogLocator;
import mondrian.xmla.context.ConnectionFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Extension to {@link mondrian.xmla.XmlaServlet} that instantiates a
 * Mondrian engine.
 *
 * @author jhyde
 */
public class MondrianXmlaServlet extends DefaultXmlaServlet {
    public static final String DEFAULT_DATASOURCE_FILE = "datasources.xml";

    protected MondrianServer server;

    @Override
    protected ConnectionFactory createConnectionFactory(
        ServletConfig servletConfig) {
        if (server == null) {
            // A derived class can alter how the catalog locator object is
            // created.
            CatalogLocator catalogLocator = makeCatalogLocator(servletConfig);

            String dataSources = makeDataSourcesUrl(servletConfig);
            RepositoryContentFinder contentFinder =
                makeContentFinder(dataSources);
            server =
                MondrianServer.createWithRepository(
                    contentFinder, catalogLocator);
        }
        return (ConnectionFactory) server;
    }

    @Override
    public void destroy() {
        super.destroy();
        if (server != null) {
            server.shutdown();
            server = null;
        }
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
    protected String makeDataSourcesUrl(ServletConfig servletConfig)
    {
        String paramValue = servletConfig.getInitParameter(PARAM_DATASOURCES_CONFIG);

        URL dataSourcesConfigUrl;
        try {
            if (paramValue == null) {
                // fallback to default
                String defaultDS = "WEB-INF/" + DEFAULT_DATASOURCE_FILE;
                ServletContext servletContext = servletConfig.getServletContext();
                File realPath = new File(servletContext.getRealPath(defaultDS));
                if (realPath.exists()) {
                    // only if it exists
                    dataSourcesConfigUrl = realPath.toURL();
                    return dataSourcesConfigUrl.toString();
                } else {
                    return null;
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

// End MondrianXmlaServlet.java
