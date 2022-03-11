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
import io.kylin.mdx.insight.core.entity.DimTableModelRel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class DimTableModelRelationDTO {

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("table_relations")
    private List<TableRelation> tableRelations;

    public DimTableModelRelationDTO(String modelName, List<DimTableModelRel> dimTableModelRels) {
        this.modelName = modelName;

        this.tableRelations = dimTableModelRels
                .stream()
                .map(TableRelation::new)
                .collect(Collectors.toList());
    }

    @Data
    @NoArgsConstructor
    public static class TableRelation {

        @JsonProperty("table_name")
        private String tableName;

        /**
         * 0:joint | 1:not joint | 2: many to many
         */
        @JsonProperty("relation_type")
        private Integer relationType;

        /**
         * such as: KYLIN_SALES.ID
         * <p>
         * take effect when relation_type is 2
         */
        @JsonProperty("relation_fact_key")
        private String relationFactKey;

        /**
         * take effect when relation_type is 2
         */
        @JsonProperty("relation_bridge_table_name")
        private String relationBridgeTableName;

        public TableRelation(DimTableModelRel dimTableModelRel) {
            this.tableName = dimTableModelRel.getDimTable();
            this.relationType = dimTableModelRel.getRelation();
            this.relationFactKey = dimTableModelRel.getPrimaryDimCol();
            this.relationBridgeTableName = dimTableModelRel.getIntermediateDimTable();
        }

    }
}
