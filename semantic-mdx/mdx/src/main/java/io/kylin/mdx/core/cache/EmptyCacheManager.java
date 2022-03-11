/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.kylin.mdx.core.cache;

import io.kylin.mdx.rolap.cache.CacheManager;
import io.kylin.mdx.rolap.cache.HierarchyCache;
import io.kylin.mdx.rolap.cache.HierarchyMemberTree;
import io.kylin.mdx.rolap.cache.MondrianCache;
import io.kylin.mdx.rolap.cache.NamedSetCache;
import io.kylin.mdx.rolap.cache.SqlResultCache;
import lombok.Data;
import mondrian.olap.NamedSet;
import mondrian.rolap.RolapCubeHierarchy;
import mondrian.rolap.RolapMember;
import mondrian.server.Execution;
import mondrian.server.Locus;
import mondrian.util.ByteString;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Data
public class EmptyCacheManager implements CacheManager {

    private static final EmptyCacheManager INSTANCE = new EmptyCacheManager();

    private NamedSetCache namedSetCache = new EmptyNamedSetCache();

    private HierarchyCache hierarchyCache = new EmptyHierarchyCache();

    private SqlResultCache sqlResultCache = new EmptySqlResultCache();

    private MondrianCache mondrianCache = new EmtpyMondrianCache();

    public static EmptyCacheManager getInstance() {
        return INSTANCE;
    }

    private EmptyCacheManager() {
    }

    @Override
    public NamedSetCache getNamedSetCache() {
        return namedSetCache;
    }

    @Override
    public HierarchyCache getHierarchyCache() {
        return hierarchyCache;
    }

    @Override
    public SqlResultCache getSqlResultCache() {
        return sqlResultCache;
    }

    @Override
    public MondrianCache getMondrianCache() {
        return mondrianCache;
    }

    @Override
    public void expireAllForOneSchema(String schemaName, ByteString md5String) {
        // Empty implement
    }

    @Override
    public void expireAllForAllSchemas() {
        // Empty implement
    }

    public static class EmptyNamedSetCache implements NamedSetCache {

        @Override
        public List<RolapMember> getMemberList(NamedSet namedSet, Execution execution) throws ExecutionException {
            return null;
        }

        @Override
        public void setCacheEnabled(boolean cacheEnabled) {
            // Empty implement
        }

        @Override
        public boolean isCacheEnabled() {
            return false;
        }

        @Override
        public void clear() {
            // Empty implement
        }

        @Override
        public void expireAll() {
            // Empty implement
        }

        @Override
        public void expireOneSchema(String schemaName, ByteString md5String) {
        }
    }

    public static class EmptyHierarchyCache implements HierarchyCache {
        @Override
        public Boolean checkInitHierarchy(RolapCubeHierarchy hierarchy) {
            return null;
        }

        @Override
        public void initialize(Collection<RolapCubeHierarchy> hierarchies) {
            // Empty implement
        }

        @Override
        public HierarchyMemberTree getMemberTree(RolapCubeHierarchy hierarchy) throws ExecutionException, SQLException {
            return null;
        }

        @Override
        public HierarchyMemberTree getMemberTreeIfPresent(RolapCubeHierarchy hierarchy) {
            return null;
        }

        @Override
        public HierarchyMemberTree getMemberTreeIfPresentOrInitialized(RolapCubeHierarchy hierarchy) {
            return null;
        }

        @Override
        public void setCacheEnabled(boolean cacheEnabled) {
            // Empty implement
        }

        @Override
        public boolean isCacheEnabled() {
            return false;
        }

        @Override
        public void clear() {
            // Empty implement
        }

        @Override
        public void expireAll() {
            // Empty implement
        }

        @Override
        public void expireOneSchema(String schemaName, ByteString md5String) {
            // Empty implement
        }

    }

    public static class EmptySqlResultCache implements SqlResultCache {

        @Override
        public ResultSet getResultSet(String schemaName, String md5, Statement statement, String sql, Locus locus) throws ExecutionException, SQLException, NoSuchFieldException, IllegalAccessException {
            return null;
        }

        @Override
        public void setCacheEnabled(boolean cacheEnabled) {
            // Empty implement
        }

        @Override
        public boolean isCacheEnabled() {
            return false;
        }

        @Override
        public void clear() {
            // Empty implement
        }

        @Override
        public void expireAll() {
            // Empty implement
        }

        @Override
        public void expireOneSchema(String schemaName, ByteString md5String) {
            // Empty implement
        }

    }

    public static class EmtpyMondrianCache implements MondrianCache {

        @Override
        public void setCacheEnabled(boolean cacheEnabled) {
            // Empty implement
        }

        @Override
        public void expireAll() {
            // Empty implement
        }

    }

}
