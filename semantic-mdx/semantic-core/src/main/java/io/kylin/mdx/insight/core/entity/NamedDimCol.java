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
import io.kylin.mdx.insight.core.model.generic.ColumnInfo;
import io.kylin.mdx.insight.core.model.semantic.ModelColumnIdentity;
import io.kylin.mdx.insight.core.support.SemanticUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.List;

@Data
@NoArgsConstructor
@Table(name = "named_dim_col")
public class NamedDimCol implements Visibility {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "dataset_id")
    private Integer datasetId;

    @Column(name = "model")
    private String model;

    @Column(name = "dim_table")
    private String dimTable;

    @Column(name = "dim_col")
    private String dimCol;

    @Column(name = "dim_col_alias")
    private String dimColAlias;

    /**
     * column type
     * values: 0:default, 1:levelYears, 2:levelQuarters, 3:levelMonths, 4:levelWeeks
     */
    @Column(name = "col_type")
    private Integer colType;

    @Column(name = "data_type")
    private String dataType;

    @Column(name = "extend")
    private String extend;

    @Column(name = "visible_flag")
    private Boolean visibleFlag;

    @Column(name = "name_column")
    private String nameColumn;

    @Column(name = "value_column")
    private String valueColumn;

    @Column(name = "translation")
    private String translation;

    @Column(name = "subfolder")
    private String subfolder;

    @Column(name = "default_member")
    private String defaultMember;


    public NamedDimCol(Integer datasetId) {
        this.datasetId = datasetId;
    }

    public NamedDimCol(Integer datasetId, String model, String dimTable) {
        this.datasetId = datasetId;
        this.model = model;
        this.dimTable = dimTable;
    }

    public NamedDimCol(Integer datasetId, String model, String dimTable,
                       String dimCol, String dimColAlias, Integer colType, String dataType) {
        this.datasetId = datasetId;
        this.model = model;
        this.dimTable = dimTable;
        this.dimCol = dimCol;
        this.dimColAlias = dimColAlias;
        this.colType = colType;
        this.dataType = dataType;
        this.extend = "";
        this.visibleFlag = true;
        this.subfolder = "";
        this.defaultMember = "";
    }

    public NamedDimCol(Integer datasetId, ModelColumnIdentity modelColumnIdentity, ColumnInfo columnInfo) {
        this.datasetId = datasetId;
        this.model = modelColumnIdentity.getModelName();
        this.dimTable = modelColumnIdentity.getColumnIdentity().getTableAlias();
        this.dimCol = modelColumnIdentity.getColumnIdentity().getColName();
        this.dimColAlias = this.dimCol;
        this.colType = 0;
        this.dataType = SemanticUtils.getDataTypeStr(columnInfo);
        this.extend = "";
        this.visibleFlag = true;
        this.subfolder = "";
        this.defaultMember = "";
    }

    public NamedDimCol(Integer datasetId, String model, String dimTable, String dimCol, String dimColAlias,
                       Integer colType, String dataType, List<VisibleAttr> visible, List<VisibleAttr> invisible,
                       String description, Boolean visibleFlag, String nameColumn, String valueColumn,
                       List<PropertyAttr> properties, TranslationEntity translation, String folder, String defaultMember) {
        this.datasetId = datasetId;
        this.model = model;
        this.dimTable = dimTable;
        this.dimCol = dimCol;
        this.dimColAlias = dimColAlias;
        this.colType = colType;
        this.dataType = dataType;
        this.extend = new DescWrapperExtend().withDescription(description).withProperties(properties).
                withVisible(visible).withInvisible(invisible).take();
        this.visibleFlag = visibleFlag;
        this.nameColumn = nameColumn;
        this.valueColumn = valueColumn;
        this.translation = JSON.toJSONString(translation);
        this.subfolder = folder;
        this.defaultMember = defaultMember;
    }

    @Override
    public DescWrapperExtend buildExtend() {
        return JSON.parseObject(extend, DescWrapperExtend.class);
    }

    public String getTableColName() {
        return "[" + this.getDimTable() + "]." + "[" + this.getDimCol() + "]";
    }

    public String getTableColAlias() {
        return "[" + this.getDimTable() + "]." + "[" + this.getDimColAlias() + "]";
    }

}
