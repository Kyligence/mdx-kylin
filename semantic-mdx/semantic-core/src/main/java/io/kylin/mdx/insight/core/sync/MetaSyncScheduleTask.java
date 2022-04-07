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

import io.kylin.mdx.insight.common.MdxContext;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.service.MetaSyncService;
import io.kylin.mdx.insight.core.service.MetadataService;
import io.kylin.mdx.insight.core.support.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MetaSyncScheduleTask {
    public static final int DATASET_VERIFY_INTERNAL = SemanticConfig.getInstance().getDatasetVerifyInterval();

    private final ScheduledExecutorService scheduledExecService = new ScheduledThreadPoolExecutor(1,
            new DefaultThreadFactory("meta-sync"));

    private static final SemanticConfig SEMANTIC_CONFIG = SemanticConfig.getInstance();

    private int count = 0;

    private MetaSyncService metaSyncService;

    private MetadataService metadataService;

    private MetaStore metaStore;

    public MetaSyncScheduleTask(MetaSyncService metaSyncService, MetadataService metadataService, MetaStore metaStore) {
        this.metaSyncService = metaSyncService;
        this.metadataService = metadataService;
        this.metaStore = metaStore;
    }

    public void start() {
        scheduledExecService.scheduleWithFixedDelay(
                new MetaSyncTask(),
                0,
                SemanticConfig.getInstance().getMetaSyncInterval(),
                TimeUnit.SECONDS
        );
    }

    public class MetaSyncTask implements Runnable {
        @Override
        public void run() {
            log.info("Metadata sync task has started...");
            try {
                metaSyncService.syncCheck();
            } catch (Exception s) {
                log.error("[MetaSync$CubeChangedMonitor] Metadata check failed...", s);
                MdxContext.getAndSetSyncStatus(true, false);
                if (!SemanticConfig.getInstance().isConvertorMock()) {
                    return;
                }
            }

            try {
                syncMetadata();
                MdxContext.getAndSetSyncStatus(false, true);
                log.info("Metadata sync task has done...");
            } catch (Throwable e) {
                log.error("Metadata sync failed, {}", e.getMessage());
            }
        }

        private void syncMetadata() {
            metaStore.getLastUpdateTime().set(Utils.currentTimeStamp());
            metaSyncService.syncUser();
            metaSyncService.syncGroup();
            metaSyncService.syncCube();
            metaSyncService.syncProjects();
            metaSyncService.syncProjectAclChange();
            if (++count % DATASET_VERIFY_INTERNAL != 0 && !MdxContext.isFirstVerify()) {
                return;
            }
            metaSyncService.loadKiLicense();
            if (SEMANTIC_CONFIG.isEnableSyncSegment()) {
                metaSyncService.syncSegment();
            }
            metaSyncService.syncDataset();
            metadataService.syncCardinalityInfo();
            MdxContext.setFirstVerify(false);
        }
    }


}
