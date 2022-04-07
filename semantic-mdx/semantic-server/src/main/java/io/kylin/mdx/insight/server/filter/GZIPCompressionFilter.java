/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.kylin.mdx.insight.server.filter;

import io.kylin.mdx.insight.common.SemanticConfig;
import org.springframework.http.HttpHeaders;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class GZIPCompressionFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!SemanticConfig.getInstance().isEnableCompressResult()) {
            chain.doFilter(request, response);
            return;
        }
        GZIPCompressionHttpServletResponseWrapper gzipCompressionResponse = new GZIPCompressionHttpServletResponseWrapper((HttpServletResponse) response);
        gzipCompressionResponse.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
        chain.doFilter(request, gzipCompressionResponse);
        gzipCompressionResponse.close();
    }

    @Override
    public void destroy() {
    }

}
