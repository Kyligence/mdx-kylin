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


package io.kylin.mdx.insight.server.bean.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kylin.mdx.insight.core.entity.TranslationEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import java.util.LinkedList;
import java.util.List;

@Data
@NoArgsConstructor
public class SemanticModelDTO {

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("model_alias")
    private String modelAlias;

    @JsonProperty("fact_table")
    private String factTable;

    @JsonProperty("dimension_tables")
    @Valid
    private List<DimensionTableDTO> dimensionTables;

    @Valid
    private List<MeasureDTO> measures;

    @JsonProperty("translation")
    private TranslationEntity translation;

    public SemanticModelDTO(String modelName) {
        this.modelName = modelName;
    }

    public void addDimensionTableDTO(DimensionTableDTO dimensionTableDTO) {
        if (dimensionTables == null) {
            dimensionTables = new LinkedList<>();
        }
        dimensionTables.add(dimensionTableDTO);
    }

    public void addMeasureDTO(MeasureDTO measureDTO) {
        if (measures == null) {
            measures = new LinkedList<>();
        }
        measures.add(measureDTO);
    }

}
