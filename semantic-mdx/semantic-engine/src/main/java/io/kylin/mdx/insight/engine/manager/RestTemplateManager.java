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


package io.kylin.mdx.insight.engine.manager;

import io.kylin.mdx.insight.common.SemanticConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Configuration
public class RestTemplateManager {

    @Bean
    public RestTemplate httpsRestTemplate(HttpComponentsClientHttpRequestFactory httpsFactory) {
        RestTemplate restTemplate = new RestTemplate(httpsFactory);
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse clientHttpResponse) {
                return false;
            }

            @Override
            public void handleError(ClientHttpResponse clientHttpResponse) {
            }
        });
        return restTemplate;
    }

    @Bean(name = "httpsFactory")
    public HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory() throws Exception {
        CloseableHttpClient httpClient = acceptsUntrustedCertsHttpClient();
        HttpComponentsClientHttpRequestFactory httpsFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        httpsFactory.setReadTimeout(SemanticConfig.getInstance().getSocketTimeout());
        httpsFactory.setConnectTimeout(SemanticConfig.getInstance().getConnectTimeout());
        return httpsFactory;
    }

    public static CloseableHttpClient acceptsUntrustedCertsHttpClient() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        HttpClientBuilder b = HttpClientBuilder.create();

        // setup a Trust Strategy that allows all certificates.
        SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                return true;
            }
        }).build();
        b.setSSLContext(sslContext);

        // don't check Host names, either.
        HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;

        // create a SSL Socket Factory, to use our weakened "trust strategy"  and create a Registry, to register it.
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();

        // create connection-manager using registry allows multi-threaded use
        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connMgr.setMaxTotal(200);
        connMgr.setDefaultMaxPerRoute(100);
        b.setConnectionManager(connMgr);

        return b.build();
    }
}
