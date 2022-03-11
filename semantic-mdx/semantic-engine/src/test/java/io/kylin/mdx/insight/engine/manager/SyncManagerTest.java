package io.kylin.mdx.insight.engine.manager;

import com.google.common.collect.Sets;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.meta.mock.MockTestLoader;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.service.BrokenService;
import io.kylin.mdx.insight.core.service.ModelService;
import io.kylin.mdx.insight.core.sync.DatasetEventObject;
import io.kylin.mdx.insight.core.sync.KylinEventObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class SyncManagerTest {
    @Mock
    private ModelService modelService;

    @Mock
    private BrokenService brokenService;

    @InjectMocks
    private SyncManager syncManager;

    @BeforeClass
    public static void before() {
        System.setProperty("converter.mock", "true");
        System.setProperty("MDX_HOME", "src/test/resources/kylin");
        System.setProperty("MDX_CONF", "src/test/resources/kylin/conf");
        MockTestLoader.put(new MockTestLoader.Prefix("/kylin/api/access/ProjectInstance/AdventureWorks"), "/ke/ke_access_project.json");
    }

    @Test
    public void asyncNotifyTest() {
        KylinEventObject kylinEventObject = new KylinEventObject();
        syncManager.asyncNotify(kylinEventObject);
    }

    @Test
    public void notifyKeTest() {

        KylinEventObject kylinEventObject = new KylinEventObject();
        kylinEventObject.setEventType(KylinEventObject.KylinEventType.CUBE_CHANGED);
        KylinEventObject.KylinCubeChanged kylinCubeChanged = new KylinEventObject.KylinCubeChanged("test",
                Sets.newHashSet("test"),
                Arrays.asList(new KylinGenericModel()));
        kylinEventObject.setChanged(kylinCubeChanged);

        KylinEventObject kylinEventObject1 = new KylinEventObject();
        kylinEventObject1.setEventType(KylinEventObject.KylinEventType.CUBE_NEWED);
        kylinEventObject1.setChanged(kylinCubeChanged);

        KylinEventObject kylinEventObject2 = new KylinEventObject();
        kylinEventObject2.setEventType(KylinEventObject.KylinEventType.CUBE_DELETED);
        kylinEventObject2.setChanged(kylinCubeChanged);

        KylinEventObject kylinEventObject3 = new KylinEventObject();
        kylinEventObject3.setEventType(KylinEventObject.KylinEventType.CUBE_LATEST);
        kylinEventObject3.setChanged(kylinCubeChanged);

        syncManager.notify(kylinEventObject);
        syncManager.notify(kylinEventObject1);
        syncManager.notify(kylinEventObject2);
        syncManager.notify(kylinEventObject3);
    }

    @Test
    public void notifyDatasetsTest() {
        DatasetEventObject datasetEventObject = new DatasetEventObject();
        datasetEventObject.setEventType(DatasetEventObject.DatasetEventType.DATASET_INVALIDATED);
        DatasetEventObject.BrokenDataset brokenDataset = new DatasetEventObject.BrokenDataset();

        DatasetEntity datasetEntity = new DatasetEntity();
        datasetEntity.setId(1);
        datasetEntity.setStatus("BROKEN");
        datasetEntity.setDataset("test");
        brokenDataset.setBrokenDataset(datasetEntity);
        DatasetEventObject.DatasetInvalidateSource datasetInvalidateSource = new DatasetEventObject.DatasetInvalidateSource(
                Arrays.asList(brokenDataset), Arrays.asList(datasetEntity));
        datasetEventObject.setChanged(datasetInvalidateSource);

        syncManager.notify(datasetEventObject);


        DatasetEventObject datasetEventObject1 = new DatasetEventObject();
        datasetEventObject1.setEventType(DatasetEventObject.DatasetEventType.DATASET_NEWED);
        DatasetEventObject.DatasetChangedSource datasetChangedSource = new DatasetEventObject.DatasetChangedSource("test","test");
        datasetEventObject1.setChanged(datasetChangedSource);
        syncManager.notify(datasetEventObject1);


    }


}
