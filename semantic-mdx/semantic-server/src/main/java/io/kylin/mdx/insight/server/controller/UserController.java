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

import io.kylin.mdx.ErrorCode;
import io.kylin.mdx.ExceptionUtils;
import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.UserInfo;
import io.kylin.mdx.insight.core.manager.ProjectManager;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.core.service.UserService;
import io.kylin.mdx.insight.core.support.UserOperResult;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.bean.dto.UserInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("api")
public class UserController {

    private final UserService userService;

    private final AuthService authService;

    private final ProjectManager projectManager;


    @Autowired
    public UserController(UserService userService, AuthService authService, ProjectManager projectManager) {
        this.userService = userService;
        this.authService = authService;
        this.projectManager = projectManager;
    }

    /**
     * 用户登录
     */
    @GetMapping("/login")
    public Response<String> login(HttpServletResponse httpServletResponse,
                                  @RequestHeader(SemanticConstants.BASIC_AUTH_HEADER_KEY) String basicAuth)
            throws SemanticException {
        log.info("user:{} enter API:GET [/api/login]", authService.getCurrentUser());
        ConnectionInfo connInfo = new ConnectionInfo(basicAuth);
        if (userService.selectConfUser() == null) {
            userService.systemAdminCheck(connInfo.getUser(), connInfo.getPassword());
        }

        UserOperResult opResult = userService.login(basicAuth);
        httpServletResponse.setStatus(opResult.getHttpCode());
        if (opResult != UserOperResult.LOGIN_SUCCESS) {
            log.info("[login fail] code:{}, msg:{}", opResult.getCode(), opResult.getMessage());
            return new Response<String>(opResult.getCode()).data(opResult.getMessage());
        }

        boolean accessFlag = userService.hasAdminPermission(connInfo);
        if (accessFlag) {
            authService.addSessionCookie(connInfo.getUser(), connInfo.getPassword(), httpServletResponse);
            return new Response<String>(opResult.getCode()).data(opResult.getMessage());
        } else {
            String msg = ExceptionUtils.getFormattedErrorMsg(SemanticConstants.ACCESS_DENIED, ErrorCode.ACCESS_DENIED);
            return new Response<String>(Response.Status.FAIL).data(msg);
        }
    }

    /**
     * 用户退出
     */
    @GetMapping("/logout")
    public Response<String> logout(
            HttpServletResponse httpServletResponse) {
        log.info("user:{} enter API:GET [/api/logout]", authService.getCurrentUser());
        authService.removeSessionCookie(httpServletResponse);
        return new Response<String>(Response.Status.SUCCESS).data(SemanticConstants.RESP_SUC);
    }

    /**
     * 获取当前用户
     */
    @GetMapping("/current_user")
    public Response<UserInfoDTO> getCurrentUser(HttpServletRequest request, HttpServletResponse response) {
        log.info("user:{} enter API:GET [/api/current_user]", authService.getCurrentUser());
        Pair<String, String> userAndPassword;
        UserInfo userInfo = null;
        try {
            userAndPassword = authService.getUserInfoFromRequest(request);
            authService.loginMdx(userAndPassword.getLeft(), userAndPassword.getRight(), null, null);
            userInfo = userService.selectOne(userAndPassword.getLeft());
        } catch (Exception e) {
            authService.removeSessionCookie(response);
        }

        if (userInfo == null) {
            return new Response<UserInfoDTO>(Response.Status.SUCCESS).data(null);
        }
        UserInfoDTO userInfoDTO = new UserInfoDTO(userInfo);
        return new Response<UserInfoDTO>(Response.Status.SUCCESS).data(userInfoDTO);
    }

    /**
     * 获取当前用户对项目的权限信息
     */
    @GetMapping("/user/permission")
    public Response<Map<String, String>> getCurrentUserAuthInfo() throws PwdDecryptException, SemanticException {
        log.info("user:{} enter API:GET [/api/user/permission]", authService.getCurrentUser());
        String user = authService.getCurrentUser();
        UserInfo userInfo = userService.selectOne(user);
        if (userInfo == null) {
            return new Response<Map<String, String>>(Response.Status.FAIL).data(null);
        }
        String basicAuth = Utils.buildBasicAuth(user, userInfo.getDecryptedPassword());
        Map<String, String> userAccessProjects = projectManager.getUserAccessProjects(new ConnectionInfo(basicAuth));
        return new Response<Map<String, String>>(Response.Status.SUCCESS).data(userAccessProjects);
    }

}
