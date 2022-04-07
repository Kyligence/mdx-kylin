package io.kylin.mdx.insight.common.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class JacksonSerDeUtilsTest {
    @Test
    public void readStringTest()  {
        Double result = JacksonSerDeUtils.readString("test", Double.class);
        Assert.assertEquals(result, null);
    }
    @Test
    public void readFileTest() {
        String result = JacksonSerDeUtils.readXml(new File("test"), String.class);
        Assert.assertEquals(result, null);
    }

    @Test
    public void readInputstreamErrorTest() {
        Double result = JacksonSerDeUtils.readInputStream(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)), Double.class );
        Assert.assertNull(result);
    }


    @Test
    public void readInputstreamTest() {
        String json = "\"name\": \"test\"";
        String result = JacksonSerDeUtils.readInputStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), String.class );
        Assert.assertEquals(result, "name");
    }

    @Test
    public void readXmlToClazzErrorTest() {
        Double result = JacksonSerDeUtils.readXmlToClazz("test", Double.class );
        Assert.assertEquals(result, null);
    }

}
