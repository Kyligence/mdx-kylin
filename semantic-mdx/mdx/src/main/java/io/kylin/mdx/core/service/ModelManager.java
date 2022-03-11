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


package io.kylin.mdx.core.service;


import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.entity.DatasetType;
import io.kylin.mdx.insight.core.meta.*;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.model.kylin.KylinBeanWrapper;
import io.kylin.mdx.insight.core.model.semantic.SemanticDataset;
import io.kylin.mdx.insight.core.model.semantic.SemanticProject;
import io.kylin.mdx.insight.core.support.SemanticFacade;
import io.kylin.mdx.ErrorCode;
import io.kylin.mdx.core.exchange.MondrianSchemaRender;
import io.kylin.mdx.core.exchange.MondrianSchemaRenderV2;
import io.kylin.mdx.core.mondrian.MdnSchema;
import io.kylin.mdx.core.mondrian.MdnSchemaSet;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ModelManager {

    public MdnSchemaSet buildMondrianSchemaByKylin(ConnectionInfo connectionInfo) {
        IConvertor modelConverter = ConvertorFactory.createModelConvertor();
        List<MdnSchema> mdnSchemas = new LinkedList<>();
        if (modelConverter instanceof KylinConvertor) {
            KylinConvertor converter = (KylinConvertor) modelConverter;
            List<KylinBeanWrapper> kylinBeanWrappers = converter.buildDatasourceModel(connectionInfo);
            for (KylinBeanWrapper kylinBeanWrapper : kylinBeanWrappers) {
                KylinGenericModel generalModel = converter.convert(kylinBeanWrapper);
                MondrianSchemaRender mdnRender = new MondrianSchemaRender();
                mdnSchemas.add(mdnRender.render(generalModel));
            }
        } else {
            throw new SemanticException("There is no support convertor, please add a class that implements the interface IConvertor.",
                    ErrorCode.NO_AVAILABLE_CONVERTER);
        }
        MdnSchemaSet schemaSet = new MdnSchemaSet();
        schemaSet.setLastModified(System.currentTimeMillis());
        schemaSet.setMdnSchemas(mdnSchemas);
        return schemaSet;
    }

    public MdnSchemaSet buildMondrianSchemaFromDataSet(ConnectionInfo connectionInfo) {
        List<MdnSchema> mdnSchemas = new ArrayList<>(8);
        String username = connectionInfo.getUser();
        if (connectionInfo.getDelegate() != null) {
            username = connectionInfo.getDelegate();
        }
        SemanticProject semanticProject = SemanticFacade.INSTANCE.getSemanticProjectByUser(
                connectionInfo.getProject(), username);
        MondrianSchemaRenderV2 render = new MondrianSchemaRenderV2();
        long lastModified = 0L;
        for (SemanticDataset dataset : semanticProject.getSemanticDatasets()) {
            if (isVisible(dataset)) {
                mdnSchemas.add(render.create(dataset));
                lastModified = Math.max(lastModified, dataset.getLastModified());
            }
        }
        if (mdnSchemas.isEmpty()) {
            throw new SemanticException(ErrorCode.USER_NO_ACCESS_DATASET, username);
        }
        MdnSchemaSet schemaSet = new MdnSchemaSet();
        schemaSet.setLastModified(lastModified);
        schemaSet.setMdnSchemas(mdnSchemas);
        return schemaSet;
    }

    public boolean isVisible(SemanticDataset dataset) {
        boolean dimensionVisible = false, measureVisible = false;
        for (SemanticDataset.CalculateMeasure0 calculateMeasure : dataset.getCalculateMeasures()) {
            if (!calculateMeasure.isInvisible()) {
                measureVisible = true;
                break;
            }
        }
        for (SemanticDataset.NamedSet0 namedSet : dataset.getNamedSets()) {
            if (!namedSet.isInvisible()) {
                dimensionVisible = true;
                break;
            }
        }
        for (SemanticDataset.AugmentedModel model : dataset.getModels()) {
            if (!measureVisible) {
                for (SemanticDataset.AugmentedModel.AugmentMeasure baseMeasure : model.getMeasures()) {
                    if (!baseMeasure.isInvisible()) {
                        measureVisible = true;
                        break;
                    }
                }
            }
            if (!dimensionVisible) {
                for (SemanticDataset.AugmentedModel.AugmentDimensionTable dimensionTable : model.getDimensionTables()) {
                    for (SemanticDataset.AugmentedModel.AugmentDimensionTable.AugmentDimensionCol dimensionCol : dimensionTable.getDimColumns()) {
                        if (!dimensionCol.isInvisible()) {
                            dimensionVisible = true;
                            break;
                        }
                    }
                    if (dimensionVisible) {
                        break;
                    }
                }
            }
            if (dimensionVisible && measureVisible) {
                return true;
            }
        }
        return false;
    }
}
