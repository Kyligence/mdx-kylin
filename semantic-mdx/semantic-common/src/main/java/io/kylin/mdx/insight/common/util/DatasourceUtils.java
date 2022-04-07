package io.kylin.mdx.insight.common.util;

public class DatasourceUtils {

    public static String getDatasourceName(String username, String project, String delegate) {
        if (delegate != null) {
            return delegate.toUpperCase() + "_" + project;
        } else {
            return username.toUpperCase() + "_" + project;
        }
    }

    public static String getDatasourcePath(String datasourceDir, String username, String project, String delegate) {
        String prefix;
        if (delegate != null) {
            prefix = delegate.toUpperCase() + "_" + username.toUpperCase();
        } else {
            prefix = username.toUpperCase();
        }
        return Utils.endWithSlash(datasourceDir) + prefix + "_" + project + "_datasources.xml";
    }

    public static String getSchemaPath(String schemaDir, String username, String project, String catalog, String delegate) {
        String prefix = delegate != null ? delegate.toUpperCase() : username.toUpperCase();
        String schemaName = prefix + "_" + project + "_" + catalog + ".xml";
        return Utils.endWithSlash(schemaDir) + schemaName;
    }

}
