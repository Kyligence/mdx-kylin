/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2003-2005 Julian Hyde
// Copyright (C) 2005-2013 Pentaho
// Copyright (C) 2004-2005 TONBELLER AG
// All Rights Reserved.
*/
package mondrian.olap;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import io.kylin.mdx.rolap.cache.CacheManager;
import io.kylin.mdx.rolap.cache.HierarchyCache;
import io.kylin.mdx.rolap.cache.HierarchyMemberTree;
import lombok.extern.slf4j.Slf4j;
import mondrian.rolap.RolapCubeHierarchy;

/**
 * <code>CacheDelegatingSchemaReader</code> extends {@link DelegatingSchemaReader} by
 * delegating getMemberChildren methods to an underlying {@link SchemaReader}.
 */
@Slf4j
public class CacheDelegatingSchemaReader extends DelegatingSchemaReader {
    private HierarchyMemberTree hierarchyMemberTree;

    /**
     * Creates a CacheDelegatingSchemaReader.
     *
     * @param schemaReader Parent reader to delegate unhandled calls to
     */
    public CacheDelegatingSchemaReader(SchemaReader schemaReader, Hierarchy hierarchy) {
        super(schemaReader);
        try {
            HierarchyCache hierarchyCache = CacheManager.getCacheManager().getHierarchyCache();
            if (hierarchy instanceof RolapCubeHierarchy && hierarchyCache.isCacheEnabled()) {
                hierarchyMemberTree = hierarchyCache.getMemberTree((RolapCubeHierarchy)hierarchy);
            }
        } catch (ExecutionException | SQLException e) {
            log.warn("Get member tree from hierarchy {} exception", hierarchy, e);
        }
    }

    @Override
    public List<Member> getMemberChildren(Member member) {
        return getMemberChildren(member, null);
    }

    @Override
    public List<Member> getMemberChildren(Member member, Evaluator context) {
        try {
            List<? extends Member> result = hierarchyMemberTree.getChildrenMembers(member.getUniqueName());
            return  Util.cast(result);
        } catch (Throwable t) {
            log.info(String.format("Fetching descendants of member %s from hierarchy cache error.", member.getUniqueName()), t);
        }

        return schemaReader.getMemberChildren(member, context);
    }

    @Override
    public List<Member> getMemberChildren(List<Member> members, Evaluator context) {
        if (hierarchyMemberTree != null) {
            return members.stream().map(this::getMemberChildren).filter(Objects::nonNull).flatMap(Collection::stream)
                .collect(Collectors.toList());
        }
        return schemaReader.getMemberChildren(members, context);
    }
}

// End CacheDelegatingSchemaReader.java
