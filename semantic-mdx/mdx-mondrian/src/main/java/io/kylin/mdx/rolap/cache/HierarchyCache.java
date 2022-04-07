package io.kylin.mdx.rolap.cache;

import mondrian.rolap.RolapCubeHierarchy;
import mondrian.util.ByteString;

import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

public interface HierarchyCache {
    Boolean checkInitHierarchy(RolapCubeHierarchy hierarchy);

    void initialize(Collection<RolapCubeHierarchy> hierarchies);

    /**
     * Returns the members cache of input hierarchy.
     */
    HierarchyMemberTree getMemberTree(RolapCubeHierarchy hierarchy) throws ExecutionException, SQLException;

    /**
     * Returns the members cache of input hierarchy if the cache exists.
     */
    HierarchyMemberTree getMemberTreeIfPresent(RolapCubeHierarchy hierarchy);

    /**
     * Returns the members cache of input hierarchy if the cache exists or have been specified in the initializing list.
     */
    HierarchyMemberTree getMemberTreeIfPresentOrInitialized(RolapCubeHierarchy hierarchy) throws ExecutionException;

    void setCacheEnabled(boolean cacheEnabled);

    boolean isCacheEnabled();

    void clear();

    void expireAll();

    void expireOneSchema(String schemaName, ByteString md5String);

}
