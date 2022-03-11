package io.kylin.mdx.rolap.cache;

import mondrian.util.ByteString;

public interface CacheManager {

    static CacheManager getCacheManager() {
        CacheManager cacheManager;
        // TODO, implement cache
        String clazz = "io.kylin.mdx.rolap.cache.EmptyCacheManager";
        try {
            cacheManager = (CacheManager) Class.forName(clazz).getMethod("getInstance").invoke("");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cacheManager;
    }

    NamedSetCache getNamedSetCache();

    HierarchyCache getHierarchyCache();

    SqlResultCache getSqlResultCache();

    MondrianCache getMondrianCache();

    void expireAllForOneSchema(String schemaName, ByteString md5String);

    void expireAllForAllSchemas();
}
