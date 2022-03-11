package io.kylin.mdx.insight.server.filter;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.engine.service.UserServiceImpl;
import io.kylin.mdx.insight.server.service.AuthServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.Filter;
import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

@RunWith(MockitoJUnitRunner.class)
public class UrlMappingFilterTest extends BaseEnvSetting {

    @Mock
    private UserServiceImpl userService;
    @Mock
    private AuthServiceImpl authService;

    private final Filter filter = new UrlMappingFilter(userService, authService);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Before
    public void before() throws ServletException {
        filter.init(new MockFilterConfig());
        request.setRequestURI("/");
    }

    @Test
    public void testUrlFilter() throws IOException, ServletException {
        MockFilterChain filterChain = new MockFilterChain();
        filter.doFilter(request, response, filterChain);
        response.flushBuffer();
        byte[] expectedResult = new byte[]{};
        assertArrayEquals(expectedResult, response.getContentAsByteArray());

        // 测试不同链接
        String[] urls = new String[]{
                "/mdx/xmla/learn_kylin", "/mdx/xmla_server/learn_kylin",
                "/static/index.html", "/locale", "/asset-manifest.json",
                "/favicon.png", "/manifest.json", "/robots.txt", "/service-worker.js"
        };
        for (String url : urls) {
            filterChain = new MockFilterChain();
            request.setRequestURI(url);
            filter.doFilter(request, response, filterChain);
        }
    }

    @After
    public void after() {
        filter.destroy();
    }

}
