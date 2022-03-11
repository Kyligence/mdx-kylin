package mondrian.olap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Data;
import mondrian.rolap.RolapSchema;
import mondrian.rolap.RolapSchemaPool;
import mondrian.rolap.SchemaKey;
import mondrian.rolap.SqlStatement;
import mondrian.util.ByteString;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MondrianCacheControl {

    private static Map<String, ProjectCache> cacheMap = new ConcurrentHashMap<>();

    private static List<Cache> cacheList = new Vector<>();

    public static <K, V> Cache<K, V> buildCache(long expire, TimeUnit timeUnit, long maxSize) {
        Cache<K, V> cache = CacheBuilder.newBuilder()
                .softValues()
                .expireAfterWrite(expire, timeUnit)
                .maximumSize(maxSize)
                .build();
        cacheList.add(cache);
        return cache;
    }

    public static void clearCache() {
        cacheList.forEach(Cache::invalidateAll);
        RolapSchemaPool.instance().refreshAllCachedSchemas();
    }

    public static void putCacheMap(String project, CacheType cacheType, Cache cache, String cacheKey) {

        if (project == null) {
            return;
        }

        ProjectCache projectCache = getOrCreateProjectCache(project);

        if (CacheType.MEMORY_SEGMENT_CACHE.equals(cacheType)) {
            projectCache.getMemorySegmentCaches().add(cache);
        } else if (CacheType.SOFT_SMART_CACHE.equals(cacheType)) {
            projectCache.getSoftSmartCache().add(cache);
        } else if (CacheType.SQL_STATEMENT_CACHE.equals(cacheType)) {
            projectCache.getSqlStatementCacheKeys().add(cacheKey);
        }
    }

    public static void putSchemaKey(String project, RolapSchema schema) {
        if (project == null) {
            return;
        }

        ProjectCache projectCache = getOrCreateProjectCache(project);

        projectCache.getSchemaNames().add(schema.getName());
        projectCache.getSchemaKeys().add(schema.key);
        projectCache.getSchemaMd5s().add(schema.getChecksum());
    }

    private static ProjectCache getOrCreateProjectCache(String project) {
        ProjectCache projectCache = cacheMap.get(project);
        if (projectCache == null) {
            projectCache = new ProjectCache();
            cacheMap.put(project, projectCache);
        }
        return projectCache;
    }

    public static Pair<List<String>, List<ByteString>> clearProjectCache(String projectName) {
        ProjectCache projectCache = cacheMap.get(projectName);
        if (projectCache == null) {
            return null;
        }
        if (projectCache.getSoftSmartCache() != null && projectCache.getSoftSmartCache().size() > 0) {
            projectCache.getSoftSmartCache().forEach(Cache::invalidateAll);
        }
        if (projectCache.getMemorySegmentCaches() != null && projectCache.getMemorySegmentCaches().size() > 0) {
            projectCache.getMemorySegmentCaches().forEach(Cache::invalidateAll);
        }
        List<String> cacheKeys = projectCache.getSqlStatementCacheKeys();
        if (cacheKeys != null && !cacheKeys.isEmpty()) {
            for (String cacheKey : cacheKeys) {
                SqlStatement.QUERY_CACHE.invalidate(cacheKey);
            }
        }

        List<String> schemaNames = new ArrayList<>(projectCache.getSchemaNames());
        List<ByteString> md5s = new ArrayList<>(projectCache.getSchemaMd5s());
        for (int i = 0; i < projectCache.getSchemaKeys().size(); i++) {
            RolapSchemaPool.instance().refreshCachedSchema(projectCache.getSchemaKeys().get(i));
        }

        return Pair.of(schemaNames, md5s);
    }

    public static Map<String, ProjectCache> getCacheMap() {
        return cacheMap;
    }

    public static List<Cache> getCacheList() {
        return cacheList;
    }

    @Data
    public static class ProjectCache {

        private List<String> sqlStatementCacheKeys = new Vector<>();

        private List<Cache> memorySegmentCaches = new Vector<>();

        private List<Cache> SoftSmartCache = new Vector<>();

        private List<String> schemaNames = new ArrayList<>();

        private List<SchemaKey> schemaKeys = new ArrayList<>();

        private List<ByteString> schemaMd5s = new ArrayList<>();
    }

    public enum CacheType {
        SQL_STATEMENT_CACHE,
        MEMORY_SEGMENT_CACHE,
        SOFT_SMART_CACHE;
    }

}
