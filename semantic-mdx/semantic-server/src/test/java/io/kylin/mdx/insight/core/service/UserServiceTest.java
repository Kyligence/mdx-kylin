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


package io.kylin.mdx.insight.core.service;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.http.Response;
import io.kylin.mdx.insight.core.dao.UserInfoMapper;
import io.kylin.mdx.insight.core.entity.KylinPermission;
import io.kylin.mdx.insight.core.entity.UserInfo;
import io.kylin.mdx.insight.engine.manager.LicenseManagerImpl;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.core.meta.SemanticAdapter;
import io.kylin.mdx.insight.core.support.KILicenseInfo;
import io.kylin.mdx.insight.core.support.UserOperResult;
import io.kylin.mdx.insight.engine.service.UserServiceImpl;
import io.kylin.mdx.ErrorCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.Silent.class)
public class UserServiceTest extends BaseEnvSetting {

    private static final String BASIC_AUTH = "Basic YWFhOjEyMzQhQCMkYQ==";

    public static final String KE_USER_LOCKED = "{ \"code\" : \"999\",\n" +
            "\"data\" : null,\n" +
            "\"msg\" : \"User xu is locked, please try again after 16 seconds.\" }";

    @Mock
    private UserInfoMapper userInfoMapper;

    @Mock
    private LicenseManagerImpl licenseManagerImpl;

    @Mock
    private SemanticAdapter semanticAdapter;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void loginForMDX() {
        System.setProperty("converter.mock", "true");
        userService.loginForMDX("ADMIN", "KYLIN@123", "", "");

        try {
            userService.loginForMDX("ADMIN", "KYLIN@123", "automation_test", "WANGHUI");
            Assert.fail();
        } catch (SemanticException e) {
            Assert.assertEquals(ErrorCode.EXECUTE_PARAMETER_NOT_FOUND, e.getErrorCode());
        }

        when(userInfoMapper.selectByUserName("WANGHUI")).thenReturn(new UserInfo("wanghui"));
        UserOperResult opResult = userService.loginForMDX("ADMIN", "KYLIN@123", "automation_test", "wanghui");
        Assert.assertEquals(UserOperResult.LOGIN_SUCCESS, opResult);

        System.clearProperty("converter.mock");
    }

    @Test
    public void hasAdminPermission() {
        userService.setSemanticAdapter(semanticAdapter);

        ConnectionInfo connInfo = ConnectionInfo.builder().user("ADMIN").password("KYLIN").project(null).build();
        {
            when(semanticAdapter.getUserAuthority(connInfo)).thenReturn(Collections.singletonList(SemanticConstants.ROLE_ADMIN));
            Assert.assertTrue(userService.hasAdminPermission(connInfo));
            Assert.assertTrue(userService.hasAdminPermission(connInfo, true));
        }
        {
            when(semanticAdapter.getUserAuthority(connInfo)).thenReturn(Collections.emptyList());
            when(semanticAdapter.getActualProjectSet(connInfo)).thenReturn(Collections.singleton("learn_kylin"));
            ConnectionInfo newConnInfo = new ConnectionInfo(connInfo);
            newConnInfo.setProject("learn_kylin");
            when(semanticAdapter.getAccessInfo(newConnInfo)).thenReturn(KylinPermission.ADMINISTRATION.name());
            Assert.assertTrue(userService.hasAdminPermission(connInfo));
            Assert.assertFalse(userService.hasAdminPermission(connInfo, true));
            when(semanticAdapter.getAccessInfo(newConnInfo)).thenReturn(KylinPermission.READ.name());
            Assert.assertFalse(userService.hasAdminPermission(connInfo));
        }
    }

    @Test
    public void changeUserLicense() {
        userService.setSemanticAdapter(semanticAdapter);

        when(userInfoMapper.selectLicenseNumWithLock()).thenReturn(2);
        when(licenseManagerImpl.getUserLimit()).thenReturn(3);
        UserOperResult result1 = userService.changeUserLicense("ADMIN", 1);
        Assert.assertEquals(UserOperResult.USER_UPDATE_AUTH_SUCCESS, result1);

        when(licenseManagerImpl.getUserLimit()).thenReturn(2);
        UserOperResult result2 = userService.changeUserLicense("ADMIN", 1);
        Assert.assertEquals(UserOperResult.USER_LIMIT_EXCEED, result2);

        when(userInfoMapper.updateLicenseAuthByUsername("ADMIN", 0)).thenReturn(1);
        UserOperResult result3 = userService.changeUserLicense("ADMIN", 0);
        Assert.assertEquals(UserOperResult.USER_UPDATE_AUTH_SUCCESS, result3);
    }

    @Test
    public void getKiLicenseInfo() {
        when(licenseManagerImpl.getUserLimit()).thenReturn(1);
        when(licenseManagerImpl.getKiType()).thenReturn("evaluation");
        when(licenseManagerImpl.getKiVersion()).thenReturn("0.26.0");
        when(licenseManagerImpl.getCommitId()).thenReturn("234DF23254");
        when(licenseManagerImpl.getLiveDateRange()).thenReturn("2012-01-01,2014-01-01");

        KILicenseInfo kiLicenseInfo = userService.getKiLicenseInfo();

        Assert.assertEquals(new Integer(1), kiLicenseInfo.getUserLimit());
        Assert.assertEquals("evaluation", kiLicenseInfo.getKiType());
        Assert.assertEquals("0.26.0", kiLicenseInfo.getKiVersion());
        Assert.assertEquals("234DF23254", kiLicenseInfo.getCommitId());
        Assert.assertEquals("2012-01-01,2014-01-01", kiLicenseInfo.getLiveDateRange());
    }

    @Test
    public void login() {
        userService.setSemanticAdapter(semanticAdapter);

        UserOperResult kiLicenseOutdatedResult = userService.login(BASIC_AUTH);
        Assert.assertEquals(UserOperResult.KYLIN_MDX_LOGIN_LICENSE_OUTDATED, kiLicenseOutdatedResult);

        UserInfo userInfo = new UserInfo();
        userInfo.setUsername("AAA");
        Response okResponse = new Response(HttpStatus.SC_OK, "");
        userInfo.setLicenseAuth(UserServiceImpl.LicenseAuth.AUTHORIZED.ordinal());
        userInfo.setLoginCount(1);
        when(userInfoMapper.selectByUserName("AAA")).thenReturn(userInfo);
        String actualBasicAuth = StringUtils.substringAfter(BASIC_AUTH, "Basic ");
        when(semanticAdapter.authentication(actualBasicAuth)).thenReturn(okResponse);
        UserOperResult okResult = userService.login(BASIC_AUTH);
        Assert.assertEquals(UserOperResult.LOGIN_SUCCESS, okResult);

        userInfo.setLicenseAuth(UserServiceImpl.LicenseAuth.AUTHORIZED.ordinal());
        when(semanticAdapter.authentication(actualBasicAuth)).thenThrow(new SemanticException(ErrorCode.USER_DISABLE, ""),
                new SemanticException(ErrorCode.EXPIRED_LICENSE, ""),
                new SemanticException(ErrorCode.USER_OR_PASSWORD_ERROR, ""),
                new SemanticException(ErrorCode.USER_LOCKED, ""));
        UserOperResult unAuthResult = userService.login(BASIC_AUTH);
        Assert.assertEquals(UserOperResult.LOGIN_USER_DISABLED, unAuthResult);

        userInfo.setLicenseAuth(UserServiceImpl.LicenseAuth.AUTHORIZED.ordinal());
        UserOperResult outDatedResult = userService.login(BASIC_AUTH);
        Assert.assertEquals(UserOperResult.KYLIN_LOGIN_LICENSE_OUTDATED, outDatedResult);

        UserOperResult invalidResult = userService.login(BASIC_AUTH);
        Assert.assertEquals(UserOperResult.LOGIN_INVALID_USER_PWD, invalidResult);

        // HTTP状态码：400， 用户被锁
        userInfo.setLicenseAuth(UserServiceImpl.LicenseAuth.AUTHORIZED.ordinal());
        UserOperResult lockUserResult = userService.login(BASIC_AUTH);
        Assert.assertEquals(UserOperResult.USER_LOCKED, lockUserResult);
    }

}
