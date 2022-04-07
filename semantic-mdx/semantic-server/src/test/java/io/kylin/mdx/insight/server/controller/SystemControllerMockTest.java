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

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.BeanUtils;
import io.kylin.mdx.insight.core.entity.GroupInfo;
import io.kylin.mdx.insight.core.entity.UserInfo;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.core.service.UserService;
import io.kylin.mdx.insight.core.support.KILicenseInfo;
import io.kylin.mdx.insight.core.support.UserOperResult;
import io.kylin.mdx.insight.server.bean.Page;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.bean.dto.GroupInfoDTO;
import io.kylin.mdx.insight.server.bean.dto.LicenseUpdateDTO;
import io.kylin.mdx.insight.server.bean.dto.UserInfoDTO;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SystemControllerMockTest extends BaseControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/kylinmetadata");
    }

    private static final String CONTROLLER_NAME = "systemcontroller";

    @Override
    protected String getControllerName() {
        return CONTROLLER_NAME.toLowerCase();
    }

    @Test
    public void selectUserInfoTest() {
        List<UserInfo> userInfos = new ArrayList<>();
        UserInfo userInfo = new UserInfo();
        userInfos.add(userInfo);

        when(userService.selectAll()).thenReturn(userInfos);
        when(userService.selectOne("ADMIN")).thenReturn(userInfo);
        when(authService.getCurrentUser()).thenReturn("ADMIN");

        SystemController systemController = new SystemController();
        BeanUtils.setField(systemController, UserService.class, userService);
        BeanUtils.setField(systemController, AuthService.class, authService);

        Response response = systemController.selectUserInfo(null, null);
        Assert.assertEquals(userInfos, response.getData());

        Response response2 = systemController.selectUserInfo("ADMIN", null);
        Assert.assertEquals(userInfo, response2.getData());
    }

    @Test
    public void getUsersTest() throws SemanticException {
        List<UserInfo> userInfos = new LinkedList<>();

        when(userService.getAllUsers(1, 100)).thenReturn(userInfos);
        when(authService.getCurrentUser()).thenReturn("ADMIN");

        List<UserInfoDTO> userInfoDTOs = new LinkedList<>();
        userInfos.forEach(userInfo -> {
            UserInfoDTO userInfoDTO = new UserInfoDTO(userInfo);
            userInfoDTOs.add(userInfoDTO);
        });
        Page<UserInfoDTO> userInfoDTOPage = new Page<>(userInfoDTOs);
        userInfoDTOPage.setPageInfo(userInfos);

        SystemController systemController = new SystemController();
        BeanUtils.setField(systemController, UserService.class, userService);
        BeanUtils.setField(systemController, AuthService.class, authService);

        Response response = systemController.getUsers(0, 100, null);
        Assert.assertEquals(userInfoDTOPage, response.getData());
    }

    @Test
    public void getGroups() {
        List<GroupInfo> groupInfos = new LinkedList<>();
        groupInfos.add(new GroupInfo(0, "ALL_USERS"));
        when(userService.getAllGroup(0, 100)).thenReturn(groupInfos);
        when(authService.getCurrentUser()).thenReturn("ADMIN");

        SystemController systemController = new SystemController();
        BeanUtils.setField(systemController, UserService.class, userService);
        BeanUtils.setField(systemController, AuthService.class, authService);

        Response<Page<GroupInfoDTO>> response = systemController.getGroups(0, 100, null);
        Assert.assertEquals("ALL_USERS", response.getData().getList().get(0).getName());
    }

}
