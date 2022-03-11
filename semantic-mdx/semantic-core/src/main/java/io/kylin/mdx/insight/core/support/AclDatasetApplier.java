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


package io.kylin.mdx.insight.core.support;

import io.kylin.mdx.insight.core.model.acl.*;
import io.kylin.mdx.insight.core.model.semantic.SemanticDataset;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class AclDatasetApplier {

    /**
     * 将权限模型应用，将导致输出的结果减少无权限的表和列
     *
     * @param semanticDataset 输出模型
     */
    public static void apply(AclDataset aclDataset, SemanticDataset semanticDataset) {
        // Model
        List<SemanticDataset.AugmentedModel> semanticModels = semanticDataset.getModels();
        for (SemanticDataset.AugmentedModel semanticModel : semanticModels) {
            AclSemanticModel aclModel = aclDataset.getModel(semanticModel.getModelAlias());
            if (aclModel == null) {
                continue;
            }
            // Measure
            List<SemanticDataset.AugmentedModel.AugmentMeasure> removeMeasures = new ArrayList<>();
            for (SemanticDataset.AugmentedModel.AugmentMeasure semanticMeasure : semanticModel.getMeasures()) {
                AclMeasure aclMeasure = aclModel.getMeasureByAlias(semanticMeasure.getAlias());
                if (aclMeasure != null && aclMeasure.noAccessRight()) {
                    removeMeasures.add(semanticMeasure);
                }
            }
            semanticModel.getMeasures().removeAll(removeMeasures);
            // Dimension table
            List<SemanticDataset.AugmentedModel.AugmentDimensionTable> removeTables = new ArrayList<>();
            for (SemanticDataset.AugmentedModel.AugmentDimensionTable dimensionTable : semanticModel.getDimensionTables()) {
                AclDimensionTable aclTable = aclModel.getDimensionTableByAlias(dimensionTable.getAlias());
                if (aclTable != null) {
                    applyDimensionTable(aclTable, dimensionTable, removeTables);
                }
            }
            semanticModel.getDimensionTables().removeAll(removeTables);
        }

        List<SemanticDataset.CalculateMeasure0> removeMeasures = new ArrayList<>();
        for (SemanticDataset.CalculateMeasure0 semanticMeasure : semanticDataset.getCalculateMeasures()) {
            AclCalculationMeasure aclMeasure = aclDataset.getCalculateMeasure(semanticMeasure.getName());
            if (aclMeasure != null && aclMeasure.noAccessRight()) {
                removeMeasures.add(semanticMeasure);
            }
        }
        semanticDataset.getCalculateMeasures().removeAll(removeMeasures);

        List<SemanticDataset.NamedSet0> removeNamedSets = new ArrayList<>();
        for (SemanticDataset.NamedSet0 namedSet : semanticDataset.getNamedSets()) {
            AclNamedSet aclNamedSet = aclDataset.getNamedSet(namedSet.getName());
            if (aclNamedSet != null && aclNamedSet.noAccessRight()) {
                removeNamedSets.add(namedSet);
            }
        }
        semanticDataset.getNamedSets().removeAll(removeNamedSets);
    }

    private static void applyDimensionTable(AclDimensionTable aclTable, SemanticDataset.AugmentedModel.AugmentDimensionTable dimensionTable,
                                            List<SemanticDataset.AugmentedModel.AugmentDimensionTable> removeTables) {
        boolean tableNoRight = aclTable.noAccessRight();
        if (tableNoRight) {
            removeTables.add(dimensionTable);
            return;
        }
        List<SemanticDataset.AugmentedModel.AugmentDimensionTable.AugmentDimensionCol> removeDimCols = new ArrayList<>();
        for (SemanticDataset.AugmentedModel.AugmentDimensionTable.AugmentDimensionCol dimensionCol :
                dimensionTable.getDimColumns()) {
            AclDimensionCol aclColumn = aclTable.getDimensionColumnByAlias(dimensionCol.getAlias());
            if (aclColumn != null && aclColumn.noAccessRight()) {
                removeDimCols.add(dimensionCol);
            }
        }
        dimensionTable.getDimColumns().removeAll(removeDimCols);
        if (dimensionTable.getHierarchys() != null) {
            List<SemanticDataset.AugmentedModel.AugmentDimensionTable.Hierarchy0> removeHierarchys = new ArrayList<>();
            for (SemanticDataset.AugmentedModel.AugmentDimensionTable.Hierarchy0 hierarchy :
                    dimensionTable.getHierarchys()) {
                AclHierarchy aclHierarchy = aclTable.getHierarchy(hierarchy.getName());
                if (aclHierarchy != null && aclHierarchy.noAccessRight()) {
                    removeHierarchys.add(hierarchy);
                }
            }
            dimensionTable.getHierarchys().removeAll(removeHierarchys);
        }
        if (CollectionUtils.isEmpty(dimensionTable.getDimColumns())
                && CollectionUtils.isEmpty(dimensionTable.getHierarchys())) {
            removeTables.add(dimensionTable);
        }
    }

}
