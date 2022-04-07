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


package io.kylin.mdx.insight.core.support;

import com.alibaba.fastjson.JSONObject;
import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticUserAndPwd;
import io.kylin.mdx.insight.common.constants.ConfigConstants;
import io.kylin.mdx.insight.common.http.Response;
import io.kylin.mdx.insight.common.util.AESWithPBEEncryptor;
import io.kylin.mdx.insight.common.util.AESWithECBEncryptor;
import io.kylin.mdx.insight.core.entity.AADInfo;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.core.meta.SemanticAdapter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MdxPropertySourceLoader implements PropertySourceLoader {

    private final PropertiesPropertySourceLoader propLoader = new PropertiesPropertySourceLoader();

    private final YamlPropertySourceLoader yamlLoader = new YamlPropertySourceLoader();

    private final SemanticConfig semanticConfig = SemanticConfig.getInstance();

    public static final String AUTH_TYPE = "oauth2";

    private static final String REMOTE_IP_HEADER = "server.tomcat.remote-ip-header";

    private static final String PROTOCOL_HEADER = "server.tomcat.protocol-header";

    private static final String USE_FORWARD_HEADERS = "server.use-forward-headers";

    @Override
    public String[] getFileExtensions() {
        return new String[]{"properties", "xml", "yml", "yaml"};
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        String filename = resource.getFilename();
        List<PropertySource<?>> propertySources;
        if (filename == null || filename.endsWith(".properties") || filename.endsWith(".xml")) {
            propertySources = propLoader.load(name, resource);
        } else {
            propertySources = yamlLoader.load(name, resource);
        }
        List<PropertySource<?>> newSources = new ArrayList<>();
        for (PropertySource propertySource : propertySources) {
            Map<String, ?> properties = (Map<String, ?>) propertySource.getSource();
            Map<String, Object> newProperties = new LinkedHashMap<>();
            for (String propertyKey : properties.keySet()) {
                Object propertyValue = properties.get(propertyKey);
                newProperties.put(propertyKey, handle(propertyKey, propertyValue));
            }
            newSources.add(new OriginTrackedMapPropertySource(name, Collections.unmodifiableMap(newProperties), true));
        }

        try {
            handleAad();
        } catch (Exception e) {
            // Nothing to do
            log.warn("Failed to handle aad", e);
        }
        return newSources;
    }

    private Object handle(String key, Object value) {
        if (SemanticConstants.DATABASE_PASSWORD.equalsIgnoreCase(key)) {
            try {
                return AESWithECBEncryptor.decrypt(value.toString());
            } catch (PwdDecryptException e) {
                log.error("Property [{}] decrypt error, Please check it in properties file!", key, e);
                throw new Error(e);
            }
        }
        return value;
    }

    private void handleAad() {
        String username = SemanticUserAndPwd.getUser();
        String password = SemanticUserAndPwd.getDecodedPassword();
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return;
        }
        ConnectionInfo connectionInfo = ConnectionInfo.builder().user(username).password(password).project("").build();
        Response profileResp = SemanticAdapter.INSTANCE.getProfileInfo(connectionInfo);
        if (profileResp != null && profileResp.getHttpStatus() == HttpStatus.SC_OK) {
            handleAadResponse(profileResp.getContent());
        }
    }

    public void handleAadResponse(String res) {
        JSONObject jsonObject = JSONObject.parseObject(res);
        if (jsonObject != null
                && SemanticConstants.STATUS_SUC.equals(jsonObject.getString(SemanticConstants.STATUS_KEY))
                && jsonObject.getJSONObject("data") != null) {
            AADInfo aadInfo = jsonObject.getJSONObject("data").toJavaObject(AADInfo.class);
            if (aadInfo != null && AUTH_TYPE.equals(aadInfo.getAuthType())) {
                addAadProperties(aadInfo);
            }
        }
    }

    public void addAadProperties(AADInfo aadInfo) {
        System.setProperty(ConfigConstants.IS_ENABLE_AAD, "true");
        System.setProperty(ConfigConstants.TENANT_ID, aadInfo.getTenantId());
        System.setProperty(ConfigConstants.CLIENT_ID, aadInfo.getClientId());
        AESWithPBEEncryptor encryptor = new AESWithPBEEncryptor();
        String clientSecret = encryptor.decrypt(aadInfo.getClientSecret());
        System.setProperty(ConfigConstants.CLIENT_SECRET, clientSecret);
        log.info("Auth type is: {}", AUTH_TYPE);
        System.setProperty(REMOTE_IP_HEADER, "X-FORWARDED-FOR");
        System.setProperty(PROTOCOL_HEADER, "X-Forwarded-Proto");
        System.setProperty(USE_FORWARD_HEADERS, "true");

        if (StringUtils.isNotBlank(semanticConfig.getAADRedirectUriTemplate())) {
            System.setProperty(ConfigConstants.REDIRECT_URI_TEMPLATE,
                    semanticConfig.getAADRedirectUriTemplate());
        }
    }

}
