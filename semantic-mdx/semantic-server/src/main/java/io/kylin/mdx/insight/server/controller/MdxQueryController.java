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
import io.kylin.mdx.insight.core.entity.MdxQuery;
import io.kylin.mdx.insight.core.entity.SelectMdxQueryEntity;
import io.kylin.mdx.insight.core.entity.SqlQuery;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.core.service.MdxQueryService;
import io.kylin.mdx.insight.core.service.SqlQueryService;
import io.kylin.mdx.insight.server.bean.Page;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.bean.dto.MdxQueryDTO;
import io.kylin.mdx.insight.server.bean.dto.MdxQueryFilterDTO;
import io.kylin.mdx.insight.server.bean.dto.SqlQueryDTO;
import io.kylin.mdx.insight.server.support.Permission;
import lombok.extern.slf4j.Slf4j;
import mondrian.xmla.XmlaRequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("api")
public class MdxQueryController {

    private AuthService authService;

    private final MdxQueryService mdxQueryService;

    private final SqlQueryService sqlQueryService;


    @Autowired
    public MdxQueryController(MdxQueryService mdxQueryService,
                              SqlQueryService sqlQueryService,
                              AuthService authService) {
        this.mdxQueryService = mdxQueryService;
        this.sqlQueryService = sqlQueryService;
        this.authService = authService;
    }

    /**
     * 获取查询日志列表
     */
    @PostMapping("/query-history")
    @Permission
    public Response<Page<MdxQueryDTO>> selectHistoryLogList(@RequestParam("pageNum") @Min(0) Integer pageNum,
                                                            @RequestParam("pageSize") @NotNull Integer pageSize,
                                                            @RequestParam("projectName") String projectName,
                                                            @RequestParam(value = "orderBy", required = false) String orderBy,
                                                            @RequestParam(value = "direction", required = false) String direction,
                                                            @RequestBody @Validated MdxQueryFilterDTO mdxQueryFilterDTO) throws SemanticException {
        log.info("user:{} enter API:POST [/query-history]", authService.getCurrentUser());
        // 兼容查询历史中包含的 PowerBI Desktop 数据
        List<String> apps = mdxQueryFilterDTO.getApplication();
        if (apps != null && apps.contains(XmlaRequestContext.ClientType.POWERBI)) {
            apps.add(XmlaRequestContext.ClientType.POWERBI_DESKTOP);
        }
        SelectMdxQueryEntity selectMdxQueryEntity = new SelectMdxQueryEntity(projectName, mdxQueryFilterDTO.getUserName(),
                mdxQueryFilterDTO.getQueryId(), mdxQueryFilterDTO.getStatus(), apps, orderBy, direction, mdxQueryFilterDTO.getStartTimeFrom(),
                mdxQueryFilterDTO.getStartTimeTo(), mdxQueryFilterDTO.getCluster());
        List<MdxQuery> mdxQueries = mdxQueryService.selectMdxQueryByPage(selectMdxQueryEntity, pageNum + 1, pageSize);
        List<MdxQueryDTO> mdxQueryDTOs = new LinkedList<>();
        mdxQueries.forEach(mdxQuery ->
                mdxQueryDTOs.add(new MdxQueryDTO(mdxQuery)));
        Page<MdxQueryDTO> mdxQueryDTOPage = new Page<>(mdxQueryDTOs);
        mdxQueryDTOPage.setPageInfo(mdxQueries);
        return new Response<Page<MdxQueryDTO>>(Response.Status.SUCCESS).data(mdxQueryDTOPage);
    }

    /**
     * 获取单条查询日志具体信息
     */
    @GetMapping("/query-history/{mdx-query-id}")
    @Permission
    public Response<Page<SqlQueryDTO>> selectQueryInfo(@RequestParam("pageNum") @Min(0) Integer pageNum,
                                                       @RequestParam("pageSize") @NotNull Integer pageSize,
                                                       @RequestParam(value = "status", required = false) Boolean status,
                                                       @PathVariable("mdx-query-id") @NotNull String mdxQueryId) throws SemanticException {
        log.info("user:{} enter API:GET [/query-history/{}]", authService.getCurrentUser(), mdxQueryId);
        List<SqlQuery> sqlQueries = sqlQueryService.selectSqlQueryByPage(pageNum + 1, pageSize, mdxQueryId, status);
        List<SqlQueryDTO> sqlQueryDTOS = new LinkedList<>();
        sqlQueries.forEach(sqlQuery ->
                sqlQueryDTOS.add(new SqlQueryDTO(sqlQuery)));
        Page<SqlQueryDTO> sqlQueryDTOPage = new Page<>(sqlQueryDTOS);
        sqlQueryDTOPage.setPageInfo(sqlQueries);
        return new Response<Page<SqlQueryDTO>>(Response.Status.SUCCESS).data(sqlQueryDTOPage);
    }

    /**
     * 获取所有节点信息
     */
    @GetMapping("/query-history/cluster")
    @Permission
    public Response<List<String>> getNodes(@RequestParam(value = "projectName", required = false) String projectName) throws SemanticException {
        log.info("user:{} enter API:GET [/query-history/cluster]", authService.getCurrentUser());
        return new Response<List<String>>(Response.Status.SUCCESS).data(mdxQueryService.getNodes(projectName));
    }
}
