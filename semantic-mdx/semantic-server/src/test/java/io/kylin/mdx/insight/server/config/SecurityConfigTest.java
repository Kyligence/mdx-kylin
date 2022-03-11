package io.kylin.mdx.insight.server.config;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.constants.ConfigConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SecurityConfigTest extends BaseEnvSetting {

    @Mock
    private HttpSecurity security;

    @Mock
    private CorsConfigurer<HttpSecurity> corsConfigurer;

    @Mock
    private CsrfConfigurer<HttpSecurity> csrfConfigurer;

    @Mock
    private LogoutConfigurer<HttpSecurity> logoutConfigurer;

    @Mock
    private OAuth2LoginConfigurer<HttpSecurity> oAuth2LoginConfigurer;

    @Mock
    private OAuth2LoginConfigurer<HttpSecurity>.AuthorizationEndpointConfig authorizationEndpointConfig;

    @Mock
    private SessionManagementConfigurer<HttpSecurity> securitySessionManagementConfigurer;
    @Mock
    private SessionManagementConfigurer<HttpSecurity>.SessionFixationConfigurer sessionFixationConfigurer;

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    public void configure() throws Exception {
        SemanticConfig semanticConfig = SemanticConfig.getInstance();
        semanticConfig.getProperties().put(ConfigConstants.IS_ENABLE_AAD, "true");

        when(security.cors()).thenReturn(corsConfigurer);
        when(corsConfigurer.and()).thenReturn(security);
        when(security.csrf()).thenReturn(csrfConfigurer);
        when(csrfConfigurer.disable()).thenReturn(security);
        when(security.logout()).thenReturn(logoutConfigurer);
        when(logoutConfigurer.logoutRequestMatcher(any())).thenReturn(logoutConfigurer);
        when(logoutConfigurer.logoutSuccessHandler(any())).thenReturn(logoutConfigurer);
        when(logoutConfigurer.clearAuthentication(true)).thenReturn(logoutConfigurer);
        when(logoutConfigurer.invalidateHttpSession(true)).thenReturn(logoutConfigurer);
        when(logoutConfigurer.deleteCookies()).thenReturn(logoutConfigurer);
        when(logoutConfigurer.logoutSuccessUrl(any())).thenReturn(logoutConfigurer);

        SecurityConfig config = new SecurityConfig();
        config.configure(security);
        semanticConfig.getProperties().put(ConfigConstants.IS_AAD_INTERNAL_REDIRECT, "true");
        ReflectionTestUtils.setField(config, "clientRegistrationRepository", clientRegistrationRepository);
        when(security.oauth2Login()).thenReturn(oAuth2LoginConfigurer);
        when(oAuth2LoginConfigurer.authorizationEndpoint()).thenReturn(authorizationEndpointConfig);
        when(security.sessionManagement()).thenReturn(securitySessionManagementConfigurer);
        when(securitySessionManagementConfigurer.sessionFixation()).thenReturn(sessionFixationConfigurer);
        config.configure(security);
        semanticConfig.getProperties().remove(ConfigConstants.IS_ENABLE_AAD);
        config.configure(security);
    }

}