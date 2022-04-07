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


package io.kylin.mdx.insight.core.meta;

import io.kylin.mdx.insight.common.util.Utils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionInfo {

    private String user;

    private String password;

    private String project;

    private String delegate;

    public ConnectionInfo(ConnectionInfo connInfo) {
        this.user = connInfo.user;
        this.password = connInfo.password;
        this.project = connInfo.project;
        this.delegate = connInfo.delegate;
    }

    public ConnectionInfo(String basicAuthStr, String project) {
        String[] basicAuthArr = Utils.decodeBasicAuth(basicAuthStr);
        this.user = basicAuthArr[0];
        this.password = basicAuthArr[1];
        this.project = project;
    }

    public ConnectionInfo(String basicAuthStr) {
        String[] basicAuthArr = Utils.decodeBasicAuth(basicAuthStr);
        this.user = basicAuthArr[0];
        this.password = basicAuthArr[1];
    }

}
