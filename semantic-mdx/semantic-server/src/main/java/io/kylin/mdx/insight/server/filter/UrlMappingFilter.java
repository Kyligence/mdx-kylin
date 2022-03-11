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


package io.kylin.mdx.insight.server.filter;


import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.util.AESWithECBEncryptor;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.dao.UserInfoMapper;
import io.kylin.mdx.insight.core.entity.UserInfo;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.core.service.UserService;
import io.kylin.mdx.insight.core.support.SpringHolder;
import io.kylin.mdx.web.support.HttpResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * change URL from "/*" to "/" when URL don't start with "/api"、"/mdx/xmla/" or fronted file
 **/
@Slf4j
public class UrlMappingFilter implements Filter {

    private static final String BASE_URL = "{{BASE_URL}}";

    private static final SemanticConfig CONFIG = SemanticConfig.getInstance();

    private final UserService userService;

    private final AuthService authService;

    public UrlMappingFilter(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String contextPath = Utils.endWithoutSlash(CONFIG.getContextPath());
        String path = httpRequest.getRequestURI();
        if (path.startsWith(contextPath + "/api")
                || path.startsWith(contextPath + CONFIG.getMdxServletPath())
                || path.startsWith(contextPath + CONFIG.getMdxGatewayPath())
                || path.startsWith(contextPath + "/static")
                || path.startsWith(contextPath + "/locale")
                || path.startsWith(contextPath + "/asset-manifest.json")
                || path.startsWith(contextPath + "/favicon.png")
                || path.startsWith(contextPath + "/manifest.json")
                || path.startsWith(contextPath + "/robots.txt")
                || path.startsWith(contextPath + "/oauth2")
                || path.startsWith(contextPath + CONFIG.getManagementWebBasePath() + "/prometheus")
                || path.startsWith(contextPath + "/service-worker.js")) {
            /* include xmla, xmla_server, api, static resources. */
            chain.doFilter(request, response);
        } else {
            authenticate(response);
            if (!CONFIG.isEnableAAD() || StringUtils.isNotBlank(authService.getCurrentUser())) {
                replaceBaseUrl(httpRequest, response);
            } else {
                ((HttpServletResponse) response).sendRedirect(CONFIG.getAADLoginUrl());
            }
        }
    }

    private void authenticate(ServletResponse response) {
        UserInfoMapper userInfoMapper = SpringHolder.getBean(UserInfoMapper.class);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = "";
            String password = "";
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof DefaultOidcUser) {
                OidcIdToken oidcIdToken = ((DefaultOidcUser) principal).getIdToken();
                password = oidcIdToken.getTokenValue();
                username = (String) ((DefaultOidcUser) principal).getAttributes().get("preferred_username");
                UserInfo userInfo = userService.selectOne(username.toUpperCase());
                if (userInfo == null) {
                    userInfo = new UserInfo(username, AESWithECBEncryptor.encrypt(password), null);
                    userService.insertSelective(userInfo);
                } else {
                    userInfo.setPassword(AESWithECBEncryptor.encrypt(password));
                    userService.updateConfUsr(userInfo);
                }
            }
            if (StringUtils.isNotBlank(username)) {
                authService.addSessionCookie(username,  password, (HttpServletResponse) response);
            }
        }
    }

    private void replaceBaseUrl(HttpServletRequest httpRequest, ServletResponse response)
            throws IOException, ServletException {
        HttpResponseWrapper responseWrapper = new HttpResponseWrapper((HttpServletResponse) response);
        httpRequest.getRequestDispatcher("/").forward(httpRequest, responseWrapper);
        byte[] data = responseWrapper.getContent();
        String content = new String(data);
        if (content.contains(BASE_URL)) {
            data = content.replace(BASE_URL, CONFIG.getContextPath()).getBytes();
            response.setContentLength(data.length);
        }
        response.getOutputStream().write(data);
    }

}
