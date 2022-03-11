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


package io.kylin.mdx.insight.core.model.semantic;

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.ErrorCode;

public enum ModelDimRelationType {

    /**
     * 关联
     */
    JOINT,

    /**
     * 非关联
     */
    NOT_JOINT,

    /**
     * 多对多
     */
    MANY_TO_MANY;

    public static ModelDimRelationType of(Integer ordinal) throws SemanticException {

        switch (ordinal) {
            case 0:
                return JOINT;
            case 1:
                return NOT_JOINT;
            case 2:
                return MANY_TO_MANY;
            default:
                throw new SemanticException(Utils.formatStr("There isn't model_dim_relation_type for ordinal:[%d]", ordinal), ErrorCode.UNSUPPORTED_MODEL_RELATION_TYPE);
        }
    }
}
