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
import io.kylin.mdx.insight.core.entity.MdxQuery;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MdxQueryDTO {

    private Integer id;

    private Long startTime;

    private Long executeTime;

    @JsonProperty("content")
    private String mdxText;

    private String datasetName;

    @JsonProperty("status")
    private Boolean success;

    private String application;

    @JsonProperty("username")
    private String userName;

    private String project;

    private String queryId;

    @JsonProperty("isCached")
    private Boolean mdxCacheUsed;

    @JsonProperty("isOtherQueryEngine")
    private Boolean otherQueryEngineUsed;

    @JsonProperty("networkSize")
    private Integer networkPackage;

    @JsonProperty("isTimeout")
    private Boolean timeout;

    @JsonProperty("multiDimDatasetTime")
    private Long createMultiDimensionalDataset;

    private Long transferTime;

    private Boolean isGateway;

    private String node;

    public MdxQueryDTO(MdxQuery mdxQuery) {
        this.id = mdxQuery.getId();
        this.startTime = mdxQuery.getStart();
        this.executeTime = mdxQuery.getTotalExecutionTime();
        this.mdxText = mdxQuery.getMdxText();
        this.datasetName = mdxQuery.getDatasetName();
        this.success = mdxQuery.getSuccess();
        this.application = mdxQuery.getApplication();
        this.userName = mdxQuery.getUsername();
        this.project = mdxQuery.getProject();
        this.queryId = mdxQuery.getMdxQueryId();
        this.mdxCacheUsed = mdxQuery.getMdxCacheUsed();
        this.otherQueryEngineUsed = mdxQuery.getOtherUsed();
        this.networkPackage = mdxQuery.getNetworkPackage();
        this.timeout = mdxQuery.getTimeout();
        this.createMultiDimensionalDataset = mdxQuery.getCreateMultiDimensionalDataset();
        this.transferTime = mdxQuery.getMarshallSoapMessage();
        this.isGateway = mdxQuery.getGateway();
        this.node = mdxQuery.getNode();
    }
}
