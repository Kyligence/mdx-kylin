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


package io.kylin.mdx.insight.core.model.acl;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kylin.mdx.insight.core.model.semantic.SemanticDataset;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
public class AclDataset {

    @JsonProperty("models")
    private List<AclSemanticModel> models = new ArrayList<>();

    @JsonProperty("calculate_measures")
    private List<AclCalculationMeasure> calculateMeasures = new ArrayList<>();

    @JsonProperty("named_sets")
    private List<AclNamedSet> namedSets = new ArrayList<>();

    public AclDataset(SemanticDataset semanticDataset) {
        for (SemanticDataset.AugmentedModel augmentedModel : semanticDataset.getModels()) {
            models.add(new AclSemanticModel(augmentedModel));
        }
        for (SemanticDataset.CalculateMeasure0 calculateMeasure : semanticDataset.getCalculateMeasures()) {
            calculateMeasures.add(new AclCalculationMeasure(calculateMeasure));
        }
        for (SemanticDataset.NamedSet0 namedSet : semanticDataset.getNamedSets()) {
            namedSets.add(new AclNamedSet(namedSet));
        }
    }

    public AclSemanticModel getModel(String modelName) {
        for (AclSemanticModel model : models) {
            if (model.getModelAlias().equals(modelName)) {
                return model;
            }
        }
        return null;
    }

    public AclMeasure getMeasureByAlias(String measureName) {
        for (AclSemanticModel model : models) {
            for (AclMeasure measure : model.getMeasures()) {
                if (Objects.equals(measure.getAlias(), measureName)) {
                    return measure;
                }
            }
        }
        return null;
    }

    public AclDimensionTable getDimensionTableByAlias(String tableName) {
        for (AclSemanticModel model : models) {
            for (AclDimensionTable dimensionTable : model.getDimensionTables()) {
                if (Objects.equals(dimensionTable.getAlias(), tableName)) {
                    return dimensionTable;
                }
            }
        }
        return null;
    }

    public AclCalculationMeasure getCalculateMeasure(String measureName) {
        for (AclCalculationMeasure calculationMeasure : calculateMeasures) {
            if (calculationMeasure.getName().equals(measureName)) {
                return calculationMeasure;
            }
        }
        return null;
    }

    public AclNamedSet getNamedSet(String setName) {
        for (AclNamedSet namedSet : namedSets) {
            if (namedSet.getName().equals(setName)) {
                return namedSet;
            }
        }
        return null;
    }

}
