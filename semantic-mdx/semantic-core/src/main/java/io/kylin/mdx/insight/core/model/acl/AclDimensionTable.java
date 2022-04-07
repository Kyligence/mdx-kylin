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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kylin.mdx.insight.core.model.semantic.SemanticDataset;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@NoArgsConstructor
public class AclDimensionTable extends AclAccessible {

    private String alias;

    @JsonProperty("actual_table")
    private String actualTable;

    @JsonProperty("dim_cols")
    private List<AclDimensionCol> dimCols = new ArrayList<>();

    @JsonProperty("hierarchys")
    private List<AclHierarchy> hierarchys = new ArrayList<>();

    public AclDimensionTable(SemanticDataset.AugmentedModel.AugmentDimensionTable table) {
        this.setName(table.getName());
        this.alias = table.getAlias();
        this.actualTable = table.getActualTable();
        for (SemanticDataset.AugmentedModel.AugmentDimensionTable.AugmentDimensionCol dimensionCol :
                table.getDimColumns()) {
            this.dimCols.add(new AclDimensionCol(dimensionCol));
        }
        if (table.getHierarchys() != null) {
            for (SemanticDataset.AugmentedModel.AugmentDimensionTable.Hierarchy0 hierarchy : table.getHierarchys()) {
                this.hierarchys.add(new AclHierarchy(hierarchy));
            }
        }
    }

    public AclDimensionCol getDimensionColumnByName(String columnName) {
        return getDimensionColumn(columnName, AclDimensionCol::getName);
    }

    public AclDimensionCol getDimensionColumnByAlias(String columnName) {
        return getDimensionColumn(columnName, AclDimensionCol::getAlias);
    }

    private AclDimensionCol getDimensionColumn(String columnName, Function<AclDimensionCol, String> supplier) {
        for (AclDimensionCol dimensionCol : dimCols) {
            if (Objects.equals(supplier.apply(dimensionCol), columnName)) {
                return dimensionCol;
            }
        }
        return null;
    }

    public AclHierarchy getHierarchy(String columnName) {
        for (AclHierarchy hierarchy : hierarchys) {
            if (Objects.equals(hierarchy.getName(), columnName)) {
                return hierarchy;
            }
        }
        return null;
    }

}
