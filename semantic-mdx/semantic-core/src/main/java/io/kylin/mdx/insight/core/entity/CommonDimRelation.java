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
@NoArgsConstructor
@Table(name = "common_dim_relation")
public class CommonDimRelation {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "dataset_id")
    private Integer datasetId;

    @Column(name = "model")
    private String model;

    @Column(name = "model_related")
    private String modelRelated;

    /**
     * the relation between two models, like dim_t1=dim_t1,dim_t2=dim_t2
     */
    @Column(name = "relation")
    private String relation;


    public CommonDimRelation(Integer datasetId) {
        this.datasetId = datasetId;
    }

    public CommonDimRelation(Integer datasetId, String model, String modelRelated, String relation) {
        this.datasetId = datasetId;
        this.model = model;
        this.modelRelated = modelRelated;
        this.relation = relation;
    }
}
