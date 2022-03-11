package io.kylin.mdx.insight.mondrian.subquery;

import io.kylin.mdx.web.xmla.MondrianServerBuilder;
import lombok.Getter;
import mondrian.olap.MondrianProperties;
import mondrian.olap.MondrianServer;
import mondrian.olap.Query;
import mondrian.server.StatementImpl;
import mondrian.spi.VirtualFileHandler;
import mondrian.tui.MockServletConfig;
import mondrian.tui.MockServletContext;
import mondrian.xmla.XmlaRequestContext;
import mondrian.xmla.context.ConnectionFactory;
import org.olap4j.OlapConnection;
import org.olap4j.PreparedOlapStatement;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.*;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Properties;

public abstract class SubQueryEnv implements Runnable {

    private static final String RESOURCES = findResources();

    private static final String WEBAPP_DIR = RESOURCES + "/datasource";

    static {
        MondrianProperties.instance().VfsClass.set(MockVirtualFileHandler.class.getName());
    }

    @Getter
    private ConnectionFactory connectionFactory;

    public SubQueryEnv() {
    }

    public void init() {
        XmlaRequestContext context = new XmlaRequestContext();
        context.setCurrentUser("ADMIN");
        context.currentProject = "learn_kylin";
        context.currentCatalog = "ADMIN_learn_kylin";

        ServletConfig servletConfig = new MockServletConfig() {
            @Override
            public ServletContext getServletContext() {
                return new MockServletContext() {
                    @Override
                    public String getRealPath(String path) {
                        path = path.substring("WEB-INF/".length());
                        return WEBAPP_DIR + "/" + path;
                    }
                };
            }
        };
        MondrianServer server = new MondrianServerBuilder(servletConfig)
                .project(context.currentProject)
                .username(context.getLoginUser())
                .build();
        this.connectionFactory = (ConnectionFactory) server;
    }

    public void destroy() {
        ((MondrianServer) connectionFactory).shutdown();
        XmlaRequestContext.getContext().clear();
    }

    public OlapConnection getConnection() throws SQLException {
        XmlaRequestContext context = XmlaRequestContext.getContext();
        return connectionFactory.getConnection(
                context.currentCatalog,
                context.currentProject,
                null,
                new Properties());
    }

    public Query getQuery(PreparedOlapStatement statement) {
        try {
            Field field = StatementImpl.class.getDeclaredField("query");
            field.setAccessible(true);
            return (Query) field.get(statement);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Can't read query field!");
        }
    }

    @Override
    public void run() {
        init();
        try (OlapConnection connection = getConnection()) {
            execute(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        destroy();
    }

    public abstract void execute(OlapConnection connection) throws SQLException;

    private static String findResources() {
        String resources = "/src/test/resources";
        File file = new File("." + resources);
        if (!file.exists()) {
            file = new File("./semantic-server" + resources);
        }
        if (!file.exists()) {
            file = new File("./semantic-mdx/semantic-server" + resources);
        }
        try {
            if (!file.exists()) {
                throw new FileNotFoundException(file.getAbsolutePath());
            }
            resources = file.getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException("Can't find datasources dir!");
        }
        // set MDX_HOME, point to conf/insight.properties
        System.setProperty("MDX_HOME", resources);
        return resources;
    }

    private static class MockVirtualFileHandler implements VirtualFileHandler {

        @Override
        public InputStream readVirtualFile(String url) throws IOException {
            url = url.substring("file:/".length());
            return new FileInputStream(WEBAPP_DIR + "/" + url);
        }

    }

}
