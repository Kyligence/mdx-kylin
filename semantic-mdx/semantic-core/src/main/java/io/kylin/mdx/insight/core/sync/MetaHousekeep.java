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


package io.kylin.mdx.insight.core.sync;

import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.core.service.MdxQueryService;
import io.kylin.mdx.insight.core.support.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MetaHousekeep {

    private static final SemanticConfig semanticConfig = SemanticConfig.getInstance();

    private static final int MDX_QUERY_JOB_RUN_PERIOD_TIME = semanticConfig.getMdxQueryJobRunPeriodTime();

    private final ScheduledExecutorService mdxQueryScheduledExecService = new ScheduledThreadPoolExecutor(1,
            new DefaultThreadFactory("meta-housekeep"));

    private final MdxQueryService mdxQueryService;

    private final int mdxQueryMaxRows;

    public MetaHousekeep(MdxQueryService mdxQueryService, int mdxQueryMaxRows) {
        this.mdxQueryService = mdxQueryService;
        this.mdxQueryMaxRows = mdxQueryMaxRows;
    }

    public void start() {
        mdxQueryScheduledExecService.scheduleWithFixedDelay(
                () -> mdxQueryService.houseKeep(mdxQueryMaxRows),
                0,
                MDX_QUERY_JOB_RUN_PERIOD_TIME,
                TimeUnit.SECONDS
        );

        log.info("MDX QUERY limit " + mdxQueryMaxRows + " housekeep job has started...");
    }
}
