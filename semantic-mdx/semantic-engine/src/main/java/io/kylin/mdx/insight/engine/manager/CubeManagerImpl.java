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


package io.kylin.mdx.insight.engine.manager;

import com.google.common.collect.Sets;
import io.kylin.mdx.insight.core.manager.CubeManager;
import io.kylin.mdx.insight.core.meta.SemanticAdapter;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.service.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author qi.wu
 */
@Service
public class CubeManagerImpl implements CubeManager {
    private SemanticAdapter semanticAdapter = SemanticAdapter.INSTANCE;

    @Autowired
    private ModelService modelService;


    @Override
    public Set<String> getCubeByKylin(String project) {
        List<String> nocacheNameList = semanticAdapter.getNoCacheCubeNames(project);
        return Sets.newHashSet(nocacheNameList);
    }

    @Override
    public Set<String> getCubeByCache(String project) {
        List<KylinGenericModel> cachedModels = modelService.getCachedGenericModels(project);
        Set<String> cacheProjectName = cachedModels.stream().map(KylinGenericModel::getModelName).collect(Collectors.toSet());
        return cacheProjectName;
    }

    @Override
    public List<KylinGenericModel> getCubeModelByCache(String project) {
        List<KylinGenericModel> cubeModelsFromCache = modelService.getCachedGenericModels(project);
        return cubeModelsFromCache;
    }

    @Override
    public List<KylinGenericModel> getCubeModelByKylin(String project) {
        List<KylinGenericModel> cubeModelsFromKe = semanticAdapter.getNocacheGenericModels(project);
        return cubeModelsFromKe;
    }


}
