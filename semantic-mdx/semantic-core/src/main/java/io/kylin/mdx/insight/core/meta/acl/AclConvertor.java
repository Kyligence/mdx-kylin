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

import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.core.model.acl.AclProjectModel;

import java.util.List;

public interface AclConvertor {

    /**
     * 构建用户的权限模型
     * 1. 对于用户组，只考虑用户组的权限模型
     * 2. 对于用户，考虑其所属的用户组权限取交集
     *
     * @param project 项目
     * @param type    类型：区分 user 和 group
     * @param name    名称：用户名 或者 用户组名
     * @param tables  需要加载的表
     * @return 权限模型
     */
    AclProjectModel getAclProjectModel(ConnectionInfo connInfo, String type, String name, List<String> tables);

}
