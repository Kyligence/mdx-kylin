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

import io.kylin.mdx.insight.engine.manager.LicenseManagerImpl;
import io.kylin.mdx.insight.core.support.UserOperResult;
import mondrian.tui.MockHttpServletRequest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;


@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
public class GatewayControllerTest {

    @Mock
    private LicenseManagerImpl licenseManagerImpl;

    @BeforeClass
    public static void setup() {
        System.setProperty("MDX_HOME", "../mdx/src/test/resources/");
    }

    @Test
    public void handleLoginResult() throws Exception {
        GatewayController gatewayController = new GatewayController(licenseManagerImpl);
        HttpServletRequest request = new MockHttpServletRequest();
        String result1 = gatewayController.handleLoginResult(request);
        String expect1 = UserOperResult.NO_AUTH_OR_FORMAT_ERROR.getCode() + ";" + UserOperResult.NO_AUTH_OR_FORMAT_ERROR.getMessage();
        Assert.assertEquals(expect1, result1);
    }

    @Test
    public void getVersionTest() throws Exception {
        GatewayController gatewayController = new GatewayController(licenseManagerImpl);
        Map<String, String> result=  gatewayController.getVersion();
        Assert.assertEquals(result.size(), 2);
    }

    @AfterClass
    public static void clear() {
    }

}
