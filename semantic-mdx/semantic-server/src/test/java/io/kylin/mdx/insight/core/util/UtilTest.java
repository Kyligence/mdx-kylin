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


package io.kylin.mdx.insight.core.util;

import io.kylin.mdx.insight.base.BaseEnvSetting;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.http.HttpUtil;
import io.kylin.mdx.insight.common.http.Response;
import io.kylin.mdx.insight.common.util.Utils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class UtilTest extends BaseEnvSetting {

    @Test
    public void testSemanticConfig() throws IOException {

        System.setProperty("INSIGHT_HOME", "");
        String insightHome = SemanticConfig.getInstance().getInsightHome();
        System.out.println("*********INSIGHT_HOME: " + insightHome);
        Assert.assertEquals(new File("../").getCanonicalPath(), insightHome);

    }

    @Test
    public void testConvertDateStr() {
        String s = Utils.convertDateStr(1561910400, true);
        Assert.assertEquals("2019-07-01 00:00:00", s);

        String s2 = Utils.convertDateStr(1561910400000L, false);
        Assert.assertEquals("2019-07-01 00:00:00", s2);
    }

}
