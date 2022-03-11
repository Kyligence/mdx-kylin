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
import io.kylin.mdx.core.MdxException;
import io.kylin.mdx.web.support.MdxAuthenticator;
import lombok.extern.slf4j.Slf4j;
import mondrian.olap.MondrianProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j(topic = "gateway")
@RequestMapping("api")
@RestController
public class GatewayController {

    private final LicenseManagerImpl licenseManagerImpl;

    @Autowired
    public GatewayController(LicenseManagerImpl licenseManagerImpl) {
        this.licenseManagerImpl = licenseManagerImpl;
    }

    @GetMapping("version")
    @ResponseBody
    public Map<String, String> getVersion() {
        Map<String, String> serverProperties = new HashMap<>(2);
        serverProperties.put("version", licenseManagerImpl.getKiVersion());
        serverProperties.put("timeout", MondrianProperties.instance().QueryTimeout.stringValue());
        return serverProperties;
    }

    @PostMapping("auth")
    @ResponseBody
    public String handleLoginResult(HttpServletRequest request) throws MdxException {
        UserOperResult result = MdxAuthenticator.authenticate(request);
        if (result == null) {
            return UserOperResult.NO_AUTH_OR_FORMAT_ERROR.getCode() + ";" + UserOperResult.NO_AUTH_OR_FORMAT_ERROR.getMessage();
        } else {
            return result.getCode() + ";" + result.getMessage();
        }
    }

}
