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

public enum SyncResult {

    SYNC_SUCCESS("Sync success."),

    SYNC_NOT_AUTHORIZED("Invalid username or password."),

    SYNC_NOT_ADMIN("The user [xxx] is not a system administrator."),

    SYNC_INVALID_PARAMS("Request params must and only contains user and password."),

    INACTIVE_USER("User is disabled in Kylin."),

    EXPIRED_LICENSE("The KYLIN license has expired.");

    private String message;

    SyncResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
