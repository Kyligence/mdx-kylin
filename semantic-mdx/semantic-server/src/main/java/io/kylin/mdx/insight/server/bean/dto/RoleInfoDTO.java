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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kylin.mdx.insight.core.entity.VisibleAttr;
import io.kylin.mdx.insight.core.entity.RoleInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;


@Data
@NoArgsConstructor
public class RoleInfoDTO {

    private Integer id;

    @NotBlank
    @Length(max = 20)
    private String name;

    @NotNull
    @JsonProperty("contains")
    private List<VisibleAttr> visible;

    @NotNull
    @Length(max = 500)
    private String description;

    private Long createTime;

    private Long modifyTime;

    public RoleInfoDTO(RoleInfo roleInfo) {
        this.name = roleInfo.getName();
        this.visible = roleInfo.extractVisibleFromExtend();
        this.description = roleInfo.getDescription();
    }

    public RoleInfoDTO(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public RoleInfoDTO(Integer id, String name, String description) {
        this(id,name);
        this.description = description;
    }

}
