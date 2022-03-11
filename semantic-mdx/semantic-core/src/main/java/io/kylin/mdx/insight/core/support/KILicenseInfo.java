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


package io.kylin.mdx.insight.core.support;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KILicenseInfo {

    @JsonProperty("ki_version")
    private String kiVersion;

    @JsonProperty("ki_type")
    private String kiType;

    @JsonProperty("user_limit")
    private Integer userLimit;

    @JsonProperty("live_date_range")
    private String liveDateRange;

    @JsonProperty("commit_id")
    private String commitId;

    @JsonProperty("user_auth_count")
    private int userAuthCount;

    @JsonProperty("analytic_type")
    private String[] analyticTypes;

}
