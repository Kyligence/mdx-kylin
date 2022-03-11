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
public class AclSemanticModel {

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("model_alias")
    private String modelAlias;

    @JsonProperty("fact_table")
    private String factTable;

    @JsonProperty("measures")
    private List<AclMeasure> measures = new ArrayList<>();

    @JsonProperty("dimension_tables")
    private List<AclDimensionTable> dimensionTables = new ArrayList<>();

    public AclSemanticModel(SemanticDataset.AugmentedModel augmentedModel) {
        this.modelName = augmentedModel.getModelName();
        this.modelAlias = augmentedModel.getModelAlias();
        this.factTable = augmentedModel.getFactTable();
        for (SemanticDataset.AugmentedModel.AugmentMeasure measure : augmentedModel.getMeasures()) {
            measures.add(new AclMeasure(measure));
        }
        for (SemanticDataset.AugmentedModel.AugmentDimensionTable table : augmentedModel.getDimensionTables()) {
            dimensionTables.add(new AclDimensionTable(table));
        }
    }

    public AclMeasure getMeasureByAlias(String measureName) {
        for (AclMeasure measure : measures) {
            if (Objects.equals(measure.getAlias(), measureName)) {
                return measure;
            }
        }
        return null;
    }

    public AclDimensionTable getDimensionTableByAlias(String tableName) {
        for (AclDimensionTable dimensionTable : dimensionTables) {
            if (Objects.equals(dimensionTable.getAlias(), tableName)) {
                return dimensionTable;
            }
        }
        return null;
    }

}
