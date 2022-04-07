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


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetItemDTO {

    private String type;

    private String detail;

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (this.type == null ? 0 : this.type.hashCode());
        result = 31 * result + (this.detail == null ? 0 : this.detail.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object t) {
        if (this == t)
            return true;
        if (!( t instanceof DatasetItemDTO))
            return false;
        return (Objects.equals(this.type, ((DatasetItemDTO) t).type) && Objects.equals(this.detail, ((DatasetItemDTO) t).detail));
    }

}
