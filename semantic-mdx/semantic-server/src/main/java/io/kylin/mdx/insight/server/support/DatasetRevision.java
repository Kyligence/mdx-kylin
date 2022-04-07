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


package io.kylin.mdx.insight.server.support;

import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.DimTableType;
import io.kylin.mdx.insight.server.bean.dto.DimensionColDTO;
import io.kylin.mdx.insight.server.bean.dto.DimensionTableDTO;

import java.util.List;

public class DatasetRevision {

    private final static Integer DEFAULT_COL_TYPE = 0;

    public static void accept(DimensionTableDTO dimensionTableDTO) {
        List<DimensionColDTO> dimCols = dimensionTableDTO.getDimCols();

        if (Utils.isCollectionEmpty(dimCols)) {
            return;
        }

        boolean hasLevelType = false;
        for (DimensionColDTO dimCol : dimCols) {
            if (DEFAULT_COL_TYPE.compareTo(dimCol.getType()) != 0) {
                hasLevelType = true;
                break;
            }
        }

        if (hasLevelType &&
                DimTableType.REGULAR.getLowercase().equalsIgnoreCase(dimensionTableDTO.getType())) {
            dimensionTableDTO.setType(DimTableType.TIME.getLowercase());
        }

    }
}
