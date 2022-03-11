package io.kylin.mdx.web.support;

import mondrian.tui.MockHttpServletResponse;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class HttpResponseWrapperTest {

    @Test
    public void test() throws IOException {
        HttpServletResponse response = new MockHttpServletResponse();
        HttpResponseWrapper httpResponseWrapper = new HttpResponseWrapper(response);
        PrintWriter writer = httpResponseWrapper.getWriter();
        Assert.assertNotNull(writer);
        ServletOutputStream outputStream = httpResponseWrapper.getOutputStream();
        outputStream.write((int) 'a');
        outputStream.write('b');
        outputStream.write(new byte[]{'c'});
        outputStream.write(new byte[]{'d', 'e'}, 0, 2);
        outputStream.flush();
        byte[] data = httpResponseWrapper.getContent();
        Assert.assertArrayEquals(new byte[]{'a', 'b', 'c', 'd', 'e'}, data);
    }

}