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

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@Table(name = "named_dim_table")
public class NamedDimTable {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "dataset_id")
    private Integer datasetId;

    @Column(name = "model")
    private String model;

    @Column(name = "dim_table")
    private String dimTable;

    @Column(name = "dim_table_alias")
    private String dimTableAlias;

    /**
     * dimension table type
     * values: regular|time
     */
    @Column(name = "dim_table_type")
    private String dimTableType;

    @Column(name = "actual_table")
    private String actualTable;

    @Column(name = "fact_table")
    private String factTable;

    @Column(name = "translation")
    private String translation;

    public NamedDimTable(Integer datasetId) {
        this.datasetId = datasetId;
    }

    public NamedDimTable(Integer datasetId, String model, String dimTable, String dimTableAlias, String dimTableType,
                         String actualTable, String factTable) {
        this.datasetId = datasetId;
        this.model = model;
        this.dimTable = dimTable;
        this.dimTableAlias = dimTableAlias;
        this.dimTableType = dimTableType;
        this.actualTable = actualTable;
        this.factTable = factTable;
    }

    public NamedDimTable(Integer datasetId, String model, String dimTable, String dimTableAlias, String dimTableType,
                         String actualTable, String factTable, TranslationEntity translation) {
        this.datasetId = datasetId;
        this.model = model;
        this.dimTable = dimTable;
        this.dimTableAlias = dimTableAlias;
        this.dimTableType = dimTableType;
        this.actualTable = actualTable;
        this.factTable = factTable;
        this.translation = JSON.toJSONString(translation);
    }
}
