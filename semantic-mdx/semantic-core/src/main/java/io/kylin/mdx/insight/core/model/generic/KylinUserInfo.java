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


package io.kylin.mdx.insight.core.model.generic;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
public class KylinUserInfo {

    private String username;

    private String password;

    private List<AuthorityInfo> authorities;

    private boolean disabled;

    private boolean defaultPassword;

    private boolean locked;

    private long lockedTime;

    private long wrongTime;

    private long firstLoginFailedTime;

    private String uuid;

    private long lastModified;

    private String version;

    public KylinUserInfo(String username, List<AuthorityInfo> authorities) {
        this.username = username;
        this.authorities = authorities;
    }

    public KylinUserInfo(JSONObject userInfo) {
        this.username = userInfo.getString("username");
        this.password = userInfo.getString("password");
        this.authorities = Optional.ofNullable(userInfo.getJSONArray("authorities"))
                .orElse(new JSONArray()).toJavaList(AuthorityInfo.class);
        this.disabled = userInfo.getBooleanValue("disabled");
        this.defaultPassword = userInfo.getBooleanValue("default_password");
        this.locked = userInfo.getBooleanValue("locked");
        this.lockedTime = userInfo.getLongValue("locked_time");
        this.wrongTime = userInfo.getLongValue("wrong_time");
        this.firstLoginFailedTime = userInfo.getLongValue("first_login_failed_time");
        this.uuid = userInfo.getString("uuid");
        this.lastModified = userInfo.getLongValue("lastModified");
        this.version = userInfo.getString("version");
    }

    public static List<AuthorityInfo> as(String... groups) {
        List<AuthorityInfo> authorities = new ArrayList<>();
        for (String group : groups) {
            authorities.add(new AuthorityInfo(group));
        }
        return authorities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorityInfo {

        private String authority;

    }

}
