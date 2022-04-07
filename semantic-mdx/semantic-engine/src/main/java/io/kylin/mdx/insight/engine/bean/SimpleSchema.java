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


package io.kylin.mdx.insight.engine.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.Set;

@Data
@NoArgsConstructor
public class SimpleSchema {

    @JsonProperty("measures")
    private Set<String> measureAliases;

    @JsonProperty("calculate_measures")
    private Set<String> calcMeasureNames;

    @JsonProperty("named_sets")
    private Set<NamedSet> namedSets;

    @JsonProperty("dimension_tables")
    private Set<DimensionTable> dimensionTables;

    @Data
    @NoArgsConstructor
    public static class NamedSet {

        private String name;

        private String expression;

        private String location;

        public NamedSet(String name) { this.name = name; }

    }

    @Data
    @NoArgsConstructor
    public static class DimensionTable {

        private String alias;

        private String type;

        private String model;

        @JsonProperty("dim_cols")
        private Set<DimensionCol> tableColAliases;

        @JsonProperty("hierarchies")
        private Set<Hierarchy> hierarchies;

        public DimensionTable(String alias) {
            this.alias = alias;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if(!(o instanceof DimensionTable)) {
                return false;
            }

            DimensionTable that = (DimensionTable) o;
            return alias.equals(that.alias);
        }

        @Override
        public int hashCode() {
            return Objects.hash(alias);
        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionCol {

        @JsonProperty("alias")
        private String tableColAlias;

        /**
         * 0:default, 1:levelYears, 2:levelQuarters, 3:levelMonths, 4:levelWeeks
         */
        private Integer type;

        public DimensionCol(String tableColAlias) {
            this.tableColAlias = tableColAlias;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof DimensionCol)) {
                return false;
            }
            DimensionCol that = (DimensionCol) o;
            return tableColAlias.equals(that.tableColAlias);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tableColAlias);
        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Hierarchy {

        private String name;

        @JsonProperty("dim_cols")
        private Set<String> tableColAliases;

        public Hierarchy(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Hierarchy)) {
                return false;
            }

            Hierarchy that = (Hierarchy) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

    }
}
