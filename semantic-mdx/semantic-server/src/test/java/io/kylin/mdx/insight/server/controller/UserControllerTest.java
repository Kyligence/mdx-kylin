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

import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.AESWithECBEncryptor;
import io.kylin.mdx.insight.core.entity.UserInfo;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.core.manager.ProjectManager;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.core.service.UserService;
import io.kylin.mdx.insight.core.support.UserOperResult;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.ErrorCode;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.*;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class UserControllerTest extends BaseControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private ProjectManager projectManager;

    private final String basicAuth = "Basic YWFhOjEyMzQhQCMkYQ==";

    private static final String CONTROLLER_NAME = "UserController";

    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/kylinmetadata");
    }

    @Test
    public void loginTestNoConfUser() throws SemanticException {
        UserController userController = new UserController(userService, authService, projectManager);
        when(userService.login(basicAuth)).thenReturn(UserOperResult.NO_CONFIG_USER);
        Response<String> response = userController.login(new MockHttpServletResponse(), basicAuth);
        Assert.assertEquals(UserOperResult.NO_CONFIG_USER.getMessage(), response.getData());
    }

    @Test
    public void loginTest() throws SemanticException {
        UserController userController = new UserController(userService, authService, projectManager);

        when(userService.selectConfUser()).thenReturn(new UserInfo("ADMIN"));
        {
            when(userService.login(basicAuth)).thenReturn(UserOperResult.LOGIN_INVALID_USER_PWD);
            Response<String> response = userController.login(new MockHttpServletResponse(), basicAuth);
            Assert.assertTrue(response.getData().contains(ErrorCode.USER_OR_PASSWORD_ERROR.getCode()));
        }
        when(userService.login(basicAuth)).thenReturn(UserOperResult.LOGIN_SUCCESS);
        ConnectionInfo connInfo = new ConnectionInfo(basicAuth);
        {
            when(userService.hasAdminPermission(connInfo)).thenReturn(true);
            Response<String> response = userController.login(new MockHttpServletResponse(), basicAuth);
            Assert.assertEquals(UserOperResult.LOGIN_SUCCESS.getMessage(), response.getData());
        }
        {
            when(userService.hasAdminPermission(connInfo)).thenReturn(false);
            Response<String> response = userController.login(new MockHttpServletResponse(), basicAuth);
            Assert.assertTrue(response.getData().contains(ErrorCode.ACCESS_DENIED.getCode()));
        }
    }

    @Test
    public void logoutTest() {
        UserController userController = new UserController(userService, authService, projectManager);
        Response response = userController.logout((new MockHttpServletResponse()));
        Assert.assertEquals(SemanticConstants.RESP_SUC, response.getData());
    }

    @Test
    public void getCurrentUserAuthInfoTest() throws SemanticException, PwdDecryptException {
        UserController userController = new UserController(userService, authService, projectManager);
        when(authService.getCurrentUser()).thenReturn("ADMIN");
        {
            Response<Map<String, String>> response = userController.getCurrentUserAuthInfo();
            Assert.assertEquals(Integer.valueOf(Response.Status.FAIL.ordinal()), response.getStatus());
        }
        {
            when(userService.selectOne("ADMIN")).thenReturn(new UserInfo("ADMIN", AESWithECBEncryptor.encrypt("KYLIN"), 1));
            ConnectionInfo connectionInfo = ConnectionInfo.builder().user("ADMIN").password("KYLIN").project(null).build();
            when(projectManager.getActualProjectSet(connectionInfo)).thenReturn(new HashSet<>());
            Response<Map<String, String>> response = userController.getCurrentUserAuthInfo();
            Assert.assertEquals(Integer.valueOf(Response.Status.SUCCESS.ordinal()), response.getStatus());
        }
    }

    @Override
    protected String getControllerName() {
        return CONTROLLER_NAME;
    }
}
