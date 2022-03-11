package io.kylin.mdx.insight.common.util.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CountOutputStreamTest {

    @Test
    public void test() throws IOException {
        byte[] data = "Hello,world!".getBytes();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        CountOutputStream cos = new CountOutputStream(os);
        cos.write("Hello".getBytes());
        cos.write(',');
        byte[] tmp = "world!".getBytes();
        cos.write(tmp, 0, tmp.length);
        Assert.assertArrayEquals(data, os.toByteArray());
        Assert.assertEquals(data.length, cos.getCount());
    }

}