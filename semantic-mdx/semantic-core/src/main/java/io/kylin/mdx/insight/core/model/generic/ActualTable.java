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


package io.kylin.mdx.insight.core.model.generic;

import io.kylin.mdx.insight.common.SemanticConstants;
import lombok.Data;

import java.util.Objects;

@Data
public class ActualTable {

    private String schema;

    private String tableName;

    public ActualTable(String schema, String tableName) {
        this.schema = schema;
        this.tableName = tableName;
    }

    public ActualTable(String schemaWithTblName) {
        int pos = schemaWithTblName.indexOf(SemanticConstants.DOT);

        if (pos == -1) {
            throw new RuntimeException("非法的schemaWithTblName结构：" + schemaWithTblName);
        }

        this.schema = schemaWithTblName.substring(0, pos);
        this.tableName = schemaWithTblName.substring(pos + 1);
    }

    public String getFormatStr() {
        return this.schema + SemanticConstants.DOT + this.tableName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ActualTable that = (ActualTable) o;
        return schema.equals(that.schema) &&
                tableName.equals(that.tableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, tableName);
    }
}
