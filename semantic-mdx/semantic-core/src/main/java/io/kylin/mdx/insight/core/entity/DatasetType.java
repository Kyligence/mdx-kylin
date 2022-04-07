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

import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.ErrorCode;

public enum DatasetType {
    /**
     * 数据集类型
     */
    MDX,

    SQL;

    public static String ordinalToLiteral(int type) throws SemanticException {
        for (DatasetType datasetType : values()) {
            if (datasetType.ordinal() == type) {
                return datasetType.toString();
            }
        }
        throw new SemanticException(Utils.formatStr("The dataset type:[%d] doesn't support.", type), ErrorCode.DATASET_TYPE_NOT_SUPPORTED);
    }
}
