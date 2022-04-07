package io.kylin.mdx.insight.common;

import org.junit.Assert;
import org.junit.Test;

public class SemanticConfigTest {

    static {
        System.setProperty("MDX_HOME", "src/test/resources/");
        System.setProperty("MDX_CONF", "src/test/resources/conf");
    }

    @Test
    public void test() {
        SemanticConfig semanticConfig = SemanticConfig.getInstance();
        semanticConfig.setKylinUser("ADMIN");
        semanticConfig.setKylinPwd("KYLIN");
        semanticConfig.setClustersInfo("localhost:7080");
        Assert.assertEquals("http", semanticConfig.getKylinProtocol());
        Assert.assertEquals("ADMIN", semanticConfig.getKylinUser());
        Assert.assertEquals("KYLIN", semanticConfig.getKylinPwd());
        Assert.assertEquals("http", semanticConfig.getKylinProtocol());
        Assert.assertEquals("localhost:7080", semanticConfig.getClustersInfo());
        Assert.assertTrue(semanticConfig.isSyncOnStartupEnable());
        Assert.assertTrue(semanticConfig.isClearOnStartupEnable());
        Assert.assertFalse(semanticConfig.isModelVersionVerifyEnable());
        Assert.assertEquals(1000000, semanticConfig.getMdxQueryHousekeepMaxRows());
        Assert.assertFalse(semanticConfig.isDatasetAccessByDefault());
        Assert.assertEquals(50, semanticConfig.getMondrianServerSize());
        Assert.assertFalse(semanticConfig.isGetHeapDump());
        Assert.assertFalse(semanticConfig.isEnableQueryApi());
        Assert.assertTrue(semanticConfig.isRowsetVisibilitiesSupported());
        Assert.assertEquals("*", semanticConfig.getProjectRowsetVisibilities());
        Assert.assertEquals("20MB", semanticConfig.getUploadFileMaxSize());
        Assert.assertEquals("public", semanticConfig.getPostgresqlSchema());
        Assert.assertEquals(1000, semanticConfig.getMdxQueryQueueSize());
        Assert.assertEquals("http://127.0.0.1:7080/mdx/xmla/learn_kylin", semanticConfig.getDiscoverCatalogUrl("learn_kylin"));
        Assert.assertEquals("http://127.0.0.1:7080/mdx/xmla/learn_kylin/clearCache", semanticConfig.getClearCacheUrl("learn_kylin"));
        // int default
        Assert.assertEquals(1, semanticConfig.getIntValue("insight.kylin.host", 1));
        // session name
        Assert.assertNotNull(semanticConfig.getSessionName());
        boolean isConvertorMock = semanticConfig.isConvertorMock();
        semanticConfig.getProperties().put("converter.mock", "true");
        Assert.assertEquals("mdx_session", semanticConfig.getSessionName());
        semanticConfig.getProperties().put("converter.mock", isConvertorMock);
    }

}
