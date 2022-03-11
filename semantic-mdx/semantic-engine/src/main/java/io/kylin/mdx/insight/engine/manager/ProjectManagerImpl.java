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


package io.kylin.mdx.insight.engine.manager;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.SemanticUserAndPwd;
import io.kylin.mdx.insight.common.async.AsyncService;
import io.kylin.mdx.insight.common.async.BatchTaskExecutor;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.RoleType;
import io.kylin.mdx.insight.core.manager.AsyncManager;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.core.meta.SemanticAdapter;
import io.kylin.mdx.insight.core.manager.ProjectManager;
import io.kylin.mdx.insight.core.model.acl.AclProjectModel;
import io.kylin.mdx.insight.core.service.ModelService;
import io.kylin.mdx.insight.core.sync.MetaStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ProjectManagerImpl implements ProjectManager {

    @Autowired
    private ModelService modelService;

    private volatile static Set<String> cachedProjectSet = Collections.emptySet();

    private MetaStore metaStore = MetaStore.getInstance();

    private SemanticAdapter semanticAdapter = SemanticAdapter.INSTANCE;

    @Override
    public Set<String> getAllProject() {
        return cachedProjectSet;
    }

    public static void setAllProject(Set<String> projects) {
        cachedProjectSet = new HashSet<>(projects);
    }

    @Override
    public void initLoadProjectList() throws SemanticException {
        cachedProjectSet = getActualProjectSetByAdmin();
        log.info("Load project list successfully...");
    }

    @Override
    public void verifyProjectListChange() throws SemanticException {
        Set<String> actualProjectSet = getActualProjectSetByAdmin();
        ImmutableSet<String> projectAddedSet = Sets.difference(actualProjectSet, cachedProjectSet).immutableCopy();
        if (!Utils.isCollectionEmpty(projectAddedSet)) {
            cachedProjectSet = actualProjectSet;
            log.info("project added:{}", JSON.toJSONString(projectAddedSet));
        }
        ImmutableSet<String> projectDeletedSet = Sets.difference(cachedProjectSet, actualProjectSet).immutableCopy();
        if (!Utils.isCollectionEmpty(projectDeletedSet)) {
            cachedProjectSet = actualProjectSet;
            log.info("project deleted:{}", JSON.toJSONString(projectDeletedSet));
        }
    }

    @Override
    public Set<String> getActualProjectSetByAdmin() throws SemanticException {
        ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .user(SemanticUserAndPwd.getUser())
                .password(SemanticUserAndPwd.getEncodedPassword())
                .build();
        return getActualProjectSet(connectionInfo);
    }

    @Override
    public Set<String> getActualProjectSet(ConnectionInfo connInfo) throws SemanticException {
        return semanticAdapter.getActualProjectSet(connInfo);
    }

    @Override
    public Map<String, String> getUserAccessProjects(ConnectionInfo connInfo) throws SemanticException {
        Map<String, String> accessInfos = new ConcurrentHashMap<>();
        if (SemanticConfig.getInstance().isDisableAsyncHttpCall()) {
            Set<String> actualProjects = getActualProjectSet(connInfo);
            actualProjects.forEach(project -> {
                ConnectionInfo newConnInfo = new ConnectionInfo(connInfo);
                newConnInfo.setProject(project);
                String accessInfo = semanticAdapter.getAccessInfo(newConnInfo);
                accessInfos.put(project, accessInfo);
            });
        } else {
            AsyncService service = AsyncManager.getInstance().getAsyncService();
            BatchTaskExecutor executor = new BatchTaskExecutor(service);
            Set<String> actualProjects = getActualProjectSet(connInfo);
            actualProjects.forEach(project -> executor.submit(
                    () -> {
                        ConnectionInfo newConnInfo = new ConnectionInfo(connInfo);
                        newConnInfo.setProject(project);
                        String accessInfo = SemanticAdapter.INSTANCE.getAccessInfo(newConnInfo);
                        accessInfos.put(project, accessInfo);
                    }
            ));
            try {
                executor.executeWithThis();
            } catch (InterruptedException e) {
                log.error("Concurrent access to user project failed!", e);
                Thread.currentThread().interrupt();
            }
        }
        return accessInfos;
    }

    @Override
    public void doProjectAcl(String realUser, String project) {
        AclProjectModel oldModel = metaStore.getAclProjectModel(realUser, project);
        List<String> tables = new ArrayList<>(oldModel.getModels().keySet());
        AclProjectModel newModel = semanticAdapter.getAclModel(project, RoleType.USER.getType(), realUser, tables);
        if (!newModel.isCompatible(oldModel)) {
            metaStore.addForceRefreshSchema(project, realUser);
        }
    }

    @Override
    public List<String> getProjectNamesByCache() {
        return modelService.getCachedProjectNames();
    }

}
