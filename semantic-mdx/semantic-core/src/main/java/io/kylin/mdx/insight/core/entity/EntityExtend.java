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
import io.kylin.mdx.insight.common.SemanticConstants;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体扩展信息，包含可访问用户和不可访问用户，取决于 Dataset 的 access 字段
 */
@Data
public class EntityExtend {

    private List<VisibleAttr> visible;

    private List<VisibleAttr> invisible;

    public EntityExtend withVisible(List<VisibleAttr> visible) {
        this.visible = visible;
        return this;
    }

    public EntityExtend withInvisible(List<VisibleAttr> invisible) {
        this.invisible = invisible;
        return this;
    }

    public void addVisibleUser(String username) {
        if (visible == null) {
            visible = new ArrayList<>();
        }
        VisibleAttr visibleAttr = new VisibleAttr(SemanticConstants.USER, username);
        if (!visible.contains(visibleAttr)) {
            visible.add(visibleAttr);
        }
    }

    public void removeVisibleUser(String username) {
        if (visible == null) {
            visible = new ArrayList<>();
            return;
        }
        visible.remove(new VisibleAttr(SemanticConstants.USER, username));
    }

    public void addInvisibleUser(String username) {
        if (invisible == null) {
            invisible = new ArrayList<>();
        }
        VisibleAttr visibleAttr = new VisibleAttr(SemanticConstants.USER, username);
        if (!invisible.contains(visibleAttr)) {
            invisible.add(visibleAttr);
        }
    }

    public void removeInvisibleUser(String username) {
        if (invisible == null) {
            invisible = new ArrayList<>();
            return;
        }
        invisible.remove(new VisibleAttr(SemanticConstants.USER, username));
    }

    public String take() {
        return JSON.toJSONString(this);
    }

}
