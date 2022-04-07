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


package io.kylin.mdx.insight.engine.service;

import io.kylin.mdx.insight.common.util.AESWithECBEncryptor;
import io.kylin.mdx.insight.core.dao.UserInfoMapper;
import io.kylin.mdx.insight.core.entity.UserInfo;

public interface UserInfoService {

    default void doUpdateConfUsr(String username, String password, UserInfoMapper userInfoMapper) {
        UserInfo oldConfUsr = userInfoMapper.selectConfUsr();
        if (oldConfUsr != null) {
            oldConfUsr.setConfUsr(null);
            userInfoMapper.updateConfUsr(oldConfUsr);
        }
        UserInfo userInfo = userInfoMapper.selectByUserName(username.toUpperCase());
        if (userInfo == null) {
            UserInfo newConfUsr = new UserInfo(username, password, 1);
            try {
                userInfoMapper.insertSelective(newConfUsr);
            } catch (Exception e) {
                // Nothing to do
            }
        } else {
            userInfo.setPassword(AESWithECBEncryptor.encrypt(password));
            userInfo.setConfUsr(1);
            userInfoMapper.updateConfUsr(userInfo);
        }
    }

    void updateConfUsr(String userName, String password, UserInfoMapper userInfoMapper);
}
