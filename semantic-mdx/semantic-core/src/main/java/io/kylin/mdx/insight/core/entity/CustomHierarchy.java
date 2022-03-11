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
@Table(name = "custom_hierarchy")
public class CustomHierarchy {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "dataset_id")
    private Integer datasetId;

    @Column(name = "model")
    private String model;

    @Column(name = "dim_table")
    private String dimTable;

    @Column(name = "name")
    private String name;

    @Column(name = "dim_col")
    private String dimCol;

    @Column(name = "weight_col")
    private String weightCol;

    @Column(name = "description")
    private String desc;

    @Column(name = "translation")
    private String translation;

    public CustomHierarchy(Integer datasetId) {
        this.datasetId = datasetId;
    }

    public CustomHierarchy(Integer datasetId, String model, String dimTable, String name,
                           String dimCol, String weightCol,
                           String desc, TranslationEntity translation) {
        this.datasetId = datasetId;
        this.model = model;
        this.dimTable = dimTable;
        this.name = name;
        this.dimCol = dimCol;
        this.weightCol = weightCol;
        this.desc = desc;
        this.translation = JSON.toJSONString(translation);
    }

    public String getTableColName() {
        return "[" + this.getDimTable() + "]." +
                "[" + this.getDimCol() + "]";
    }

    public String getHierarchyName() {
        return "[" + this.getDimTable() + "]." +
                "[" + this.getName() + "-Hierarchy]";
    }
}
