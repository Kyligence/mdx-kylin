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


package io.kylin.mdx.insight.core.model.acl;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
public class AclTableModel {

    /**
     * 表格全名
     */
    private String table;

    /**
     * 表格是否可访问
     */
    private boolean invisible;

    /**
     * 列级可访问设置
     */
    private final Map<String, Boolean> columns = new HashMap<>();

    public AclTableModel(String table) {
        this.table = table;
    }

    public void setAccess(String column, boolean access) {
        columns.put(column, access);
    }

    public void resetAccess(String column) {
        columns.remove(column);
    }

    public boolean isAccess(String column) {
        return columns.getOrDefault(column, true);
    }

    public boolean isCompatible(AclTableModel oldTableModel) {
        Set<String> newInvisible = this.getColumns().keySet();
        Set<String> oldInvisible = oldTableModel.getColumns().keySet();
        if (newInvisible.size() != oldInvisible.size()) {
            return false;
        }
        return newInvisible.equals(oldInvisible);
    }

}
