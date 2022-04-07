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
import io.kylin.mdx.insight.core.entity.CalculateMeasure;
import io.kylin.mdx.insight.core.entity.DescWrapperExtend;
import io.kylin.mdx.insight.core.entity.CalculatedMemberNonEmptyBehaviorMeasure;
import io.kylin.mdx.insight.core.entity.TranslationEntity;
import io.kylin.mdx.insight.core.entity.VisibleAttr;
import io.kylin.mdx.insight.core.support.SemanticUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
public class CalculationMeasureDTO {

    @NotBlank
    @Length(max = 300)
    private String name;

    private String format;

    // 格式类型：例如currency
    @JsonProperty("format_type")
    private String formatType;

    @NotBlank
    private String expression;

    private String folder;

    private List<VisibleAttr> visible;

    private List<VisibleAttr> invisible;

    private String desc;

    @NotNull
    @JsonProperty("is_visible")
    private Boolean visibleFlag;

    @JsonProperty("translation")
    private TranslationEntity translation;

    @JsonProperty("subfolder")
    private String subfolder;

    @JsonProperty("non_empty_behavior")
    private List<CalculatedMemberNonEmptyBehaviorMeasure> nonEmptyBehaviorMeasures;

    public CalculationMeasureDTO(CalculateMeasure calculateMeasureEntity) {
        this.name = calculateMeasureEntity.getName();
        this.format = calculateMeasureEntity.getFormat();
        this.formatType = calculateMeasureEntity.getFormatType();
        this.expression = calculateMeasureEntity.getExpression();
        this.folder = calculateMeasureEntity.getMeasureFolder();
        this.visibleFlag = calculateMeasureEntity.getVisibleFlag();
        this.translation = JSON.parseObject(calculateMeasureEntity.getTranslation(), TranslationEntity.class);
        this.subfolder = calculateMeasureEntity.getSubfolder();
        this.nonEmptyBehaviorMeasures = JSON.parseArray(
                calculateMeasureEntity.getNonEmptyBehaviorMeasures(), CalculatedMemberNonEmptyBehaviorMeasure.class);

        DescWrapperExtend descWrapperExtend = calculateMeasureEntity.buildExtend();
        this.visible = SemanticUtils.getVisibleFromExtend(descWrapperExtend);
        this.invisible = SemanticUtils.getInvisibleFromExtend(descWrapperExtend);
        this.desc = SemanticUtils.getDescFromExtend(descWrapperExtend);
    }

}
