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
import io.kylin.mdx.insight.server.security.CustomAuthorizationRequestResolver;
import io.kylin.mdx.insight.server.support.MdxLogoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

@Component
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private AuthService authService;

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Override
    protected void configure(HttpSecurity security) throws Exception {
        if (!SemanticConfig.getInstance().isEnableAAD()) {
            security.cors().and()
                    .csrf().disable();
            return;
        }

        security.oauth2Login(Customizer.withDefaults());
        security.oauth2Client();
        security.cors().and()
                .csrf().disable();

        security.logout()
                .logoutRequestMatcher(new AntPathRequestMatcher("/api/logout", "GET"))
                .logoutSuccessHandler(new MdxLogoutHandler(HttpStatus.OK, authService))
                .clearAuthentication(true)
                .invalidateHttpSession(true)
                .deleteCookies();
        security.logout()
                .logoutSuccessUrl(SemanticConfig.getInstance().getAADLogoutUrl());

        if (SemanticConfig.getInstance().isEnableAADInternalRedirect() && clientRegistrationRepository != null) {
            security.oauth2Login()
                    .authorizationEndpoint()
                    .authorizationRequestResolver(new CustomAuthorizationRequestResolver(clientRegistrationRepository,
                            OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI));
            security.sessionManagement().sessionFixation().none();
        }
    }


}
