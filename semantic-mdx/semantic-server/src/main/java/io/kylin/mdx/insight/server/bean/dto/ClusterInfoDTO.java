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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ClusterInfoDTO {

    @JsonProperty("start_at")
    private String startAt;

    @JsonProperty("end_at")
    private String endAt;

    @JsonProperty("log_type")
    private Integer logType;

    @JsonProperty("clusters")
    private List<ClusterDTO> clusterNodes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClusterDTO {
        private String host;
        private String port;
        private String status;

        public ClusterDTO(String hostString, String portString) {
            host = hostString;
            port = portString;
            status = "unknown";
        }
    }
}
