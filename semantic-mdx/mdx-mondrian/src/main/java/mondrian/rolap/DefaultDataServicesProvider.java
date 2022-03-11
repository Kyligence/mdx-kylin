/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2013-2013 Pentaho
// All Rights Reserved.
*/
package mondrian.rolap;

import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.util.AESWithECBEncryptor;
import mondrian.olap.MondrianProperties;
import mondrian.olap.Util;
import mondrian.resource.MondrianResource;
import mondrian.rolap.agg.SegmentCacheManager;
import mondrian.rolap.agg.SegmentLoader;
import mondrian.rolap.aggmatcher.JdbcSchema;
import mondrian.rolap.sql.TupleConstraint;
import mondrian.spi.DataServicesProvider;
import mondrian.spi.DataSourceResolver;
import mondrian.spi.impl.JndiDataSourceResolver;
import mondrian.util.ClassResolver;
import org.eigenbase.util.property.StringProperty;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Default implementation of DataServicesProvider
 */
public class DefaultDataServicesProvider implements DataServicesProvider {
    private static DataSourceResolver dataSourceResolver;
    private static JdbcSchema.Factory factory;
    private static final MondrianResource mres = MondrianResource.instance();

    @Override
    public MemberReader getMemberReader(RolapCubeHierarchy hierarchy) {
        return new SqlMemberSource(hierarchy);
    }

    @Override
    public SegmentLoader getSegmentLoader(SegmentCacheManager cacheMgr) {
        return new SegmentLoader(cacheMgr);
    }

    @Override
    public TupleReader getTupleReader(TupleConstraint constraint) {
        return new SqlTupleReader(constraint);
    }

    @Override
    public SqlDescendantsLeavesAggrNumValuesReader getValueReader(TupleConstraint constraint) {
        return new SqlDescendantsLeavesAggrNumValuesReader(constraint);
    }

    @Override
    public synchronized JdbcSchema.Factory getJdbcSchemaFactory() {
        if (factory != null) {
            return factory;
        }
        String className =
            MondrianProperties.instance().JdbcFactoryClass.get();
        if (className == null) {
            factory = new StdFactory();
        } else {
            try {
                Class<?> clz =
                    ClassResolver.INSTANCE.forName(className, true);
                factory = (JdbcSchema.Factory) clz.newInstance();
            } catch (ClassNotFoundException ex) {
                throw mres.BadJdbcFactoryClassName.ex(className);
            } catch (InstantiationException ex) {
                throw mres.BadJdbcFactoryInstantiation.ex(className);
            } catch (IllegalAccessException ex) {
                throw mres.BadJdbcFactoryAccess.ex(className);
            }
        }
        return factory;
    }

    protected static class StdFactory implements JdbcSchema.Factory {
        StdFactory() {
        }
        @Override
        public JdbcSchema loadDatabase(DataSource dataSource) {
            return new JdbcSchema(dataSource);
        }
    }

    public DefaultDataServicesProvider() {
    }

    /**
     * Creates a JDBC data source from the JDBC credentials contained within a
     * set of mondrian connection properties.
     *
     * <p>This method is package-level so that it can be called from the
     * RolapConnectionTest unit test.
     *
     * @param dataSource Anonymous data source from user, or null
     * @param connectInfo Mondrian connection properties
     * @param buf Into which method writes a description of the JDBC credentials
     * @return Data source
     */
    @Override
    public DataSource createDataSource(
            DataSource dataSource,
            Util.PropertyList connectInfo,
            StringBuilder buf) {
        assert buf != null;
        final String jdbcConnectString = connectInfo.get(RolapConnectionProperties.Jdbc.name());
        final String jdbcUser = connectInfo.get(RolapConnectionProperties.JdbcUser.name());
        final String jdbcPassword = connectInfo.get(RolapConnectionProperties.JdbcPassword.name());
        String decryptPwd = "";
        try {
            decryptPwd = AESWithECBEncryptor.decrypt(jdbcPassword);
        } catch (PwdDecryptException e) {
            //TODO
        }
        final String dataSourceName = connectInfo.get(RolapConnectionProperties.DataSource.name());
        final String ssl = connectInfo.get(RolapConnectionProperties.ssl.name());

        if (dataSource != null) {
            appendKeyValue(buf, "Anonymous data source", dataSource);
            appendKeyValue(buf, RolapConnectionProperties.JdbcUser.name(), jdbcUser);
            dataSource = new UserPasswordDataSource(dataSource, jdbcUser, decryptPwd);
            return dataSource;
        } else if (jdbcConnectString != null) {
            // Get connection through own pooling datasource
            appendKeyValue(buf, RolapConnectionProperties.Jdbc.name(), jdbcConnectString);
            appendKeyValue(buf, RolapConnectionProperties.JdbcUser.name(), jdbcUser);
            String jdbcDrivers = connectInfo.get(RolapConnectionProperties.JdbcDrivers.name());
            String jdbcDelegate = connectInfo.get(RolapConnectionProperties.JdbcDelegate.name());
            if (jdbcDrivers != null) {
                RolapUtil.loadDrivers(jdbcDrivers);
            }
            // TODO: MDX Service 不需要加载 mysql oracle 等依赖
//            final String jdbcDriversProp = MondrianProperties.instance().JdbcDrivers.get();
//            RolapUtil.loadDrivers(jdbcDriversProp);

            Properties jdbcProperties = RolapConnection.getJdbcProperties(connectInfo);

            if (jdbcUser != null) {
                jdbcProperties.put("user", jdbcUser);
            }
            jdbcProperties.put("password", decryptPwd);
            if (jdbcDelegate != null) {
                jdbcProperties.put("EXECUTE_AS_USER_ID", jdbcDelegate);
            }

            // MDX-Service 支持使用 http 连接 kylin
            if (ssl != null) {
                jdbcProperties.put("ssl", ssl);
            }

            jdbcProperties.put("timeZone", MondrianProperties.instance().JDBCTimeZone.get());

            // JDBC connections are dumb beasts, so we assume they're not
            // pooled. Therefore the default is true.
            final boolean poolNeeded = connectInfo.get(RolapConnectionProperties.PoolNeeded.name(), "true")
                    .equalsIgnoreCase("true");

            if (!poolNeeded) {
                // Connection is already pooled; don't pool it again.
                return new DriverManagerDataSource(jdbcConnectString, jdbcProperties);
            }

            return RolapConnectionPool.instance()
                    .getDriverManagerPoolingDataSource(
                            jdbcConnectString,
                            jdbcProperties,
                            jdbcConnectString.toLowerCase().contains("mysql"));

        } else if (dataSourceName != null) {
            appendKeyValue(buf, RolapConnectionProperties.DataSource.name(), dataSourceName);
            appendKeyValue(buf, RolapConnectionProperties.JdbcUser.name(), jdbcUser);

            // Data sources are fairly smart, so we assume they look after
            // their own pooling. Therefore the default is false.
            final boolean poolNeeded = connectInfo.get(RolapConnectionProperties.PoolNeeded.name(), "false")
                    .equalsIgnoreCase("true");

            // Get connection from datasource.
            DataSourceResolver dataSourceResolver = getDataSourceResolver();
            try {
                dataSource = dataSourceResolver.lookup(dataSourceName);
            } catch (Exception e) {
                throw Util.newInternal(e, "Error while looking up data source (" + dataSourceName + ")");
            }
            if (poolNeeded) {
                dataSource = RolapConnectionPool.instance()
                        .getDataSourcePoolingDataSource(dataSource, dataSourceName, jdbcUser, decryptPwd);
            } else {
                dataSource = new UserPasswordDataSource(dataSource, jdbcUser, decryptPwd);
            }
            return dataSource;
        } else {
            throw Util.newInternal(
                "Connect string '" + connectInfo
                + "' must contain either '" + RolapConnectionProperties.Jdbc
                + "' or '" + RolapConnectionProperties.DataSource + "'");
        }
    }

    /**
     * Returns the instance of the {@link mondrian.spi.DataSourceResolver}
     * plugin.
     *
     * @return data source resolver
     */
    private static synchronized DataSourceResolver getDataSourceResolver() {
        if (dataSourceResolver == null) {
            final StringProperty property =
                MondrianProperties.instance().DataSourceResolverClass;
            final String className =
                property.get(
                    JndiDataSourceResolver.class.getName());
            try {
                dataSourceResolver =
                    ClassResolver.INSTANCE.instantiateSafe(className);
            } catch (ClassCastException e) {
                throw Util.newInternal(
                    e,
                    "Plugin class specified by property "
                    + property.getPath()
                    + " must implement "
                    + DataSourceResolver.class.getName());
            }
        }
        return dataSourceResolver;
    }

    /**
     * Appends "key=value" to a buffer, if value is not null.
     *
     * @param buf Buffer
     * @param key Key
     * @param value Value
     */
    private static void appendKeyValue(
        StringBuilder buf,
        String key,
        Object value)
    {
        if (value != null) {
            if (buf.length() > 0) {
                buf.append("; ");
            }
            buf.append(key).append('=').append(value);
        }
    }



    /**
     * Data source that gets connections from an underlying data source but
     * with different user name and password.
     */
    private static class UserPasswordDataSource extends DelegatingDataSource {
        private final String jdbcUser;
        private final String jdbcPassword;

        /**
         * Creates a UserPasswordDataSource
         *
         * @param dataSource Underlying data source
         * @param jdbcUser User name
         * @param jdbcPassword Password
         */
        public UserPasswordDataSource(
            DataSource dataSource,
            String jdbcUser,
            String jdbcPassword)
        {
            super(dataSource);
            this.jdbcUser = jdbcUser;
            this.jdbcPassword = jdbcPassword;
        }

        @Override
        public java.sql.Connection getConnection() throws SQLException {
            return dataSource.getConnection(jdbcUser, jdbcPassword);
        }
    }

    /**
     * Implementation of {@link javax.sql.DataSource} which calls the good ol'
     * {@link java.sql.DriverManager}.
     *
     * <p>Overrides {@link #hashCode()} and {@link #equals(Object)} so that
     * {@link mondrian.spi.Dialect} objects can be cached more effectively.
     */
    private static class DriverManagerDataSource implements DataSource {
        private final String jdbcConnectString;
        private PrintWriter logWriter;
        private int loginTimeout;
        private Properties jdbcProperties;

        public DriverManagerDataSource(
            String jdbcConnectString,
            Properties properties)
        {
            this.jdbcConnectString = jdbcConnectString;
            this.jdbcProperties = properties;
        }

        @Override
        public int hashCode() {
            int h = loginTimeout;
            h = Util.hash(h, jdbcConnectString);
            h = Util.hash(h, jdbcProperties);
            return h;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DriverManagerDataSource) {
                DriverManagerDataSource
                    that = (DriverManagerDataSource) obj;
                return this.loginTimeout == that.loginTimeout
                    && this.jdbcConnectString.equals(that.jdbcConnectString)
                    && this.jdbcProperties.equals(that.jdbcProperties);
            }
            return false;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return new org.apache.commons.dbcp.DelegatingConnection(
                java.sql.DriverManager.getConnection(
                    jdbcConnectString, jdbcProperties));
        }

        @Override
        public Connection getConnection(String username, String password)
            throws SQLException
        {
            if (jdbcProperties == null) {
                return java.sql.DriverManager.getConnection(
                    jdbcConnectString, username, password);
            } else {
                Properties temp = (Properties)jdbcProperties.clone();
                temp.put("user", username);
                temp.put("password", password);
                return java.sql.DriverManager.getConnection(
                    jdbcConnectString, temp);
            }
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return logWriter;
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            logWriter = out;
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            loginTimeout = seconds;
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return loginTimeout;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getLogger("");
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("not a wrapper");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }
    }

    /**
     * Data source that delegates all methods to an underlying data source.
     */
    private static abstract class DelegatingDataSource implements DataSource {
        protected final DataSource dataSource;

        public DelegatingDataSource(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return dataSource.getConnection();
        }

        @Override
        public Connection getConnection(
            String username,
            String password)
            throws SQLException
        {
            return dataSource.getConnection(username, password);
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return dataSource.getLogWriter();
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            dataSource.setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            dataSource.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return dataSource.getLoginTimeout();
        }

        // JDBC 4.0 support (JDK 1.6 and higher)
        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (Util.JdbcVersion >= 0x0400) {
                // Do
                //              return dataSource.unwrap(iface);
                // via reflection.
                try {
                    Method method =
                        DataSource.class.getMethod("unwrap", Class.class);
                    return iface.cast(method.invoke(dataSource, iface));
                } catch (IllegalAccessException e) {
                    throw Util.newInternal(e, "While invoking unwrap");
                } catch (InvocationTargetException e) {
                    throw Util.newInternal(e, "While invoking unwrap");
                } catch (NoSuchMethodException e) {
                    throw Util.newInternal(e, "While invoking unwrap");
                }
            } else {
                if (iface.isInstance(dataSource)) {
                    return iface.cast(dataSource);
                } else {
                    return null;
                }
            }
        }

        // JDBC 4.0 support (JDK 1.6 and higher)
        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            if (Util.JdbcVersion >= 0x0400) {
                // Do
                //              return dataSource.isWrapperFor(iface);
                // via reflection.
                try {
                    Method method =
                        DataSource.class.getMethod(
                            "isWrapperFor", boolean.class);
                    return (Boolean) method.invoke(dataSource, iface);
                } catch (IllegalAccessException e) {
                    throw Util.newInternal(e, "While invoking isWrapperFor");
                } catch (InvocationTargetException e) {
                    throw Util.newInternal(e, "While invoking isWrapperFor");
                } catch (NoSuchMethodException e) {
                    throw Util.newInternal(e, "While invoking isWrapperFor");
                }
            } else {
                return iface.isInstance(dataSource);
            }
        }

        // JDBC 4.1 support (JDK 1.7 and higher)
        @Override
        public java.util.logging.Logger getParentLogger() {
            if (Util.JdbcVersion >= 0x0401) {
                // Do
                //              return dataSource.getParentLogger();
                // via reflection.
                try {
                    Method method =
                        DataSource.class.getMethod("getParentLogger");
                    return (java.util.logging.Logger) method.invoke(dataSource);
                } catch (IllegalAccessException e) {
                    throw Util.newInternal(e, "While invoking getParentLogger");
                } catch (InvocationTargetException e) {
                    throw Util.newInternal(e, "While invoking getParentLogger");
                } catch (NoSuchMethodException e) {
                    throw Util.newInternal(e, "While invoking getParentLogger");
                }
            } else {
                // Can't throw SQLFeatureNotSupportedException... it doesn't
                // exist before JDBC 4.1.
                throw new UnsupportedOperationException();
            }
        }
    }
}
// End DefaultDataServicesProvider.java
