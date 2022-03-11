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

import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.entity.MdxQuery;
import io.kylin.mdx.insight.core.entity.SqlQuery;
import io.kylin.mdx.insight.core.service.MdxQueryService;
import io.kylin.mdx.insight.core.service.SqlQueryService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Slf4j
public class QueryLogPersistence implements Runnable {

    private MdxQueryService mdxQueryService;

    private SqlQueryService sqlQueryService;

    public static QueryLogPersistence INSTANCE;

    public static final SemanticConfig CONFIG = SemanticConfig.getInstance();

    private final BlockingQueue<Object> queryQueue = new ArrayBlockingQueue<>(CONFIG.getMdxQueryQueueSize());

    public QueryLogPersistence(MdxQueryService mdxQueryService, SqlQueryService sqlQueryService) {
        this.mdxQueryService = mdxQueryService;
        this.sqlQueryService = sqlQueryService;
    }

    public void start() {
        Thread thread = new Thread(this, "QueryLogPersistence");
        thread.setDaemon(true);
        thread.start();

        log.info("QueryLogPersistence has started...");
    }

    @Override
    public void run() {
        try {
            for (; ; ) {
                Object object = queryQueue.take();

                try {
                    handleQueryLog(object);
                } catch (SemanticException e) {
                    log.error("Handle queryLog get SemanticException, [{}], {}",
                            JSON.toJSONString(object), Throwables.getStackTraceAsString(e));
                } catch (Throwable e) {
                    log.error("Handle queryLog get unknown exception, [{}], {}",
                            JSON.toJSONString(object), Throwables.getStackTraceAsString(e));
                }
            }
        } catch (Exception ex) {
            log.error("QueryLogQueue pulling thread gets an exception", ex);
        }
    }

    private void handleQueryLog(Object object) throws SemanticException {
        if (object instanceof MdxQuery) {
            mdxQueryService.insertMdxQuery((MdxQuery) object);
        }

        if (object instanceof SqlQuery) {
            sqlQueryService.insertSqlQuery((SqlQuery) object);
        }
    }

    public void asyncNotify(Object object) {
        if (object instanceof MdxQuery || object instanceof SqlQuery) {
            try {
                if (queryQueue.size() >= CONFIG.getMdxQueryQueueSize()) {
                    log.warn("MDX query queue reach upper limit.");
                    return;
                }
                queryQueue.put(object);
            } catch (Exception e) {
                log.error("[QueryLogManager] puts mdx query, but gets an exception. [{}], {}",
                        JSON.toJSONString(object), Throwables.getStackTraceAsString(e));
            }
        }
    }
}
