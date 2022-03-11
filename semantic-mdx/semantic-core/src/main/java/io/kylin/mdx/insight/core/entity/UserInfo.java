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


package io.kylin.mdx.insight.core.entity;


import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.util.AESWithECBEncryptor;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_info")
public class UserInfo {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    /**
     * active: 0-inactive | 1-active
     */
    @Column(name = "active")
    private Integer active;

    /**
     * licenseAuth: 0-unauthorized | 1-authorized
     */
    @Column(name = "license_auth")
    private Integer licenseAuth;

    @Column(name = "login_count")
    private Integer loginCount;

    @Column(name = "last_login")
    private Long lastLogin;

    @Column(name = "create_time")
    private Long createTime;

    /**
     * confUsr: 0/Null-not config user | 1-config user
     */
    @Column(name = "conf_usr")
    private Integer confUsr;

    public UserInfo(String username){
        this.username = username.toUpperCase();
        this.password = AESWithECBEncryptor.encrypt(RandomStringUtils.randomAlphanumeric(10));
        this.active = 1;
        this.licenseAuth = 1;
        this.loginCount = 0;
        this.lastLogin = Utils.currentTimeStamp();
        this.createTime = Utils.currentTimeStamp();
    }
    public UserInfo(ConnectionInfo connectionInfo, Integer active, Integer licenseAuth) {
        this.username = connectionInfo.getUser().toUpperCase();
        this.password = AESWithECBEncryptor.encrypt(connectionInfo.getPassword());
        this.active = active;
        this.licenseAuth = licenseAuth;
        this.loginCount = 1;
        this.lastLogin = Utils.currentTimeStamp();
        this.createTime = Utils.currentTimeStamp();
    }

    public UserInfo(String username, String password, Integer confUsr) {
        this.username = username.toUpperCase();
        this.password = password;
        this.active = 1;
        this.licenseAuth = 1;
        this.loginCount = 0;
        this.lastLogin = Utils.currentTimeStamp();
        this.createTime = Utils.currentTimeStamp();
        this.confUsr = confUsr;
    }

    public String getDecryptedPassword() throws PwdDecryptException {
        return AESWithECBEncryptor.decrypt(password);
    }
}
