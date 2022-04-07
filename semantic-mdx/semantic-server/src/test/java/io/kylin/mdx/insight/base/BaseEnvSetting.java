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


package io.kylin.mdx.insight.base;

import io.kylin.mdx.insight.core.meta.mock.MockTestLoader;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class BaseEnvSetting {

    static {
        System.setProperty("MDX_HOME", "src/test/resources/");
        System.setProperty("MDX_CONF", "src/test/resources/conf");
        mockResource();
    }

    public static String loadResource(String resource) {
        try (InputStream inputStream = BaseEnvSetting.class.getResourceAsStream(resource)) {
            return IOUtils.toString(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void mockResource() {
        MockTestLoader.put(new MockTestLoader.Prefix("/kylin/api/user_group/usersWithGroup"), "/kylin/kylin_get_groups.json");
        MockTestLoader.put(new MockTestLoader.Prefix("/kylin/api/acl/table/paged/"), new MockTestLoader.Value() {
            @Override
            public String get(String uri) {
                String fileName = uri.substring("/kylin/api/acl/table/paged/".length());
                fileName = "table_" + fileName.substring(0, fileName.indexOf("?")).replaceAll("/", "_").replace(".", "_");
                return "/json/aclcontroller/" + fileName + ".json";
            }
        });
        MockTestLoader.put(new MockTestLoader.Prefix("/kylin/api/acl/column/paged/"), new MockTestLoader.Value() {
            @Override
            public String get(String uri) {
                String fileName = uri.substring("/kylin/api/acl/column/paged/".length());
                fileName = "column_" + fileName.substring(0, fileName.indexOf("?")).replaceAll("/", "_").replace(".", "_");
                return "/json/aclcontroller/" + fileName + ".json";
            }
        });
    }

}
