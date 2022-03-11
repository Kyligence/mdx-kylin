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


package io.kylin.mdx.insight.core.model.generic;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.kylin.mdx.insight.common.SemanticConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CubeMeasure {

    private String measureName;

    private String alias;

    private ColumnIdentity colMeasured;

    private String dataType;

    private String expression;

    @JsonIgnore
    public String getTableColumnStr() {
        if (colMeasured == null || (SemanticConstants.FUNCTION_CONSTANT_TYPE.equals(colMeasured.getTableAlias())
                && "1".equals(colMeasured.getColName()))) {
            return SemanticConstants.FUNCTION_CONSTANT_TYPE;
        }

        return colMeasured.getTableAlias() + SemanticConstants.DOT + colMeasured.getColName();
    }
}
