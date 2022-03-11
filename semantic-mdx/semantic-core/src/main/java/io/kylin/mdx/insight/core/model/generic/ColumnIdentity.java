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
public class ColumnIdentity implements Comparable<ColumnIdentity> {

    private Integer id;

    private String tableAlias;

    private String colName;

    private String colAlias;

    public ColumnIdentity() {

    }

    public ColumnIdentity(Integer id, String tblAliasWithCol) {
        this(tblAliasWithCol);
        this.id = id;
    }

    public ColumnIdentity(String tblAliasWithCol) {
        int pos = tblAliasWithCol.indexOf(SemanticConstants.DOT);

        if (pos == -1) {
            throw new RuntimeException("Your data structure about tblAliasWithCol [" + tblAliasWithCol + "] seems not to be correct, please revise it.");
        }

        this.tableAlias = tblAliasWithCol.substring(0, pos);
        this.colName = tblAliasWithCol.substring(pos + 1);
    }

    public ColumnIdentity(String tableAlias, String colName) {
        this.tableAlias = tableAlias;
        this.colName = colName;
    }

    public ColumnIdentity(String tableAlias, String colName, String colAlias) {
        this.tableAlias = tableAlias;
        this.colName = colName;
        this.colAlias = colAlias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ColumnIdentity that = (ColumnIdentity) o;


        return tableAlias.equals(that.tableAlias) &&
                colName.equals(that.colName);

    }

    @Override
    public int hashCode() {
        return Objects.hash(tableAlias, colName);
    }


    @Override
    public int compareTo(ColumnIdentity o) {

        int r1 = tableAlias.compareTo(o.tableAlias);
        if (r1 == 0) {
            return colName.compareTo(o.colName);
        }

        return r1;
    }
}
