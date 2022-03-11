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
@Table(name = "named_set")
public class NamedSet implements Visibility {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "dataset_id")
    private Integer datasetId;

    @Column(name = "name")
    private String name;

    @Column(name = "expression")
    private String expression;

    @Column(name = "folder")
    private String folder;

    @Column(name = "extend")
    private String extend;

    @Column(name = "location")
    private String location;

    @Column(name = "visible_flag")
    private Boolean visibleFlag;

    @Column(name = "translation")
    private String translation;

    public NamedSet(Integer datasetId) {
        this.datasetId = datasetId;
    }

    public NamedSet(Integer datasetId, String name, String expression, String folder, String location,
                    List<VisibleAttr> visible, List<VisibleAttr> invisible, boolean visibleFlag, TranslationEntity translationEntity) {
        this.datasetId = datasetId;
        this.name = name;
        this.expression = expression;
        this.folder = folder;
        this.location = location;
        this.extend = new EntityExtend().withVisible(visible).withInvisible(invisible).take();
        this.visibleFlag = visibleFlag;
        this.translation = JSON.toJSONString(translationEntity);
    }

    @Override
    public DescWrapperExtend buildExtend() {
        return JSON.parseObject(extend, DescWrapperExtend.class);
    }

}
