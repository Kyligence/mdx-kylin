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

package io.kylin.mdx.insight.core.sync;

import io.kylin.mdx.insight.core.service.MetadataService;
import io.kylin.mdx.insight.core.support.SpringHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class DimensionCardinality {

    public static Map<String, Map<String, Long>> getCardinalityMap(String project) {
        MetadataService metadataService = SpringHolder.getBean(MetadataService.class);
        return metadataService.getCardinalityMap(project);
    }

    public static Map<String, Long> getCardinalityMap(String project, String dataset) {
        MetadataService metadataService = SpringHolder.getBean(MetadataService.class);
        return metadataService.getCardinalityMap(project, dataset);
    }

}
