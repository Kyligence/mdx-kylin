package io.kylin.mdx.insight.core.service;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.http.Response;
import io.kylin.mdx.insight.common.util.BeanUtils;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.manager.ProjectManager;
import io.kylin.mdx.insight.core.meta.SemanticAdapter;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.engine.service.UserServiceImpl;
import io.kylin.mdx.ErrorCode;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SysAndCheckTest extends BaseEnvSetting {

    @Mock
    private SemanticAdapter semanticAdapter;

    @Mock
    private ProjectManager projectManager;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void sysAndCheck() {
        BeanUtils.setField(userService, SemanticAdapter.class, semanticAdapter);
        BeanUtils.setField(userService, ProjectManager.class, projectManager);

        // 登录成功
        ConnectionInfo connectionInfo = ConnectionInfo.builder().user("ADMIN").password("KYLIN").project(null).build();
        when(semanticAdapter.authentication(Utils.buildAuthentication("ADMIN", "KYLIN")))
                .thenReturn(new Response(HttpStatus.SC_OK, "OK"));
        Map<String, String> accessInfos = new HashMap<>();
        when(projectManager.getUserAccessProjects(connectionInfo))
                .thenReturn(accessInfos);
        // 校验成功
        accessInfos.put("learn_kylin", "GLOBAL_ADMIN");
        userService.systemAdminCheck("ADMIN", "KYLIN");
        // 非系统管理员
        accessInfos.put("learn_kylin", "ADMIN");
        try {
            userService.systemAdminCheck("ADMIN", "KYLIN");
            Assert.fail();
        } catch (SemanticException e) {
            Assert.assertEquals(ErrorCode.NOT_ADMIN_USER, e.getErrorCode());
        }
        // 非全局管理员
        accessInfos.clear();
        ConnectionInfo connectionInfo2 = ConnectionInfo.builder().user("ADMIN").password("KYLIN").project(null).delegate(null).build();
        when(semanticAdapter.getUserAuthority(connectionInfo2)).thenReturn(Arrays.asList("ALL_USERS", "ROLE_ADMIN"));
        userService.systemAdminCheck("ADMIN", "KYLIN");

        // 登录失败 - 错误账号密码
        when(semanticAdapter.authentication(Utils.buildAuthentication("ADMIN", "KYLIN")))
                .thenReturn(new Response(HttpStatus.SC_UNAUTHORIZED, "Invalid username or password."));
        try {
            userService.systemAdminCheck("ADMIN", "KYLIN");
            Assert.fail();
        } catch (SemanticException e) {
            Assert.assertEquals(ErrorCode.AUTH_FAILED, e.getErrorCode());
        }
        // 登录失败 - 用户禁用
        when(semanticAdapter.authentication(Utils.buildAuthentication("ADMIN", "KYLIN")))
                .thenReturn(new Response(HttpStatus.SC_UNAUTHORIZED, "User is disabled"));
        try {
            userService.systemAdminCheck("ADMIN", "KYLIN");
            Assert.fail();
        } catch (SemanticException e) {
            Assert.assertEquals(ErrorCode.INACTIVE_USER, e.getErrorCode());
        }
        // 登录失败 - 许可证过期
        when(semanticAdapter.authentication(Utils.buildAuthentication("ADMIN", "KYLIN")))
                .thenReturn(new Response(HttpStatus.SC_UNAUTHORIZED, "expired"));
        try {
            userService.systemAdminCheck("ADMIN", "KYLIN");
            Assert.fail();
        } catch (SemanticException e) {
            Assert.assertEquals(ErrorCode.EXPIRED_LICENSE, e.getErrorCode());
        }
    }

}
