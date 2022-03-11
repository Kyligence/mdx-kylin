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


package io.kylin.mdx.insight.common.http;

import com.alibaba.fastjson.JSONObject;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.constants.ErrorConstants;
import org.apache.commons.lang3.StringUtils;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.ErrorCode;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.*;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpUtil {

    private static SemanticConfig config = SemanticConfig.getInstance();

    @Setter
    private static ApacheHttpClient httpClient = ApacheHttpClient.INSTANCE;

    private static final String HTTP_URL_NOT_ALLOW_EMPTY = "The HTTP calls require that the uri not be empty.";

    public static Response httpPostWithResponse(String uri, byte[] auth, String reqBody, boolean isFullUri) {
        CloseableHttpResponse response = httpPost(uri, auth, reqBody, isFullUri);
        return createResponse(response);
    }

    private static Response createResponse(CloseableHttpResponse response) {
        Response httpResponse = new Response();
        httpResponse.setHttpStatus(response.getStatusLine().getStatusCode());
        httpResponse.setContent(getHttpRespBody(response));

        return httpResponse;
    }

    public static CloseableHttpResponse httpPost(String uri, byte[] auth, String reqBody, boolean isFullUri) {
        if (StringUtils.isBlank(uri)) {
            throw new SemanticException(HTTP_URL_NOT_ALLOW_EMPTY);
        }

        HttpPost httpPost;
        if (isFullUri) {
            httpPost = new HttpPost(uri);
        } else {
            httpPost = new HttpPost(buildFullURI(getHttpAPI(uri), null));
        }

        httpPost.setEntity(new StringEntity(reqBody, ContentType.APPLICATION_JSON));
        prepareHttpHeaders(httpPost, auth);

        return execute(httpPost);
    }

    public static CloseableHttpResponse httpPost(String uri, byte[] auth, String reqBody, boolean isV2, boolean isFullUri) {
        if (StringUtils.isBlank(uri)) {
            throw new SemanticException(HTTP_URL_NOT_ALLOW_EMPTY);
        }

        HttpPost httpPost;
        if (isFullUri) {
            httpPost = new HttpPost(uri);
        } else {
            httpPost = new HttpPost(buildFullURI(getHttpAPI(uri), null));
        }

        httpPost.setEntity(new StringEntity(reqBody, ContentType.APPLICATION_JSON));
        prepareHttpHeaders(httpPost, auth, isV2);

        return execute(httpPost);
    }

    private static String getHttpRespBody(CloseableHttpResponse response) throws SemanticException {
        try {
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SemanticException("The http calling invoke exception.", e);
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                log.error("The http closing response gets exception", e);
            }
        }
    }

    public static CloseableHttpResponse httpGet(String uri, byte[] auth) throws SemanticException {
        return httpGet(uri, auth, null, false);
    }

    public static CloseableHttpResponse httpGet(String uri, byte[] auth,
                                                List<NameValuePair> nameValuePairs,
                                                boolean isFullUri) throws SemanticException {
        if (StringUtils.isBlank(uri)) {
            throw new SemanticException(HTTP_URL_NOT_ALLOW_EMPTY);
        }
        HttpGet httpGet;
        if (isFullUri) {
            httpGet = new HttpGet(uri);
        } else {
            httpGet = new HttpGet(buildFullURI(getHttpAPI(uri), nameValuePairs));
        }
        prepareHttpHeaders(httpGet, auth);
        return execute(httpGet);
    }

    private static void prepareHttpHeaders(HttpUriRequest request, byte[] auth) {
        if (auth != null) {
            request.setHeader("Authorization", "Basic " + new String(auth, StandardCharsets.UTF_8));
        }
        request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setHeader("Accept-Language", "en-US,en;q=0.5");
    }

    private static CloseableHttpResponse execute(HttpUriRequest request) throws SemanticException {
        try {
            CloseableHttpResponse httpResponse = httpClient.execute(request);
            preHandleHttpResponse(httpResponse, request.getURI().toString());
            return httpResponse;
        } catch (IOException e) {
            //TODO: httpclient execute need to handle precisely
            throw new SemanticException(e, ErrorCode.DISCONNECT_TO_KYLIN);
        }
    }

    /**
     * for dealing with Exception from Kylin
     *
     * @throws Exception
     */
    @SneakyThrows
    public static void preHandleHttpResponse(HttpResponse response, String api) {
        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode >= HttpStatus.SC_BAD_REQUEST) {
            String content = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JSONObject jsonObject = JSONObject.parseObject(content);
            String message = jsonObject.getString("msg");
            if (StringUtils.isBlank(message)) {
                message = content;
            }
            log.error("Http call not 200 status, statusCode:[{}], api:[{}], content:[{}]", responseCode, api, content);
            handleHttpError(message);
        }
    }

    private static void handleHttpError(String errorMessage) {
        // user locked
        if (errorMessage.contains("locked")) {
            throw new SemanticException(errorMessage, ErrorCode.USER_LOCKED);
        }
        // user disabled
        if (errorMessage.contains(ErrorConstants.KE_USER_DISABLE)) {
            throw new SemanticException(errorMessage, ErrorCode.USER_DISABLE);
        }
        // license expired
        if (errorMessage.contains(ErrorConstants.LICENSE_OUTDATED)) {
            throw new SemanticException(errorMessage, ErrorCode.EXPIRED_LICENSE);
        }
        // invalid password
        if (errorMessage.contains("Invalid")) {
            throw new SemanticException(errorMessage, ErrorCode.USER_OR_PASSWORD_ERROR);
        }
        // user not found
        if (errorMessage.contains("User") && errorMessage.contains("not found")) {
            throw new SemanticException(errorMessage, ErrorCode.USER_NOT_FOUND);
        }
        // project not found
        if (errorMessage.contains("Can't find project")) {
            throw new SemanticException(errorMessage, ErrorCode.KYLIN_NOT_FIND_PROJECT);
        }
        throw new SemanticException(errorMessage, ErrorCode.UNKNOWN_ERROR);
    }

    private static URI buildFullURI(String httpUrl, List<NameValuePair> list) {
        try {
            if (Utils.isCollectionEmpty(list)) {
                return new URIBuilder(httpUrl).build();
            } else {
                return new URIBuilder(httpUrl).addParameters(list).build();
            }

        } catch (URISyntaxException e) {
            throw new SemanticException(e);
        }
    }

    private static void prepareHttpHeaders(HttpUriRequest request, byte[] auth, boolean isV2) {
        if (auth != null) {
            request.setHeader("Authorization", "Basic " + new String(auth, StandardCharsets.UTF_8));
        }
        request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setHeader("Accept-Language", "en-US,en;q=0.5");
        // TODO: remove useless header
        if (config.isKylinAutoHeaderEnable()) {
            request.addHeader("Auto", "true");
        }
        if (isV2) {
            request.setHeader("Accept", "application/vnd.apache.kylin-v2+json");
        }
    }

    private static String getHttpAPI(String api) throws SemanticException {
        if (config == null) {
            config = SemanticConfig.getInstance();
        }
        if (StringUtils.isBlank(config.getKylinHost()) || StringUtils.isBlank(config.getKylinPort())) {
            throw new SemanticException("Kylin host or port is not configured.");
        }
        //对空格进行URL编码
        api = api.replaceAll(" ", "%20");
        return config.getKylinProtocol() + "://" + config.getKylinHost() + ":" + config.getKylinPort() + api;
    }

    @Slf4j
    public static class ApacheHttpClient implements Runnable {

        public static final ApacheHttpClient INSTANCE = new ApacheHttpClient();

        private final CloseableHttpClient httpClient;

        private final PoolingHttpClientConnectionManager connectionManager;

        private final int closeConnTimeInterval = 30000;

        public static class TrustAllTrustManager implements TrustManager, X509TrustManager {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

        }

        private ApacheHttpClient() {
            TrustManager[] trustAllCerts = {new TrustAllTrustManager()};
            SSLContext sc;
            try {
                sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, null);
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                sc = null;
                log.error("SSLContexts init catch exception", e);
            }
            Registry<ConnectionSocketFactory> registry;
            if (sc != null) {
                registry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", new PlainConnectionSocketFactory())
                        .register("https", new SSLConnectionSocketFactory(sc, NoopHostnameVerifier.INSTANCE))
                        .build();
            } else {
                registry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", new PlainConnectionSocketFactory())
                        .register("https", new SSLConnectionSocketFactory(SSLContexts.createDefault(), NoopHostnameVerifier.INSTANCE))
                        .build();
            }

            this.connectionManager = new PoolingHttpClientConnectionManager(registry);
            connectionManager.setMaxTotal(config.getConnectionMaxTotal());
            connectionManager.setDefaultMaxPerRoute(100);
            connectionManager.setValidateAfterInactivity(30000);

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(config.getConnectTimeout())
                    .setSocketTimeout(config.getSocketTimeout())
                    .setConnectionRequestTimeout(config.getConnectionRequestTimeout())
                    .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                    .build();


            ConnectionKeepAliveStrategy keepAliveStrategy = (response, context) -> {
                // Honor 'keep-alive' header
                HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                    HeaderElement he = it.nextElement();
                    String param = he.getName();
                    String value = he.getValue();
                    if (value != null && "timeout".equalsIgnoreCase(param)) {
                        try {
                            return Long.parseLong(value) * 1000;
                        } catch (NumberFormatException ignore) {
                        }
                    }
                }

                return 15 * 1000;
            };

            this.httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setConnectionManagerShared(config.getConnectionManagerShared())
                    .setDefaultRequestConfig(requestConfig)
                    .setKeepAliveStrategy(keepAliveStrategy)
                    .build();

            SecurityManager s = System.getSecurityManager();
            ThreadGroup group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            Thread idleMonitor = new Thread(group, this, "http-idle-connection-Monitor");
            idleMonitor.setDaemon(true);
            idleMonitor.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    log.info("close http client...");
                    httpClient.close();
                } catch (IOException e) {
                    log.error("close httpclient get exception", e);
                }
            }));
        }

        public PoolingHttpClientConnectionManager getConnectionManager() {
            return connectionManager;
        }

        public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
            return httpClient.execute(request);
        }

        public int getCloseConnTimeInterval() {
            return closeConnTimeInterval;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Thread.sleep(getCloseConnTimeInterval());
                    connectionManager.closeExpiredConnections();
                    connectionManager.closeIdleConnections(60, TimeUnit.SECONDS);
                }
            } catch (Exception ex) {
                log.error("IdleConnectionMonitor thread get exception and exit", ex);
            }
        }

    }

}
