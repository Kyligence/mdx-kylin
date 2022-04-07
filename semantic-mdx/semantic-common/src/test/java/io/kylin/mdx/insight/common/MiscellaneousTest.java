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


package io.kylin.mdx.insight.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kylin.mdx.insight.common.http.HttpUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class MiscellaneousTest {

    @BeforeClass
    public static void setup() {
        System.setProperty("MDX_HOME", "../mdx/src/test/resources/");
    }

    @Test
    public void testGroupBy() throws JsonProcessingException {

        List<Apple> apples = Arrays.asList(
                new Apple("name1", "addr1"),
                new Apple("name2", "addr2"),
                new Apple("name1", "addr22"),
                new Apple("name2", "addr33"),
                new Apple("name3", "addr44")
        );

        apples.stream()
                .collect(Collectors.groupingBy(Apple::getName))
                .forEach((k, v) -> System.out.println(k + " " + Arrays.toString(v.toArray(new Apple[0]))));

        Map<String, List<Apple>> collect = apples.stream()
                .collect(Collectors.groupingBy(Apple::getName));

        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(collect));

        Assert.assertEquals(3, collect.keySet().size());
        Assert.assertEquals(2, collect.get("name1").size());
    }

    @Test
    public void testScheduleExecutor() {
        /*ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(3);

        executor.scheduleAtFixedRate(() -> log.info("1s..."), 0, 1, TimeUnit.SECONDS);

        executor.scheduleAtFixedRate(() -> log.info("5s..."), 0, 5, TimeUnit.SECONDS);

        try {
            Thread.sleep(60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }


    @Test
    public void testUri() throws URISyntaxException, UnsupportedEncodingException {

        String url = "http://ss.w.com/a/da/大反问我/d放到/" + URLEncoder.encode("a da  dafd", "UTF-8").replaceAll("\\+", "%20");

        URI build = new URIBuilder(url).build();

        Assert.assertEquals(build.toString(), url);

        URI uri = URI.create(url);
        Assert.assertEquals(uri.toString(), url);

        String str = URLDecoder.decode("http://www.a.com/da+/daa%203?da=da+da&aa=44%2044", "UTF-8");
        Assert.assertEquals(str, "http://www.a.com/da /daa 3?da=da da&aa=44 44");
    }

    @Test
    public void testSemanticException() {
        String errorMsg = "error on purpose";
        SemanticException semanticException = new SemanticException(errorMsg, new RuntimeException("run time exception"));

        Assert.assertEquals(errorMsg, semanticException.getErrorMsg());

        String errorMsg2 = "error on purpose again";
        semanticException.setErrorMsg(errorMsg2);

        Assert.assertEquals(errorMsg2, semanticException.getErrorMsg());

    }

    @Test
    public void testApacheHttpClient() {
        HttpUtil.ApacheHttpClient client = HttpUtil.ApacheHttpClient.INSTANCE;

        PoolingHttpClientConnectionManager connectionManager = client.getConnectionManager();

        Assert.assertEquals(200, connectionManager.getMaxTotal());
        Assert.assertEquals(30000, connectionManager.getValidateAfterInactivity());
        Assert.assertEquals(100, connectionManager.getMaxPerRoute(new HttpRoute(new HttpHost("10.1.9.45"))));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Apple {
        private String name;
        private String address;

    }

    @AfterClass
    public static void clear() {
        System.clearProperty("MDX_HOME");
    }
}
