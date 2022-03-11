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


package io.kylin.mdx.core.mondrian.dimension;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.kylin.mdx.core.mondrian.MdnCustomTranslationWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class MdnHierarchy {

    @JacksonXmlProperty(localName = "name", isAttribute = true)
    private String name;

    @JacksonXmlProperty(localName = "hasAll", isAttribute = true)
    private String hasAll = "true";

    @JacksonXmlProperty(localName = "Level")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<MdnLevel> mdnLevels;

    @JacksonXmlProperty(localName = "visible", isAttribute = true)
    private String visible;

    @JacksonXmlProperty(localName = "CustomTranslations")
    private MdnCustomTranslationWrapper customTranslationWrapper;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MdnLevel {

        @JacksonXmlProperty(localName = "attribute", isAttribute = true)
        private String attribute;

        @JacksonXmlProperty(localName = "weightAttribute", isAttribute = true)
        private String weightAttribute;

        @JacksonXmlProperty(localName = "visible", isAttribute = true)
        private String visible;

        public MdnLevel(String attribute) {
            this.attribute = attribute;
        }

        public MdnLevel(String attribute, String weightAttribute) {
            this.attribute = attribute;
            this.weightAttribute = weightAttribute;
        }
    }
}
