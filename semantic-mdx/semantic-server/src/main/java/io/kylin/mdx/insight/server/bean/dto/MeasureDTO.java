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
import io.kylin.mdx.insight.core.entity.DescWrapperExtend;
import io.kylin.mdx.insight.core.entity.TranslationEntity;
import io.kylin.mdx.insight.core.entity.VisibleAttr;
import io.kylin.mdx.insight.core.entity.NamedMeasure;
import io.kylin.mdx.insight.core.support.SemanticUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
public class MeasureDTO {

    private String name;

    @NotBlank
    @Length(max = 300)
    private String alias;

    private String expression;

    private String format;

    // 格式类型：例如currency
    @JsonProperty("format_type")
    private String formatType;

    @JsonProperty("data_type")
    private String dataType;

    @JsonProperty("dim_column")
    private String dimColumn;

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



    public MeasureDTO(NamedMeasure namedMeasure) {
        this.name = namedMeasure.getName();
        this.alias = Utils.blankToDefaultString(namedMeasure.getAlias(), name);
        this.expression = namedMeasure.getExpression();
        this.format = namedMeasure.getFormat();
        this.formatType = namedMeasure.getFormatType();
        this.dataType = namedMeasure.getDataType();
        this.dimColumn = SemanticUtils.getUIMeasureArg(namedMeasure.getDimColumn());
        this.visibleFlag = namedMeasure.getVisibleFlag();
        this.translation = JSON.parseObject(namedMeasure.getTranslation(), TranslationEntity.class);
        this.subfolder = namedMeasure.getSubfolder();

        DescWrapperExtend descWrapperExtend = namedMeasure.buildExtend();
        this.visible = SemanticUtils.getVisibleFromExtend(descWrapperExtend);
        this.invisible = SemanticUtils.getInvisibleFromExtend(descWrapperExtend);
        this.desc = SemanticUtils.getDescFromExtend(descWrapperExtend);
    }

}
