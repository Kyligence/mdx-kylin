package io.kylin.mdx.insight.server.bean.dto;

import io.kylin.mdx.insight.core.entity.UserInfo;
import org.junit.Assert;
import org.junit.Test;

public class UserInfoDTOTest {
    @Test
    public void createUserInfo() {
        UserInfo userInfo =new UserInfo();
        userInfo.setUsername("test");
        UserInfoDTO userInfoDTO = new UserInfoDTO(userInfo);
        Assert.assertEquals(userInfoDTO.getName(), userInfo.getUsername());
    }
}
