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


package io.kylin.mdx.insight.server.controller;

import com.google.common.base.Throwables;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.SemanticOmitDetailException;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.lang.reflect.UndeclaredThrowableException;
import static io.kylin.mdx.ErrorCode.UPLOAD_EXCEL_FILE_OVERSIZE;

@Slf4j
@ControllerAdvice
public class ExceptionCatchAdvice {

    private static final String NL = System.getProperty("line.separator");

    @ExceptionHandler(SemanticException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<String> handle(SemanticException e) {
        String errorMsg = ExceptionUtils.getFormattedErrorMsg(e, e.getErrorCode());
        log.error(errorMsg, e);
        return getResponse(errorMsg, e, true);
    }

    @ExceptionHandler(SemanticOmitDetailException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<String> handle(SemanticOmitDetailException e) {
        String errorMsg = ExceptionUtils.getFormattedErrorMsg(e, e.getErrorCode());
        log.error(errorMsg, e);
        return getResponse(errorMsg, e, false);
    }

    @ExceptionHandler(value = UndeclaredThrowableException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<String> othersErrorHandler(UndeclaredThrowableException e) {
        String errorMsg = ExceptionUtils.getFormattedErrorMsg(e, null);
        log.error(errorMsg, e);
        return getResponse(errorMsg, e, true);
    }

    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<String> othersErrorHandler(MaxUploadSizeExceededException e) {
        String errorMsg = ExceptionUtils.getFormattedErrorMsg(e, UPLOAD_EXCEL_FILE_OVERSIZE);
        log.error(errorMsg, e);
        return getResponse(errorMsg, e, false);
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<String> othersErrorHandler(Exception e) {
        String errorMsg = ExceptionUtils.getFormattedErrorMsg(e, null);
        log.error(errorMsg, e);
        return getResponse(errorMsg, e, true);
    }

    private Response<String> getResponse(String msg, Throwable e, boolean detail) {
        if (detail) {
            msg = msg + NL + Throwables.getStackTraceAsString(e);
        }
        return new Response<String>(Response.Status.FAIL)
                .data(SemanticConstants.RESP_FAIL)
                .errorMsg(msg);
    }
}
