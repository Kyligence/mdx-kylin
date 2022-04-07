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


package io.kylin.mdx.core.exchange;

import io.kylin.mdx.insight.common.util.JacksonSerDeUtils;
import io.kylin.mdx.insight.core.model.semantic.SemanticDataset;
import io.kylin.mdx.core.mondrian.MdnSchema;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class MondrianSchemaRenderV2Test {

    @Test
    public void testOneCubeDataset() {
        URL oneCubeDatasetURL = MondrianSchemaRenderV2Test.class.getResource("/dataset/1_cube_dataset.json");
        URL oneCubeSchemaURL = MondrianSchemaRenderV2Test.class.getResource("/schema/1_cube_dataset.xml");
        SemanticDataset dataset = JacksonSerDeUtils.readJson(oneCubeDatasetURL, SemanticDataset.class);
        MondrianSchemaRenderV2 renderV2 = new MondrianSchemaRenderV2();
        MdnSchema mdnSchema = renderV2.create(dataset);
        String result = JacksonSerDeUtils.writeXmlAsString(mdnSchema);
        MdnSchema expectMdnSchema = JacksonSerDeUtils.readXml(oneCubeSchemaURL, MdnSchema.class);
        String expect = JacksonSerDeUtils.writeXmlAsString(expectMdnSchema);
        Assert.assertEquals(result, expect);
    }

    @Test
    public void testTwoCubeDataset() {
        URL twoCubeDatasetURL = MondrianSchemaRenderV2Test.class.getResource("/dataset/2_cube_dataset.json");
        URL twoCubeSchemaURL = MondrianSchemaRenderV2Test.class.getResource("/schema/2_cube_dataset.xml");
        SemanticDataset dataset = JacksonSerDeUtils.readJson(twoCubeDatasetURL, SemanticDataset.class);
        MondrianSchemaRenderV2 renderV2 = new MondrianSchemaRenderV2();
        MdnSchema mdnSchema = renderV2.create(dataset);
        String result = JacksonSerDeUtils.writeXmlAsString(mdnSchema);
        MdnSchema expectMdnSchema = JacksonSerDeUtils.readXml(twoCubeSchemaURL, MdnSchema.class);
        String expect = JacksonSerDeUtils.writeXmlAsString(expectMdnSchema);
        Assert.assertEquals(result, expect);
    }

    @Test
    public void testThreeCubeDataset() {
        URL ThreeCubeDatasetURL = MondrianSchemaRenderV2Test.class.getResource("/dataset/3Cube_1.json");
        URL ThreeCubeSchemaURL = MondrianSchemaRenderV2Test.class.getResource("/schema/3Cube_1.xml");
        SemanticDataset dataset = JacksonSerDeUtils.readJson(ThreeCubeDatasetURL, SemanticDataset.class);
        MondrianSchemaRenderV2 renderV2 = new MondrianSchemaRenderV2();
        MdnSchema mdnSchema = renderV2.create(dataset);
        String result = JacksonSerDeUtils.writeXmlAsString(mdnSchema);
        MdnSchema expectMdnSchema = JacksonSerDeUtils.readXml(ThreeCubeSchemaURL, MdnSchema.class);
        String expect = JacksonSerDeUtils.writeXmlAsString(expectMdnSchema);
        Assert.assertEquals(result, expect);
    }

    @Test
    public void testCubeVisible() {
        URL testVisibleURL = MondrianSchemaRenderV2Test.class.getResource("/dataset/test_visible.json");
        URL ThreeCubeSchemaURL = MondrianSchemaRenderV2Test.class.getResource("/schema/test_visible.xml");
        SemanticDataset dataset = JacksonSerDeUtils.readJson(testVisibleURL, SemanticDataset.class);
        MondrianSchemaRenderV2 renderV2 = new MondrianSchemaRenderV2();
        MdnSchema mdnSchema = renderV2.create(dataset);
        String result = JacksonSerDeUtils.writeXmlAsString(mdnSchema);
        MdnSchema expectMdnSchema = JacksonSerDeUtils.readXml(ThreeCubeSchemaURL, MdnSchema.class);
        String expect = JacksonSerDeUtils.writeXmlAsString(expectMdnSchema);
        Assert.assertEquals(result, expect);
    }
}
