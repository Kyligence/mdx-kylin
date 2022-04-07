package io.kylin.mdx.insight.common.util;


import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class NetworkUtilsTest {

    @Test
    public void testIpAddress() {
        try {
            new NetworkUtils.IpAddress("10.1.1");
            Assert.fail();
        } catch (Exception ignored) {
        }
        String ip = "172.4.5.6";
        NetworkUtils.IpAddress ipAddress = new NetworkUtils.IpAddress(ip);
        Assert.assertEquals(172, ipAddress.getA());
        Assert.assertEquals(4, ipAddress.getB());
        Assert.assertEquals(5, ipAddress.getC());
        Assert.assertEquals(6, ipAddress.getD());
    }

    @Test
    public void testGetLANIPAddress() {
        Assert.assertNotNull(NetworkUtils.getLocalIP());
        Assert.assertEquals("127.0.0.1", NetworkUtils.getLANIPAddress(
                Collections.emptyList()
        ));
        Assert.assertEquals("192.169.1.1", NetworkUtils.getLANIPAddress(
                Collections.singletonList("192.169.1.1")
        ));
        Assert.assertEquals("192.168.1.1", NetworkUtils.getLANIPAddress(
                Arrays.asList("192.169.1.1", "192.168.1.1")
        ));
        Assert.assertEquals("172.16.1.1", NetworkUtils.getLANIPAddress(
                Arrays.asList("192.169.1.1", "192.168.1.1", "172.16.1.1")
        ));
        Assert.assertEquals("10.1.2.3", NetworkUtils.getLANIPAddress(
                Arrays.asList("192.169.1.1", "192.168.1.1", "10.1.2.3")
        ));
    }

}
