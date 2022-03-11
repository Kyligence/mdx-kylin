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

import java.util.List;
import io.kylin.mdx.insight.common.util.Utils;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DescWrapperExtend extends EntityExtend {

    private String description;

    private List<PropertyAttr> properties;

    public DescWrapperExtend withDescription(String description) {
        this.description = Utils.nullToEmptyStr(description);
        return this;
    }

    public EntityExtend withProperties(List<PropertyAttr> properties) {
        this.properties = properties;
        return this;
    }
}
