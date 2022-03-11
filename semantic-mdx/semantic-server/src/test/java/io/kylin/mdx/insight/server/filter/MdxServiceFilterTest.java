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

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.core.service.UserService;
import io.kylin.mdx.insight.core.support.SpringHolder;
import io.kylin.mdx.insight.core.support.UserOperResult;
import io.kylin.mdx.core.MdxException;
import io.kylin.mdx.web.xmla.XmlaDatasourceManager;
import mondrian.tui.MockHttpServletRequest;
import mondrian.tui.MockHttpServletResponse;
import mondrian.xmla.XmlaRequestContext;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpSession;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MdxServiceFilterTest extends BaseEnvSetting {

    @InjectMocks
    private MdxServiceFilter filter;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private UserService userService;

    @Mock
    private HttpSession session;

    @Mock
    private FilterChain filterChain;

    @Spy
    private MockHttpServletRequest request;

    @Spy
    private MockHttpServletResponse response;

    @Before
    public void prepareContext() {
        new SpringHolder().setApplicationContext(applicationContext);
        when(applicationContext.getBean(UserService.class)).thenReturn(userService);
        when(request.getRequestURI()).thenReturn("http://localhost:7080/mdx/xmla/learn_kylin");
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("currentUser")).thenReturn(null);
        XmlaRequestContext.newContext();
    }

    @After
    public void clearContext() {
        XmlaRequestContext.getContext().clear();
    }

    @Test
    public void testClearCache() {
        when(userService.loginForMDX("ADMIN", "KYLIN", "", null)).thenReturn(UserOperResult.LOGIN_SUCCESS);
        when(request.getHeader("Authorization")).thenReturn("Basic QURNSU46S1lMSU4=");
        when(request.getRequestURI()).thenReturn("http://localhost:7080/mdx/xmla/clearCache");
        filter.doFilter(request, response, filterChain);
        Assert.assertEquals(200, response.getStatusCode());
    }

    @Test
    public void testClearCacheProject() {
        XmlaDatasourceManager.newInstance("src/test/resources/datasource");
        when(userService.loginForMDX("ADMIN", "KYLIN", "learn_kylin", null)).thenReturn(UserOperResult.LOGIN_SUCCESS);
        when(request.getHeader("Authorization")).thenReturn("Basic QURNSU46S1lMSU4=");
        when(request.getRequestURI()).thenReturn("http://localhost:7080/mdx/xmla/learn_kylin/clearCache");
        filter.doFilter(request, response, filterChain);
        Assert.assertEquals(200, response.getStatusCode());
    }

    @Test
    public void testRightPassword() {
        when(userService.loginForMDX("ADMIN", "KYLIN", "learn_kylin", null)).thenReturn(UserOperResult.LOGIN_SUCCESS);
        when(request.getHeader("Authorization")).thenReturn("Basic QURNSU46S1lMSU4=");

        request.setBodyContent("test right password");
        filter.doFilter(request, response, filterChain);
        Assert.assertEquals(200, response.getStatusCode());
    }

    @Test
    public void testRightPasswordWithDelegateUser() {
        when(request.getParameter("EXECUTE_AS_USER_ID")).thenReturn("TEST");
        request.setBodyContent("test right password");
        filter.doFilter(request, response, filterChain);
        Assert.assertEquals(401, response.getStatusCode());
    }

    @Test
    public void testWrongPwd() {
        when(userService.loginForMDX("ADMIN", "KYLIN", "learn_kylin", null)).thenReturn(UserOperResult.LOGIN_INVALID_USER_PWD);
        when(request.getHeader("Authorization")).thenReturn("Basic QURNSU46S1lMSU4=");

        request.setBodyContent("test wrong password");
        filter.doFilter(request, response, filterChain);
        Assert.assertEquals(200, response.getStatusCode());
    }

    @Test
    public void testWithoutPwd() {
        request.setBodyContent("test no password");
        filter.doFilter(request, response, filterChain);
        Assert.assertEquals(401, response.getStatusCode());
        Assert.assertEquals("Basic realm=\"localhost\"", response.getHeader("WWW-Authenticate"));
    }

    @Test
    public void testWrongPwdInStressMode() {
        System.setProperty("insight.mdx.mode.is-stress-test", "true");
        when(userService.loginForMDX("ADMIN", "KYLIN", "learn_kylin", null)).thenReturn(UserOperResult.LOGIN_INVALID_USER_PWD);
        when(request.getHeader("Authorization")).thenReturn("Basic QURNSU46S1lMSU4=");
        filter.doFilter(request, response, filterChain);
        Assert.assertEquals(500, response.getStatusCode());
        System.clearProperty("insight.mdx.mode.is-stress-test");
    }

    @Test
    public void testSessionUserEqualUsername() {
        when(request.getHeader("Authorization")).thenReturn("Basic QURNSU46S1lMSU4=");
        when(session.getAttribute("currentUser")).thenReturn("ADMIN");
        request.setBodyContent("test right password");
        filter.doFilter(request, response, filterChain);
        Assert.assertEquals(200, response.getStatusCode());
    }

    @Test
    public void testSessionUserNotEqualUsername() {
        when(userService.loginForMDX("ADMIN", "KYLIN", "learn_kylin", null)).thenReturn(UserOperResult.LOGIN_SUCCESS);
        when(request.getHeader("Authorization")).thenReturn("Basic QURNSU46S1lMSU4=");
        when(session.getAttribute("currentUser")).thenReturn("TEST");
        request.setBodyContent("test right password");
        filter.doFilter(request, response, filterChain);
        Assert.assertEquals(200, response.getStatusCode());
    }

    @Test
    public void getProjectContext() throws MdxException {
        Assert.assertEquals("/clearCache",
                MdxServiceFilter.getProjectContext("/mdx/xmla//clearCache").getRight());
        Assert.assertEquals("learn_kylin/clearCache",
                MdxServiceFilter.getProjectContext("/mdx/xmla/learn_kylin/clearCache").getRight());
        Assert.assertEquals("clearCache",
                MdxServiceFilter.getProjectContext("/mdx/xmla/clearCache").getRight());
        Assert.assertEquals("learn_kylin",
                MdxServiceFilter.getProjectContext("/mdx/xmla/learn_kylin").getRight());
        Assert.assertEquals("learn_kylin",
                MdxServiceFilter.getProjectContext("/mdx/xmla_server/learn_kylin").getRight());
        boolean expect;
        try {
            MdxServiceFilter.getProjectContext("/mdx/xmla/");
            expect = false;
        } catch (MdxException e) {
            expect = true;
        }
        Assert.assertTrue(expect);
    }

    @Test
    public void checkClearFlag() {
        Assert.assertEquals(new ImmutableTriple<>("", true, true), MdxServiceFilter.checkClearFlag("/clearCache"));
        Assert.assertEquals(new ImmutableTriple<>("learn_kylin", true, false), MdxServiceFilter.checkClearFlag("learn_kylin/clearCache"));
        Assert.assertEquals(new ImmutableTriple<>("", true, false), MdxServiceFilter.checkClearFlag("clearCache"));
        Assert.assertEquals(new ImmutableTriple<>("learn_kylin", false, false), MdxServiceFilter.checkClearFlag("learn_kylin"));
    }

}
