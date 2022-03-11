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

import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.support.Permission;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Validated
@RestController
@RequestMapping("api")
public class IndicatorController {

    private static final String INDICATOR_FILE = "indicators.json";

    @Autowired
    private AuthService authService;

    @GetMapping("/indicators")
    @Permission
    public Response<String> getIndicator(@RequestParam("pageNum") @Min(0) Integer pageNum,
                                         @RequestParam("pageSize") @NotNull Integer pageSize,
                                         @RequestParam(value = "category", required = false) String category) throws SemanticException {
        log.info("user:{} enter API:GET [/api/indicators], pageNum:{}, pageSize:{}", authService.getCurrentUser(), pageNum, pageSize);
        String indicatorPath = SemanticConfig.getInstance().getPropertiesDirPath();
        File indicatorFile = new File(indicatorPath, INDICATOR_FILE);
        String indicatorString;
        try (InputStream input = new FileInputStream(indicatorFile)) {
            indicatorString = IOUtils.toString(input);
        } catch (IOException e) {
            log.error("Read indicators.json file error", e);
            indicatorString = "";
        }
        return new Response<String>(Response.Status.SUCCESS).data(indicatorString);
    }

}
