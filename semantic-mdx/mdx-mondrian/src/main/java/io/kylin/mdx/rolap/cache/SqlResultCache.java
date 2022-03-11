package io.kylin.mdx.rolap.cache;

import mondrian.server.Locus;
import mondrian.util.ByteString;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;

public interface SqlResultCache {
    /**
     * return the map of named set expression to the set of members of the named set
     */
    ResultSet getResultSet(String schemaName, String md5, Statement statement, String sql, Locus locus) throws ExecutionException, SQLException, NoSuchFieldException, IllegalAccessException;

    void setCacheEnabled(boolean cacheEnabled);

    boolean isCacheEnabled();

    void clear();

    void expireAll();

    void expireOneSchema(String schemaName, ByteString md5String);
}
