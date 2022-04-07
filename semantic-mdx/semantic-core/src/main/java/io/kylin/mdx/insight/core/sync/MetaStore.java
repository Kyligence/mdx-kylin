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


package io.kylin.mdx.insight.core.sync;

import io.kylin.mdx.insight.core.entity.RoleType;
import io.kylin.mdx.insight.core.model.acl.AclProjectModel;
import io.kylin.mdx.insight.core.model.acl.AclTableModel;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.model.generic.KylinUserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 元数据内存缓存
 * #  保存用户和用户组之间的映射关系，区分大小写
 */
public class MetaStore {

    private static final MetaStore INSTANCE = new MetaStore();

    private final ReadWriteLock userLock = new ReentrantReadWriteLock();

    private final Lock projectLock = new ReentrantLock();

    private final ReadWriteLock segmentLock = new ReentrantReadWriteLock();
    /**
     * 全部用户组
     */
    private final AtomicReference<List<String>> groupNameList = new AtomicReference<>(new ArrayList<>());

    /**
     * 全大写用户名 -> 原始用户名
     * 如果没有该用户，直接返回输入
     */
    private final Map<String, String> originalNameMap = new HashMap<>();

    /**
     * 全大写用户名 -> 用户组
     * 如果用户不属于用户组或者没有该用户，返回 [] （一般至少属于 ALL_USERS）
     */
    private final Map<String, List<String>> userToGroupsMap = new HashMap<>();

    /**
     * 用户组 -> 全大写用户名
     * 如果用户组不包含用户或者如果没有该用户组，返回 []
     */
    private final Map<String, List<String>> groupToUsersMap = new HashMap<>();

    /**
     * 工程名 -> 全部用户
     * 保存工程下需要更新的用户
     */
    private final Map<String, Set<String>> projectRefreshMap = new HashMap<>();

    /**
     * 项目名 -> 已加载该工程的用户
     */
    private final Map<String, Set<String>> projectLoadingMap = new HashMap<>();

    /**
     * (user,project) -> project acl
     */
    private final Map<AclKey, AclProjectModel> projectModelMap = new HashMap<>();

    private final Map<String, Set<String>> segmentsCache = new ConcurrentHashMap<>();

    private static final AtomicLong lastUpdateTime = new AtomicLong();

    private final Set<String> notFoundProjects = new HashSet<>();

    private final Map<String, List<KylinGenericModel>> project2Models = new ConcurrentHashMap<>();

    private MetaStore() {
    }

    public Set<String> getNotFoundProjects() {
        return notFoundProjects;
    }

    public Map<String, List<KylinGenericModel>> getProject2Models() {
        return project2Models;
    }

    public AtomicLong getLastUpdateTime() {
        return lastUpdateTime;
    }

    public static MetaStore getInstance() {
        return INSTANCE;
    }

    public List<String> getAllGroupName() {
        return groupNameList.get();
    }

    public void setAllGroupName(List<String> groupNames) {
        groupNameList.set(groupNames);
    }

    public String getOriginalName(String user) {
        if (user == null) {
            return null;
        }
        Lock lock = userLock.readLock();
        lock.lock();
        try {
            return originalNameMap.getOrDefault(user.toUpperCase(), user);
        } finally {
            lock.unlock();
        }
    }

    public List<String> getGroupsByUser(String user) {
        if (user == null) {
            return Collections.emptyList();
        }
        Lock lock = userLock.readLock();
        lock.lock();
        try {
            return userToGroupsMap.getOrDefault(user.toUpperCase(), Collections.emptyList());
        } finally {
            lock.unlock();
        }
    }

    public List<String> getUsersByGroup(String group) {
        if (group == null) {
            return Collections.emptyList();
        }
        Lock lock = userLock.readLock();
        lock.lock();
        try {
            return groupToUsersMap.getOrDefault(group, Collections.emptyList());
        } finally {
            lock.unlock();
        }
    }

    public void syncUserAndGroup(List<KylinUserInfo> userInfoList) {
        Lock lock = userLock.writeLock();
        lock.lock();
        try {
            userToGroupsMap.clear();
            groupToUsersMap.clear();
            originalNameMap.clear();
            for (KylinUserInfo userInfo : userInfoList) {
                String user = userInfo.getUsername();
                originalNameMap.put(user.toUpperCase(), user);
                for (KylinUserInfo.AuthorityInfo authority : userInfo.getAuthorities()) {
                    String group = authority.getAuthority();
                    userToGroupsMap.computeIfAbsent(user.toUpperCase(), k -> new ArrayList<>()).add(group);
                    groupToUsersMap.computeIfAbsent(group, k -> new ArrayList<>()).add(user.toUpperCase());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void addForceRefreshSchema(String project, String realUser) {
        synchronized (projectRefreshMap) {
            projectRefreshMap.computeIfAbsent(project, k -> new HashSet<>()).add(realUser);
        }
    }

    public boolean isForceRefreshSchema(String project, String realUser) {
        synchronized (projectRefreshMap) {
            Set<String> users = projectRefreshMap.getOrDefault(project, Collections.emptySet());
            boolean exists = users.contains(realUser);
            if (exists) {
                users.remove(realUser);
            }
            return exists;
        }
    }

    public Set<String> getAllUserOnProject(String project) {
        projectLock.lock();
        try {
            return projectLoadingMap.getOrDefault(project, Collections.emptySet());
        } finally {
            projectLock.unlock();
        }
    }

    public AclProjectModel getAclProjectModel(String user, String project) {
        projectLock.lock();
        try {
            return projectModelMap.get(new AclKey(user, project));
        } finally {
            projectLock.unlock();
        }
    }

    public void recordAclProjectModel(AclProjectModel newModel) {
        if (!Objects.equals(newModel.getType(), RoleType.USER.getType())) {
            return;
        }
        projectLock.lock();
        try {
            projectLoadingMap.computeIfAbsent(newModel.getProject(), k -> new HashSet<>())
                    .add(newModel.getName());
            AclKey aclKey = new AclKey(newModel.getName(), newModel.getProject());
            AclProjectModel aclModel = projectModelMap.computeIfAbsent(aclKey,
                    k -> new AclProjectModel(newModel.getType(), newModel.getName(), newModel.getProject()));
            for (AclTableModel newTableModel : newModel.getModels().values()) {
                aclModel.setModel(newTableModel.getTable(), newTableModel);
            }
            projectModelMap.put(aclKey, aclModel);
        } finally {
            projectLock.unlock();
        }
    }

    public Set<String> getSegmentCacheByProject(String project) {
        if (project == null) {
            return Collections.emptySet();
        }
        Lock lock = segmentLock.readLock();
        lock.lock();
        try {
            return segmentsCache.getOrDefault(project, Collections.emptySet());
        } finally {
            lock.unlock();
        }
    }

    public void setSegmentsCache(String project, Set<String> segment) {
        Lock lock = segmentLock.writeLock();
        lock.lock();
        try {
            segmentsCache.put(project, segment);
        } finally {
            lock.unlock();
        }
    }

    public void setProject2Models(String project, List<KylinGenericModel> genericModels) {
        project2Models.put(project, genericModels);
    }

    public void clearProjectModels() {
        project2Models.clear();
    }

    @Data
    @AllArgsConstructor
    private static class AclKey {

        private String user;

        private String project;

    }

}
