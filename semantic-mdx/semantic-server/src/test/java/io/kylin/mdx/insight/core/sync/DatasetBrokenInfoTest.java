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


package io.kylin.mdx.insight.core.sync;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;

public class DatasetBrokenInfoTest {

    @Test
    public void testDatasetBrokenInfo() throws JSONException, IOException {

        DatasetBrokenInfo datasetBrokenInfo = new DatasetBrokenInfo();

        datasetBrokenInfo.setBrokenModelList(new HashSet<>(Arrays.asList("cube_1", "cube_2")));

        datasetBrokenInfo.addBrokenModelName("cube_3");

        datasetBrokenInfo.addCommonTableBroken("cube_1", "table_1");

        datasetBrokenInfo.addBridgeDimTableBroken("cube_1", "bridge_table");

        datasetBrokenInfo.addBrokenHierarchy("cube_1", "table_1", "col_1", "weight_col_1", "hierarchy", 0x1);

        datasetBrokenInfo.addBrokenManyToManyKey("cube_1", "table_1", "key_1");

        JSONCompareResult result = JSONCompare.compareJSON(getRespJSONContent(), JSON.toJSONString(datasetBrokenInfo), JSONCompareMode.LENIENT);

        Assert.assertTrue(result.passed());

    }

    protected String getRespJSONContent() throws IOException {
        String jsonPath = "/json/datasetBrokenInfo.json";
        try (InputStream inputStream = DatasetBrokenInfoTest.class.getResourceAsStream(jsonPath)) {
            return IOUtils.toString(inputStream);
        }
    }

}
