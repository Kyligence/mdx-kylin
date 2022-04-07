package io.kylin.mdx.insight.core.sync;

import io.kylin.mdx.insight.common.MdxContext;
import io.kylin.mdx.insight.core.meta.mock.MockTestLoader;
import io.kylin.mdx.insight.core.service.MetaSyncService;
import io.kylin.mdx.insight.core.service.MetadataService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MetaSyncScheduleTaskTest {

    @Mock
    private MetaSyncService metaSyncService;

    @Mock
    private MetadataService metadataService;

    @Mock
    private MetaStore metaStore;

    @BeforeClass
    public static void before() {
        System.setProperty("converter.mock", "true");
        System.setProperty("MDX_HOME", "src/test/resources/kylin");
        System.setProperty("MDX_CONF", "src/test/resources/kylin/conf");
        MockTestLoader.put(new MockTestLoader.Prefix("/kylin/api/access/ProjectInstance/AdventureWorks"), "/kylin/kylin_access_project.json");
    }


    @Test
    public void startTest() {
        MetaSyncScheduleTask metaSyncScheduleTask = new MetaSyncScheduleTask(metaSyncService, metadataService, metaStore);
        metaSyncScheduleTask.start();

    }
    @Test
    public void test() {
        MdxContext.getSyncStatus();
        MdxContext.getAndSetSyncStatus(false, true);
    }

}
