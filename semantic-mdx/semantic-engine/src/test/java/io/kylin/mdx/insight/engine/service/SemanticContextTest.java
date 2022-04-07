package io.kylin.mdx.insight.engine.service;

import io.kylin.mdx.insight.core.dao.RoleInfoMapper;
import io.kylin.mdx.insight.core.entity.*;
import io.kylin.mdx.insight.core.service.RoleInfoLoader;
import io.kylin.mdx.insight.core.service.SemanticContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SemanticContextTest {

    @Mock
    private RoleInfoMapper roleInfoMapper;

    @Test
    public void testRoleInfoLoader() {
        RoleInfo roleInfo = new RoleInfo("Admin");
        roleInfo.setExtend("{\"contains\":[{\"type\":\"user\",\"name\":\"ADMIN\"}]}");
        when(roleInfoMapper.selectOne(roleInfo)).thenReturn(roleInfo);
        RoleInfoLoader infoLoader = new SemanticContextImpl.DefaultRoleInfoLoader(roleInfoMapper);
        List<VisibleAttr> attrs = infoLoader.load(roleInfo);
        assertEquals(attrs.size(), 1);
    }

    @Test
    public void testDatasetDesc() {
        DatasetKey datasetKey = new DatasetKey("ADMIN",
                "default_project", "learn_kylin", "MDX");
        assertEquals("default_project", datasetKey.getProject());
        assertEquals("learn_kylin", datasetKey.getDatasetName());
        assertEquals(DatasetType.MDX, DatasetType.valueOf(datasetKey.getDatasetType()));
    }

    @Test
    public void testUserDatasetDesc() {
        DatasetKey datasetKey = new DatasetKey(
                "ADMIN", "default_project", "learn_kylin", "MDX");
        assertEquals("ADMIN", datasetKey.getUsername());
        assertEquals("default_project", datasetKey.getProject());
        assertEquals("learn_kylin", datasetKey.getDatasetName());
        assertEquals(DatasetType.MDX, DatasetType.valueOf(datasetKey.getDatasetType()));
    }

    @Test
    public void isVisible() {
        SemanticContext semanticContext = new SemanticContextImpl();
        // 验证白名单模式-用户
        {
            BaseVisibility visibility = new BaseVisibility() {
                @Override
                public DescWrapperExtend buildExtend() {
                    VisibleAttr attr0 = new VisibleAttr();
                    attr0.setName("ZhangSan");
                    attr0.setType("user");
                    DescWrapperExtend extend = new DescWrapperExtend();
                    extend.addVisibleUser("ZhangSan");
                    return extend;
                }
            };
            RoleInfoLoader roleInfoLoader = roleInfo -> null;
            boolean visible = semanticContext.isVisible(visibility, false, "ZhangSan", roleInfoLoader);
            Assert.assertTrue(visible);
        }
        // 验证黑名单模式-用户
        {
            BaseVisibility visibility = new BaseVisibility() {
                @Override
                public DescWrapperExtend buildExtend() {
                    VisibleAttr attr0 = new VisibleAttr();
                    attr0.setName("ZhangSan");
                    attr0.setType("user");
                    DescWrapperExtend extend = new DescWrapperExtend();
                    extend.addInvisibleUser("ZhangSan");
                    return extend;
                }
            };
            RoleInfoLoader roleInfoLoader = roleInfo -> null;
            boolean visible = semanticContext.isVisible(visibility, true, "ZhangSan", roleInfoLoader);
            Assert.assertFalse(visible);
        }
        // 验证白名单模式-角色
        {
            BaseVisibility visibility = new BaseVisibility() {
                @Override
                public DescWrapperExtend buildExtend() {
                    VisibleAttr attr0 = new VisibleAttr();
                    attr0.setName("Admin");
                    attr0.setType("group");
                    DescWrapperExtend extend = new DescWrapperExtend();
                    extend.withVisible(Collections.singletonList(attr0));
                    return extend;
                }
            };
            RoleInfoLoader roleInfoLoader = roleInfo -> {
                VisibleAttr attr0 = new VisibleAttr();
                attr0.setName("ZhangSan");
                attr0.setType("group");
                return Collections.singletonList(attr0);
            };
            boolean visible1 = semanticContext.isVisible(visibility, false, "ZhangSan", roleInfoLoader);
            Assert.assertFalse(visible1);
        }
        // 验证白名单模式-角色
        {
            BaseVisibility visibility = new BaseVisibility() {
                @Override
                public DescWrapperExtend buildExtend() {
                    VisibleAttr attr0 = new VisibleAttr();
                    attr0.setName("Admin");
                    attr0.setType("role");
                    DescWrapperExtend extend = new DescWrapperExtend();
                    extend.withVisible(Collections.singletonList(attr0));
                    return extend;
                }
            };
            RoleInfoLoader roleInfoLoader = roleInfo -> {
                VisibleAttr attr0 = new VisibleAttr();
                attr0.setName("ZhangSan");
                attr0.setType("user");
                return Collections.singletonList(attr0);
            };
            boolean visible1 = semanticContext.isVisible(visibility, false, "ZhangSan", roleInfoLoader);
            Assert.assertTrue(visible1);
            boolean visible2 = semanticContext.isVisible(visibility, false, "LiSi", roleInfoLoader);
            Assert.assertFalse(visible2);
        }
        // 验证黑名单模式-角色
        {
            BaseVisibility visibility = new BaseVisibility() {
                @Override
                public DescWrapperExtend buildExtend() {
                    VisibleAttr attr0 = new VisibleAttr();
                    attr0.setName("Ops");
                    attr0.setType("role");
                    DescWrapperExtend extend = new DescWrapperExtend();
                    extend.withInvisible(Collections.singletonList(attr0));
                    return extend;
                }
            };
            RoleInfoLoader roleInfoLoader = roleInfo -> {
                VisibleAttr attr0 = new VisibleAttr();
                attr0.setName("ZhangSan");
                attr0.setType("user");
                return Collections.singletonList(attr0);
            };
            boolean visible1 = semanticContext.isVisible(visibility, true, "ZhangSan", roleInfoLoader);
            Assert.assertFalse(visible1);
            boolean visible2 = semanticContext.isVisible(visibility, true, "LiSi", roleInfoLoader);
            Assert.assertTrue(visible2);
        }
    }

    @Test
    public void testGetSemanticDataset() {
        SemanticContext semanticContext = new SemanticContextImpl();
        try {
            semanticContext.getSemanticDataset("ADMIN", "learn_kylin", "1");
        } catch (Exception e) {
        }
    }

    private static abstract class BaseVisibility implements Visibility {

        @Override
        public Boolean getVisibleFlag() {
            return true;
        }

    }

}
