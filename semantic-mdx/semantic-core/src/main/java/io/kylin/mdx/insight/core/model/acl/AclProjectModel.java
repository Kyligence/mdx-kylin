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


package io.kylin.mdx.insight.core.model.acl;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

@Data
public class AclProjectModel {

    /**
     * 类型
     */
    private String type;

    /**
     * 名称
     */
    private String name;

    /**
     * 项目
     */
    private String project;

    /**
     * 维表权限设置
     * 表名:  DEFAULT.KYLIN_SALES
     * <p>
     * 涉及 维度 和 度量 的权限设置
     */
    private final Map<String, AclTableModel> models = new HashMap<>();

    public AclProjectModel(String type, String name, String project) {
        this.type = type;
        this.name = name;
        this.project = project;
    }

    public void setModel(String tableName, AclTableModel model) {
        models.put(tableName, model);
    }

    public AclTableModel getModel(String tableName) {
        return models.get(tableName);
    }

    /**
     * 模型是否兼容
     *
     * @param aclProjectModel
     * @return
     */
    public boolean isCompatible(AclProjectModel aclProjectModel) {
        for (String tableName : getModels().keySet()) {
            AclTableModel newTableModel = getModel(tableName);
            AclTableModel oldTableModel = aclProjectModel.getModel(tableName);
            if (oldTableModel == null) {
                return false;
            }
            if (newTableModel.isInvisible() != oldTableModel.isInvisible()) {
                return false;
            }
            if (newTableModel.isInvisible()) {
                continue;
            }
            if (!newTableModel.isCompatible(oldTableModel)) {
                return false;
            }
        }
        return true;
    }

}
