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


package io.kylin.mdx.insight.core.model.kylin;

import io.kylin.mdx.insight.core.model.generic.RawJoinTable;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class KylinDataModelDesc {

    private List<DataModel> models;

    @Data
    public static class DataModel {

        private String name;
        private Long last_modified;
        private String description;
        private String fact_table;
        private String project;
        private List<RawJoinTable> lookups;
        private List<Dimension> dimensions;
        private List<String> metrics;
        private List<ComputedColumn> computed_columns;

        @Data
        public static class ComputedColumn {
            private String tableIdentity;
            private String tableAlias;
            private String columnName;
            private String expression;
            private String innerExpression;
            private String datatype;
            private Object comment;
        }

        @Data
        public static class Dimension {

            private String table;
            private List<String> columns;

        }
    }
}
