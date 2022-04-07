package io.kylin.mdx.insight.core.model.generic;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class KylinUserInfoTest {

    @Test
    public void test() {
        JSONObject kylinJson = JSON.parseObject("{\n" +
                "    \"locked_time\": \"0\",\n" +
                "    \"password\": \"KYLIN\",\n" +
                "    \"default_password\": \"true\",\n" +
                "    \"disabled\": \"false\",\n" +
                "    \"locked\": \"false\",\n" +
                "    \"uuid\": \"\",\n" +
                "    \"last_modified\": \"0\",\n" +
                "    \"version\": \"3\",\n" +
                "    \"authorities\": [\n" +
                "        {\n" +
                "            \"authority\": \"test_group\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"wrong_time\": \"0\",\n" +
                "    \"first_login_failed_time\": \"0\",\n" +
                "    \"username\": \"test_user\"\n" +
                "}");
        KylinUserInfo userInfo = new KylinUserInfo(kylinJson);
        assertEquals(userInfo.getUsername(), "test_user");
        assertEquals(userInfo.getPassword(), "KYLIN");
        assertEquals(userInfo.getAuthorities().get(0).getAuthority(), "test_group");
        assertFalse(userInfo.isDisabled());
        assertTrue(userInfo.isDefaultPassword());
        assertFalse(userInfo.isLocked());
        assertEquals(userInfo.getLockedTime(), 0L);
        assertEquals(userInfo.getWrongTime(), 0L);
        assertEquals(userInfo.getFirstLoginFailedTime(), 0L);
        assertEquals(userInfo.getUuid(), "");
        assertEquals(userInfo.getLastModified(), 0L);
        assertEquals(userInfo.getVersion(), "3");
    }

}