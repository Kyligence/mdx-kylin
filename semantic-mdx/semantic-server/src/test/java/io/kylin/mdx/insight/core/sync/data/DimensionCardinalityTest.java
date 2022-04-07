package io.kylin.mdx.insight.core.sync.data;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.core.meta.mock.MockTestLoader;
import io.kylin.mdx.insight.core.service.MetadataService;
import io.kylin.mdx.insight.server.SemanticLauncher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = SemanticLauncher.class)
@RunWith(SpringRunner.class)
public class DimensionCardinalityTest extends BaseEnvSetting {

    private final static String PROJECT_NAME = "mdx_automation_test";

    @Autowired
    private MetadataService metadataService;

    @Before
    public void init() {
        System.setProperty("converter.mock", "true");
        SemanticConfig.getInstance().getProperties().put("insight.semantic.datasource-version", "4");
        MockTestLoader.put(new MockTestLoader.Prefix("/kylin/api/tables"), "/json/kylinmetadata/mdx_automation_get_tables.json");
    }

    @After
    public void clear() {
        SemanticConfig.getInstance().getProperties().put("insight.semantic.datasource-version", "3");
        System.clearProperty("converter.mock");
    }

    @Test
    public void syncHighCardinalityDimension() {
        metadataService.syncCardinalityInfo();
    }

}
