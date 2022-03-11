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


package io.kylin.mdx.insight.engine.manager;

import com.alibaba.fastjson.JSON;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.model.semantic.DatasetStatus;
import io.kylin.mdx.insight.core.service.BrokenService;
import io.kylin.mdx.insight.core.service.ModelService;
import io.kylin.mdx.insight.core.sync.*;
import io.kylin.mdx.insight.core.sync.DatasetEventObject.BrokenDataset;
import io.kylin.mdx.insight.core.sync.DatasetEventObject.DatasetChangedSource;
import io.kylin.mdx.insight.core.sync.DatasetEventObject.DatasetEventType;
import io.kylin.mdx.insight.core.sync.DatasetEventObject.DatasetInvalidateSource;
import io.kylin.mdx.insight.core.sync.KylinEventObject.KylinCubeChanged;
import io.kylin.mdx.insight.core.sync.KylinEventObject.KylinEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * handle all events and delegate event to different observers separately
 */
@Slf4j
@Component
public class SyncManager implements Observer, Runnable {

    @Autowired
    private ModelService modelService;

    @Autowired
    private BrokenService brokenService;

    private final BlockingQueue<EventObject> eventQueue = new ArrayBlockingQueue<>(1000);

    public void start() {
        Thread thread = new Thread(this, "SyncManager$EventPuller");
        thread.setDaemon(true);
        thread.start();
        log.info("SyncManager$EventPuller has started...");
    }

    @Override
    public void asyncNotify(EventObject eventObject) { //NOSONAR no throw exception
        try {
            eventQueue.put(eventObject);
        } catch (Exception e) {
            log.error("[SyncManager] put eventObject, but gets an exception", e);
        }
    }

    @Override
    public void notify(EventObject eventObject) throws SemanticException {
        handleEvent(eventObject);
    }

    private void handleEvent(EventObject eventObject) throws SemanticException {
        if (eventObject instanceof KylinEventObject) {
            handleKeEvent((KylinEventObject) eventObject);
        }
        if (eventObject instanceof DatasetEventObject) {
            handleDatasetEvent((DatasetEventObject) eventObject);
        }
    }

    private void handleKeEvent(KylinEventObject keEventObject) {
        KylinEventType kylinEventType = keEventObject.getKeEventType();
        KylinCubeChanged cubeChanged = (KylinCubeChanged) keEventObject.getChanged();
        if (kylinEventType == KylinEventType.CUBE_CHANGED) {
            log.warn("The project:[{}] has cube changed", cubeChanged.getProjectName());

            modelService.refreshGenericModels(cubeChanged.getProjectName(), cubeChanged.getModels());
        } else if (kylinEventType == KylinEventType.CUBE_NEWED) {
            log.warn("The project:[{}] has new cubes added or disabled cube ready, cubes:{}",
                    cubeChanged.getProjectName(), JSON.toJSONString(cubeChanged.getCubeNames()));

            //try to recover the broken dataset whose models turn to be enable or added

            brokenService.tryRecoverDataset(cubeChanged.getProjectName(), cubeChanged.getCubeNames());

            modelService.refreshGenericModels(cubeChanged.getProjectName(), cubeChanged.getModels());
        } else if (kylinEventType == KylinEventType.CUBE_DELETED) {
            log.warn("The project:[{}] has cubes deleted or ready cube disabled, cubes:{}",
                    cubeChanged.getProjectName(), JSON.toJSONString(cubeChanged.getCubeNames()));

            //set broken status to the dataset whose models become disabled or deleted
            brokenService.setDatasetsBroken(cubeChanged.getProjectName(), cubeChanged.getCubeNames());

            modelService.refreshGenericModels(cubeChanged.getProjectName(), cubeChanged.getModels());
        } else if (kylinEventType == KylinEventType.CUBE_LATEST) {
            modelService.refreshGenericModels(cubeChanged.getProjectName(), cubeChanged.getModels());
        }
    }

    private void handleDatasetEvent(DatasetEventObject datasetEventObject) throws SemanticException {
        DatasetEventType datasetEventType = datasetEventObject.getDatasetEventType();
        Object changed = datasetEventObject.getChanged();

        if (datasetEventType == DatasetEventType.DATASET_INVALIDATED) {
            DatasetInvalidateSource invalidateSource = (DatasetInvalidateSource) changed;

            List<BrokenDataset> brokenDatasets = invalidateSource.getBrokenDatasets();
            for (BrokenDataset brokenDataset : brokenDatasets) {

                DatasetEntity targetDataset = brokenDataset.getBrokenDataset();

                brokenService.setOneDatasetBroken(targetDataset.getId(), brokenDataset.getBrokenInfo());
                log.info("[Dataset-invalidated setBrokenDataset] id:{}, dataset:{}, project:{}, brokenInfo:{}",
                        targetDataset.getId(), targetDataset.getDataset(), targetDataset.getProject(), JSON.toJSONString(brokenDataset.getBrokenInfo()));
            }

            List<DatasetEntity> selfFixedDatasets = invalidateSource.getSelfFixedDatasets();
            for (DatasetEntity selfFixDataset : selfFixedDatasets) {
                // broken dataset become self-fix, so it should be NORMAL
                if (DatasetStatus.BROKEN.name().equals(selfFixDataset.getStatus())) {
                    brokenService.recoverOneDatasetNormal(selfFixDataset.getId());
                    log.info("[Dataset-invalidated recoverBrokenDataset] id:{}, dataset:{}, project:{}",
                            selfFixDataset.getId(), selfFixDataset.getDataset(), selfFixDataset.getProject());

                }
                UserSyncHolder.INSTANCE.putEvent(new DatasetChangedSource(selfFixDataset));
            }
        } else {
            DatasetChangedSource changedSource = (DatasetChangedSource) changed;
            log.info("[MDX send event] DatasetEventObject: {}", JSON.toJSONString(datasetEventObject));
            UserSyncHolder.INSTANCE.putEvent(changedSource);
        }
    }

    @Override
    public void run() { //NOSONAR no throw exception
        for (; ; ) {
            try {
                EventObject eventObject = eventQueue.take();
                try {
                    handleEvent(eventObject);
                } catch (SemanticException e) {
                    log.error("Handle eventObject get SemanticException, eventObject:{}",
                            JSON.toJSONString(eventObject), e);
                } catch (Throwable e) {
                    log.error("Handle eventObject get unknown exception, eventObject:{}",
                            JSON.toJSONString(eventObject), e);
                }
            } catch (Exception ex) {
                log.error("EventQueue pulling thread get exception", ex);
            }
        }
    }

}
