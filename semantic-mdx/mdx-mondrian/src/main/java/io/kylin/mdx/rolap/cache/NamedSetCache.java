package io.kylin.mdx.rolap.cache;

import mondrian.olap.NamedSet;
import mondrian.rolap.RolapMember;
import mondrian.server.Execution;
import mondrian.util.ByteString;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface NamedSetCache {
    /**
     * return the map of named set expression to the set of members of the named set
     */
    List<RolapMember> getMemberList(NamedSet namedSet, Execution execution) throws ExecutionException;

    /*public List<Exp> getNewConstraint(NamedSet namedSet, Execution execution) throws ExecutionException;

    static NamedSetCache getDefault() {
        return DefaultNamedSetCache.INSTANCE;
    }

    static NamedSetCache getDefault(boolean cacheEnabled) {
        DefaultNamedSetCache.INSTANCE.setCacheEnabled(cacheEnabled);
        return DefaultNamedSetCache.INSTANCE;
    }

     */

    void setCacheEnabled(boolean cacheEnabled);

    boolean isCacheEnabled();

    void clear();

    void expireAll();

    void expireOneSchema(String schemaName, ByteString md5String);
}
