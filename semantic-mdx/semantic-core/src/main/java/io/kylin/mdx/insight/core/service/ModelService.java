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


package io.kylin.mdx.insight.core.service;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.entity.NamedDimTable;
import io.kylin.mdx.insight.core.meta.ProjectModel;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.model.semantic.DimensionTable;
import io.kylin.mdx.insight.core.model.semantic.SemanticModel;
import io.kylin.mdx.insight.core.sync.DatasetValidator;

import java.util.*;

/**
 * @author qi.wu
 */
public interface ModelService {
    /**
     * fetch cached genericModels from project
     *
     * @param project
     * @return List<KylinGenericModel>
     * @throws SemanticException
     */
    List<KylinGenericModel> getCachedGenericModels(String project) throws SemanticException;

    /**
     * load load genericModels from project
     *
     * @param project
     * @return List<KylinGenericModel>
     * @throws SemanticException
     */
    List<KylinGenericModel> loadGenericModels(String project) throws SemanticException;

    /**
     * refresh genericModels from project
     *
     * @param project
     * @param genericModels
     * @return null
     * @throws SemanticException
     */
    void refreshGenericModels(String project, List<KylinGenericModel> genericModels) throws SemanticException;

    /**
     * fetch genericModels from project
     *
     * @param project
     * @return Map<ProjectModel, SemanticModel>
     * @throws SemanticException
     */
    Map<ProjectModel, SemanticModel> getSemanticModelsByProject(String project) throws SemanticException;

    /**
     * fetch semantic model from project and model
     *
     * @param project
     * @param model
     * @return SemanticModel
     * @throws SemanticException
     */
    SemanticModel getSemanticModel(String project, String model) throws SemanticException;

    /**
     * fetch semantic model from project, model and genericModel
     *
     * @param project
     * @param model
     * @param genericModel
     * @return SemanticModel
     * @throws SemanticException
     */
    SemanticModel buildSemanticModelInternal(String project, String model, KylinGenericModel genericModel) throws SemanticException;

    /**
     * fetch semantic model from project ...
     *
     * @param genericModel
     * @param oldTableMap
     * @param namedDimTableCountMap
     * @return SemanticModel
     * @throws SemanticException
     */
    Map<String, DimensionTable> createDimensionTableMap(KylinGenericModel genericModel, Map<DatasetValidator.ModelTable, NamedDimTable> oldTableMap, Map<String, Integer> namedDimTableCountMap) throws SemanticException;

    /**
     * getDimensionTableWithoutFactTableByModel
     *
     * @param project
     * @param model
     * @return List
     * @throws SemanticException
     */
    List<String> getDimensionTableWithoutFactTableByModel(String project, String model) throws SemanticException;

    /**
     * getDimtablesByModel
     *
     * @param project
     * @param model
     * @return
     * @throws SemanticException
     */
    List<String> getDimtablesByModel(String project, String model) throws SemanticException;

    /**
     * getSemanticModelDetail
     *
     * @param project
     * @param model
     * @return SemanticModel
     * @throws SemanticException
     */
    SemanticModel getSemanticModelDetail(String project, String model) throws SemanticException;

    /**
     * getModelsByProject
     *
     * @param project
     * @return List<KylinGenericModel>
     * @throws SemanticException
     */
    List<KylinGenericModel> getModelsByProject(String project) throws SemanticException;

    /**
     * getCachedProjectNames
     *
     * @param null
     * @return List
     * @throws SemanticException
     */
    List<String> getCachedProjectNames() throws SemanticException;
}
