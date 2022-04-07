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
import io.kylin.mdx.insight.core.model.generic.ColumnIdentity;
import io.kylin.mdx.insight.core.model.generic.HierachyInfo;
import io.kylin.mdx.core.mondrian.MdnCustomTranslationWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedList;
import java.util.List;

@Data
public class MdnDimension {

    @JacksonXmlProperty(localName = "name", isAttribute = true)
    private String name;

    @JacksonXmlProperty(localName = "table", isAttribute = true)
    private String table;

    @JacksonXmlProperty(localName = "key", isAttribute = true)
    private String key;

    @JacksonXmlProperty(localName = "type", isAttribute = true)
    private String type;

    @JacksonXmlProperty(localName = "Attributes")
    private AttributeWrapper attributeWrapper;

    @JacksonXmlProperty(localName = "Hierarchies")
    private MdnHierarchyWrapper mdnHierarchyWrapper;

    @JacksonXmlProperty(localName = "CustomTranslations")
    private MdnCustomTranslationWrapper customTranslationWrapper;

    @JacksonXmlProperty(localName = "visible", isAttribute = true)
    private String visible;

    public void setMdnHierarchies(List<HierachyInfo> hierachyInfos) {
        List<MdnHierarchy> mdnHierarchies = new LinkedList<>();

        for (HierachyInfo hierarchy : hierachyInfos) {
            MdnHierarchy mdnHierarchy = new MdnHierarchy();

            mdnHierarchy.setName(hierarchy.getHierachy().get(0).getColName() + "-Hierarchy");
            mdnHierarchy.setHasAll("true");

            List<MdnHierarchy.MdnLevel> mdnLevels = new LinkedList<>();
            for (ColumnIdentity columnIdentity : hierarchy.getHierachy()) {
                if (columnIdentity.getTableAlias().equals(this.table)) {
                    mdnLevels.add(new MdnHierarchy.MdnLevel(columnIdentity.getColName()));
                }
            }
            mdnHierarchy.setMdnLevels(mdnLevels);

            mdnHierarchies.add(mdnHierarchy);
        }

        this.mdnHierarchyWrapper = new MdnHierarchyWrapper(mdnHierarchies);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttributeWrapper {

        @JacksonXmlProperty(localName = "Attribute")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<MdnAttribute> mdnAttributes;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MdnHierarchyWrapper {

        @JacksonXmlProperty(localName = "Hierarchy")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<MdnHierarchy> mdnHierarchies;
    }

}
