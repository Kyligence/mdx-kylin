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


package io.kylin.mdx.insight.core.entity;

import com.alibaba.fastjson.JSON;
import io.kylin.mdx.insight.core.support.SemanticUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.List;

@Data
@NoArgsConstructor
@Table(name = "named_measure")
public class NamedMeasure implements Visibility {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "dataset_id")
    private Integer datasetId;

    @Column(name = "model")
    private String model;

    @Column(name = "name")
    private String name;

    @Column(name = "alias")
    private String alias;

    @Column(name = "expression")
    private String expression;

    @Column(name = "format")
    private String format;

    @Column(name = "format_type")
    private String formatType;

    @Column(name = "data_type")
    private String dataType;

    @Column(name = "dim_column")
    private String dimColumn;

    @Column(name = "extend")
    private String extend;

    @Column(name = "visible_flag")
    private Boolean visibleFlag;

    @Column(name = "translation")
    private String translation;

    @Column(name = "subfolder")
    private String subfolder;

    public NamedMeasure(Integer datasetId) {
        this.datasetId = datasetId;
    }

    public NamedMeasure(Integer datasetId, String model, String name, String alias,
                        String expression, String dataType, String dimColumn) {
        this.datasetId = datasetId;
        this.model = model;
        this.name = name;
        this.alias = alias;
        this.expression = expression;
        this.format = "";
        this.formatType = "";
        this.dataType = dataType;
        this.dimColumn = SemanticUtils.getNormalMeasureArg(dimColumn);
        this.extend = "";
        this.visibleFlag = true;
        this.translation = "{}";
        this.subfolder = "";
    }

    public NamedMeasure(Integer datasetId, String model, String name, String alias,
                        String expression, String format, String formatType, String dataType, String dimColumn,
                        List<VisibleAttr> visible, List<VisibleAttr> invisible, String desc,
                        Boolean visibleFlag, TranslationEntity translationEntity, String subfolder) {
        this.datasetId = datasetId;
        this.model = model;
        this.name = name;
        this.alias = alias;
        this.expression = expression;
        this.format = format;
        this.formatType = formatType;
        this.dataType = dataType;
        this.dimColumn = SemanticUtils.getNormalMeasureArg(dimColumn);
        this.visibleFlag = visibleFlag;
        this.translation = JSON.toJSONString(translationEntity);
        this.subfolder = subfolder;
        this.extend = new DescWrapperExtend().withDescription(desc).
                withVisible(visible).withInvisible(invisible).take();
    }

    @Override
    public DescWrapperExtend buildExtend() {
        return JSON.parseObject(extend, DescWrapperExtend.class);
    }

}
