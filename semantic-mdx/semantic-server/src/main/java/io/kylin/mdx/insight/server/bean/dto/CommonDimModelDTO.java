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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Splitter;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.core.entity.CommonDimRelation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class CommonDimModelDTO {

    @NotBlank
    @JsonProperty("model_left")
    private String modelLeft;

    @JsonProperty("model_right")
    private String modelRight;

    private List<CommonDimRel> relation;

    public CommonDimModelDTO(CommonDimRelation commonDimRelation) {
        this.modelLeft = commonDimRelation.getModel();
        this.modelRight = commonDimRelation.getModelRelated();


        if (StringUtils.isNotBlank(modelRight) && StringUtils.isNotBlank(commonDimRelation.getRelation())) {
            this.relation = new LinkedList<>();
            Map<String, String> split = Splitter.on(SemanticConstants.COMMA)
                    .omitEmptyStrings()
                    .trimResults()
                    .withKeyValueSeparator(SemanticConstants.EQUAL)
                    .split(commonDimRelation.getRelation());

            split.forEach((tableLeft, tableRight) -> relation.add(new CommonDimRel(tableLeft, tableRight)));

        } else {
            this.relation = Collections.emptyList();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommonDimRel {

        private String left;

        private String right;
    }
}
