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


package io.kylin.mdx.insight.engine.service;

import io.kylin.mdx.insight.core.entity.GroupInfo;
import io.kylin.mdx.insight.core.entity.UserInfo;
import io.kylin.mdx.insight.core.meta.mock.MockTestLoader;
import io.kylin.mdx.insight.core.sync.MetaStore;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;


@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeClass
    public static void before() {
        System.setProperty("converter.mock", "true");
        System.setProperty("MDX_HOME", "src/test/resources/kylin");
        System.setProperty("MDX_CONF", "src/test/resources/kylin/conf");
        MockTestLoader.put(new MockTestLoader.Prefix("/kylin/api/access/ProjectInstance/AdventureWorks"), "/ke/ke_access_project.json");
    }

    @Test
    public void testGetAllUserByProject() {
        List<UserInfo> users = userService.getAllUserByProject("AdventureWorks", 0, 100);
        Assert.assertTrue(users.size() > 0);
    }

    @Test
    public void testGetAllGroupByProject() {
        List<GroupInfo> groups = userService.getAllGroupByProject("AdventureWorks", 0, 100);
        Assert.assertTrue(groups.size() > 0);
    }

    @Test
    public void testGetAllGroup() {
        MetaStore.getInstance().setAllGroupName(Collections.singletonList("ALL_USERS"));
        List<GroupInfo> groups = userService.getAllGroup(0, 100);
        Assert.assertEquals("ALL_USERS", groups.get(0).getGroup());
    }

}
