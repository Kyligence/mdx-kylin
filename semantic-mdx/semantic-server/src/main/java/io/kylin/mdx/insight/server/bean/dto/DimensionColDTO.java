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
import io.kylin.mdx.insight.core.entity.*;
import io.kylin.mdx.insight.core.support.SemanticUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
public class DimensionColDTO {

    @NotBlank
    private String name;

    /**
     * 0:default, 1:levelYears, 2:levelQuarters, 3:levelMonths, 4:levelWeeks
     */
    private Integer type;

    @NotBlank
    @Length(max = 300)
    private String alias;

    @NotBlank
    @JsonProperty("data_type")
    private String dataType;

    private String desc;

    private List<VisibleAttr> visible;

    private List<VisibleAttr> invisible;

    @NotNull
    @JsonProperty("is_visible")
    private Boolean visibleFlag;

    @JsonProperty("name_column")
    private String nameColumn;

    @JsonProperty("value_column")
    private String valueColumn;

    private List<PropertyAttr> properties;

    @JsonProperty("translation")
    private TranslationEntity translation;

    @JsonProperty("subfolder")
    private String subfolder;

    @JsonProperty("default_member")
    private String defaultMember;

    public DimensionColDTO(NamedDimCol namedDimCol) {
        this.name = namedDimCol.getDimCol();
        this.type = namedDimCol.getColType();
        this.alias = Utils.blankToDefaultString(namedDimCol.getDimColAlias(), name);
        this.dataType = namedDimCol.getDataType();
        this.visibleFlag = namedDimCol.getVisibleFlag();
        this.nameColumn = namedDimCol.getNameColumn();
        this.valueColumn = namedDimCol.getValueColumn();
        this.translation = JSON.parseObject(namedDimCol.getTranslation(), TranslationEntity.class);
        this.subfolder = namedDimCol.getSubfolder();
        this.defaultMember = namedDimCol.getDefaultMember();

        DescWrapperExtend descWrapperExtend = namedDimCol.buildExtend();
        this.desc = SemanticUtils.getDescFromExtend(descWrapperExtend);
        this.visible = SemanticUtils.getVisibleFromExtend(descWrapperExtend);
        this.invisible = SemanticUtils.getInvisibleFromExtend(descWrapperExtend);
        this.properties = SemanticUtils.getPropertiesFromExtend(descWrapperExtend);
    }

}
