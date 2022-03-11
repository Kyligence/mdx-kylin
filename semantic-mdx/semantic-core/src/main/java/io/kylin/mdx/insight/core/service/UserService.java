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


package io.kylin.mdx.insight.core.service;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.entity.GroupInfo;
import io.kylin.mdx.insight.core.entity.SyncResult;
import io.kylin.mdx.insight.core.entity.UserInfo;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.core.meta.SemanticAdapter;
import io.kylin.mdx.insight.core.model.generic.KylinUserInfo;
import io.kylin.mdx.insight.core.support.KILicenseInfo;
import io.kylin.mdx.insight.core.support.UserOperResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * @author qi.wu
 */
public interface UserService {
    /**
     *
     * @param semanticAdapter
     */
    void setSemanticAdapter(SemanticAdapter semanticAdapter);

    /**
     *
     * @param user
     * @param password
     * @param project
     * @param delegate
     * @return
     * @throws SemanticException
     */
    UserOperResult loginForMDX(String user, String password, String project, String delegate) throws SemanticException;

    /**
     *
     * @param user
     * @param encryptedPwd
     * @return
     */
    boolean checkUserAndPwd(String user, String encryptedPwd);

    /**
     *
     * @param basicAuth
     * @return
     * @throws SemanticException
     */
    UserOperResult login(String basicAuth) throws SemanticException;

    /**
     *
     * @param connInfo
     * @return
     */
    boolean hasAdminPermission(ConnectionInfo connInfo);

    /**
     *
     * @param connInfo
     * @param global
     * @return
     */
    boolean hasAdminPermission(ConnectionInfo connInfo, boolean global);

    /**
     *
     * @param username
     * @param licenseAuth
     * @return
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    UserOperResult changeUserLicense(String username, Integer licenseAuth);

    /**
     *
     * @return
     */
    List<UserInfo> selectAll();

    /**
     *
     * @param username
     * @return
     */
    UserInfo selectOne(String username);

    /**
     *
     * @return
     */
    KILicenseInfo getKiLicenseInfo();

    /**
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    List<UserInfo> getAllUsers(Integer pageNum, Integer pageSize);

    /**
     *
     * @return
     */
    UserInfo selectConfUser();

    /**
     *
     * @param project
     * @param pageNum
     * @param pageSize
     * @return
     */
    List<UserInfo> getAllUserByProject(String project, Integer pageNum, Integer pageSize);

    /**
     *
     * @param project
     * @param pageNum
     * @param pageSize
     * @return
     */
    List<GroupInfo> getAllGroupByProject(String project, Integer pageNum, Integer pageSize);

    /**
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    List<GroupInfo> getAllGroup(Integer pageNum, Integer pageSize);

    /**
     *
     * @param userName
     * @param password
     */
    void updateConfUsr(String userName, String password);

    /**
     *
     * @return
     */
    List<KylinUserInfo> getUsersByKylin();

    /**
     *
     * @param users
     */
    void saveUsersToCache(List<KylinUserInfo> users);

    /**
     *
     * @return
     */
    Set<String> getUsersNameByKylin();

    /**
     *
     * @return
     */
    Set<String> getUsersNameByDatabase();

    /**
     *
     * @param userInfos
     */
    void insertUsers(List<UserInfo> userInfos);

    /**
     *
     * @param userInfos
     */
    void deleteUsers(List<String> userInfos);

    /**
     *
     * @param project
     * @return
     */
    List<String> getUsersByProjectFromCache(String project);

    /**
     *
     * @param user
     * @param password
     * @return
     * @throws SemanticException
     */
    void systemAdminCheck(String user, String password) throws SemanticException;

    int insertSelective(UserInfo record);

    int updateConfUsr(UserInfo userInfo);

}
