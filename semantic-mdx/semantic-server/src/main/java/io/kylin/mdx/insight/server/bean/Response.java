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


package io.kylin.mdx.insight.server.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class Response<T> {

    private Integer status;

    @JsonInclude
    private T data;

    private String errorMesg;

    public Response() {
        this.status = Status.SUCCESS.ordinal();
    }

    public Response(T data) {
        this.status = Status.SUCCESS.ordinal();
        this.data = data;
    }

    public Response(int status) {
        this.status = status;
    }

    public Response(Status status) {
        this.status = status.ordinal();
    }

    public Response(Status status, T data) {
        this.status = status.ordinal();
        this.data = data;
    }

    public Response<T> data(T data) {
        this.data = data;
        return this;
    }

    public Response<T> errorMsg(String errorMesg) {
        this.errorMesg = errorMesg;
        return this;
    }

    public enum Status {

        SUCCESS,

        FAIL

    }

}
