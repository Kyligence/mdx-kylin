package io.kylin.mdx.insight.common.util.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class CountInputStreamTest {

    @Test
    public void test() throws IOException {
        byte[] data = "Hello,world!".getBytes();
        InputStream is = new ByteArrayInputStream(data);
        CountInputStream cis = new CountInputStream(is);
        byte[] buf = new byte[data.length];
        int len = cis.read(buf, 0, 5);
        Assert.assertArrayEquals("Hello".getBytes(), Arrays.copyOf(buf, len));
        int b = cis.read();
        Assert.assertEquals(b, (int) ',');
        len = cis.read(buf);
        Assert.assertArrayEquals("world!".getBytes(), Arrays.copyOf(buf, len));
        Assert.assertEquals(data.length, cis.getCount());
    }

}