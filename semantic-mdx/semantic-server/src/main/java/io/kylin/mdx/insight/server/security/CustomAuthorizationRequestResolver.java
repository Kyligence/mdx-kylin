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


package io.kylin.mdx.insight.server.security;

import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String REGISTRATION_ID_URI_VARIABLE_NAME = "registrationId";

    private static final char PATH_DELIMITER = '/';

    private final ClientRegistrationRepository clientRegistrationRepository;

    private final AntPathRequestMatcher authorizationRequestMatcher;

    private StringKeyGenerator stateGenerator;

    private final StringKeyGenerator secureKeyGenerator = new Base64StringKeyGenerator(
            Base64.getUrlEncoder().withoutPadding(), 96);

    private Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer = (customizer) -> {
    };

    /**
     * Constructs a {@code DefaultOAuth2AuthorizationRequestResolver} using the provided
     * parameters.
     * @param clientRegistrationRepository the repository of client registrations
     * @param authorizationRequestBaseUri the base {@code URI} used for resolving
     * authorization requests
     */
    public CustomAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository,
                                              String authorizationRequestBaseUri) {
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
        Assert.hasText(authorizationRequestBaseUri, "authorizationRequestBaseUri cannot be empty");
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.authorizationRequestMatcher = new AntPathRequestMatcher(
                authorizationRequestBaseUri + "/{" + REGISTRATION_ID_URI_VARIABLE_NAME + "}");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        String registrationId = this.resolveRegistrationId(request);
        if (registrationId == null) {
            return null;
        }
        String redirectUriAction = getAction(request, "login");
        return resolve(request, registrationId, redirectUriAction);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
        if (registrationId == null) {
            return null;
        }
        String redirectUriAction = getAction(request, "authorize");
        return resolve(request, registrationId, redirectUriAction);
    }

    /**
     * Sets the {@code Consumer} to be provided the
     * {@link OAuth2AuthorizationRequest.Builder} allowing for further customizations.
     * @param authorizationRequestCustomizer the {@code Consumer} to be provided the
     * {@link OAuth2AuthorizationRequest.Builder}
     * @since 5.3
     */
    public void setAuthorizationRequestCustomizer(
            Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer) {
        Assert.notNull(authorizationRequestCustomizer, "authorizationRequestCustomizer cannot be null");
        this.authorizationRequestCustomizer = authorizationRequestCustomizer;
    }

    private String getAction(HttpServletRequest request, String defaultAction) {
        String action = request.getParameter("action");
        if (action == null) {
            return defaultAction;
        }
        return action;
    }

    private OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId,
                                               String redirectUriAction) {
        if (registrationId == null) {
            return null;
        }
        ClientRegistration clientRegistration = this.clientRegistrationRepository.findByRegistrationId(registrationId);
        if (clientRegistration == null) {
            throw new IllegalArgumentException("Invalid Client Registration with Id: " + registrationId);
        }
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(OAuth2ParameterNames.REGISTRATION_ID, clientRegistration.getRegistrationId());
        OAuth2AuthorizationRequest.Builder builder = getBuilder(clientRegistration, attributes);

        String redirectUriStr = expandRedirectUri(request, clientRegistration, redirectUriAction);

        this.stateGenerator = new HexInternalOauth2KeyGenerator(request.getSession().getId());

        // @formatter:off
        builder.clientId(clientRegistration.getClientId())
                .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
                .redirectUri(redirectUriStr)
                .scopes(clientRegistration.getScopes())
                .state(this.stateGenerator.generateKey())
                .attributes(attributes);
        // @formatter:on

        this.authorizationRequestCustomizer.accept(builder);

        return builder.build();
    }

    private OAuth2AuthorizationRequest.Builder getBuilder(ClientRegistration clientRegistration,
                                                          Map<String, Object> attributes) {
        if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(clientRegistration.getAuthorizationGrantType())) {
            OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode();
            Map<String, Object> additionalParameters = new HashMap<>();
            if (!CollectionUtils.isEmpty(clientRegistration.getScopes())
                    && clientRegistration.getScopes().contains(OidcScopes.OPENID)) {
                // Section 3.1.2.1 Authentication Request -
                // https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest scope
                // REQUIRED. OpenID Connect requests MUST contain the "openid" scope
                // value.
                addNonceParameters(attributes, additionalParameters);
            }
            if (ClientAuthenticationMethod.NONE.equals(clientRegistration.getClientAuthenticationMethod())) {
                addPkceParameters(attributes, additionalParameters);
            }
            builder.additionalParameters(additionalParameters);
            return builder;
        }
        if (AuthorizationGrantType.IMPLICIT.equals(clientRegistration.getAuthorizationGrantType())) {
            return OAuth2AuthorizationRequest.implicit();
        }
        throw new IllegalArgumentException(
                "Invalid Authorization Grant Type (" + clientRegistration.getAuthorizationGrantType().getValue()
                        + ") for Client Registration with Id: " + clientRegistration.getRegistrationId());
    }

    private String resolveRegistrationId(HttpServletRequest request) {
        if (this.authorizationRequestMatcher.matches(request)) {
            return this.authorizationRequestMatcher.matcher(request).getVariables()
                    .get(REGISTRATION_ID_URI_VARIABLE_NAME);
        }
        return null;
    }

    /**
     * Expands the {@link ClientRegistration#getRedirectUri()} with following provided
     * variables:<br/>
     * - baseUrl (e.g. https://localhost/app) <br/>
     * - baseScheme (e.g. https) <br/>
     * - baseHost (e.g. localhost) <br/>
     * - basePort (e.g. :8080) <br/>
     * - basePath (e.g. /app) <br/>
     * - registrationId (e.g. google) <br/>
     * - action (e.g. login) <br/>
     * <p/>
     * Null variables are provided as empty strings.
     * <p/>
     * Default redirectUri is:
     * {@code org.springframework.security.config.oauth2.client.CommonOAuth2Provider#DEFAULT_REDIRECT_URL}
     * @return expanded URI
     */
    private static String expandRedirectUri(HttpServletRequest request, ClientRegistration clientRegistration,
                                            String action) {
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("registrationId", clientRegistration.getRegistrationId());
        // @formatter:off
        UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(UrlUtils.buildFullRequestUrl(request))
                .replacePath(request.getContextPath())
                .replaceQuery(null)
                .fragment(null)
                .build();
        // @formatter:on
        String scheme = uriComponents.getScheme();
        uriVariables.put("baseScheme", (scheme != null) ? scheme : "");
        String host = uriComponents.getHost();
        uriVariables.put("baseHost", (host != null) ? host : "");
        // following logic is based on HierarchicalUriComponents#toUriString()
        int port = uriComponents.getPort();
        uriVariables.put("basePort", (port == -1) ? "" : ":" + port);
        String path = uriComponents.getPath();
        if (StringUtils.hasLength(path)) {
            if (path.charAt(0) != PATH_DELIMITER) {
                path = PATH_DELIMITER + path;
            }
        }
        uriVariables.put("basePath", (path != null) ? path : "");
        uriVariables.put("baseUrl", uriComponents.toUriString());
        uriVariables.put("action", (action != null) ? action : "");
        return UriComponentsBuilder.fromUriString(clientRegistration.getRedirectUri()).buildAndExpand(uriVariables)
                .toUriString();
    }

    /**
     * Creates nonce and its hash for use in OpenID Connect 1.0 Authentication Requests.
     * @param attributes where the {@link OidcParameterNames#NONCE} is stored for the
     * authentication request
     * @param additionalParameters where the {@link OidcParameterNames#NONCE} hash is
     * added for the authentication request
     *
     * @since 5.2
     * @see <a target="_blank" href=
     * "https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest">3.1.2.1.
     * Authentication Request</a>
     */
    private void addNonceParameters(Map<String, Object> attributes, Map<String, Object> additionalParameters) {
        try {
            String nonce = this.secureKeyGenerator.generateKey();
            String nonceHash = createHash(nonce);
            attributes.put(OidcParameterNames.NONCE, nonce);
            additionalParameters.put(OidcParameterNames.NONCE, nonceHash);
        }
        catch (NoSuchAlgorithmException ex) {
        }
    }

    /**
     * Creates and adds additional PKCE parameters for use in the OAuth 2.0 Authorization
     * and Access Token Requests
     * @param attributes where {@link PkceParameterNames#CODE_VERIFIER} is stored for the
     * token request
     * @param additionalParameters where {@link PkceParameterNames#CODE_CHALLENGE} and,
     * usually, {@link PkceParameterNames#CODE_CHALLENGE_METHOD} are added to be used in
     * the authorization request.
     *
     * @since 5.2
     * @see <a target="_blank" href="https://tools.ietf.org/html/rfc7636#section-1.1">1.1.
     * Protocol Flow</a>
     * @see <a target="_blank" href="https://tools.ietf.org/html/rfc7636#section-4.1">4.1.
     * Client Creates a Code Verifier</a>
     * @see <a target="_blank" href="https://tools.ietf.org/html/rfc7636#section-4.2">4.2.
     * Client Creates the Code Challenge</a>
     */
    private void addPkceParameters(Map<String, Object> attributes, Map<String, Object> additionalParameters) {
        String codeVerifier = this.secureKeyGenerator.generateKey();
        attributes.put(PkceParameterNames.CODE_VERIFIER, codeVerifier);
        try {
            String codeChallenge = createHash(codeVerifier);
            additionalParameters.put(PkceParameterNames.CODE_CHALLENGE, codeChallenge);
            additionalParameters.put(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256");
        }
        catch (NoSuchAlgorithmException ex) {
            additionalParameters.put(PkceParameterNames.CODE_CHALLENGE, codeVerifier);
        }
    }

    private static String createHash(String value) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(value.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

}
