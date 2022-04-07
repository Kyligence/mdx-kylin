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


package io.kylin.mdx.core.mondrian.measuregroup;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.kylin.mdx.core.mondrian.MdnForeignKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class MdnMeasureGroup {

    @JsonIgnore
    private String defaultMeasure;

    @JacksonXmlProperty(localName = "name", isAttribute = true)
    private String name = "measures";

    @JacksonXmlProperty(localName = "table", isAttribute = true)
    private String table;

    @JacksonXmlProperty(localName = "PK", isAttribute = true)
    private String pk;

    @JacksonXmlProperty(localName = "isM2M", isAttribute = true)
    private String isM2M;

    @JacksonXmlProperty(localName = "bridgeTable", isAttribute = true)
    private String bridgeTable;

    @JacksonXmlProperty(localName = "Measures")
    private MeasureWrapper measureWrapper;

    @JacksonXmlProperty(localName = "DimensionLinks")
    private MdnDimensionLinkWrapper mdnDimensionLinkWrapper;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeasureWrapper {

        @JacksonXmlProperty(localName = "Measure")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<MdnMeasure> mdnMeasures;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MdnDimensionLinkWrapper {

        @JacksonXmlProperty(localName = "FactLink")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<MdnFactLink> mdnFactLinks;

        @JacksonXmlProperty(localName = "NoLink")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<MdnNoLink> mdnNoLinks;

        @JacksonXmlProperty(localName = "ForeignKeyLink")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<MdnForeignKeyLink> mdnForeignKeyLinks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MdnFactLink {

        @JacksonXmlProperty(localName = "dimension", isAttribute = true)
        private String dimension;
    }

    @Data
    public static class MdnForeignKeyLink {

        @JacksonXmlProperty(localName = "dimension", isAttribute = true)
        private String dimension;

        @JacksonXmlProperty(localName = "attribute", isAttribute = true)
        private String attribute;

        @JacksonXmlProperty(localName = "ForeignKey")
        private MdnForeignKey mdnForeignKey;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MdnNoLink {

        @JacksonXmlProperty(localName = "dimension", isAttribute = true)
        private String dimension;
    }



}
