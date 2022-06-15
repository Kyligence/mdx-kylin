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


package io.kylin.mdx.insight.core.meta.acl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.kylin.mdx.insight.common.http.HttpUri;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.RoleType;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.core.meta.IHttpCall;
import io.kylin.mdx.insight.core.model.acl.AclProjectModel;
import io.kylin.mdx.insight.core.model.acl.AclTableModel;
import io.kylin.mdx.insight.core.sync.MetaStore;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class KylinAclConvertor implements AclConvertor {
    private final static String TYPE_USER = "user";

    private final static String TYPE_GROUP = "group";

    private final IHttpCall httpCall;

    public KylinAclConvertor(IHttpCall httpCall) {
        this.httpCall = httpCall;
    }

    @Override
    public AclProjectModel getAclProjectModel(ConnectionInfo connInfo, String type, String name, List<String> tables) {
        // 使用同步用户获取信息
        byte[] auth = Utils.encodeAuthentication(connInfo.getUser(), connInfo.getPassword());
        String project = Objects.requireNonNull(connInfo.getProject());
        AclProjectModel projectModel = new AclProjectModel(type, name, project);
        if (RoleType.GROUP.getType().equals(type)) {
            generateAclByGroup(projectModel, name, tables, auth, project);
        } else {
            generateAclByUser(projectModel, name, tables, auth, project);
        }
        return projectModel;
    }

    private void generateAclByGroup(AclProjectModel projectModel, String group, List<String> tables,
                                    byte[] auth, String project) {
        for (String tableName : tables) {
            AclTableModel tableModel = new AclTableModel(tableName);
            projectModel.setModel(tableName, tableModel);
            boolean hasPermission = hasTablePermissionByGroup(auth, project, tableName, group);
            if (!hasPermission) {
                tableModel.setInvisible(true);
                continue;
            }
            // NOTE: Kylin doesn't support acl of column
        }
    }

    private void generateAclByUser(AclProjectModel projectModel, String user, List<String> tables,
                                   byte[] auth, String project) {

        // 获取用户所属用户组
        List<String> authorities = MetaStore.getInstance().getGroupsByUser(user);
        for (String tableName : tables) {
            AclTableModel tableModel = new AclTableModel(tableName);
            projectModel.setModel(tableName, tableModel);

            boolean hasPermission = hasTablePermissionByUser(auth, project, tableName, user, authorities);
            if (hasPermission) {
                continue;
            }
            tableModel.setInvisible(true);
        }
    }

    private boolean hasTablePermissionByUser(byte[] auth, String project, String tableName,
                                             String user, List<String> authorities) {
        String tableUri = HttpUri.getKylinAclTableUri(project, TYPE_USER, tableName);
        String content = httpCall.doHttpCall(tableUri, auth);
        JSONObject result = JSON.parseObject(content);
        if (result == null || !result.containsKey("data")) {
            return false;
        }
        JSONArray userList = result.getJSONArray("data");
        if (userList == null) {
            return false;
        }
        // 用户匹配，不区分大小写
        for (int i = 0; i < userList.size(); i++) {
            if (!userList.getString(i).equalsIgnoreCase(user)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean hasTablePermissionByGroup(byte[] auth, String project, String tableName,
                                              String group) {
        String tableUri = HttpUri.getKylinAclTableUri(project, TYPE_GROUP, tableName);
        String content = httpCall.doHttpCall(tableUri, auth);
        JSONObject result = JSON.parseObject(content);
        if (result == null || !result.containsKey("data")) {
            return false;
        }
        JSONArray groupList = result.getJSONArray("data");
        return groupList.contains(group);
    }

    private void addUnavailableColumn(JSONArray infoList, Predicate<String> predicate, Set<String> unavailable) {
        if (infoList == null) {
            return;
        }
        for (int i = 0; i < infoList.size(); i++) {
            JSONObject info = infoList.getJSONObject(i);
            for (String name : info.keySet()) {
                if (!predicate.test(name)) {
                    continue;
                }
                JSONArray columns = info.getJSONArray(name);
                for (int j = 0; j < columns.size(); j++) {
                    unavailable.add(columns.getString(j));
                }
            }
        }
    }

}
