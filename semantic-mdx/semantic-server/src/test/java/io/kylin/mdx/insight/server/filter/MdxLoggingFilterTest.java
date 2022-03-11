package io.kylin.mdx.insight.server.filter;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.util.UUIDUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.Filter;
import javax.servlet.ServletException;
import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class MdxLoggingFilterTest extends BaseEnvSetting {

    private final Filter filter = new MdxLoggingFilter();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Before
    public void before() throws ServletException {
        filter.init(new MockFilterConfig());
        request.setRequestURI("/mdx/xmla/learn_kylin");
        request.addParameter("enableDebugMode", "true");
        request.addHeader("Content-Type", "text/xml");
        request.addHeader("X-Trace-Id", UUIDUtils.randomUUID());
        response.addHeader("Content-Type", "text/xml;charset=UTF-8");
    }

    @Test
    public void testUrlFilter() throws IOException, ServletException {
        MockFilterChain filterChain = new MockFilterChain();
        filter.doFilter(request, response, filterChain);
    }

    @After
    public void after() {
        filter.destroy();
    }

}
