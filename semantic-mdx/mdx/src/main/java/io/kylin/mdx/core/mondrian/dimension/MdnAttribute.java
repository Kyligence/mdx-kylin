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
import io.kylin.mdx.core.mondrian.MdnColumn;
import io.kylin.mdx.core.mondrian.MdnCustomTranslationWrapper;
import io.kylin.mdx.core.mondrian.MdnKeyColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MdnAttribute {

    @JacksonXmlProperty(localName = "name", isAttribute = true)
    private String name;

    @JacksonXmlProperty(localName = "keyColumn", isAttribute = true)
    private String keyColumn;

    @JacksonXmlProperty(localName = "levelType", isAttribute = true)
    private String levelType;

    @JacksonXmlProperty(localName = "table", isAttribute = true)
    private String table;

    @JacksonXmlProperty(localName = "nameColumn", isAttribute = true)
    private String nameColumn;

    @JacksonXmlProperty(localName = "captionColumn", isAttribute = true)
    private String captionColumn;

    @JacksonXmlProperty(localName = "orderByColumn", isAttribute = true)
    private String orderByColumn;

    @JacksonXmlProperty(localName = "valueColumn", isAttribute = true)
    private String valueColumn;

    @JacksonXmlProperty(localName = "key")
    private MdnKeyColumn mdnKeyColumn;

    @JacksonXmlProperty(localName = "Name")
    private NamedColumn namedColumn;

    @JacksonXmlProperty(localName = "visible", isAttribute = true)
    private String visible;

    @JacksonXmlProperty(localName = "Property")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<MdnProperty> properties;

    @JacksonXmlProperty(localName = "CustomTranslations")
    private MdnCustomTranslationWrapper customTranslationWrapper;

    @JacksonXmlProperty(localName = "subfolder", isAttribute = true)
    private String subfolder;

    @JacksonXmlProperty(localName = "hierarchyDefaultMember", isAttribute = true)
    private String defaultMember;

    public MdnAttribute(String name) {
        this.name = name;
    }

    public MdnAttribute(String name, String keyColumn, String valueColumn) {
        this(name);
        this.keyColumn = keyColumn;
        this.nameColumn = keyColumn;
        this.valueColumn = valueColumn;
    }

    public MdnAttribute(String name, String keyColumn, String nameColumn, String valueColumn, String subfolder) {
        this(name);
        this.keyColumn = keyColumn;
        this.nameColumn = nameColumn;
        this.valueColumn = valueColumn;
        this.subfolder = subfolder;
    }

    public MdnAttribute(String name, String keyColumn, String nameColumn, String valueColumn, String levelType, String subfolder) {
        this(name);
        this.keyColumn = keyColumn;
        this.nameColumn = nameColumn;
        this.valueColumn = valueColumn;
        this.levelType = levelType;
        this.subfolder = subfolder;
    }

    public void addMdnKeyColumn(String dimTable, ColumnIdentity colLevel) {
        if (mdnKeyColumn == null) {
            mdnKeyColumn = new MdnKeyColumn();
        }

        if (dimTable.equalsIgnoreCase(colLevel.getTableAlias())) {
            mdnKeyColumn.addKeyColumn(colLevel.getColName(), null);
        } else {
            mdnKeyColumn.addKeyColumn(colLevel.getColName(), colLevel.getTableAlias());
        }

    }

    public void addProperty(String name, String attribute) {
        if(properties == null) {
            properties = new ArrayList<>();
        }

        properties.add(new MdnProperty(name, attribute));

    }

    @Data
    public static class NamedColumn {

        @JacksonXmlProperty(localName = "Column")
        @JacksonXmlElementWrapper(useWrapping = false)
        private MdnColumn mdnColumn;

        public NamedColumn() {
        }

        public NamedColumn(String col) {
            this.mdnColumn = new MdnColumn(col);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MdnProperty {
        @JacksonXmlProperty(localName =  "name", isAttribute = true)
        private String name;

        @JacksonXmlProperty(localName = "attribute", isAttribute = true)
        private String attribute;
    }
}
