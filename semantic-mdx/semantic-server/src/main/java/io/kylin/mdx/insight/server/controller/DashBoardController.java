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

import io.kylin.mdx.insight.core.entity.BasicStatistics;
import io.kylin.mdx.insight.core.entity.QueryCostStatistics;
import io.kylin.mdx.insight.core.entity.StatisticsTrend;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.core.service.MdxQueryService;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.bean.dto.DashBoardDTO;
import io.kylin.mdx.insight.server.support.Permission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("api")
public class DashBoardController {

    private final AuthService authService;

    private final MdxQueryService mdxQueryService;

    @Autowired
    public DashBoardController(AuthService authService, MdxQueryService mdxQueryService) {
        this.authService = authService;
        this.mdxQueryService = mdxQueryService;
    }

    /**
     * 获取查询统计
     */
    @GetMapping("/statistics/basic")
    @Permission
    public Response<BasicStatistics> getBasicStatistics(@RequestParam("startTime") @NotNull Long startTime,
                                                        @RequestParam("endTime") @NotNull Long endTime,
                                                        @RequestParam("projectName") @NotNull String projectName) {
        log.info("user:{} enter API:GET [/statistics/basic/]", authService.getCurrentUser());

        return new Response<BasicStatistics>(Response.Status.SUCCESS).data(mdxQueryService.getBasicStatistics(startTime, endTime, projectName));
    }

    /**
     * 获取近期趋势统计
     */
    @PostMapping("/statistics/trend")
    @Permission
    public Response<StatisticsTrend> getTrendStatistics(@RequestBody DashBoardDTO dashBoardDTO,
                                                        @RequestParam("projectName") @NotNull String projectName) {
        log.info("user:{} enter API:GET [/statistics/trend/]", authService.getCurrentUser());

        List<String> datasetNames = dashBoardDTO.getDatasetNames();
        List<Long> axis = dashBoardDTO.getAxis();
        return new Response<StatisticsTrend>(Response.Status.SUCCESS).data(mdxQueryService.getStatisticsTrend(axis, projectName, datasetNames));
    }

    /**
     * 获取查询时间统计
     */
    @PostMapping("/statistics/query-cost")
    @Permission
    public Response<QueryCostStatistics> getQueryCostStatistics(@RequestBody DashBoardDTO dashBoardDTO,
                                                                @RequestParam("projectName") @NotNull String projectName) {
        log.info("user:{} enter API:GET [/statistics//query-cos/]", authService.getCurrentUser());

        List<Long> axis = dashBoardDTO.getAxis();
        return new Response<QueryCostStatistics>(Response.Status.SUCCESS).data(mdxQueryService.getQueryCostStatistics(axis, projectName));
    }

    /**
     * 获取数据集排名
     */
    @GetMapping("/statistics/ranking")
    @Permission
    public Response<List<Map<String,String>>> getRankingStatistics(@RequestParam("startTime") @NotNull Long startTime,
                                                                  @RequestParam("endTime") @NotNull Long endTime,
                                                                  @RequestParam("count") @NotNull Long count,
                                                                  @RequestParam("direction") @NotNull String direction,
                                                                  @RequestParam("projectName") @NotNull String projectName) {
        log.info("user:{} enter API:GET [/statistics/ranking/]", authService.getCurrentUser());

        return new Response<List<Map<String,String>>>(Response.Status.SUCCESS).data(mdxQueryService.getRankingStatistics(startTime, endTime, count, direction, projectName));
    }

}
