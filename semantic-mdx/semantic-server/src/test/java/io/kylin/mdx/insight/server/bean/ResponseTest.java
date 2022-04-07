package io.kylin.mdx.insight.server.bean;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ResponseTest {

    @Test
    public void ensureStatusCode() {
        assertEquals(0, Response.Status.SUCCESS.ordinal());
        assertEquals(1, Response.Status.FAIL.ordinal());
    }

}