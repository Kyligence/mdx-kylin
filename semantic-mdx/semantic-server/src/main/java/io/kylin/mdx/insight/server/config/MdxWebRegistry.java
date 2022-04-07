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


package io.kylin.mdx.insight.server.config;

import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.core.service.UserService;
import io.kylin.mdx.insight.server.filter.GZIPCompressionFilter;
import io.kylin.mdx.insight.server.filter.MdxLoggingFilter;
import io.kylin.mdx.insight.server.filter.MdxServiceFilter;
import io.kylin.mdx.insight.server.filter.UrlMappingFilter;
import io.kylin.mdx.web.xmla.MdxXmlaServlet;
import mondrian.xmla.XmlaServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MdxWebRegistry {

    private final Map<String, String> initParams = new HashMap<>();

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    public MdxWebRegistry() {
        initParams.put("CharacterEncoding", "UTF-8");
    }

    @Bean
    public ServletRegistrationBean<XmlaServlet> mdxServlet() {
        ServletRegistrationBean<XmlaServlet> mdxServletBean = new ServletRegistrationBean<>();
        mdxServletBean.setInitParameters(initParams);
        mdxServletBean.setName("MdxServlet");
        mdxServletBean.setServlet(new MdxXmlaServlet());
        mdxServletBean.addUrlMappings(
                SemanticConfig.getInstance().getMdxServletPath() + "*",
                SemanticConfig.getInstance().getMdxGatewayPath() + "*"
        );
        mdxServletBean.setLoadOnStartup(1);
        return mdxServletBean;
    }

    @Bean
    public FilterRegistrationBean<MdxServiceFilter> mdxFilter() {
        FilterRegistrationBean<MdxServiceFilter> registration = new FilterRegistrationBean<>();
        registration.setName("MdxFilter");
        registration.setFilter(new MdxServiceFilter());
        registration.addUrlPatterns(
                SemanticConfig.getInstance().getMdxServletPath() + "*",
                SemanticConfig.getInstance().getMdxGatewayPath() + "*"
        );
        registration.setOrder(4);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<GZIPCompressionFilter> gzipFilter() {
        FilterRegistrationBean<GZIPCompressionFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new GZIPCompressionFilter());
        registration.addUrlPatterns(SemanticConfig.getInstance().getMdxGatewayPath() + "*");
        registration.setName("GzipFilter");
        registration.setOrder(3);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<MdxLoggingFilter> logFilter() {
        FilterRegistrationBean<MdxLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MdxLoggingFilter());
        registration.addUrlPatterns(
                SemanticConfig.getInstance().getMdxServletPath() + "*",
                SemanticConfig.getInstance().getMdxGatewayPath() + "*"
        );
        registration.setName("LogFilter");
        registration.setOrder(2);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<UrlMappingFilter> urlFilter() {
        FilterRegistrationBean<UrlMappingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new UrlMappingFilter(userService, authService));
        registration.addUrlPatterns("/*");
        registration.setName("UrlFilter");
        registration.setOrder(1);
        return registration;
    }

}
