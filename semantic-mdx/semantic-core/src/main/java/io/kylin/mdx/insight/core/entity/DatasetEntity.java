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
import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "dataset")
public class DatasetEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Integer id;

    @Column(name = "project")
    private String project;

    @Column(name = "dataset")
    private String dataset;

    @Column(name = "access")
    private Integer access;

    @Column(name = "create_user")
    private String createUser;

    @Column(name = "canvas")
    private String canvas;

    @Column(name = "status")
    private String status;

    @Column(name = "broken_msg")
    private String brokenMsg;

    @Column(name = "create_time")
    private Long createTime;

    @Column(name = "modify_time")
    private Long modifyTime;

    @Column(name = "front_v")
    private String frontVersion;

    @Column(name = "extend")
    private String extend;

    @Column(name = "translation_types")
    private String translationTypes;

    public DatasetEntity(String project) {
        this.project = project;
    }

    /**
     * 将 access 转换为是否走黑名单模式 boolean 值
     *
     * @return 如果 access == 1 返回 true，否则返回 false
     */
    public boolean toAllowAccessByDefault() {
        return access != null && access > 0;
    }

    @Data
    public static class ExtendHelper {

        @JSONField(name = "model_aliases")
        private List<ModelAlias> modelAliasList;

        public void addModelAlias(String modelName, String modelAlias, TranslationEntity translation) {
            modelAliasList = Optional.ofNullable(modelAliasList).orElse(new ArrayList<>());
            modelAliasList.add(new ModelAlias(modelName, modelAlias, translation));
        }

        public String json() {
            return JSON.toJSONString(this);
        }

        public void setToDatasetEntity(DatasetEntity datasetEntity) {
            if (datasetEntity == null) {
                return;
            }
            datasetEntity.setExtend(json());
        }

        public static ExtendHelper restoreModelAlias(DatasetEntity datasetEntity) {
            if (datasetEntity == null) {
                return null;
            }

            return Optional.ofNullable(JSON.parseObject(datasetEntity.getExtend(), ExtendHelper.class)).orElse(new ExtendHelper());

        }

        public String getModelAlias(String modelName) {
            if (modelAliasList == null) {
                return modelName;
            }

            for (ModelAlias modelAlias : modelAliasList) {
                if (modelAlias.getName().equalsIgnoreCase(modelName)) {
                    return modelAlias.getAlias();
                }
            }

            return modelName;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        static class ModelAlias {
            private String name;
            private String alias;
            private TranslationEntity translation;
        }
    }
}
