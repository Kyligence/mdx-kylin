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


package io.kylin.mdx.insight.common.http;

import io.kylin.mdx.insight.common.SemanticConfig;

public class HttpUri {

    private static SemanticConfig config = SemanticConfig.getInstance();

    public static final String USER_AUTHENTICATION = "/kylin/api/user/authentication";

    public static final String GET_KYLIN_MODELS = "/kylin/api/models/mdx?modelName={modelName}&project={project}" +
            "&offset=0&limit=" + config.getProjectPageSize();

    public static final String GET_KYLIN_CUBE_DESC = "/kylin/api/cube_desc/{cubeName}/mdx";

    public static final String GET_COLUMNS_DESC_V2 = "/kylin/api/tables_and_columns?project={project}";

    public static final String GET_KYLIN_COLUMNS_DESC = "/kylin/api/tables_and_columns/mdx?project={project}";

    public static final String GET_KYLIN_CUBES_OF_PROJECT = "/kylin/api/cubes/mdx?projectName={project}" +
            "&offset=0&page=" + config.getProjectPageSize();

    public final static String GET_KYLIN_TABLE_INFO = "/kylin/api/tables/mdx?project={project}&ext={ext}";

    public static final String GET_KYLIN_USERS = "/kylin/api/user/users?isFuzzMatch=true";

    public static final String GET_KYLIN_ACL_TABLE = "/kylin/api/acl/table/{project}/{type}/{table}/mdx" +
            "?offset=0&limit=" + config.getUserPageSize();

    public static final String GET_KYLIN_GROUPS = "/kylin/api/user_group/groups?isFuzzMatch=true&offset=0&limit="
            + config.getUserPageSize();

    /**
     * 获取指定用户组的用户列表
     */
    public static final String GET_ACCESS_PROJECT = "/kylin/api/access/ProjectInstance/{project}" +
            "?pageOffset=0&pageSize=" + config.getUserPageSize();

    public static final String GET_KYLIN_ACCESS_PROJECT = "/kylin/api/access/ProjectInstance/{project}/mdx";

    public static final String GET_KYLIN_PROJECTS_URI = "/kylin/api/projects/mdx?offset=0&limit=" +
            config.getProjectPageSize();

    public static final String GET_KYLIN_ACCESS_INFO = "/kylin/api/access/user/permission/{project}/mdx";

    public static  String getKylinProjectCubesAPI(String project) {
        return GET_KYLIN_CUBES_OF_PROJECT.replace("{project}", project);
    }

    public static String getKylinCubeDesc(String cubeName) {
        return GET_KYLIN_CUBE_DESC.replace("{cubeName}", cubeName);
    }

    public static String getKylinModelsAPI(String modelName, String project) {
        return GET_KYLIN_MODELS.replace("{modelName}", modelName)
                .replace("{project}", project);
    }

    public static String getProjectColumnDescAPI(String projectName) {
        return GET_COLUMNS_DESC_V2.replace("{project}", projectName);
    }

    public static String getKylinProjectColumnDescAPI(String project) {
        return GET_KYLIN_COLUMNS_DESC.replace("{project}", project);
    }

    public static String getKylinTableInfoUrI(String project) {
        return GET_KYLIN_TABLE_INFO.replace("{project}", project).replace("{ext}", "true");
    }

    public static String getKylinAclTableUri(String project, String userOrGroup, String table) {
        return GET_KYLIN_ACL_TABLE.replace("{project}", project)
                .replace("{type}", userOrGroup)
                .replace("{table}", table);
    }

    public static String getKylinUsers(String pageSize) {
        return GET_KYLIN_USERS.concat("&limit=" + pageSize);
    }

    public static String getKylinUsrAuthority(String limit, String userName) {
        return GET_KYLIN_USERS.concat("&limit=" + limit).concat("&name=" + userName);
    }

    public static String getKylinGroupMembers(String groupName) {
        return GET_KYLIN_GROUPS + "&name=" + groupName;
    }

    public static String getAccessProject(String project) {
        return GET_ACCESS_PROJECT.replace("{project}", project);
    }

    public static String getKylinAccessProject(String project) {
        return GET_KYLIN_ACCESS_PROJECT.replace("{project}", project);
    }

}
