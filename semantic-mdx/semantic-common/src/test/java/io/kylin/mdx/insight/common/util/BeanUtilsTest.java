package io.kylin.mdx.insight.common.util;

import io.kylin.mdx.insight.common.http.Response;
import org.junit.Assert;
import org.junit.Test;

public class BeanUtilsTest {

    @Test
    public void setFieldByName() {
        Response response = new Response(200, "OK");
        BeanUtils.setField(response, "httpStatus", 400);
        Assert.assertEquals(400, response.getHttpStatus());
    }

    @Test
    public void setFieldByClass() {
        Response response = new Response(200, "OK");
        BeanUtils.setField(response, String.class, "Success");
        Assert.assertEquals("Success", response.getContent());
    }

    @Test
    public void getField() {
        Response response = new Response(200, "OK");
        Integer status = (Integer) BeanUtils.getField(response, "httpStatus");
        Assert.assertEquals(Integer.valueOf(200), status);
    }

}