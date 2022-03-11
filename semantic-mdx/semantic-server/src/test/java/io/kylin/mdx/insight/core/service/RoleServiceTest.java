package io.kylin.mdx.insight.core.service;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.core.dao.RoleInfoMapper;
import io.kylin.mdx.insight.core.dao.UserInfoMapper;
import io.kylin.mdx.insight.core.entity.RoleInfo;
import io.kylin.mdx.insight.core.entity.UserInfo;
import io.kylin.mdx.insight.core.entity.VisibleAttr;
import io.kylin.mdx.insight.engine.service.RoleServiceImpl;
import io.kylin.mdx.insight.server.SemanticLauncher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(classes = SemanticLauncher.class)
@RunWith(SpringRunner.class)
public class RoleServiceTest extends BaseEnvSetting {

    @InjectMocks
    private RoleServiceImpl roleService;

    @Mock
    private RoleInfoMapper roleInfoMapper;

    @Mock
    private UserInfoMapper userInfoMapper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    @Test
    public void testInsertRole() {
        List<VisibleAttr> visibleAttrs = new ArrayList<>();
        VisibleAttr visibleAttr = new VisibleAttr();
        visibleAttr.setName("vis");
        visibleAttr.setType(SemanticConstants.USER);
        visibleAttrs.add(visibleAttr);
        RoleInfo roleInfo = new RoleInfo("admin", visibleAttrs, "admin");
        Mockito.when(userInfoMapper.selectByUserName(any())).thenReturn(null);
        roleService.insertRole(roleInfo);
    }
    @Test
    public void testInsertRoleDB() {
        RoleInfo roleInfo = createRoleInfo();
        roleInfo.setId(1);
        Mockito.when(userInfoMapper.selectByUserName(any())).thenReturn(new UserInfo());
        Mockito.when(roleInfoMapper.insertOneReturnId(any())).thenReturn(1);
        roleService.insertRole(roleInfo);
    }
    @Test
    public void testUpdateRoleVerifyName() {
        RoleInfo roleInfo = new RoleInfo();
        roleInfo.setName("a+b");
        roleService.updateRole(roleInfo, 1);
    }
    @Test
    public void testUpdateRoleDBHave() {
        Mockito.when(userInfoMapper.selectByUserName(any())).thenReturn(new UserInfo());

        Mockito.when(userInfoMapper.selectByUserName(any())).thenReturn(new UserInfo());
        roleService.updateRole(createRoleInfo(), 1);
    }
    @Test
    public void testUpdateRoleDB() {
        RoleInfo roleInfo = createRoleInfo();
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(roleInfo);
        roleService.updateRole(roleInfo, 1);
    }

    @Test
    public void testUpdateRoleDBOtherName() {
        RoleInfo roleInfo = createRoleInfo();
        roleInfo.setName("role");
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(roleInfo);
        roleService.updateRole(roleInfo, 1);
    }

    private RoleInfo createRoleInfo() {
        List<VisibleAttr> visibleAttrs = new ArrayList<>();
        VisibleAttr visibleAttr = new VisibleAttr();
        visibleAttr.setName("vis");
        visibleAttr.setType(SemanticConstants.USER);
        visibleAttrs.add(visibleAttr);
        RoleInfo roleInfo = new RoleInfo("admin", visibleAttrs, "admin");
        return roleInfo;
    }
    private RoleInfo createRoleInfoWithNameType(String name, String type) {
        List<VisibleAttr> visibleAttrs = new ArrayList<>();
        VisibleAttr visibleAttr = new VisibleAttr();
        visibleAttr.setName(name);
        visibleAttr.setType(type);
        visibleAttrs.add(visibleAttr);
        RoleInfo roleInfo = new RoleInfo("admin", visibleAttrs, "admin");
        return roleInfo;
    }
    @Test
    public void testDeleteUserFromRole() {
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(null);
        RoleInfo roleInfo = new RoleInfo();
        roleInfo.setName("zhang");
        Assert.assertNotNull(roleService.deleteUserFromRole(roleInfo, 1));
    }
    @Test
    public void testDeleteUserFromRoleNotEqRoleId() {
        RoleInfo roleInfo = new RoleInfo();
        roleInfo.setName("zhang");
        roleInfo.setId(2);
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(roleInfo);
        RoleInfo roleInfoUser = new RoleInfo();
        roleInfoUser.setName("zhang");
        Assert.assertNotNull(roleService.deleteUserFromRole(roleInfo, 1));
    }
    @Test
    public void testDeleteUserFromRoleNumError() {
        RoleInfo roleInfo = new RoleInfo();
        roleInfo.setName("zhang");
        roleInfo.setId(1);
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(roleInfo);
        RoleInfo roleInfoUser = new RoleInfo();
        roleInfoUser.setName("zhang");
        Assert.assertNotNull(roleService.deleteUserFromRole(roleInfo, 1));
    }
    @Test
    public void testDeleteUserFromRoleNameError() {
        RoleInfo roleInfo = createRoleInfoWithNameType("", SemanticConstants.USER);
        roleInfo.setId(1);
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(roleInfo);
        Assert.assertNotNull(roleService.deleteUserFromRole(roleInfo, 1));
    }
    @Test
    public void testDeleteUserFromRoleTypeError() {
        RoleInfo roleInfo = createRoleInfoWithNameType("zhang", SemanticConstants.ROLE);
        roleInfo.setId(1);
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(roleInfo);
        Assert.assertNotNull(roleService.deleteUserFromRole(roleInfo, 1));
    }
    @Test
    public void testDeleteUserFromRoleVisibleAttrNameError() {
        RoleInfo roleInfo = createRoleInfoWithNameType("zhang", SemanticConstants.USER);
        roleInfo.setId(1);
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(roleInfo);
        Assert.assertNotNull(roleService.deleteUserFromRole(roleInfo, 1));
    }
    @Test
    public void testAddUserToRoleDBNo() {
        RoleInfo roleInfo = createRoleInfoWithNameType("zhang", SemanticConstants.USER);
        roleService.addUserToRole(roleInfo, 1);
    }
    @Test
    public void testAddUserToRoleIdError() {
        RoleInfo roleInfo = createRoleInfoWithNameType("zhang", SemanticConstants.USER);
        roleInfo.setId(1);
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(roleInfo);
        roleService.addUserToRole(roleInfo, 2);
    }
    @Test
    public void testAddUserToRoleNumError() {
        RoleInfo roleInfo = new RoleInfo();
        roleInfo.setName("zhang");
        roleInfo.setId(1);
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(roleInfo);
        RoleInfo roleInfoUser = new RoleInfo();
        roleInfoUser.setName("zhang");
        roleService.addUserToRole(roleInfo, 1);
    }
    @Test
    public void testAddUserToRoleNameError() {
        RoleInfo roleInfo = createRoleInfoWithNameType("", SemanticConstants.USER);
        roleInfo.setId(1);
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(roleInfo);
        Assert.assertNotNull(roleService.addUserToRole(roleInfo, 1));
    }

    @Test
    public void testAddUserToRoleTypeError() {
        RoleInfo roleInfo = createRoleInfoWithNameType("zhang", SemanticConstants.ROLE);
        roleInfo.setId(1);
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(roleInfo);
        Assert.assertNotNull(roleService.addUserToRole(roleInfo, 1));
    }
    @Test
    public void testAddUserToRole() {
        RoleInfo roleInfo = createRoleInfoWithNameType("zhang", SemanticConstants.USER);
        roleInfo.setId(1);
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(roleInfo);
        Assert.assertNotNull(roleService.addUserToRole(roleInfo, 1));
    }
    @Test
    public void testAddUserToRoleDBHasUser() {
        RoleInfo roleInfo = createRoleInfoWithNameType("admin", SemanticConstants.USER);
        roleInfo.setId(1);
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(roleInfo);
        Mockito.when(userInfoMapper.selectByUserName(any())).thenReturn(new UserInfo());
        Assert.assertNotNull(roleService.addUserToRole(roleInfo, 1));
    }
    @Test
    public void testAddUserToRoleContainName() {
        RoleInfo roleInfo = createRoleInfoWithNameType("zhang", SemanticConstants.USER);
        roleInfo.setId(1);
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(roleInfo);
        Mockito.when(userInfoMapper.selectByUserName(any())).thenReturn(new UserInfo());
        Assert.assertNotNull(roleService.addUserToRole(createRoleInfoWithNameType("wang", SemanticConstants.USER), 1));
    }

    @Test
    public void testAddUserToRoleNoAttr() {
        RoleInfo roleInfo = new RoleInfo();
        roleInfo.setName("zhang");
        roleInfo.setId(1);
        Mockito.when(roleInfoMapper.selectOne(any())).thenReturn(roleInfo);
        Mockito.when(userInfoMapper.selectByUserName(any())).thenReturn(new UserInfo());
        Assert.assertNotNull(roleService.addUserToRole(createRoleInfoWithNameType("zhang", SemanticConstants.USER), 1));
    }

}
