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


package io.kylin.mdx.insight.server.support;

import io.kylin.mdx.insight.core.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author liang.xu
 */
public class MdxLogoutHandler extends HttpStatusReturningLogoutSuccessHandler {

    private final HttpStatus httpStatusToReturn;

    AuthService authService;

    public MdxLogoutHandler(HttpStatus httpStatusToReturn, AuthService authService) {
        Assert.notNull(httpStatusToReturn, "The provided HttpStatus must not be null.");
        this.httpStatusToReturn = httpStatusToReturn;
        this.authService = authService;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        authService.removeSessionCookie(response);
        response.setStatus(this.httpStatusToReturn.value());
        response.getWriter().flush();
    }
}
