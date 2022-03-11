package io.kylin.mdx.insight.server.security;

import com.azure.spring.aad.AADClientRegistrationRepository;
import com.google.common.collect.Sets;
import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.SemanticConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.OidcScopes;

import javax.servlet.http.HttpSession;

import static io.kylin.mdx.insight.common.constants.ConfigConstants.AAD_AUTHENTICATION_CALLBACK_URL;
import static io.kylin.mdx.insight.common.constants.ConfigConstants.AAD_AUTHENTICATION_CODE_URL;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomAuthorizationRequestResolverTest extends BaseEnvSetting {

    @Mock
    private AADClientRegistrationRepository clientRegistrationRepository;

    @Test
    public void testResolve() {
        SemanticConfig semanticConfig = SemanticConfig.getInstance();
        semanticConfig.getProperties().put(AAD_AUTHENTICATION_CALLBACK_URL, "http://mdx/callback_url");
        semanticConfig.getProperties().put(AAD_AUTHENTICATION_CODE_URL, "http://mdx/login/oauth2/code/");

        ClientRegistration clientRegistration = Mockito.mock(ClientRegistration.class);
        when(clientRegistrationRepository.findByRegistrationId("azure")).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("azure");
        when(clientRegistration.getAuthorizationGrantType()).thenReturn(AuthorizationGrantType.AUTHORIZATION_CODE);
        when(clientRegistration.getRedirectUri()).thenReturn("https://kc/internal/oauth2/login/code/");
        when(clientRegistration.getClientId()).thenReturn("client-id");
        ClientRegistration.ProviderDetails providerDetails = Mockito.mock(ClientRegistration.ProviderDetails.class);
        when(clientRegistration.getProviderDetails()).thenReturn(providerDetails);
        when(providerDetails.getAuthorizationUri()).
                thenReturn("https://login.microsoftonline.com/9b3afd1e-9454-4d38-a14c-a0670c820d48/oauth2/v2.0/authorize");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setPathInfo("/oauth2/authorization/azure");
        HttpSession session = Mockito.mock(HttpSession.class);
        request.setSession(session);
        when(session.getId()).thenReturn("mock-session-id");

        CustomAuthorizationRequestResolver customAuthorizationRequestResolver =
                new CustomAuthorizationRequestResolver(clientRegistrationRepository,
                        OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);

        OAuth2AuthorizationRequest authorizationRequest = customAuthorizationRequestResolver.resolve(request);

        Assert.assertEquals("https://kc/internal/oauth2/login/code/", authorizationRequest.getRedirectUri());
        Assert.assertEquals("5f63616c6c6261636b5f75726c3d687474703a2f2f6d64782f63616c6c6261636b5f75726c265f636f64655f757" +
                "26c3d687474703a2f2f6d64782f6c6f67696e2f6f61757468322f636f64652f265f73657373696f6e5f69643d6d6f636b" +
                "2d73657373696f6e2d6964", authorizationRequest.getState());
    }

    @Test
    public void testResolveWithRegistrationId() {
        SemanticConfig semanticConfig = SemanticConfig.getInstance();
        semanticConfig.getProperties().put(AAD_AUTHENTICATION_CALLBACK_URL, "http://mdx/callback_url");
        semanticConfig.getProperties().put(AAD_AUTHENTICATION_CODE_URL, "http://mdx/login/oauth2/code/");

        ClientRegistration clientRegistration = Mockito.mock(ClientRegistration.class);
        when(clientRegistrationRepository.findByRegistrationId("azure")).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("azure");
        when(clientRegistration.getScopes()).thenReturn(Sets.newHashSet(OidcScopes.OPENID));
        when(clientRegistration.getClientAuthenticationMethod()).thenReturn(ClientAuthenticationMethod.NONE);
        when(clientRegistration.getAuthorizationGrantType()).thenReturn(AuthorizationGrantType.AUTHORIZATION_CODE);
        when(clientRegistration.getRedirectUri()).thenReturn("https://kc/internal/oauth2/login/code/");
        when(clientRegistration.getClientId()).thenReturn("client-id");
        ClientRegistration.ProviderDetails providerDetails = Mockito.mock(ClientRegistration.ProviderDetails.class);
        when(clientRegistration.getProviderDetails()).thenReturn(providerDetails);
        when(providerDetails.getAuthorizationUri()).
                thenReturn("https://login.microsoftonline.com/9b3afd1e-9454-4d38-a14c-a0670c820d48/oauth2/v2.0/authorize");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setPathInfo("/oauth2/authorization/azure");
        HttpSession session = Mockito.mock(HttpSession.class);
        request.setSession(session);
        when(session.getId()).thenReturn("mock-session-id");

        CustomAuthorizationRequestResolver customAuthorizationRequestResolver =
                new CustomAuthorizationRequestResolver(clientRegistrationRepository,
                        OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);

        OAuth2AuthorizationRequest authorizationRequest = customAuthorizationRequestResolver.resolve(request, "azure");

        Assert.assertEquals("https://kc/internal/oauth2/login/code/", authorizationRequest.getRedirectUri());
        Assert.assertEquals("5f63616c6c6261636b5f75726c3d687474703a2f2f6d64782f63616c6c6261636b5f75726c265f636f64655f757" +
                "26c3d687474703a2f2f6d64782f6c6f67696e2f6f61757468322f636f64652f265f73657373696f6e5f69643d6d6f636b" +
                "2d73657373696f6e2d6964", authorizationRequest.getState());
    }
}