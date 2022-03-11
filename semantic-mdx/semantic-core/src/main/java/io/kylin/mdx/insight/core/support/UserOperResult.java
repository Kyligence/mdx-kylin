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

import io.kylin.mdx.ErrorCode;
import io.kylin.mdx.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

public enum UserOperResult {

    /**
     * user login success
     */
    LOGIN_SUCCESS(0, HttpStatus.SC_OK, "Login success."),

    LOGIN_NOT_AUTHORIZED(1001, HttpStatus.SC_UNAUTHORIZED, "User hasn't been authorized in MDX for Kylin.", ErrorCode.INACTIVE_USER),

    LOGIN_INVALID_USER_PWD(1002, HttpStatus.SC_UNAUTHORIZED, "Invalid username or password.", ErrorCode.USER_OR_PASSWORD_ERROR),

    LOGIN_USER_DISABLED(1003, HttpStatus.SC_UNAUTHORIZED, "User is disabled in Kylin.", ErrorCode.AUTH_FAILED),

    KYLIN_LOGIN_LICENSE_OUTDATED(1004, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Kylin License has expired.", ErrorCode.EXPIRED_LICENSE),

    LOGIN_UNKNOWN_ERROR(1005, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unknown exception."),

    USER_LIMIT_EXCEED(1006, HttpStatus.SC_UNAUTHORIZED, "User number reaches MDX for Kylin License's max limited number.", ErrorCode.LICENSE_USER_EXCEED_LIMIT),

    KYLIN_MDX_LOGIN_LICENSE_OUTDATED(1007, HttpStatus.SC_UNAUTHORIZED, "MDX for Kylin License has expired.", ErrorCode.EXPIRED_LICENSE),

    USER_NO_ACCESS_PROJECT(1008, HttpStatus.SC_UNAUTHORIZED, "[MDX-04020003] Access denied, This user has no access to this project.", ErrorCode.WITHOUT_PROJECT_PERMISSION),

    NO_CONFIG_USER(1009, HttpStatus.SC_UNAUTHORIZED, "The system lacks user information for syncing Kylin tasks. Please log in to MDX for Kylin with a Kylin system administrator account to configure connection information.", ErrorCode.SYSTEM_CONFIG_NO_ADMIN_USER),

    NO_AUTH_OR_FORMAT_ERROR(1010, HttpStatus.SC_UNAUTHORIZED, "No auth info or invalid.", ErrorCode.MISSING_AUTH_INFO),

    USER_LOCKED(1011, HttpStatus.SC_BAD_REQUEST, "User is locked.", ErrorCode.USER_LOCKED),

    UNKNOWN_ERROR(1012, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unknown exception."),

    USER_UPDATE_AUTH_SUCCESS(0, HttpStatus.SC_OK, "Update user auth success.");

    private final int code;

    private String message;

    private final int httpCode;

    private ErrorCode errorCode;

    UserOperResult(int code, int httpStatus, String message) {
        this.code = code;
        this.message = message;
        this.httpCode = httpStatus;
    }

    UserOperResult(int code, int httpStatus, String message, ErrorCode errorCode) {
        this.code = code;
        this.message = message;
        this.httpCode = httpStatus;
        this.errorCode = errorCode;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        if (errorCode != null) {
            return ExceptionUtils.getFormattedErrorMsg(message, errorCode);
        }
        return message;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public void setMessage(String message) {
        if (StringUtils.isNotBlank(message)) {
            this.message = message;
        }
    }

}
