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

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WebUtils {

    private static final String LOGIN_USER_KEY = "user";
    private static final String LOGIN_PWD_KEY = "pwd";

    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert attributes != null;
        return attributes.getRequest();
    }

    public static HttpServletResponse getResponse() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert attributes != null;
        return attributes.getResponse();
    }

    public static void putRequestAttribute(String name, Object o) {
        HttpServletRequest request = getRequest();
        request.setAttribute(name, o);
    }

    public static void setCurrentLoginUser(String username, String pwd) {
        HttpServletRequest request = getRequest();
        request.setAttribute(LOGIN_USER_KEY, username);
        request.setAttribute(LOGIN_PWD_KEY, pwd);
    }

    public static String getCurrentLoginUser() {
        HttpServletRequest request = getRequest();
        return (String) request.getAttribute(LOGIN_USER_KEY);
    }

    public static String getCurrentUserPwd() {
        HttpServletRequest request = getRequest();
        return (String) request.getAttribute(LOGIN_PWD_KEY);
    }
}
