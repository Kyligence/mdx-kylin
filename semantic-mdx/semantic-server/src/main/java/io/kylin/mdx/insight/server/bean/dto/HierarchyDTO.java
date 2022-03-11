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
import io.kylin.mdx.insight.core.entity.CustomHierarchy;
import io.kylin.mdx.insight.core.entity.TranslationEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class HierarchyDTO {

    @NotBlank
    @Length(max = 300)
    private String name;

    @JsonProperty("dim_cols")
    private List<String> dimCols;

    @JsonProperty("weight_cols")
    private List<String> weightCols;

    private String desc;

    @JsonProperty("translation")
    private TranslationEntity translation;

    public HierarchyDTO(String name, List<CustomHierarchy> cHierarchies) {
        this.name = name;
        this.dimCols = cHierarchies.stream()
                .map(CustomHierarchy::getDimCol)
                .collect(Collectors.toList());
        this.weightCols = cHierarchies.stream()
                .map(CustomHierarchy::getWeightCol)
                .collect(Collectors.toList());
        this.desc = cHierarchies.get(0).getDesc();
        this.translation = JSON.parseObject(cHierarchies.get(0).getTranslation(), TranslationEntity.class);
    }
}
