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

package io.kylin.mdx.insight.core.utils;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

@Slf4j(topic = "mondrian")
public class HttpLogUtils {

    private static final String NL = System.getProperty("line.separator");

    public static void log(HttpServletRequest request, String requestBody) {
        StringBuilder buf = new StringBuilder();
        buf.append(NL);
        buf.append("************************* MDX Request Message **************************").append(NL);
        String requestUri = request.getRequestURL().toString();
        if (request.getQueryString() != null) {
            requestUri = requestUri + "?" + request.getQueryString();
        }
        buf.append(request.getMethod()).append(" ").append(requestUri);
        buf.append(NL);
        Enumeration<String> headerIter = request.getHeaderNames();
        while (headerIter.hasMoreElements()) {
            String headerName = headerIter.nextElement();
            Enumeration<String> values = request.getHeaders(headerName);
            while (values.hasMoreElements()) {
                buf.append(headerName).append(" : ").append(values.nextElement());
                buf.append(NL);
            }
        }
        buf.append(NL);
        buf.append(requestBody);
        buf.append(NL);
        buf.append("************************************************************************");
        log.info(buf.toString());
    }

    public static void log(HttpServletResponse response, String responseBody) {
        StringBuilder buf = new StringBuilder();
        buf.append(NL);
        buf.append("************************* MDX Response Message *************************").append(NL);
        buf.append("HTTP/1.1 ").append(response.getStatus()).append(NL);
        Set<String> headers = new HashSet<>(response.getHeaderNames());
        for (String headerName : headers) {
            Collection<String> values = response.getHeaders(headerName);
            for (String value : values) {
                buf.append(headerName).append(" : ").append(value);
                buf.append(NL);
            }
        }
        if (responseBody != null) {
            buf.append(NL);
            buf.append(responseBody);
            buf.append(NL);
        }
        buf.append("************************************************************************");
        log.info(buf.toString());
    }

}
