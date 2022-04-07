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


package io.kylin.mdx.insight.core.dao;

import io.kylin.mdx.insight.core.entity.UserInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface UserInfoMapper {

    int insertSelective(UserInfo record);

    int selectLicenseNumWithLock();

    int selectLicenseNum();

    List<UserInfo> selectAll();

    UserInfo selectByUserName(String username);

    UserInfo selectByUserAndPwd(@Param("username") String username, @Param("password") String password);

    int updateByPrimaryKeySelective(UserInfo record);

    int updateLicenseAuthByUsername(@Param("username") String username, @Param("licenseAuth") Integer licenseAuth);

    List<UserInfo> selectAllUsersByPage(RowBounds rowBounds);

    List<String> selectAllUsersName();

    int insertUsers(List<UserInfo> users);

    int deleteUsersByNames(List<String> userNames);

    UserInfo selectConfUsr();

    int updateConfUsr(UserInfo userInfo);

    int deleteUsersById(List<Integer> ids);
}
