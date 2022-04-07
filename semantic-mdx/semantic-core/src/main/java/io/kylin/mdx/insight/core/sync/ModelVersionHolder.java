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


package io.kylin.mdx.insight.core.sync;

import io.kylin.mdx.insight.core.meta.ProjectModel;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ModelVersionHolder {

    private static Map<ProjectModel, String> modelVersionMap = new HashMap<>();


    public static void tryAddNewModelVersions(String project, List<KylinGenericModel> genericModels) {

        for (KylinGenericModel genericModel : genericModels) {
            ProjectModel projectModel = new ProjectModel(project, genericModel.getModelName());

            String version = modelVersionMap.get(projectModel);
            if (StringUtils.isBlank(version)) {
                modelVersionMap.put(projectModel, genericModel.getSignature());
                log.info("Add model version, project:{}, model:{}, version:{}", project, genericModel.getModelName(), genericModel.getSignature());
            }
        }
    }

    public static String getVersion(String project, String model) {
        return modelVersionMap.get(new ProjectModel(project, model));
    }

    public static void setNewVersion(String project, String model, String newVersion) {
        modelVersionMap.put(new ProjectModel(project, model), newVersion);

    }

}
