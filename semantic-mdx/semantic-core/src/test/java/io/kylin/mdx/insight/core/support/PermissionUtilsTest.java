package io.kylin.mdx.insight.core.support;

import org.junit.Assert;
import org.junit.Test;

public class PermissionUtilsTest {

    @Test
    public void hasAdminPermission() {
        Assert.assertTrue(PermissionUtils.hasAdminPermission("GLOBAL_ADMIN"));
        Assert.assertTrue(PermissionUtils.hasAdminPermission("ADMINISTRATION"));
        Assert.assertFalse(PermissionUtils.hasAdminPermission("READ"));
    }

    @Test
    public void hasQueryPermission() {
        Assert.assertTrue(PermissionUtils.hasQueryPermission("READ"));
        Assert.assertFalse(PermissionUtils.hasQueryPermission(""));
    }

}