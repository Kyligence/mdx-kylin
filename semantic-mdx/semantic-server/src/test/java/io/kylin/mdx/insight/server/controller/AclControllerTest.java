package io.kylin.mdx.insight.server.controller;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.util.JacksonSerDeUtils;
import io.kylin.mdx.insight.core.model.acl.AclDataset;
import io.kylin.mdx.insight.core.model.generic.KylinUserInfo;
import io.kylin.mdx.insight.core.sync.MetaStore;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.facade.AclFacade;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AclControllerTest extends BaseEnvSetting {

    private static final String RESOURCE_ROOT = "/json/aclcontroller";

    private final AclController aclController;

    public AclControllerTest() {
        this.aclController = new AclController(new AclFacade());
    }

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("converter.mock", "true");
        System.setProperty("kylin.json.path", RESOURCE_ROOT);
        // 建议 wanghui -> w_group 用户关系
        List<KylinUserInfo> userInfoList = new ArrayList<>();
        KylinUserInfo userInfo = new KylinUserInfo();
        userInfo.setUsername("wanghui");
        userInfo.setAuthorities(Collections.singletonList(new KylinUserInfo.AuthorityInfo("w_group")));
        userInfoList.add(userInfo);
        MetaStore.getInstance().syncUserAndGroup(userInfoList);
    }

    @Test
    public void testIllegalArg() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> aclController.getAcl("learn_kylin", "bad", "Admin", null));
    }

}
