package io.kylin.mdx.insight.core.support;

import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.constants.ConfigConstants;
import io.kylin.mdx.insight.core.entity.AADInfo;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileUrlResource;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class MdxPropertySourceLoaderTest {

    @BeforeClass
    public static void setup() {
        System.setProperty("MDX_HOME", "../mdx/src/test/resources/");
    }

    @Test
    public void test() throws IOException {
        MdxPropertySourceLoader loader = new MdxPropertySourceLoader();
        assertEquals(4, loader.getFileExtensions().length);
        List<PropertySource<?>> propertySources = loader.load("insight", new FileUrlResource("src/test/resources/conf/insight.properties"));
        PropertySource<?> propertySource = propertySources.get(0);
        assertNotNull(propertySource);
        assertEquals("root", String.valueOf(propertySource.getProperty(SemanticConstants.DATABASE_PASSWORD)));
    }

    @Test
    public void testDecryptedFail() throws IOException {
        String data = "insight.database.password=root";
        MdxPropertySourceLoader loader = new MdxPropertySourceLoader();
        boolean mark = false;
        try {
            loader.load("insight", new ByteArrayResource(data.getBytes()));
            mark = true;
        } catch (Error ignored) {
        }
        assertFalse(mark);
    }

    @Test
    public void testHandleAadResponse() {
        String res = "{\n" +
                "    \"code\": \"000\",\n" +
                "    \"data\": {\n" +
                "        \"isCloud\": true,\n" +
                "        \"securityProfile\": \"aad\",\n" +
                "        \"authType\": \"oauth2\",\n" +
                "        \"clientId\": \"c50fb4cb-6e9b-4cf8-ab4b-e0b570900101\",\n" +
                "        \"tenantId\": \"9b3afd1e-9454-4d38-a14c-a0670c820d48\",\n" +
                "        \"clientSecret\": \"NUCPZM06RiYqHoAp3D4okAWwxxdiRfqe3cM4UaF2M0eOa2s6fHxa5WRbG7v1PxBp\"\n" +
                "    },\n" +
                "    \"msg\": \"\"\n" +
                "}";
        MdxPropertySourceLoader loader = new MdxPropertySourceLoader();
        loader.handleAadResponse(res);
    }

    @Test
    public void testAddAadProperties() {
        AADInfo aadInfo = new AADInfo();
        aadInfo.setClientSecret("NUCPZM06RiYqHoAp3D4okAWwxxdiRfqe3cM4UaF2M0eOa2s6fHxa5WRbG7v1PxBp");
        aadInfo.setTenantId("9b3afd1e-9454-4d38-a14c-a0670c820d48");
        aadInfo.setClientId("c50fb4cb-6e9b-4cf8-ab4b-e0b570900101");

        SemanticConfig semanticConfig = SemanticConfig.getInstance();
        semanticConfig.getProperties().put(ConfigConstants.REDIRECT_URI_TEMPLATE,
                "https://mock-kc/internal/oauth2/login/code/");
        MdxPropertySourceLoader loader = new MdxPropertySourceLoader();
        loader.addAadProperties(aadInfo);

        Assert.assertEquals("https://mock-kc/internal/oauth2/login/code/",
                System.getProperty("azure.activedirectory.redirect-uri-template"));
    }

    @AfterClass
    public static void clearProperty() {
        System.clearProperty("MDX_HOME");
        System.clearProperty(ConfigConstants.IS_ENABLE_AAD);
        System.clearProperty(ConfigConstants.TENANT_ID);
        System.clearProperty(ConfigConstants.CLIENT_ID);
        System.clearProperty("server.tomcat.remote-ip-header");
        System.clearProperty("server.tomcat.protocol-header");
        System.clearProperty("server.use-forward-headers");
    }

}
