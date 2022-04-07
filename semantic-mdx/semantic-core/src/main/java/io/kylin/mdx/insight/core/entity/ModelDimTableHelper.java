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

import io.kylin.mdx.insight.common.util.Utils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class ModelDimTableHelper {

    private final Set<String> modelSet = new TreeSet<>();

    private final Map<String, List<String>> model2DimTables = new HashMap<>();

    public boolean containsModel(String modelName) {
        return modelSet.contains(modelName);
    }

    public Set<String> getModelNameSet() {
        return modelSet;
    }

    public void addModelName(CommonDimRelation commonDimRelation) {
        if (commonDimRelation == null) {
            return;
        }
        addModelName(commonDimRelation.getModel());
        addModelName(commonDimRelation.getModelRelated());
    }

    public void addModelName(String modelName) {
        if (StringUtils.isNotBlank(modelName)) {
            modelSet.add(modelName);
        }
    }

    public void addIgnoreItem(String model, String dimTable) {
        List<String> dimTables = model2DimTables.get(model);
        if (Utils.isCollectionEmpty(dimTables)) {
            dimTables = new LinkedList<>();
            model2DimTables.put(model, dimTables);
        }
        dimTables.add(dimTable);
    }

    public boolean displayThisTable(String model, String dimTable) {
        List<String> dimTables = model2DimTables.get(model);
        if (Utils.isCollectionEmpty(dimTables)) {
            return false;
        }
        return dimTables.contains(dimTable);
    }
}
