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


package io.kylin.mdx.insight.core.meta;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.http.Response;
import io.kylin.mdx.insight.core.model.acl.AclProjectModel;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.model.generic.KylinUserInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * KYLIN Meta 数据提供
 *
 * @param <T>
 */
public interface IConvertor<T> {

    KylinGenericModel convert(T sourceModel);

    List<T> buildDatasourceModel(ConnectionInfo datasourceInfo) throws Exception;

    Response authentication(String basicAuth) throws SemanticException;

    Response getLicense() throws SemanticException;

    Set<String> getActualProjectSet(ConnectionInfo connInfo) throws SemanticException;

    String getAccessInfo(ConnectionInfo connInfo) throws SemanticException;

    List<String> getCubeNames(ConnectionInfo connInfo) throws SemanticException;

    List<String> getSegments(ConnectionInfo connInfo) throws SemanticException;

    List<KylinUserInfo> getUsers(ConnectionInfo connInfo) throws SemanticException;

    List<String> getGroups(ConnectionInfo connInfo) throws SemanticException;

    List<String> getUserAuthorities(ConnectionInfo connInfo, String username) throws SemanticException;

    AclProjectModel getAclProjectModel(ConnectionInfo connInfo, String type, String name, List<String> tables);

    List<String> getGroupsByProject(ConnectionInfo connectionInfo);

    List<String> getUsersByProject(ConnectionInfo connectionInfo);

    Map<String, Long> getHighCardinalityDimension(ConnectionInfo connInfo) throws SemanticException;

    Response getProfile(ConnectionInfo connectionInfo) throws SemanticException;

}
