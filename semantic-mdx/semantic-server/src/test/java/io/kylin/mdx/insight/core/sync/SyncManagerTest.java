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

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.base.ExceptionUtils;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.service.ModelService;
import io.kylin.mdx.insight.core.sync.DatasetEventObject.BrokenDataset;
import io.kylin.mdx.insight.core.sync.KylinEventObject.KylinCubeChanged;
import io.kylin.mdx.insight.core.sync.KylinEventObject.KylinEventType;
import io.kylin.mdx.insight.engine.manager.SyncManager;
import io.kylin.mdx.insight.engine.service.ModelServiceImpl;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SyncManagerTest extends BaseEnvSetting {


    private ModelService modelService = new ModelServiceImpl();


    private SyncManager syncManager = new SyncManager();

    @BeforeClass
    public static void init() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", "/json/kylinmetadata");
    }

    public void notifyKEEventObject() throws SemanticException {
        String project = "mdx_automation_test_mock";

        syncManager.notify(
                new KylinEventObject(
                        new KylinCubeChanged(project, Collections.emptySet(), Collections.emptyList()),
                        KylinEventType.CUBE_NEWED
                )
        );
        Assert.assertEquals(0, modelService.getCachedGenericModels(project).size());

        syncManager.notify(
                new KylinEventObject(
                        new KylinCubeChanged(project, Collections.emptySet(), Collections.emptyList()),
                        KylinEventType.CUBE_DELETED
                )
        );
        Assert.assertEquals(0, modelService.getCachedGenericModels(project).size());

        syncManager.notify(
                new KylinEventObject(
                        new KylinCubeChanged(project, Collections.emptySet(), Collections.emptyList()),
                        KylinEventType.CUBE_CHANGED
                )
        );
        Assert.assertEquals(0, modelService.getCachedGenericModels(project).size());
    }

    public void notifyDatasetInvalidated() throws SemanticException {
        DatasetEntity.DatasetEntityBuilder datasetEntityBuilder = DatasetEntity.builder()
                .project("mdx_automation_test")
                .dataset("snowflake_dataset");
        DatasetEntity brokenEntity = datasetEntityBuilder.build();
        brokenEntity.setId(4);
        List<BrokenDataset> brokenDatasets = Collections.singletonList(new BrokenDataset(brokenEntity, new DatasetBrokenInfo()));
        List<DatasetEntity> selfFixedDatasets = Collections.singletonList(datasetEntityBuilder.build());
        syncManager.notify(
                new DatasetEventObject(
                        new DatasetEventObject.DatasetInvalidateSource(brokenDatasets, selfFixedDatasets),
                        DatasetEventObject.DatasetEventType.DATASET_INVALIDATED
                )
        );
    }

    @Test
    public void notifySQLDatasetChange() throws SemanticException {
        String project = "mdx_automation_test";
        String datasetNameSQL = "snowflake_dataset_sql";

        try {
            DatasetEventObject eventObjectNewed = new DatasetEventObject(
                    new DatasetEventObject.DatasetChangedSource(project, datasetNameSQL),
                    DatasetEventObject.DatasetEventType.DATASET_NEWED);
            syncManager.notify(eventObjectNewed);
        } catch (SemanticException ex) {
            if (!ExceptionUtils.equalHttpException(ex)) {
                throw ex;
            }
        }

        try {
            DatasetEventObject eventObjectDeleted = new DatasetEventObject(
                    new DatasetEventObject.DatasetChangedSource(project, datasetNameSQL),
                    DatasetEventObject.DatasetEventType.DATASET_DELETED);
            syncManager.notify(eventObjectDeleted);
        } catch (SemanticException ex) {
            if (!ExceptionUtils.equalHttpException(ex)) {
                throw ex;
            }
        }
    }

    @Test
    public void notifyMDXDatasetChange() throws SemanticException {
        String project = "mdx_automation_test";
        String datasetNameMDX = "snowflake_dataset";

        UserSyncHolder.INSTANCE.getChangedProjectsByUser("ADMIN");

        DatasetEventObject eventObjectNewed = new DatasetEventObject(
                new DatasetEventObject.DatasetChangedSource(project, datasetNameMDX),
                DatasetEventObject.DatasetEventType.DATASET_NEWED);
        syncManager.notify(eventObjectNewed);
        Set<String> eventsNewed = UserSyncHolder.INSTANCE.getChangedProjectsByUser("ADMIN");
        Assert.assertEquals(1, eventsNewed.size());
        Assert.assertSame(project, eventsNewed.iterator().next());
    }

}
