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


package io.kylin.mdx.core.meta;

import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class KETest {

    @BeforeClass
    public static void setup() {
        System.setProperty("MDX_HOME", "src/test/resources/kylin");
        System.setProperty("MDX_CONF", "src/test/resources/kylin");
    }

    @Test
    public void buildConnectionInfoTest() {
        ConnectionInfo connInfo = ConnectionInfo.builder()
                .user("ADMIN")
                .password("KYLIN")
                .project("learn_kylin")
                .build();

        Assert.assertEquals("ADMIN", connInfo.getUser());
        Assert.assertEquals("KYLIN", connInfo.getPassword());
        Assert.assertEquals("learn_kylin", connInfo.getProject());
    }

    @Test
    public void testTime() {
        long t = 1557891357386L;
        System.out.println("long = " + t);

        // +8:00 time zone:
        SimpleDateFormat sdf_8 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        sdf_8.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        Assert.assertEquals("2019-05-15 11:35", sdf_8.format(t));

        // +7:00 time zone:
        SimpleDateFormat sdf_7 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        sdf_7.setTimeZone(TimeZone.getTimeZone("GMT+7:00"));
        Assert.assertEquals("2019-05-15 10:35", sdf_7.format(t));

        // -9:00 time zone:
        SimpleDateFormat sdf_la = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        sdf_la.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        Assert.assertEquals("2019-05-14 20:35", sdf_la.format(t));

        TimeZone timeZone = Calendar.getInstance().getTimeZone();
        String displayName = timeZone.getDisplayName();
        System.out.println(timeZone.getRawOffset());
        System.out.println(displayName);
    }
}
