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


package io.kylin.mdx.core.mondrian;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.kylin.mdx.core.mondrian.calculation.CalculatedMember;
import io.kylin.mdx.core.mondrian.calculation.MdnNamedSet;
import io.kylin.mdx.core.mondrian.dimension.MdnDimension;
import io.kylin.mdx.core.mondrian.measuregroup.MdnMeasureGroup;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class MdnCube {

    @JacksonXmlProperty(localName = "name", isAttribute = true)
    private String name;

    @JacksonXmlProperty(localName = "defaultMeasure", isAttribute = true)
    private String defaultMeasure;

    @JacksonXmlProperty(localName = "Dimensions")
    private MdnDimensionWrapper mdnDimensionWrapper;

    @JacksonXmlProperty(localName = "MeasureGroups")
    private MdnMeasureGroupWrapper mdnMeasureGroupWrapper;

    @JacksonXmlProperty(localName = "CalculatedMembers")
    private CalculatedMemberWrapper calculatedMemberWrapper;

    @JacksonXmlProperty(localName = "NamedSets")
    private NamedSetWrapper namedSetWrapper;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NamedSetWrapper {

        @JacksonXmlProperty(localName = "NamedSet")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<MdnNamedSet> mdnNamedSets;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CalculatedMemberWrapper {

        @JacksonXmlProperty(localName = "CalculatedMember")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<CalculatedMember> calculatedMembers;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MdnDimensionWrapper {

        @JacksonXmlProperty(localName = "Dimension")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<MdnDimension> mdnDimensions;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MdnMeasureGroupWrapper {

        @JacksonXmlProperty(localName = "MeasureGroup")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<MdnMeasureGroup> mdnMeasureGroups;
    }

}
