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


package io.kylin.mdx.insight.server.facade;

import org.apache.commons.lang3.StringUtils;
import io.kylin.mdx.insight.core.meta.SemanticAdapter;
import io.kylin.mdx.insight.core.model.acl.*;
import io.kylin.mdx.insight.core.support.AclPermission;
import io.kylin.mdx.insight.core.sync.MetaStore;
import io.kylin.mdx.insight.server.support.DependResolver;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AclFacade implements AclPermission {

    /**
     * 解析依赖时可能存在循环引用，需要控制最大深度，超出直接跳出
     */
    private static final int MAX_CHECK_DEEP = 10;

    @Setter
    private DependResolver resolver = new DependResolver();

    @Getter
    @Setter
    private SemanticAdapter semanticAdapter = SemanticAdapter.INSTANCE;

    @Override
    public AclDataset preparePermission(String project, String type, String name, AclDataset dataset) {
        List<String> tables = countKylinTables(dataset);
        AclProjectModel model = getSemanticAdapter().getAclModel(project, type, name, tables);
        MetaStore.getInstance().recordAclProjectModel(model);
        validatePermission(model, dataset);
        return dataset;
    }

    private List<String> countKylinTables(AclDataset datasetDTO) {
        List<AclSemanticModel> models = datasetDTO.getModels();
        Set<String> tables = new HashSet<>();
        for (AclSemanticModel model : models) {
            // 添加 fact_table, 不必考虑为空情形
            if (StringUtils.isNotBlank(model.getFactTable())) {
                tables.add(model.getFactTable());
            }
            for (AclDimensionTable table : model.getDimensionTables()) {
                // 可能存在 actual_table 为空情形
                if (StringUtils.isNotBlank(table.getActualTable())) {
                    tables.add(table.getActualTable());
                }
            }
        }
        return new ArrayList<>(tables);
    }

    private void validatePermission(AclProjectModel projectModel, AclDataset datasetDTO) {
        // Model
        List<AclSemanticModel> models = datasetDTO.getModels();
        for (AclSemanticModel model : models) {
            // Dimension Table
            for (AclDimensionTable table : model.getDimensionTables()) {
                checkNamedDimTable(projectModel.getModel(table.getActualTable()), table);
            }
            // Fact Table
            for (AclMeasure measure : model.getMeasures()) {
                checkNamedMeasure(projectModel, measure, model);
            }
        }
        // Calculated Measure
        for (AclCalculationMeasure calculationMeasure : datasetDTO.getCalculateMeasures()) {
            checkDependColumn(datasetDTO, calculationMeasure, 0);
        }
        // Named Set
        for (AclNamedSet namedSet : datasetDTO.getNamedSets()) {
            checkDependColumn(datasetDTO, namedSet, 0);
        }
    }

    private void checkNamedDimTable(AclTableModel tableModel, AclDimensionTable dimTable) {
        if (tableModel == null || tableModel.isInvisible()) {
            dimTable.setAccess(false);
            return;
        }
        dimTable.setAccess(true);
        // 校验维度列
        List<AclDimensionCol> columns = dimTable.getDimCols();
        for (AclDimensionCol column : columns) {
            checkNamedDimColumn(tableModel, column, dimTable, 0);
        }
        // 校验自定义维度
        List<AclHierarchy> hierarchies = dimTable.getHierarchys();
        for (AclHierarchy hierarchy : hierarchies) {
            Set<String> effectBy = new HashSet<>();
            if (hierarchy.getDimCols() != null) {
                for (String dimCol : hierarchy.getDimCols()) {
                    AclDimensionCol aclDimCol = dimTable.getDimensionColumnByName(dimCol);
                    if (aclDimCol.noAccessRight()) {
                        effectBy.addAll(aclDimCol.getEffectedBy());
                    }
                }
            }
            if (hierarchy.getWeightCols() != null) {
                for (String weightCol : hierarchy.getWeightCols()) {
                    if (weightCol == null) {
                        continue;
                    }
                    AclDimensionCol aclDimCol = dimTable.getDimensionColumnByName(weightCol);
                    if (aclDimCol.noAccessRight()) {
                        effectBy.addAll(aclDimCol.getEffectedBy());
                    }
                }
            }
            hierarchy.setAccess(effectBy.isEmpty());
            hierarchy.setEffectedBy(new ArrayList<>(effectBy));
        }
    }

    private void checkNamedDimColumn(AclTableModel tableModel, AclDimensionCol column,
                                     AclDimensionTable dimTable, int deep) {
        if (column.getAccess() != null) {
            return;
        }
        Set<String> effectBy = parseNamedDimColumn(tableModel, column, dimTable, deep);
        column.setAccess(effectBy.isEmpty());
        column.setEffectedBy(new ArrayList<>(effectBy));
    }

    private Set<String> parseNamedDimColumn(AclTableModel tableModel, AclDimensionCol column,
                                            AclDimensionTable dimTable, int deep) {
        Set<String> effectBy = new HashSet<>();
        if (!tableModel.isAccess(column.getName())) {
            effectBy.add(column.getName());
        }
        if (StringUtils.isNotBlank(column.getNameColumn())) {
            resolveDependColumn(tableModel, dimTable, column.getNameColumn(), effectBy, deep);
        }
        if (StringUtils.isNotBlank(column.getValueColumn())) {
            resolveDependColumn(tableModel, dimTable, column.getValueColumn(), effectBy, deep);
        }
        if (column.getProperties() != null) {
            for (AclProperty property : column.getProperties()) {
                resolveDependColumn(tableModel, dimTable, property.getColName(), effectBy, deep);
            }
        }
        return effectBy;
    }

    private void resolveDependColumn(AclTableModel tableModel, AclDimensionTable dimTable,
                                     String colName, Set<String> effectBy, int deep) {
        AclDimensionCol dimCol = dimTable.getDimensionColumnByName(colName);
        if (dimCol == null || deep > MAX_CHECK_DEEP) {
            return;
        }
        Boolean access = dimCol.getAccess();
        if (dimCol.getAccess() == null) {
            checkNamedDimColumn(tableModel, dimCol, dimTable, deep + 1);
            access = dimCol.getAccess();
        }
        if (!access) {
            effectBy.addAll(dimCol.getEffectedBy());
        }
    }

    private void checkNamedMeasure(AclProjectModel projectModel, AclMeasure measureDTO, AclSemanticModel semanticModel) {
        Pair<String, String> dimAndCol = measureDTO.toDimAndCol();
        if (dimAndCol == null) {
            return;
        }
        AclTableModel tableModel = null;
        if (dimAndCol.getLeft() != null) {
            AclDimensionTable dimTable = semanticModel.getDimensionTableByAlias(dimAndCol.getLeft());
            if (dimTable != null) {
                tableModel = projectModel.getModel(dimTable.getActualTable());
            }
        }
        if (tableModel == null) {
            tableModel = projectModel.getModel(semanticModel.getFactTable());
        }
        String columnName = dimAndCol.getRight();
        if (tableModel == null || tableModel.isInvisible()) {
            measureDTO.setAccess(false);
            measureDTO.setEffectedBy(Collections.singletonList(columnName));
        } else {
            boolean access = tableModel.isAccess(columnName);
            measureDTO.setAccess(access);
            if (access) {
                measureDTO.setEffectedBy(Collections.emptyList());
            } else {
                measureDTO.setEffectedBy(Collections.singletonList(columnName));
            }
        }
    }

    /**
     * 校验命名集和计算度量是否可访问，可能存在循环验证
     */
    private void checkDependColumn(AclDataset dataset,
                                   AclDependColumn dependColumn, int deep) {
        if (dependColumn.getAccess() != null) {
            // 已经被验证过的，直接跳过
            return;
        }
        Set<String> effectBy = parseExpression(dataset, dependColumn, deep);
        dependColumn.setAccess(effectBy.isEmpty());
        dependColumn.setEffectedBy(new ArrayList<>(effectBy));
    }

    private Set<String> parseExpression(AclDataset dataset,
                                        AclDependColumn dependColumn, int deep) {
        Set<String> effectBy = new HashSet<>();
        DependResolver.DependResult result = resolver.resolve(dataset, dependColumn);
        for (Pair<String, String> dimName : result.getDimNames()) {
            AclDimensionTable dimensionTable = dataset.getDimensionTableByAlias(dimName.getLeft());
            if (dimensionTable != null) {
                forDimensionColumn(dimensionTable, dimName.getRight(), effectBy);
            }
        }
        for (AclDependColumn column : result.getDepends()) {
            resolveDependColumn(dataset, column, effectBy, deep);
        }
        return effectBy;
    }

    /**
     * 处理 度量、计算度量 和 命名集
     * 由于 度量 和 维度 优先于 计算度量 和 命名集 判断，因此后两者需要计算一个递归判断
     * 其中对于度量，已经前置计算过 access 不可能为空，可以直接复用
     */
    private void resolveDependColumn(AclDataset dataset, AclDependColumn dependColumn,
                                     Set<String> effectBy, int deep) {
        if (dependColumn == null || deep > MAX_CHECK_DEEP) {
            // 循环依赖, 直接结束
            return;
        }
        Boolean access = dependColumn.getAccess();
        if (access == null) {
            // 优先计算依赖关系，度量不可能到这一步
            assert !(dependColumn instanceof AclMeasure);
            checkDependColumn(dataset, dependColumn, deep + 1);
            access = dependColumn.getAccess();
        }
        if (!access) {
            // 添加全部依赖字段
            effectBy.addAll(dependColumn.getEffectedBy());
        }
    }

    /**
     * 处理 维度
     * 维表存在表级可见性和列级可见性，同时维度也区分普通维度和层级维度
     */
    private void forDimensionColumn(AclDimensionTable dimTable, String dimColName, Set<String> effectBy) {
        if (dimColName == null) {
            return;
        }
        if (dimTable.getAccess() != null && !dimTable.getAccess()) {
            // 维表不可访问, 直接记录维度名称
            effectBy.add(dimColName);
            return;
        }
        AclAccessible accessible = dimTable.getDimensionColumnByAlias(dimColName);
        if (accessible == null) {
            // 普通维度找不到, 尝试找层级维度
            accessible = dimTable.getHierarchy(dimColName);
        }
        if (accessible != null && accessible.noAccessRight()) {
            effectBy.addAll(accessible.getEffectedBy());
        }
    }

}
