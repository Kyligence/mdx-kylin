package io.kylin.mdx.insight.server.filter;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.*;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

@RunWith(MockitoJUnitRunner.class)
public class GZipCompressionFilterTest {

    private static final String TEST_STRING = "Test string: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBBBBBBBBBB";

    private Filter filter;
    private MockFilterChain filterChain;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static class GZIPTestServlet implements Servlet {

        @Override
        public void init(ServletConfig config) {
        }

        @Override
        public ServletConfig getServletConfig() {
            return null;
        }

        @Override
        public void service(ServletRequest req, ServletResponse res) throws IOException {
            res.getWriter().println(TEST_STRING);
            res.flushBuffer();
        }

        @Override
        public String getServletInfo() {
            return null;
        }

        @Override
        public void destroy() {
        }

    }

    @Before
    public void before() throws ServletException {
        filter = new GZIPCompressionFilter();
        filter.init(new MockFilterConfig());
        Servlet servlet = new GZIPTestServlet();
        filterChain = new MockFilterChain(servlet);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    public void testGZIPCompression() throws IOException, ServletException {
        filter.doFilter(request, response, filterChain);
        response.flushBuffer();
        byte[] expectedResult = new byte[]{31, -117, 8, 0, 0, 0, 0, 0, 0, 0, 11, 73, 45, 46, 81, 40, 46, 41, -54, -52, 75, -73, 82, 112, -60, 11, -100, 112, 1, 46, 0, 54, 83, -71, 9, 69, 0, 0, 0};
        assertArrayEquals(expectedResult, response.getContentAsByteArray());
    }

    @After
    public void after() {
        filter.destroy();
    }

}
