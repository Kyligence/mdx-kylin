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


package io.kylin.mdx.insight.engine.service;

import org.apache.commons.lang3.StringUtils;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.entity.NamedDimCol;
import io.kylin.mdx.insight.core.entity.NamedDimTable;
import io.kylin.mdx.insight.core.meta.SemanticAdapter;
import io.kylin.mdx.insight.core.service.DatasetService;
import io.kylin.mdx.insight.core.service.MetadataService;
import io.kylin.mdx.insight.core.manager.ProjectManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MetadataServiceImpl implements MetadataService {

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private ProjectManager projectManager;

    /**
     * project -> (dataset -> cardinalities)
     * cardinalities: database + table_alias + col_alias -> cardinality
     */
    private final Map<String, Map<String, Map<String, Long>>> cardinalityMaps = new ConcurrentHashMap<>();

    @Override
    public Map<String, Map<String, Long>> getCardinalityMap(String project) {
        return cardinalityMaps.getOrDefault(project, Collections.emptyMap());
    }

    @Override
    public Map<String, Long> getCardinalityMap(String project, String dataset) {
        return cardinalityMaps.getOrDefault(project, Collections.emptyMap())
                .getOrDefault(dataset, Collections.emptyMap());
    }

    @Override
    public void syncCardinalityInfo() {
        List<String> datasetProjects = datasetService.getProjectsRelatedDataset();
        Set<String> effectiveProjects = projectManager.getAllProject();
        for (String project : datasetProjects) {
            if (!effectiveProjects.contains(project)) {
                log.info("Project {} doesn't exist in Kylin 4, skip it dataset verify.", project);
                continue;
            }
            Map<String, Long> cardDimensionMap = SemanticAdapter.INSTANCE.getDimensionCardinality(project);
            List<DatasetEntity> datasetList = datasetService.selectDatasetByProjectName(project);
            Map<String, Map<String, Long>> projectMap = new LinkedHashMap<>();

            for (DatasetEntity dataset : datasetList) {
                Map<String, Long> newCardDimensionMap = new HashMap<>();
                Integer datasetId = dataset.getId();
                String datasetName = dataset.getDataset();
                List<NamedDimTable> namedDimTables = datasetService.selectDimTablesByDatasetId(datasetId);
                for (NamedDimTable namedDimTable : namedDimTables) {
                    String actualTable = namedDimTable.getActualTable();
                    if (StringUtils.isBlank(actualTable)) {
                        // 跳过空事实表
                        continue;
                    }
                    String databaseName = actualTable.split("\\.")[0];
                    String tableName = null;
                    if (StringUtils.isNotBlank(actualTable) && actualTable.contains(".")) {
                        tableName = actualTable.split("\\.")[1];
                    }
                    if (StringUtils.isBlank(tableName)) {
                        tableName = namedDimTable.getDimTable();
                    }

                    NamedDimCol search = new NamedDimCol(namedDimTable.getDatasetId(), null, namedDimTable.getDimTable());
                    List<NamedDimCol> namedDimCols = datasetService.selectAllDimColByDatasetIdAndTable(search);
                    for (NamedDimCol namedDimCol : namedDimCols) {
                        String oldDimCol = "[" + databaseName + "].[" + tableName + "].[" + namedDimCol.getDimCol() + "]";
                        long oldValue = cardDimensionMap.getOrDefault(oldDimCol, 0L);
                        if (oldValue == 0) {
                            continue;
                        }
                        String newDimCol = "[" + databaseName + "].[" + namedDimTable.getDimTableAlias() + "].[" + namedDimCol.getDimColAlias() + "]";
                        newCardDimensionMap.put(newDimCol, oldValue);
                    }
                }
                projectMap.put(datasetName, newCardDimensionMap);
            }
            cardinalityMaps.put(project, projectMap);
        }
    }

}
