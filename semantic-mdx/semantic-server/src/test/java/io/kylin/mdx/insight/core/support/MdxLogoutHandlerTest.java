package io.kylin.mdx.insight.core.support;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.server.support.MdxLogoutHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MdxLogoutHandlerTest extends BaseEnvSetting {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private AuthService authService;

    @Test
    public void test() throws IOException {
        PrintWriter writer = new PrintWriter(new StringWriter());
        when(response.getWriter()).thenReturn(writer);

        MdxLogoutHandler handler = new MdxLogoutHandler(HttpStatus.OK, authService);
        handler.onLogoutSuccess(request, response, authentication);
    }

}



