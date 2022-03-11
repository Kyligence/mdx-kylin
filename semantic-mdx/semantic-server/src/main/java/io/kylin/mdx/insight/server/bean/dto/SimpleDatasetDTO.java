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
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SimpleDatasetDTO {

    private Integer id;

    private String project;

    private String dataset;

    private Integer type;

    private String status;

    private String createUser;

    private Long createTime;

    private Long modifyTime;

    @JsonProperty("front_v")
    private String frontVersion;

    public SimpleDatasetDTO(DatasetEntity datasetEntity) {
        this.id = datasetEntity.getId();
        this.project = datasetEntity.getProject();
        this.dataset = datasetEntity.getDataset();
        this.status = datasetEntity.getStatus();
        this.createUser = datasetEntity.getCreateUser();
        this.createTime = Utils.getMilliseconds(datasetEntity.getCreateTime());
        this.modifyTime = Utils.getMilliseconds(datasetEntity.getModifyTime());
        this.frontVersion = datasetEntity.getFrontVersion();
    }

}
