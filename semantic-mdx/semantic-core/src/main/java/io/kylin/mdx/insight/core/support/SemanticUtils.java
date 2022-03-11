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


package io.kylin.mdx.insight.core.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.*;
import io.kylin.mdx.insight.core.meta.TypeConverter;
import io.kylin.mdx.insight.core.model.generic.*;
import io.kylin.mdx.insight.core.model.semantic.ModelDimTableColKey;
import io.kylin.mdx.insight.core.model.semantic.ModelDimTableKey;
import io.kylin.mdx.insight.core.model.semantic.ModelMeasureKey;
import io.kylin.mdx.insight.core.sync.ModelVersionHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SemanticUtils {

    public static String[] getSchemaAndTable(String tableWithSchema) {
        assert tableWithSchema != null && tableWithSchema.indexOf('.') != -1;
        return tableWithSchema.split("[.]");
    }

    public static String[] getTableAndCol(String tableWithCol) {
        assert tableWithCol != null && tableWithCol.indexOf('.') != -1;
        return tableWithCol.split("[.]");
    }

    public static String[] getModelAndTable(String modelTable) {
        assert modelTable != null && modelTable.indexOf('.') != -1;
        return modelTable.split("[.]");
    }

    public static Set<String> extractModelNames(List<CommonDimRelation> commonDimRelations) {
        Set<String> modelUsedSet = new TreeSet<>();
        commonDimRelations.forEach(commonDimRelation -> {
            Utils.addNoEmptyStrToSet(modelUsedSet, commonDimRelation.getModel());
            Utils.addNoEmptyStrToSet(modelUsedSet, commonDimRelation.getModelRelated());
        });
        return modelUsedSet;
    }

    public static boolean anyThingChangedInModels(String project, List<KylinGenericModel> nocacheModels) {
        for (KylinGenericModel noCacheModel : nocacheModels) {
            String modelName = noCacheModel.getModelName();
            String version = ModelVersionHolder.getVersion(project, modelName);
            if (StringUtils.isBlank(version)) {
                log.warn("Model version miss! project:{}, model:{}", project, modelName);
            }

            if (StringUtils.isNotBlank(version) && !version.equals(noCacheModel.getSignature())) {
                log.info("Model version not match. project:{}, model:{}", project, modelName);

                ModelVersionHolder.setNewVersion(project, modelName, noCacheModel.getSignature());
                return true;
            }
        }

        return false;
    }

    public static ColumnInfo getColumnInfo(KylinGenericModel genericModel, ColumnIdentity colIdentity) {

        Map<ActualTableCol, ColumnInfo> actCol2ColInfo = genericModel.getActCol2ColInfo();

        Map<String, ActualTable> tblAlias2ActTbl = genericModel.getTblAlias2ActTbl();
        ActualTable actualTable = tblAlias2ActTbl.get(colIdentity.getTableAlias());

        return actCol2ColInfo.get(new ActualTableCol(actualTable, colIdentity.getColName()));
    }

    public static String getDataTypeStr(ColumnInfo columnInfo) {
        if (columnInfo.getDataType() != null) {
            return TypeConverter.getLiteralSqlType(columnInfo.getDataType());
        } else {
            return columnInfo.getDataTypeStr();
        }
    }

    public static String getNormalMeasureArg(String dimColumn) {
        return SemanticConstants.MEASURE_CONSTANT.equals(dimColumn) ?
                SemanticConstants.FUNCTION_CONSTANT_TYPE : dimColumn;
    }

    public static String getUIMeasureArg(String dimColumn) {
        return SemanticConstants.FUNCTION_CONSTANT_TYPE.equals(dimColumn) ?
                SemanticConstants.MEASURE_CONSTANT : dimColumn;
    }

    public static String getNormalMeasureArgByColMeasured(ColumnIdentity colMeasured) {
        if (colMeasured == null || (SemanticConstants.FUNCTION_CONSTANT_TYPE.equals(colMeasured.getTableAlias())
                && "1".equals(colMeasured.getColName()))) {
            return SemanticConstants.FUNCTION_CONSTANT_TYPE;
        }

        return colMeasured.getTableAlias() + SemanticConstants.DOT + colMeasured.getColName();
    }

    public static Map<String, List<NamedDimTable>> buildModelToNamedTables(List<NamedDimTable> namedDimTables) {
        if (Utils.isCollectionEmpty(namedDimTables)) {
            return Collections.emptyMap();
        }

        Map<String, List<NamedDimTable>> namedTablesMap = new HashMap<>();
        for (NamedDimTable namedDimTable : namedDimTables) {
            Utils.insertValToMap(namedTablesMap, namedDimTable.getModel(), namedDimTable);
        }

        return namedTablesMap;
    }

    public static Map<ModelDimTableKey, List<CustomHierarchy>> buildModelDimTableToHierarchies(List<CustomHierarchy> customHierarchys) {
        if (Utils.isCollectionEmpty(customHierarchys)) {
            return Collections.emptyMap();
        }

        Map<ModelDimTableKey, List<CustomHierarchy>> hierarchyMap = new HashMap<>(3);
        for (CustomHierarchy hierarchy : customHierarchys) {
            ModelDimTableKey modelDimTableKey = new ModelDimTableKey(hierarchy);

            Utils.insertValToMap(hierarchyMap, modelDimTableKey, hierarchy);
        }

        return hierarchyMap;
    }

    public static Map<ModelDimTableKey, List<NamedDimCol>> buildModelDimTableToDimCols(List<NamedDimCol> namedDimCols) {

        if (Utils.isCollectionEmpty(namedDimCols)) {
            return Collections.emptyMap();
        }

        Map<ModelDimTableKey, List<NamedDimCol>> colMap = new HashMap<>(namedDimCols.size());
        for (NamedDimCol namedDimCol : namedDimCols) {
            ModelDimTableKey modelDimTableKey = new ModelDimTableKey(namedDimCol.getModel(), namedDimCol.getDimTable());
            Utils.insertValToMap(colMap, modelDimTableKey, namedDimCol);
        }

        return colMap;
    }

    public static Map<ModelDimTableColKey, NamedDimCol> buildModelDimTableColToDimCol(List<NamedDimCol> namedDimCols) {

        if (Utils.isCollectionEmpty(namedDimCols)) {
            return Collections.emptyMap();
        }

        Map<ModelDimTableColKey, NamedDimCol> colMap = new HashMap<>(namedDimCols.size());
        for (NamedDimCol namedDimCol : namedDimCols) {
            colMap.put(new ModelDimTableColKey(namedDimCol), namedDimCol);
        }

        return colMap;
    }

    public static Map<String, List<NamedMeasure>> buildModelToMeasures(List<NamedMeasure> namedMeasures) {

        if (Utils.isCollectionEmpty(namedMeasures)) {
            return Collections.emptyMap();
        }

        Map<String, List<NamedMeasure>> measureMap = new HashMap<>();
        for (NamedMeasure namedMeasure : namedMeasures) {
            String modelKey = namedMeasure.getModel();
            Utils.insertValToMap(measureMap, modelKey, namedMeasure);
        }

        return measureMap;
    }

    public static Map<ModelMeasureKey, NamedMeasure> buildModelMeasureToMeasure(List<NamedMeasure> namedMeasures) {
        if (Utils.isCollectionEmpty(namedMeasures)) {
            return Collections.emptyMap();
        }

        return namedMeasures.stream()
                .collect(Collectors.toMap(ModelMeasureKey::new, namedMeasure -> namedMeasure));
    }

    public static Map<ModelDimTableKey, NamedDimTable> buildModelDimTableToNamedTable(List<NamedDimTable> namedDimTables) {
        if (Utils.isCollectionEmpty(namedDimTables)) {
            return Collections.emptyMap();
        }

        Map<ModelDimTableKey, NamedDimTable> dimTableMap = new HashMap<>();
        for (NamedDimTable namedDimTable : namedDimTables) {
            dimTableMap.put(new ModelDimTableKey(namedDimTable), namedDimTable);
        }

        return dimTableMap;
    }

    public static List<DimTableModelRel> convertToDimTableModelRels(List<NamedDimTable> toAddNamedDimTables) {
        if (Utils.isCollectionEmpty(toAddNamedDimTables)) {
            return Collections.emptyList();
        }

        return toAddNamedDimTables.stream()
                .map(toAddNamedDimTable -> {
                    DimTableModelRel dimTableModelRel = new DimTableModelRel();
                    dimTableModelRel.setDatasetId(toAddNamedDimTable.getDatasetId());
                    dimTableModelRel.setModel(toAddNamedDimTable.getModel());
                    dimTableModelRel.setDimTable(toAddNamedDimTable.getDimTable());
                    dimTableModelRel.setRelation(DimensionUsage.JOINT.ordinal());
                    dimTableModelRel.setIntermediateDimTable("");
                    dimTableModelRel.setPrimaryDimCol("");
                    return dimTableModelRel;
                }).collect(Collectors.toList());
    }


    public static <T extends EntityExtend> T getExtend(String extend, Class<T> extendClass) {
        try {
            return Optional.ofNullable(JSON.parseObject(extend, extendClass)).orElse(extendClass.newInstance());
        } catch (Exception e) {
            log.error("Reflection newInstance has error", e);
            throw new RuntimeException(e);
        }
    }

    public static <T extends EntityExtend> T addInvisibleUser(String extend, String targetUser, Class<T> extendClass) {
        T o = getExtend(extend, extendClass);

        o.addVisibleUser(targetUser);

        return o;
    }

    public static <T extends EntityExtend> T removeInvisibleUser(String extend, String targetUser, Class<T> extendClass) {
        T o = getExtend(extend, extendClass);

        o.removeVisibleUser(targetUser);

        return o;
    }

    public static List<VisibleAttr> getVisibleFromExtend(EntityExtend entityExtend) {
        return entityExtend == null ? Collections.emptyList() :
                entityExtend.getVisible() == null ? Collections.emptyList() : entityExtend.getVisible();
    }

    public static List<VisibleAttr> getInvisibleFromExtend(EntityExtend entityExtend) {
        return entityExtend == null ? Collections.emptyList() :
                entityExtend.getInvisible() == null ? Collections.emptyList() : entityExtend.getInvisible();
    }

    public static String getDescFromExtend(DescWrapperExtend descWrapperExtend) {
        return descWrapperExtend == null ? "" : Utils.nullToEmptyStr(descWrapperExtend.getDescription());
    }

    public static List<PropertyAttr> getPropertiesFromExtend(DescWrapperExtend descWrapperExtend) {
        return descWrapperExtend == null ? Collections.emptyList() :
                descWrapperExtend.getProperties() == null ? Collections.emptyList() : descWrapperExtend.getProperties();
    }

    public static Map<String, String> buildTranslation(String translation){
        Map<String, String> m = new LinkedHashMap<>();
        if(translation != null && !"".equals(translation)) {
            TranslationEntity te = JSON.parseObject(translation, TranslationEntity.class);
            String s;
            try {
                s = new ObjectMapper().writeValueAsString(te);
                JSONObject jsonObject = JSONObject.parseObject(s);
                if (jsonObject != null) {
                    for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                        String value = (String)entry.getValue();
                        if(value != null && !"".equals(value)) {
                            String key = entry.getKey();
                            if("en-UK".equals(key)) {
                                key = "en-GB";
                            }
                            m.put(key, value);
                        }
                    }
                }

            } catch (JsonProcessingException e) {
                //TODO
            }
        }

        return m;
    }

}
