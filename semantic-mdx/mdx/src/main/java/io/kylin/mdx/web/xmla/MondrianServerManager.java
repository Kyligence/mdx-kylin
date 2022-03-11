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


package io.kylin.mdx.web.xmla;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.kylin.mdx.insight.common.SemanticConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalListeners;
import com.google.common.cache.RemovalNotification;

import mondrian.olap.MondrianServer;

public class MondrianServerManager {

    private static Logger log = LoggerFactory.getLogger(MondrianServerManager.class);

    private static ExecutorService pool = new ThreadPoolExecutor(5, 10, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1024), new ThreadFactoryBuilder().setNameFormat("remove-pool-%d").build(),
            new ThreadPoolExecutor.AbortPolicy());

    private static Cache<String, MondrianServer> mondrianServerCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS).maximumSize(SemanticConfig.getInstance().getMondrianServerSize())
            .removalListener(RemovalListeners.asynchronous(new RemovalListener<String, MondrianServer>() {
                @Override
                public void onRemoval(RemovalNotification<String, MondrianServer> removalNotification) {
                    String key = removalNotification.getKey();
                    MondrianServer server = removalNotification.getValue();

                    releaseOldServerResource(key, server, removalNotification.getCause());
                }
            }, pool)).build();

    public static MondrianServer getIfPresent(String cacheKey) {
        return mondrianServerCache.getIfPresent(cacheKey);
    }

    public static void putMondrianServer(String cacheKey, MondrianServer server) {
        mondrianServerCache.put(cacheKey, server);
    }

    public static Map<String, MondrianServer> asMap() {
        return mondrianServerCache.asMap();
    }

    private static void releaseOldServerResource(String cacheKey, MondrianServer server, RemovalCause removalCause) {
        try {
            if (server != null) {
                server.shutdown();
                log.info("mondrianServerCache has entry invalidated, cachekey:{}, cause:{}", cacheKey, removalCause);
            }
        } catch (Throwable e) {
            log.error("mondrian release old server exception, cacheKey:{}, info:{}", cacheKey,
                    Throwables.getStackTraceAsString(e));
        }

    }

}
