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


package io.kylin.mdx.core.mondrian.physicalschema;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.kylin.mdx.core.mondrian.MdnForeignKey;
import io.kylin.mdx.core.mondrian.MdnKeyColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MdnPhysicalSchema {

    @JacksonXmlProperty(localName = "Table")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<MdnTable> mdnTables;

    @JacksonXmlProperty(localName = "Link")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<MdnLink> mdnLinks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MdnTable {

        @JacksonXmlProperty(localName = "name", isAttribute = true)
        private String name;

        @JacksonXmlProperty(localName = "schema", isAttribute = true)
        private String schema;

        @JacksonXmlProperty(localName = "alias", isAttribute = true)
        private String alias;

        @JacksonXmlProperty(localName = "keyColumn", isAttribute = true)
        private String column;

        @JacksonXmlProperty(localName = "key")
        private MdnKeyColumn key;

        public void addPrimaryKeys(List<String> pks) {
            if (key == null) {
                key = new MdnKeyColumn();
            }

            key.addPrimaryKeys(pks);
        }

    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MdnLink {

        @JacksonXmlProperty(localName = "target", isAttribute = true)
        private String target;

        @JacksonXmlProperty(localName = "source", isAttribute = true)
        private String source;

        @JacksonXmlProperty(localName = "foreignKeyColumn", isAttribute = true)
        private String foreignKeyColumn;

        @JacksonXmlProperty(localName = "type", isAttribute = true)
        private String type;

        @JacksonXmlProperty(localName = "ForeignKey")
        public MdnForeignKey foreignKey;

        public MdnLink(String leftTable, String rightTable, String joinType) {
            this.target = leftTable;
            this.source = rightTable;
            this.type = joinType;
        }

        public void addForeignKeys(List<MutablePair<String, String>> joinConds) {
            List<String> fks = new LinkedList<>();

            for (MutablePair<String, String> pair : joinConds) {
                fks.add(pair.getLeft());
            }

            if (foreignKey == null) {
                foreignKey = new MdnForeignKey();
            }

            foreignKey.addMdnColumns(fks);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MdnLink mdnLink = (MdnLink) o;
            return Objects.equals(getTarget(), mdnLink.getTarget()) &&
                    Objects.equals(getSource(), mdnLink.getSource()) &&
                    Objects.equals(getForeignKeyColumn(), mdnLink.getForeignKeyColumn()) &&
                    Objects.equals(getType(), mdnLink.getType()) &&
                    Objects.equals(getForeignKey(), mdnLink.getForeignKey());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getTarget(), getSource(), getForeignKeyColumn(), getType(), getForeignKey());
        }
    }


}
