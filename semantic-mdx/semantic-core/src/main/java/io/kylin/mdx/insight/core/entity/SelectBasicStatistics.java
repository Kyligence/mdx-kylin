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

@Data
public class SelectBasicStatistics {

    private Long startTime;

    private Long endTime;

    private Long tick;

    private Integer minTime;

    private Integer maxTime;

    private String projectName;

    private String datasetName;

    private Long count;

    private Boolean executeState;

    public SelectBasicStatistics(Long startTime, Long endTime, String projectName) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.projectName = projectName;
    }

    public SelectBasicStatistics(Long startTime, Long endTime, String projectName, String datasetName) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.projectName = projectName;
        this.datasetName = datasetName;
    }

    public SelectBasicStatistics(Long startTime, Long endTime, String projectName, Integer minTime, Integer maxTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.projectName = projectName;
        this.minTime = minTime;
        this.maxTime = maxTime;
    }

    public SelectBasicStatistics(Long startTime, Long endTime, Long count, String projectName) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.count = count;
        this.projectName = projectName;
    }
}
