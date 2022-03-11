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


package io.kylin.mdx.insight.common;

import io.kylin.mdx.ErrorCode;
import io.kylin.mdx.ErrorCodeSupplier;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class SemanticException extends RuntimeException implements ErrorCodeSupplier {

    @Getter
    @Setter
    private String errorMsg;

    @Getter
    private ErrorCode errorCode;

    public SemanticException() {
    }

    public SemanticException(Throwable e) {
        super(e);
    }

    // 标准的异常抛出

    public SemanticException(ErrorCode errorCode) {
        super(errorCode.getErrorMsg());
        this.errorMsg = getMessage();
        this.errorCode = errorCode;
    }

    public SemanticException(ErrorCode errorCode, String... parameters) {
        super(errorCode.getErrorMsg(parameters));
        this.errorMsg = getMessage();
        this.errorCode = errorCode;
    }

    public SemanticException(ErrorCode errorCode, Throwable t, String... parameters) {
        super(errorCode.getErrorMsg(parameters));
        this.errorMsg = getMessage();
        this.errorCode = errorCode;
    }

    // 兼容旧的异常处理

    public SemanticException(String errorMsg) {
        super(errorMsg);
        this.errorMsg = errorMsg;
    }

    public SemanticException(String msg, Throwable e) {
        super(msg, e);
        this.errorMsg = msg;
    }

    public SemanticException(Throwable e, ErrorCode errorCode) {
        super(e);
        this.errorCode = errorCode;
    }

    public SemanticException(String errorMsg, ErrorCode errorCode) {
        super(errorMsg);
        this.errorMsg = errorMsg;
        this.errorCode = errorCode;
    }

    public SemanticException(String msg, Throwable e, ErrorCode errorCode) {
        super(msg, e);
        this.errorMsg = msg;
        this.errorCode = errorCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getErrorCode(), this.getErrorMsg());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof SemanticException &&
                this.getErrorCode().equals(((SemanticException) obj).getErrorCode()) &&
                this.getErrorMsg().equals(((SemanticException) obj).getErrorMsg());
    }
}
