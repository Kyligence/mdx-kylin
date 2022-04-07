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


package io.kylin.mdx.insight.server.bean.dto;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.NamedDimTable;
import io.kylin.mdx.insight.core.entity.TranslationEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.LinkedList;
import java.util.List;

@Data
@NoArgsConstructor
public class DimensionTableDTO {

    @NotBlank
    private String name;

    /**
     * values: regular | time
     */
    @NotBlank
    private String type;

    @NotBlank
    @Length(max = 300)
    private String alias;

    @JsonProperty("actual_table")
    private String actualTable;

    @JsonProperty("dim_cols")
    @Valid
    private List<DimensionColDTO> dimCols = new LinkedList<>();

    @Valid
    private List<HierarchyDTO> hierarchys;

    @JsonProperty("translation")
    private TranslationEntity translation;


    public DimensionTableDTO(NamedDimTable namedDimTbl) {
        this.name = namedDimTbl.getDimTable();
        this.type = namedDimTbl.getDimTableType();
        this.alias = Utils.blankToDefaultString(namedDimTbl.getDimTableAlias(), name);
        this.actualTable = namedDimTbl.getActualTable();
        this.translation = JSON.parseObject(namedDimTbl.getTranslation(), TranslationEntity.class);
    }

    public void addDimensionCol(DimensionColDTO dimensionColDTO) {
        dimCols.add(dimensionColDTO);
    }

    public void addHierarchy(HierarchyDTO hierarchyDTO) {
        if (hierarchys == null) {
            hierarchys = new LinkedList<>();
        }

        hierarchys.add(hierarchyDTO);
    }
}
