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


package io.kylin.mdx.insight.core.manager;

import io.kylin.mdx.insight.common.async.AsyncService;

/**
 * 附加线程池最大数量 = max(2 * core + 1, 15)
 */
public class AsyncManager {

    private static final AsyncManager INSTANCE = new AsyncManager();

    private final AsyncService asyncService;

    private final AsyncService cancelQueryAsyncService;

    private AsyncManager() {
        int coreSize = Math.max(Runtime.getRuntime().availableProcessors() * 2 + 1, 15);
        this.asyncService = new AsyncService(coreSize);
        this.cancelQueryAsyncService = new AsyncService(coreSize);
    }

    public static AsyncManager getInstance() {
        return INSTANCE;
    }

    public AsyncService getAsyncService() {
        return asyncService;
    }

    public AsyncService getCancelQueryAsyncService() {
        return cancelQueryAsyncService;
    }

}
