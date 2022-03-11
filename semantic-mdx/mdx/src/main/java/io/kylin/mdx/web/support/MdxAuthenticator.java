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


package io.kylin.mdx.web.support;

import io.kylin.mdx.core.service.KylinAuthenticationService;
import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.constants.ParamConstants;
import io.kylin.mdx.insight.common.util.AESWithECBEncryptor;
import org.apache.commons.lang3.StringUtils;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.support.UserOperResult;
import io.kylin.mdx.ErrorCode;
import io.kylin.mdx.core.MdxConfig;
import io.kylin.mdx.core.MdxException;
import lombok.extern.slf4j.Slf4j;
import mondrian.util.Pair;
import mondrian.xmla.XmlaRequestContext;
import org.olap4j.impl.Base64;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MdxAuthenticator {

    private static final MdxConfig MDX_CONFIG = MdxConfig.getInstance();

    private static final KylinAuthenticationService authenticationService = KylinAuthenticationService.getInstance();

    private static final ConcurrentHashMap<Pair<String, String>, String> mapPwdToUserAndProject = new ConcurrentHashMap<>();

    private static final int SALT_LENGTH = 5;

    private static final String MDXAUTH_KEY = "MDXAUTH";

    private static final String HEADER_NAME_GATEWAY = "GatewayAuth";

    private static final String USER_SESSION_KEY = "currentUser";

    private static final String DISCOVER_TYPE = "discover";

    private MdxAuthenticator() {
    }

    public static String getPassword(Pair<String, String> userProjectPair) {
        return mapPwdToUserAndProject.get(userProjectPair);
    }

    public static UserOperResult authenticate(HttpServletRequest request) throws SemanticException, MdxException {
        Pair<String, String> authPair = getAuthInfo(request);
        if (authPair == null) {
            return null;
        }
        String username = authPair.getLeft();
        username = upperAdminName(username);
        String password = authPair.getRight();
        return authenticationService.authenticate(username, password);
    }

    public static String authenticate(String project, HttpServletRequest request, HttpServletResponse response) throws MdxException {
        XmlaRequestContext context = XmlaRequestContext.getContext();
        String sessionUser = (String) request.getSession().getAttribute(USER_SESSION_KEY);
        String delegateUser = request.getParameter(ParamConstants.EXECUTE_AS_USER_ID);
        if (delegateUser != null) {
            if (Objects.equals(sessionUser, delegateUser)) {
                // 委任自身直接忽略
                delegateUser = null;
            } else {
                context.delegateUser = checkDelegateUser(delegateUser);
            }
        }

        String isDiscover = request.getHeader("Check-Type");
        if (DISCOVER_TYPE.equals(isDiscover)) {
            context.notLogRequest = true;
        }

        Pair<String, String> authPair = getAuthInfo(request);
        if (authPair == null) {
            throw new MdxException("Authorization is required. Please add authorization information in http header.", ErrorCode.MISSING_AUTH_INFO);
        }
        String username = upperAdminName(authPair.getLeft());
        String password = authPair.getRight();
        Pair<String, String> userProjectPair = new Pair<>(username, project);
        boolean validPassword = validPassword(userProjectPair, password);
        if (StringUtils.isNotBlank(sessionUser)
                && sessionUser.equals(username)
                && validPassword) {
            return sessionUser;
        }
        boolean isUserValid = authenticationService.authenticate(username, password, project, delegateUser);
        if (!isUserValid) {
            log.error("Authentication failed! username or password is not valid.");
            throw new MdxException("Authentication failed! username or password is not valid.", ErrorCode.MISSING_AUTH_INFO);
        }
        mapPwdToUserAndProject.put(userProjectPair, password);
        request.getSession().setAttribute(USER_SESSION_KEY, username);
        addSessionAuthCookie(username, password, response);
        return username;
    }

    private static boolean validPassword(Pair<String, String> userProjectPair, String password) {
        String oldPassword = mapPwdToUserAndProject.get(userProjectPair);
        if (StringUtils.isNotBlank(oldPassword) && oldPassword.equals(password)) {
            return true;
        } else {
            XmlaRequestContext.getContext().invalidPassword = true;
            return false;
        }
    }

    private static String checkDelegateUser(String delegateUser) {
        if (!SemanticConfig.getInstance().isEnableQueryWithExecuteAs()) {
            throw new SemanticException(ErrorCode.EXECUTE_PARAMETER_NOT_ENABLED);
        }
        if (StringUtils.isBlank(delegateUser)) {
            throw new SemanticException(ErrorCode.EXECUTE_PARAMETER_CANNOT_EMPTY);
        }
        if (delegateUser.length() > 1024) {
            throw new SemanticException(ErrorCode.EXECUTE_PARAMETER_TOO_LONG);
        }
        return delegateUser;
    }

    private static String upperAdminName(String username) {
        if ("ADMIN".equalsIgnoreCase(username) && MDX_CONFIG.isUpperAdminName()) {
            return username.toUpperCase();
        }
        if (MDX_CONFIG.isUpperUserName()) {
            return username.toUpperCase();
        }
        return username;
    }

    private static void addSessionAuthCookie(String username, String password, HttpServletResponse response) {
        String cookie = MDXAUTH_KEY + "=" +
                Utils.encodeTxt(SALT_LENGTH, username + ":" + AESWithECBEncryptor.encrypt(password)) +
                ";" +
                "Path=/; HttpOnly;";
        response.addHeader("Set-Cookie", cookie);
    }

    private static Pair<String, String> parseAuthInfo(String authorization) {
        String[] basicAuthInfos = authorization.split("\\s");
        if (basicAuthInfos.length < 2) {
            return null;
        }
        String basicAuth = new String(Base64.decode(basicAuthInfos[1]));
        String[] authInfos = basicAuth.split(":", 2);
        if (authInfos.length < 2) {
            return null;
        }
        String username = authInfos[0];
        String password = authInfos[1];
        return new Pair<>(username, password);
    }

    private static Pair<String, String> getAuthInfo(HttpServletRequest request) throws MdxException {
        // use http url query parameter firstly
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        if (username != null && password != null) {
            return new Pair<>(username, password);
        } else {
            // use authorization secondly
            String authorization = request.getHeader("Authorization");
            if (authorization != null) {
                Pair<String, String> auth = parseAuthInfo(authorization);
                if (auth == null) {
                    throw new MdxException("Authorization failed!", ErrorCode.MISSING_AUTH_INFO);
                }
                return auth;
            } else {
                Cookie mdxauthCookie = getSessionAuthCookie(request);
                if (mdxauthCookie != null) {
                    String cookieValue = mdxauthCookie.getValue();
                    if (cookieValue != null && !"".equals(cookieValue)) {
                        try {
                            String decodeTxt = Utils.decodeTxt(SALT_LENGTH, cookieValue);
                            String[] authInfos = decodeTxt.split(":");
                            if (authInfos.length != 3) {
                                throw new MdxException("Cookie authorization failed!", ErrorCode.MISSING_AUTH_INFO);
                            }
                            username = authInfos[0];
                            password = AESWithECBEncryptor.decrypt(authInfos[1]);
                            return new Pair<>(username, password);
                        } catch (PwdDecryptException e) {
                            //TODO
                        }
                    }
                } else {
                    String gatewayAuth = request.getHeader(HEADER_NAME_GATEWAY);
                    if (gatewayAuth != null) {
                        return parseAuthInfo(gatewayAuth);
                    }
                }
            }
        }
        return null;
    }

    private static Cookie getSessionAuthCookie(HttpServletRequest request) {
        Cookie[] cs = request.getCookies();
        if (cs == null) {
            return null;
        }
        return Arrays.stream(cs).
                filter(cookie -> MDXAUTH_KEY.equals(cookie.getName())).findFirst().orElse(null);
    }

}
