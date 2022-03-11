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

import io.kylin.mdx.insight.common.SemanticConfig;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.keygen.StringKeyGenerator;

import java.nio.charset.StandardCharsets;


public class HexInternalOauth2KeyGenerator implements StringKeyGenerator {
    private static final String CALLBACK_URL = "_callback_url";
    private static final String AUTHENTICATION_CODE_URL = "_code_url";
    private static final String SESSION_ID = "_session_id";


    private String sessionId;

    public HexInternalOauth2KeyGenerator(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String generateKey() {
        SemanticConfig config = SemanticConfig.getInstance();
        byte[] bytes = String.format("%s=%s&%s=%s&%s=%s", CALLBACK_URL, config.getAADAuthenticationCallbackUrl(),
                        AUTHENTICATION_CODE_URL, config.getAADAuthenticationCodeUrl(), SESSION_ID, sessionId)
                .getBytes(StandardCharsets.UTF_8);
        return new String(Hex.encode(bytes));
    }
}
