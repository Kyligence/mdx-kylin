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
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.List;

@Data
@NoArgsConstructor
@Table(name = "calculate_measure")
public class CalculateMeasure implements Visibility {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "dataset_id")
    private Integer datasetId;

    @Column(name = "name")
    private String name;

    @Column(name = "expression")
    private String expression;

    @Column(name = "format")
    private String format;

    @Column(name = "format_type")
    private String formatType;

    @Column(name = "measure_folder")
    private String measureFolder;

    @Column(name = "extend")
    private String extend;

    @Column(name = "visible_flag")
    private Boolean visibleFlag;

    @Column(name = "translation")
    private String translation;

    @Column(name = "subfolder")
    private String subfolder;

    @Column(name = "non_empty_behavior")
    private String nonEmptyBehaviorMeasures;

    public CalculateMeasure(Integer datasetId) {
        this.datasetId = datasetId;
    }

    public CalculateMeasure(Integer datasetId, String name, String expression, String format, String formatType, String measureFolder,
                            List<VisibleAttr> visible, List<VisibleAttr> invisible, String desc, boolean visibleFlag,
                            TranslationEntity translationEntity, String subfolder, List<CalculatedMemberNonEmptyBehaviorMeasure> nonEmptyBehaviorMeasures) {
        this.datasetId = datasetId;
        this.name = name;
        this.expression = expression;
        this.format = format;
        this.formatType = formatType;
        this.measureFolder = measureFolder;
        this.extend = new DescWrapperExtend().withDescription(desc)
                .withVisible(visible).withInvisible(invisible).take();
        this.visibleFlag = visibleFlag;
        this.translation = JSON.toJSONString(translationEntity);
        this.subfolder = subfolder;
        this.nonEmptyBehaviorMeasures = nonEmptyBehaviorMeasures == null ? "[]" : JSON.toJSONString(nonEmptyBehaviorMeasures);
    }

    @Override
    public DescWrapperExtend buildExtend() {
        return JSON.parseObject(extend, DescWrapperExtend.class);
    }

}
