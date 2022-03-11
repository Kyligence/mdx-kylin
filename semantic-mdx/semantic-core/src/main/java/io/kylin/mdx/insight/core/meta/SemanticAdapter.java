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

import io.kylin.mdx.insight.common.MdxContext;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.SemanticUserAndPwd;
import io.kylin.mdx.insight.common.http.Response;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.model.generic.KylinUserInfo;
import org.apache.commons.lang3.StringUtils;
import io.kylin.mdx.insight.core.model.acl.AclProjectModel;
import io.kylin.mdx.insight.core.model.kylin.KylinBeanWrapper;
import io.kylin.mdx.insight.core.sync.ModelVersionHolder;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SemanticAdapter {

    public static final SemanticAdapter INSTANCE = new SemanticAdapter();

    private SemanticAdapter() {
    }

    public List<String> getNoCacheCubeNames(String project) {
        ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .user(SemanticUserAndPwd.getUser())
                .password(SemanticUserAndPwd.getEncodedPassword())
                .project(project)
                .build();
        IConvertor<?> modelConvertor = ConvertorFactory.createModelConvertor();
        return modelConvertor.getCubeNames(connectionInfo);
    }

    public List<KylinUserInfo> getNoCacheUsers() {
        ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .user(SemanticUserAndPwd.getUser())
                .password(SemanticUserAndPwd.getEncodedPassword())
                .build();
        IConvertor<?> modelConvertor = ConvertorFactory.createModelConvertor();
        return modelConvertor.getUsers(connectionInfo);
    }

    public List<String> getNoCacheGroups() {
        ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .user(SemanticUserAndPwd.getUser())
                .password(SemanticUserAndPwd.getEncodedPassword())
                .build();
        IConvertor<?> modelConvertor = ConvertorFactory.createModelConvertor();
        return modelConvertor.getGroups(connectionInfo);
    }

    public List<String> getUserAuthority(ConnectionInfo connInfo) {
        ConnectionInfo connectionInfo;
        if (StringUtils.isEmpty(SemanticUserAndPwd.getUser()) || !MdxContext.isSyncStatus()) {
            connectionInfo = connInfo;
        } else {
            connectionInfo = ConnectionInfo.builder()
                    .user(SemanticUserAndPwd.getUser())
                    .password(SemanticUserAndPwd.getEncodedPassword())
                    .build();
        }
        IConvertor<?> modelConvertor = ConvertorFactory.createModelConvertor();
        return modelConvertor.getUserAuthorities(connectionInfo, connInfo.getUser());
    }

    public List<String> getSegments(String project) {
        ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .user(SemanticUserAndPwd.getUser())
                .password(SemanticUserAndPwd.getEncodedPassword())
                .project(project)
                .build();;
        IConvertor<?> modelConvertor = ConvertorFactory.createModelConvertor();
        return modelConvertor.getSegments(connectionInfo);
    }

    public Map<String, Long> getDimensionCardinality(String project) {
        ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .user(SemanticUserAndPwd.getUser())
                .password(SemanticUserAndPwd.getEncodedPassword())
                .project(project)
                .build();
        IConvertor<?> modelConvertor = ConvertorFactory.createModelConvertor();
        return modelConvertor.getHighCardinalityDimension(connectionInfo);
    }

    public List<KylinGenericModel> getNocacheGenericModels(String project) {
        ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .user(SemanticUserAndPwd.getUser())
                .password(SemanticUserAndPwd.getEncodedPassword())
                .project(project)
                .build();
        IConvertor<?> modelConvertor = ConvertorFactory.createModelConvertor();
        List<KylinGenericModel> genericModels = new LinkedList<>();
        if (modelConvertor instanceof KylinConvertor) {
            KylinConvertor convertor = (KylinConvertor) modelConvertor;
            List<KylinBeanWrapper> kylinBeans = convertor.buildDatasourceModel(connectionInfo);
            for (KylinBeanWrapper kylinBean: kylinBeans) {
                genericModels.add(convertor.convert(kylinBean));
            }
        } else {
            throw new SemanticException("There is no support convertor, please add a class that implements the interface IConvertor，thanks.");
        }
        ModelVersionHolder.tryAddNewModelVersions(project, genericModels);
        return genericModels;
    }

    public Set<String> getActualProjectSet(ConnectionInfo connectionInfo) {
        IConvertor<?> convertor = ConvertorFactory.createModelConvertor();
        return convertor.getActualProjectSet(connectionInfo);
    }

    public Response authentication(String basicAuth) {
        IConvertor<?> convertor = ConvertorFactory.createModelConvertor();
        return convertor.authentication(basicAuth);
    }

    public Response getLicense() {
        IConvertor<?> convertor = ConvertorFactory.createModelConvertor();
        return convertor.getLicense();
    }

    public String getAccessInfo(ConnectionInfo connectionInfo) {
        IConvertor<?> convertor = ConvertorFactory.createModelConvertor();
        return convertor.getAccessInfo(connectionInfo);
    }

    public AclProjectModel getAclModel(String project, String type, String name, List<String> tables) {
        ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .user(SemanticUserAndPwd.getUser())
                .password(SemanticUserAndPwd.getEncodedPassword())
                .project(project)
                .build();
        IConvertor<?> convertor = ConvertorFactory.createModelConvertor();
        return convertor.getAclProjectModel(connectionInfo, type, name, tables);
    }

    public List<String> getGroupsByProject(String project) {
        ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .user(SemanticUserAndPwd.getUser())
                .password(SemanticUserAndPwd.getEncodedPassword())
                .project(project)
                .build();
        IConvertor<?> convertor = ConvertorFactory.createModelConvertor();
        return convertor.getGroupsByProject(connectionInfo);
    }

    public List<String> getUsersByProject(String project) {
        ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .user(SemanticUserAndPwd.getUser())
                .password(SemanticUserAndPwd.getEncodedPassword())
                .project(project)
                .build();
        IConvertor<?> convertor = ConvertorFactory.createModelConvertor();
        return convertor.getUsersByProject(connectionInfo);
    }

    public Response getProfileInfo(ConnectionInfo connectionInfo) {
        IConvertor<?> convertor = ConvertorFactory.createModelConvertor();
        return convertor.getProfile(connectionInfo);
    }

}
