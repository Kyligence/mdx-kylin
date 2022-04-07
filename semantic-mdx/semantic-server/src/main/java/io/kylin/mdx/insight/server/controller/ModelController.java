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
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.model.semantic.SemanticModel;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.engine.service.ModelServiceImpl;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.bean.vo.ModelAndLastModifyVO;
import io.kylin.mdx.insight.server.support.Permission;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("api")
public class ModelController {

    private final ModelServiceImpl modelServiceImpl;

    @Autowired
    private AuthService authService;

    @Autowired
    public ModelController(ModelServiceImpl modelServiceImpl) {
        this.modelServiceImpl = modelServiceImpl;
    }

    /**
     * health检测接口
     */
    @GetMapping("/health")
    public Response<String> checkHealth() {
        return new Response<String>(Response.Status.SUCCESS).data("health");
    }

    /**
     * 查询某个项目下model列表
     */
    @GetMapping("/model/list/{project}")
    @Permission
    public Response<List<ModelAndLastModifyVO>> getModelsByProject(
            @PathVariable("project") String project) throws SemanticException {
        log.info("user:{} enter API:GET [/api/model/list/{}]", authService.getCurrentUser(), project);
        List<KylinGenericModel> models = modelServiceImpl.getModelsByProject(project);
        List<ModelAndLastModifyVO> vos = new LinkedList<>();
        if (CollectionUtils.isNotEmpty(models)) {
            for (KylinGenericModel genericModel : models) {
                vos.add(new ModelAndLastModifyVO(genericModel.getModelName(), genericModel.getLastModified()));
            }
        }
        return new Response<>(vos);
    }

    /**
     * 查询某个项目下某个model的table列表,不包含主表
     */
    @GetMapping("/dimtable/{project}/{model}")
    @Permission
    public Response<List<String>> getDimtablesByModel(@PathVariable("project") @NotNull String project,
                                                      @PathVariable("model") @NotNull String model) throws SemanticException {
        log.info("user:{} enter API:GET [/api/dimtable/{}/{}]", authService.getCurrentUser(), project, model);
        return new Response<>(modelServiceImpl.getDimtablesByModel(project, model));
    }

    /**
     * 查询某个项目下某个model的table、measure详情
     */
    @GetMapping("/model/detail/{project}/{model}")
    @Permission
    public Response<SemanticModel> getSemanticModel(@PathVariable("project") @NotNull String project,
                                                    @PathVariable("model") @NotNull String model) throws SemanticException {

        log.info("user:{} enter API:GET [/api/model/detail/{}/{}]", authService.getCurrentUser(), project, model);
        SemanticModel semanticModelDetail = modelServiceImpl.getSemanticModelDetail(project, model);
        if (semanticModelDetail != null) {
            semanticModelDetail.setJoinTables(null);
        }
        return new Response<>(semanticModelDetail);
    }

}
