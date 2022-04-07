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

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class KylinColumnDesc {

    private List<Table> data;

    @Data
    public static class Table {

        private String table_CAT;
        private String table_SCHEM;
        private String table_NAME;
        private String table_TYPE;
        private List<Column> columns;
        private List<String> type;

        @Data
        public static class Column {
            private String column_NAME;
            private int data_TYPE;
            private int column_SIZE;
            private int sql_DATA_TYPE;
            private int sql_DATETIME_SUB;
            private int char_OCTET_LENGTH;
            private int ordinal_POSITION;
            private String is_NULLABLE;
            private int source_DATA_TYPE;
            private String table_CAT;
            private String type_NAME;
            private String table_SCHEM;
            private String table_NAME;
            private List<String> type;
        }
    }

}
