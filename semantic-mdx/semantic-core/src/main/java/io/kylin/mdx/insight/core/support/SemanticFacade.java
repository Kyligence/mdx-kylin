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


package io.kylin.mdx.insight.core.support;

import io.kylin.mdx.insight.common.MdxContext;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.model.semantic.SemanticProject;
import io.kylin.mdx.insight.core.service.SemanticContext;
import io.kylin.mdx.ErrorCode;

import javax.annotation.Nonnull;

public class SemanticFacade {

    public final static SemanticFacade INSTANCE = new SemanticFacade();

    private SemanticFacade() {
    }

    public void clearProjectCache(String project, String username) {
        Execution execution = new Execution("clearProjectCache[project=%s][username=%s]", project, username);
        SemanticContext semanticCtx = SpringHolder.getBean(SemanticContext.class);
        try {
            semanticCtx.clearProjectCache(project, username);
        } finally {
            execution.logTimeConsumed();
        }
    }

    @Nonnull
    public SemanticProject getSemanticProject(String project) throws SemanticException {
        Execution execution = new Execution("GetSemanticProject[project=%s]", project);
        SemanticContext semanticCtx = SpringHolder.getBean(SemanticContext.class);
        try {
            return semanticCtx.createSemanticProject(project);
        } finally {
            execution.logTimeConsumed();
        }
    }

    @Nonnull
    public SemanticProject getSemanticProjectByUser(String project, String username) throws SemanticException {
        // 同步用户信息有误 或 同步任务未启动
        if (!MdxContext.isSyncStatus()) {
            throw new SemanticException(ErrorCode.INVALIDATE_SYNC_INFO, project);
        }
        Execution execution = new Execution("getSemanticProjectByUser[project=%s][username=%s]", project, username);
        SemanticContext semanticCtx = SpringHolder.getBean(SemanticContext.class);
        try {
            return semanticCtx.createSemanticProject(username, project);
        } finally {
            execution.logTimeConsumed();
        }
    }

}
