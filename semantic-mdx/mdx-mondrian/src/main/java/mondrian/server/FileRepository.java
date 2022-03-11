/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2010-2012 Pentaho
// All Rights Reserved.
*/
package mondrian.server;

import mondrian.olap.DriverManager;
import mondrian.olap.MondrianServer;
import mondrian.olap.Util;
import mondrian.olap4j.MondrianOlap4jDriver;
import mondrian.rolap.RolapConnection;
import mondrian.rolap.RolapConnectionProperties;
import mondrian.rolap.RolapSchema;
import mondrian.spi.CatalogLocator;
import mondrian.tui.XmlaSupport;
import mondrian.util.ByteString;
import mondrian.util.ClassResolver;
import mondrian.util.LockBox;
import mondrian.xmla.DataSourcesConfig;
import mondrian.xmla.XmlaRequestContext;
import org.apache.log4j.Logger;
import org.olap4j.OlapConnection;
import org.olap4j.OlapException;
import org.olap4j.OlapWrapper;
import org.olap4j.impl.Olap4jUtil;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link mondrian.server.Repository} that reads
 * from a {@code datasources.xml} file.
 *
 * <p>Note that for legacy reasons, the datasources.xml file's root
 * element is called DataSource whereas the olap4j standard calls them
 * Databases. This is why those two concepts are linked, as in
 * {@link FileRepository#getDatabaseNames(RolapConnection)} for example.
 *
 * @author Julian Hyde, Luc Boudreau
 */
public class FileRepository implements Repository {
    private final Object ServerInfoLock = new Object();
    private final RepositoryContentFinder repositoryContentFinder;

    private static final Logger LOGGER = Logger.getLogger(MondrianServer.class);

    private ServerInfo serverInfo;

    private final CatalogLocator locator;

    private static final ConcurrentHashMap<String, CatalogInfo> catalogInfoMap = new ConcurrentHashMap<>();

    public FileRepository(RepositoryContentFinder repositoryContentFinder, CatalogLocator locator) {
        this.repositoryContentFinder = repositoryContentFinder;
        this.locator = locator;
        assert repositoryContentFinder != null;
    }

    @Override
    public List<Map<String, Object>> getDatabases(RolapConnection connection) {
        final List<Map<String, Object>> propsList = new ArrayList<>();
        for (DatabaseInfo dsInfo : getServerInfo().datasourceMap.values()) {
            propsList.add(dsInfo.properties);
        }
        return propsList;
    }

    @Override
    public OlapConnection getConnection(MondrianServer server, String databaseName, String catalogName,
                                        String roleName, Properties props)
            throws SQLException {
        final ServerInfo serverInfo = getServerInfo();
        final DatabaseInfo datasourceInfo;
        if (databaseName == null || XmlaRequestContext.getContext().delegateUser != null) {
            if (serverInfo.datasourceMap.size() == 0) {
                throw new OlapException("No databases configured on this server");
            }
            datasourceInfo = serverInfo.datasourceMap.values().iterator().next();
        } else {
            datasourceInfo = serverInfo.datasourceMap.get(databaseName);
        }
        if (datasourceInfo == null) {
            throw Util.newError("Unknown database '" + databaseName + "'");
        }

        final CatalogInfo catalogInfo;
        if (catalogName == null) {
            if (datasourceInfo.catalogMap.size() == 0) {
                throw new OlapException("No available dataset in database " + datasourceInfo.name);
            }
            catalogInfo = datasourceInfo.catalogMap.values().iterator().next();
        } else {
            catalogInfo = datasourceInfo.catalogMap.get(catalogName);
        }
        if (catalogInfo == null) {
            throw Util.newError("Unknown catalog '" + catalogName + "'");
        }
        String connectString = catalogInfo.olap4jConnectString;

        // Save the server for the duration of the call to 'getConnection'.
        final LockBox.Entry entry = MondrianServerRegistry.INSTANCE.lockBox.register(server);

        final Properties properties = new Properties();
        properties.setProperty(RolapConnectionProperties.Instance.name(), entry.getMoniker());
        if (roleName != null) {
            properties.setProperty(RolapConnectionProperties.Role.name(), roleName);
        }
        properties.putAll(props);
        // Make sure we load the Mondrian driver into the ClassLoader.
        return createOlapConnection(connectString, properties);
    }

    @Override
    public void shutdown() {
        repositoryContentFinder.shutdown();
    }

    private ServerInfo getServerInfo() {
        synchronized (ServerInfoLock) {
            final String content = repositoryContentFinder.getContent();
            if (content == null) {
                throw Util.newError("Can't read content from repository.");
            }
            ByteString datasourceMD5 = new ByteString(Util.digestMd5(content));
            if (this.serverInfo != null) {
                if (this.serverInfo.datasourceMD5.equals(datasourceMD5)) {
                    return this.serverInfo;
                }
            }
            DataSourcesConfig.DataSources xmlDataSources = XmlaSupport.parseDataSources(content, LOGGER);
            ServerInfo serverInfo = new ServerInfo();

            for (DataSourcesConfig.DataSource xmlDataSource : xmlDataSources.dataSources) {
                final Map<String, Object> dsPropsMap =
                        Olap4jUtil.mapOf("DataSourceName", xmlDataSource.getDataSourceName(),
                                "DataSourceDescription", xmlDataSource.getDataSourceDescription(),
                                "URL", xmlDataSource.getURL(),
                                "DataSourceInfo", xmlDataSource.getDataSourceName(),
                                "ProviderName", xmlDataSource.getProviderName(),
                                "ProviderType", xmlDataSource.providerType,
                                "AuthenticationMode", xmlDataSource.authenticationMode);
                final DatabaseInfo databaseInfo = new DatabaseInfo(xmlDataSource.name, dsPropsMap);
                serverInfo.datasourceMap.put(xmlDataSource.name, databaseInfo);

                String user = null;
                String project = null;
                String userName = XmlaRequestContext.getContext().getQueryUser();
                int idxOfDbname = databaseInfo.name.indexOf(userName) + userName.length();
                if (idxOfDbname != -1 && idxOfDbname <= databaseInfo.name.length()) {
                    user = databaseInfo.name.substring(0, idxOfDbname);
                    project = databaseInfo.name.substring(idxOfDbname + 1);
                }

                for (DataSourcesConfig.Catalog xmlCatalog : xmlDataSource.catalogs.catalogs) {
                    if (databaseInfo.catalogMap.containsKey(xmlCatalog.name)) {
                        throw Util.newError("more than one DataSource object has name '" + xmlCatalog.name + "'");
                    }
                    String connectString = xmlCatalog.dataSourceInfo != null ?
                            xmlCatalog.dataSourceInfo : xmlDataSource.dataSourceInfo;
                    // Check if the catalog is part of the connect
                    // string. If not, add it.
                    final Util.PropertyList connectProperties = Util.parseConnectString(connectString);
                    if (connectProperties.get(RolapConnectionProperties.Catalog.name()) == null) {
                        connectString += ";" + RolapConnectionProperties.Catalog.name() + "=" + xmlCatalog.definition;
                    }
                    final CatalogInfo catalogInfo = new CatalogInfo(xmlCatalog.name, connectString, locator);
                    catalogInfoMap.put(user.toUpperCase() + "_" + project + "_" + xmlCatalog.name, catalogInfo);
                    try {
                        checkCatalogAvailability(catalogInfo);
                        databaseInfo.catalogMap.put(xmlCatalog.name, catalogInfo);
                    } catch (Throwable e) {
                        databaseInfo.unavailableCatalogList.add(catalogInfo.name);
                        StringBuilder errorBuff = new StringBuilder();
                        errorBuff.append("unable to connect to dataset ").append(catalogInfo.name)
                                .append(" in project ").append(project).append(", user is ").append(user);
                        LOGGER.error(errorBuff, e);
                    }
                }
                if (databaseInfo.unavailableCatalogList.size() > 0) {
                    StringBuilder infoBuff = new StringBuilder();
                    infoBuff.append("there are ").append(databaseInfo.catalogMap.size())
                            .append(" success dataset, and ").append(databaseInfo.unavailableCatalogList.size())
                            .append(" failed dataset in project ").append(project).append(".");
                    infoBuff.append(" Failed dataset list: ");
                    for (String catalog : databaseInfo.unavailableCatalogList) {
                        infoBuff.append(catalog);
                        infoBuff.append(",");
                    }
                    infoBuff.deleteCharAt(infoBuff.length() - 1);

                    LOGGER.info(infoBuff);
                }
            }
            serverInfo.datasourceMD5 = datasourceMD5;
            this.serverInfo = serverInfo;
            return serverInfo;
        }
    }

    public void checkDataset(String datasetId) throws Exception {
        try {
            CatalogInfo catalogInfo = catalogInfoMap.get(datasetId);
            if (catalogInfo == null) {
                return;
            }
            checkCatalogAvailability(catalogInfo);
        } catch (Throwable e) {
            StringBuilder errorBuff = new StringBuilder();
            errorBuff.append("unable to connect to dataset ").append(datasetId);
            LOGGER.error(errorBuff, e);
            throw e;
        }
    }

    private void checkCatalogAvailability(CatalogInfo catalogInfo) throws SQLException {
        OlapConnection connection = getConnectionWithCatalog(catalogInfo);
        assert connection.getCatalog() != null && connection.getCatalog().length() != 0;
    }

    private OlapConnection getConnectionWithCatalog(CatalogInfo catalogInfo) throws SQLException {
        String connectString = catalogInfo.olap4jConnectString;
        // replace catalog url with absolute path
        int idxOfStartCatalog = connectString.indexOf("Catalog");
        int idxOfEndCatalog = connectString.indexOf(";", idxOfStartCatalog);
        if (idxOfEndCatalog == -1) {
            idxOfEndCatalog = connectString.length();
        }
        String catalogStr = connectString.substring(idxOfStartCatalog, idxOfEndCatalog).split("=")[1];
        String catalogStrWithAbsPath = locator.locate(catalogStr);
        connectString = connectString.replace(catalogStr, catalogStrWithAbsPath);

        final Properties properties = new Properties();
        // Make sure we load the Mondrian driver into
        // the ClassLoader.
        return createOlapConnection(connectString, properties);
    }

    public OlapConnection createOlapConnection(String connectString, Properties properties) throws SQLException {
        try {
            ClassResolver.INSTANCE.forName(MondrianOlap4jDriver.class.getName(), true);
        } catch (ClassNotFoundException e) {
            throw new OlapException("Cannot find mondrian olap4j driver.");
        }
        // Now create the connection
        final java.sql.Connection connection =
                java.sql.DriverManager.getConnection(connectString, properties);
        return ((OlapWrapper) connection).unwrap(OlapConnection.class);
    }

    @Override
    public List<String> getCatalogNames(RolapConnection connection, String databaseName) {
        return new ArrayList<>(getServerInfo().datasourceMap.get(databaseName).catalogMap.keySet());
    }

    @Override
    public List<String> getDatabaseNames(RolapConnection connection) {
        return new ArrayList<>(getServerInfo().datasourceMap.keySet());
    }

    @Override
    public Map<String, RolapSchema> getRolapSchemas(
            RolapConnection connection,
            String databaseName,
            String catalogName) {
        final RolapSchema schema = getServerInfo()
                .datasourceMap.get(databaseName)
                .catalogMap.get(catalogName)
                .getRolapSchema();
        return Collections.singletonMap(schema.getName(), schema);
    }

    private static class ServerInfo {
        private Map<String, DatabaseInfo> datasourceMap = new ConcurrentHashMap<>();
        private ByteString datasourceMD5;
    }

    private static class DatabaseInfo {
        private final String name;
        private final Map<String, Object> properties;
        private Map<String, CatalogInfo> catalogMap = new HashMap<>();
        private List<String> unavailableCatalogList = new ArrayList<>();

        DatabaseInfo(String name, Map<String, Object> properties) {
            this.name = name;
            this.properties = properties;
        }
    }

    private static class CatalogInfo {
        private final String connectString;
        private RolapSchema rolapSchema; // populated on demand
        private final String olap4jConnectString;
        private final CatalogLocator locator;
        private final String name;

        CatalogInfo(
                String name,
                String connectString,
                CatalogLocator locator) {
            this.name = name;
            this.connectString = connectString;
            this.locator = locator;
            this.olap4jConnectString = connectString.startsWith("jdbc:")
                    ? connectString : "jdbc:mondrian:" + connectString;
        }

        private RolapSchema getRolapSchema() {
            if (rolapSchema == null) {
                RolapConnection rolapConnection = null;
                try {
                    rolapConnection = (RolapConnection) DriverManager.getConnection(connectString, this.locator);
                    rolapSchema = rolapConnection.getSchema();
                } finally {
                    if (rolapConnection != null) {
                        rolapConnection.close();
                    }
                }
            }
            return rolapSchema;
        }
    }
}

// End FileRepository.java
