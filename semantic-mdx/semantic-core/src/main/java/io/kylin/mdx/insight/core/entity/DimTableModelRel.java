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


package io.kylin.mdx.insight.core.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Table(name = "dim_table_model_rel")
@NoArgsConstructor
public class DimTableModelRel {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "dataset_id")
    private Integer datasetId;

    @Column(name = "model")
    private String model;

    @Column(name = "dim_table")
    private String dimTable;

    @Column(name = "relation")
    private Integer relation;

    @Column(name = "intermediate_dim_table")
    private String intermediateDimTable;

    @Column(name = "primary_dim_col")
    private String primaryDimCol;

    public DimTableModelRel(Integer datasetId) {
        this.datasetId = datasetId;
    }

    public DimTableModelRel(Integer datasetId, String model, String dimTable) {
        this.datasetId = datasetId;
        this.model = model;
        this.dimTable = dimTable;
    }

    public DimTableModelRel(Integer datasetId, String model, String dimTable, Integer relation) {
        this.datasetId = datasetId;
        this.model = model;
        this.dimTable = dimTable;
        this.relation = relation;
    }

    public DimTableModelRel(Integer datasetId, String model, String dimTable, Integer relation,
                            String intermediateDimTable, String primaryDimCol) {
        this.datasetId = datasetId;
        this.model = model;
        this.dimTable = dimTable;
        this.relation = relation;
        this.intermediateDimTable = intermediateDimTable;
        this.primaryDimCol = primaryDimCol;
    }
}
