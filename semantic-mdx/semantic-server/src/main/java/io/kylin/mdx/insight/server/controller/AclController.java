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


import io.kylin.mdx.insight.core.entity.RoleType;
import io.kylin.mdx.insight.core.model.acl.AclDataset;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.facade.AclFacade;
import io.kylin.mdx.insight.server.support.Permission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;

@Slf4j
@Validated
@RestController
@RequestMapping("api")
public class AclController {

    private final AclFacade aclFacade;

    @Autowired
    public AclController(AclFacade aclFacade) {
        this.aclFacade = aclFacade;
    }

    /**
     * 查询所有项目名
     */
    @PostMapping("acl/{project}")
    @Permission
    public Response<AclDataset> getAcl(@PathVariable("project") @NotBlank String project,
                                       @RequestParam("type") @NotBlank String type,
                                       @RequestParam("name") @NotBlank String name,
                                       @RequestBody @Validated AclDataset dataset) {
        try {
            RoleType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal argument about type : " + type);
        }
        AclDataset result = aclFacade.preparePermission(project, type, name, dataset);
        return new Response<>(result);
    }

}
