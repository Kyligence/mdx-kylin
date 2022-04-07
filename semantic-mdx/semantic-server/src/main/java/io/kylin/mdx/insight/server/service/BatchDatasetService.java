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


package io.kylin.mdx.insight.server.service;


import com.alibaba.fastjson.JSON;
import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.JacksonSerDeUtils;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.*;
import io.kylin.mdx.insight.core.meta.SemanticAdapter;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.model.semantic.DatasetStatus;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.core.service.DatasetService;
import io.kylin.mdx.insight.core.sync.DatasetEventObject;
import io.kylin.mdx.insight.engine.manager.SyncManager;
import io.kylin.mdx.insight.engine.service.parser.CalcMemberParserImpl;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.bean.dto.*;
import io.kylin.mdx.insight.server.bean.dto.CalculationMeasureDTO;
import io.kylin.mdx.insight.server.bean.dto.CommonDimModelDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetContrastDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetImportDetailsDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetImportRequestDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetImportResponseDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetItemDTO;
import io.kylin.mdx.insight.server.bean.dto.DimTableModelRelationDTO;
import io.kylin.mdx.insight.server.bean.dto.DimensionColDTO;
import io.kylin.mdx.insight.server.bean.dto.DimensionTableDTO;
import io.kylin.mdx.insight.server.bean.dto.HierarchyDTO;
import io.kylin.mdx.insight.server.bean.dto.MeasureDTO;
import io.kylin.mdx.insight.server.bean.dto.NamedSetDTO;
import io.kylin.mdx.insight.server.bean.dto.SemanticModelDTO;
import io.kylin.mdx.insight.server.bean.vo.DatasetIdVO;
import io.kylin.mdx.insight.server.exception.CalculateMeasureValidateException;
import io.kylin.mdx.insight.server.support.DatasetRevision;
import io.kylin.mdx.ErrorCode;
import io.kylin.mdx.insight.server.bean.dto.DatasetDTOBuilder;
import lombok.extern.slf4j.Slf4j;
import mondrian.olap.MondrianException;
import mondrian.parser.TokenMgrError;
import mondrian.xmla.XmlaRequestContext;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


@Slf4j
@Service
public class BatchDatasetService {

    private static final String ERROR_FOLDER_NAME = "TYPE[xxx] display folder name only supports Chinese, English, numbers and spaces.";

    private static final String FOLDER_NAME_FORMAT = "[\\u4e00-\\u9fa5A-Za-z0-9\\s\\\\]*";

    private static final String CALCULATED_MEASURE_TEMPLATE = "[Measures].[xxx]";

    private static final String NAME_SET_TEMPLATE = "{[xxx]}";

    private static final String CM_PARSER_SYNTAX_ERROR1 = "While parsing WITH MEMBER";

    private static final String CM_PARSER_SYNTAX_ERROR2 = "Syntax error";

    public static final Map<String, Pair<DatasetDTO, DatasetDTO>> importDatasetMap = new HashMap<>();

    public static final AtomicInteger globleDatasetId = new AtomicInteger();

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private CalcMemberParserImpl calcMemberParserImpl;

    @Autowired
    private AuthService authService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SyncManager syncManager;

    public Response<DatasetIdVO> doInsert(DatasetDTO datasetDTO, String basicAuth, boolean insertOnUpdate,
                                          Long createTime, String createUser) throws SemanticException {
        String user = Utils.decodeBasicAuth(basicAuth)[0].toUpperCase();
        String project = datasetDTO.getProject();
        String datasetName = datasetDTO.getDatasetName();

        if (SemanticConfig.getInstance().isGrantVisibilityToCreator()) {
            grantVisibilityToCurrentUser(datasetDTO, user);
        }

        boolean calculatedMeasuresValid = true;
        try {
            Response<DatasetIdVO> validationResult = validateDataset(datasetDTO, false);
            if (validationResult.getStatus() == Response.Status.FAIL.ordinal()) {
                return validationResult;
            }
        } catch (CalculateMeasureValidateException e) {
            calculatedMeasuresValid = false;
        }

        // 可能存在模型与 Kylin 不同步情况
        AtomicBoolean modelValid = new AtomicBoolean(true);
        List<KylinGenericModel> modelsByKylin = SemanticAdapter.INSTANCE.getNocacheGenericModels(datasetDTO.getProject());
        List<String> modelNamesByKylin = modelsByKylin.stream().map(model -> model.getModelName()).collect(Collectors.toList());
        List<CommonDimModelDTO> commonDimRelations = datasetDTO.getModelRelations();
        commonDimRelations.forEach(modelRelation -> {
            if(!modelRelation.getModelLeft().isEmpty() && !modelNamesByKylin.contains(modelRelation.getModelLeft())){
                modelValid.set(false);
                return;
            }
            if(!modelRelation.getModelRight().isEmpty() && !modelNamesByKylin.contains(modelRelation.getModelRight())){
                modelValid.set(false);
                return;
            }
        });

        Integer datasetId;
        String datasetExtend = buildDatasetExtend(datasetDTO);

        DatasetEntity.DatasetEntityBuilder datasetEntityBuilder = DatasetEntity.builder()
                .project(project)
                .dataset(datasetName)
                .access(datasetDTO.getAccess())
                .canvas(datasetDTO.getCanvas())
                .createTime(createTime)
                .modifyTime(Utils.currentTimeStamp())
                .frontVersion(datasetDTO.getFrontVersion())
                .extend(datasetExtend)
                .translationTypes(JSON.toJSONString(datasetDTO.getTranslationTypes()));
        DatasetEntity insertDataset;
        if (insertOnUpdate) {
            insertDataset = datasetEntityBuilder.createUser(createUser).build();
            datasetId = datasetDTO.getDatasetId();
            insertDataset.setId(datasetId);
            datasetService.insertDatasetWithId(insertDataset);
        } else {
            insertDataset = datasetEntityBuilder.createUser(user).build();
            datasetId = datasetService.insertDatasetSummary(insertDataset);
        }

        if (!calculatedMeasuresValid || !modelValid.get()) {
            datasetService.updateDatasetStatusAndBrokenInfo(datasetId, DatasetStatus.BROKEN,"");
        }

        //common_dim_relation
        datasetService.insertCommonDimModels(convert2CommonDimRelation(datasetId, datasetDTO.getModelRelations()));

        //insert CM
        List<CalculationMeasureDTO> calculateMeasures = datasetDTO.getCalculateMeasures();
        if (!Utils.isCollectionEmpty(calculateMeasures)) {
            datasetService.insertCalculateMeasures(convert2CalculateMeasureEntities(datasetId, calculateMeasures));
        }

        //insert namedSet
        List<NamedSetDTO> namedSets = datasetDTO.getNamedSets();
        if (!Utils.isCollectionEmpty(namedSets)) {
            datasetService.insertNamedSets(convert2NamedSetEntitys(datasetId, namedSets));
        }

        //insert semantic model
        List<SemanticModelDTO> models = datasetDTO.getModels();
        if (!Utils.isCollectionEmpty(models)) {
            insertSemanticModels(datasetId, models);
        }

        //insert dim_table_model_rel
        List<DimTableModelRelationDTO> dimTableModelRelationDTOs = datasetDTO.getDimTableModelRelations();
        if (!Utils.isCollectionEmpty(dimTableModelRelationDTOs)) {
            datasetService.insertDimTableModelRelation(convert2DimTableModelRelation(datasetId, dimTableModelRelationDTOs));
        }

        log.info("insert dataset successfully, datasetId:{}", datasetId);

        if (!insertOnUpdate) {
            syncManager.asyncNotify(
                    new DatasetEventObject(
                            new DatasetEventObject.DatasetChangedSource(project, datasetName),
                            DatasetEventObject.DatasetEventType.DATASET_NEWED)
            );
        }

        return new Response<DatasetIdVO>(Response.Status.SUCCESS)
                .data(new DatasetIdVO(datasetId, null, null, null));
    }


    public void grantVisibilityToCurrentUser(DatasetDTO datasetDTO, String user) {

        for (SemanticModelDTO modelDTO : datasetDTO.getModels()) {
            // grant dimension visibility
            Optional.ofNullable(modelDTO.getDimensionTables()).ifPresent(dimensionTableDTOS ->
                    dimensionTableDTOS.forEach(dimensionTableDTO ->
                            Optional.ofNullable(dimensionTableDTO.getDimCols()).ifPresent(dimensionColDTOS ->
                                    dimensionColDTOS.forEach(dimensionColDTO -> {
                                        List<VisibleAttr> visibleAttrList = addUserToVisibleAttr(dimensionColDTO.getVisible(), user);
                                        dimensionColDTO.setVisible(visibleAttrList);
                                    }))));

            // grant measure visibility
            Optional.ofNullable(modelDTO.getMeasures()).ifPresent(measureDTOS -> measureDTOS.forEach(measureDTO -> {
                List<VisibleAttr> visibleAttrList = addUserToVisibleAttr(measureDTO.getVisible(), user);
                measureDTO.setVisible(visibleAttrList);
            }));
        }

        // grant calculated measures visibility
        Optional.ofNullable(datasetDTO.getNamedSets()).ifPresent(namedSetDTOS ->
                namedSetDTOS.forEach(namedSetDTO -> {
                    List<VisibleAttr> visibleAttrList = addUserToVisibleAttr(namedSetDTO.getVisible(), user);
                    namedSetDTO.setVisible(visibleAttrList);
                }));

        // grant namedset visibility
        Optional.ofNullable(datasetDTO.getCalculateMeasures()).ifPresent(calculationMeasureDTOS ->
                calculationMeasureDTOS.forEach(calculationMeasureDTO -> {
                    List<VisibleAttr> visibleAttrList = addUserToVisibleAttr(calculationMeasureDTO.getVisible(), user);
                    calculationMeasureDTO.setVisible(visibleAttrList);
                }));

    }

    public Response<DatasetIdVO> validateDataset(DatasetDTO datasetDTO, boolean updateDataset) throws SemanticException {

        if (!updateDataset) {
            DatasetEntity datasetEntity = datasetService.getDatasetEntity(datasetDTO.getProject(), datasetDTO.getDatasetName());
            if (datasetEntity != null) {
                throw new SemanticException(Utils.formatStr("There is already the same dataset[%s,%s], please correct it", datasetDTO.getProject(), datasetDTO.getDatasetName()), ErrorCode.DATASET_ALREADY_EXISTS);
            }
        }

        Response<DatasetIdVO> response = new Response<DatasetIdVO>(Response.Status.SUCCESS)
                .data(new DatasetIdVO(null, null, null, null));

        if (!Utils.isCollectionEmpty(datasetDTO.getModels())) {

            validateDimension(datasetDTO, response);

            validateMeasure(datasetDTO, response);
        }

        return response;
    }


    public List<VisibleAttr> addUserToVisibleAttr(List<VisibleAttr> origin, String user) {
        if (origin == null) {
            origin = new ArrayList<>();
        }

        for (VisibleAttr visibleAttr : origin) {
            if (visibleAttr.getName().equalsIgnoreCase(user) && visibleAttr.getType().equalsIgnoreCase("user")) {
                return origin;
            }
        }

        origin.add(new VisibleAttr("user", user));

        return origin;
    }

    public String buildDatasetExtend(DatasetDTO datasetDTO) {
        DatasetEntity.ExtendHelper extendHelper = new DatasetEntity.ExtendHelper();
        for (SemanticModelDTO modelDTO : datasetDTO.getModels()) {
            extendHelper.addModelAlias(modelDTO.getModelName(), Utils.blankToDefaultString(modelDTO.getModelAlias(), modelDTO.getModelName()), modelDTO.getTranslation());
        }

        return extendHelper.json();
    }

    public List<NamedSet> convert2NamedSetEntitys(Integer datasetId, List<NamedSetDTO> namedSetDTOs) {

        List<NamedSet> namedSetEntityList = new LinkedList<>();

        for (NamedSetDTO namedSetDTO : namedSetDTOs) {
            namedSetEntityList.add(
                    new NamedSet(datasetId, namedSetDTO.getName(), namedSetDTO.getExpression(),
                            Utils.nullToEmptyStr(namedSetDTO.getFolder()),
                            namedSetDTO.getLocation(),
                            namedSetDTO.getVisible(), namedSetDTO.getInvisible(), namedSetDTO.getVisibleFlag(), namedSetDTO.getTranslation()));

        }

        return namedSetEntityList;
    }


    public void validateDimension(DatasetDTO datasetDTO, Response<DatasetIdVO> response) throws SemanticException {

        List<SemanticModelDTO> modelDTOs = datasetDTO.getModels();
        for (SemanticModelDTO modelDTO : modelDTOs) {
            List<DimensionTableDTO> dimensionTables = modelDTO.getDimensionTables();
            if (Utils.isCollectionEmpty(dimensionTables)) {
                continue;
            }

            Set<String> uniqueDimTable = new TreeSet<>();
            Set<String> uniqueDimTableAlias = new TreeSet<>();

            List<String> errorDimension = new ArrayList<>();
            for (DimensionTableDTO dimTable : dimensionTables) {
                if (!uniqueDimTable.add(dimTable.getName())) {
                    throw new SemanticException(Utils.formatStr("The dimension table name [%s] should be unique in the model, please correct it.", dimTable.getName()), ErrorCode.DIMENSION_NAME_NOT_UNIQUE);
                }

                if (!uniqueDimTableAlias.add(dimTable.getAlias())) {
                    throw new SemanticException(Utils.formatStr("The dimension table alias name [%s] should be unique in the model, please correct it.", dimTable.getAlias()), ErrorCode.DIMENSION_NAME_NOT_UNIQUE);
                }

                List<DimensionColDTO> dimCols = dimTable.getDimCols();
                if (Utils.isCollectionEmpty(dimCols)) {
                    continue;
                }

                Set<String> uniqueDimColNames = new TreeSet<>();
                Set<String> uniqueDimColAlias = new TreeSet<>();
                for (DimensionColDTO col : dimCols) {
                    uniqueDimColNames.add(col.getName());

                    if (!uniqueDimColAlias.add(col.getAlias())) {
                        errorDimension.add(col.getAlias());
                    }
                }

                List<HierarchyDTO> hierarchys = dimTable.getHierarchys();
                if (Utils.isCollectionEmpty(hierarchys)) {
                    continue;
                }

                for (HierarchyDTO hierarchy : hierarchys) {
                    for (String dimColReference : hierarchy.getDimCols()) {
                        if (!uniqueDimColNames.contains(dimColReference)) {
                            throw new SemanticException(Utils.formatStr("The column:[%s] in hierarchy:[%s] should be referred to the columns in its table, please correct it.", dimColReference, hierarchy.getName()), ErrorCode.MULTI_TABLES_IN_HIERARCHY);
                        }
                    }
                }
            }
            if (errorDimension.size() > 0) {
                response.setStatus((Response.Status.FAIL.ordinal()));
                DatasetIdVO datasetIdVO = response.getData();
                datasetIdVO.setDimension(errorDimension);
            }
        }
    }

    public static String collectCmParserErrorMessage(Throwable e) {
        if (e == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        while (e != null) {

            if (e instanceof MondrianException
                    && e.getMessage().contains(CM_PARSER_SYNTAX_ERROR1)) {
                e = e.getCause();
                continue;
            } else if (e instanceof MondrianException
                    && e.getMessage().contains(CM_PARSER_SYNTAX_ERROR2)) {
                sb.append(CM_PARSER_SYNTAX_ERROR2).append("\n");
            } else if (e instanceof TokenMgrError) {
                sb.append(CM_PARSER_SYNTAX_ERROR2).append("\n");
            } else {
                sb.append(e.getMessage()).append("\n");
            }

            e = e.getCause();
        }

        return sb.toString();
    }

    public void validateMeasure(DatasetDTO datasetDTO, Response<DatasetIdVO> response) throws SemanticException {
        List<SemanticModelDTO> modelDTOs = datasetDTO.getModels();
        Set<String> uniqueMeasures = new TreeSet<>();

        for (SemanticModelDTO modelDTO : modelDTOs) {
            List<MeasureDTO> measures = modelDTO.getMeasures();
            if (Utils.isCollectionEmpty(measures)) {
                continue;
            }

            for (MeasureDTO measure : measures) {
                if (!uniqueMeasures.add(measure.getAlias())) {
                    throw new SemanticException(Utils.formatStr("The measure alias name [%s] should be unique, please correct it.", measure.getName()), ErrorCode.MEASURE_NAME_NOT_UNIQUE);
                }
            }
        }

        List<CalculationMeasureDTO> calculateMeasures = datasetDTO.getCalculateMeasures();
        List<String> errorCM = new ArrayList<>();
        if (!Utils.isCollectionEmpty(calculateMeasures)) {
            for (CalculationMeasureDTO cmDTO : calculateMeasures) {
                if (!uniqueMeasures.add(cmDTO.getName())) {
                    throw new SemanticException(Utils.formatStr("The calculation measure name [%s] should be globally unique, please correct it.", cmDTO.getName()), ErrorCode.CALCULATED_MEASURE_NAME_NOT_UNIQUE);
                }
                if (cmDTO.getExpression().equals(CALCULATED_MEASURE_TEMPLATE.replace("xxx", cmDTO.getName()))) {
                    errorCM.add(cmDTO.getName());
                    continue;
                }

                try {
                    calcMemberParserImpl.parse(cmDTO.getExpression(), datasetDTO.makeSimpleSchema());
                } catch (Throwable e) {
                    if ("There are duplicate dimensions in tuple.".equals(e.getMessage())) {
                        errorCM.add(cmDTO.getName());
                    } else {
                        String errorMsg = collectCmParserErrorMessage(e);
                        log.error("Calculated Measure \"" + cmDTO.getName() + "\" is invalid, please fix it. Error:\n" + errorMsg);
                        throw new CalculateMeasureValidateException(cmDTO.getName(), errorMsg);
                    }
                }
            }
        }
        if (errorCM.size() > 0) {
            response.setStatus((Response.Status.FAIL.ordinal()));
            DatasetIdVO datasetIdVO = response.getData();
            datasetIdVO.setCm(errorCM);
        }

        List<NamedSetDTO> namedSetDTOs = datasetDTO.getNamedSets();
        List<String> errorNameSet = new ArrayList<>();
        if (!Utils.isCollectionEmpty(namedSetDTOs)) {
            HashSet<String> namedSetSet = new HashSet<>();
            for (NamedSetDTO namedSetDTO : namedSetDTOs) {
                if (!namedSetSet.add(namedSetDTO.getName())) {
                    throw new SemanticException(Utils.formatStr("The named set name [%s] should be unique, please correct it.", namedSetDTO.getName()), ErrorCode.NAMEDSET_NAME_NOT_UNIQUE);
                }
                if (namedSetDTO.getExpression().equals(NAME_SET_TEMPLATE.replace("xxx", namedSetDTO.getName()))) {
                    errorNameSet.add(namedSetDTO.getName());
                }
            }
        }
        if (errorNameSet.size() > 0) {
            response.setStatus((Response.Status.FAIL.ordinal()));
            DatasetIdVO datasetIdVO = response.getData();
            datasetIdVO.setNameSet(errorNameSet);
        }
    }


    public void insertSemanticModels(Integer datasetId, List<SemanticModelDTO> modelDTOS) throws SemanticException {
        List<CustomHierarchy> customHierarchyEntitys = new LinkedList<>();
        List<NamedDimCol> namedDimColEntitys = new LinkedList<>();
        List<NamedMeasure> namedMeasureEntitys = new LinkedList<>();
        List<NamedDimTable> namedDimTableEntitys = new LinkedList<>();

        for (SemanticModelDTO modelDTO : modelDTOS) {
            String modelName = modelDTO.getModelName();
            List<DimensionTableDTO> dimensionTableDTOS = modelDTO.getDimensionTables();

            if (!Utils.isCollectionEmpty(dimensionTableDTOS)) {
                for (DimensionTableDTO tableDTO : dimensionTableDTOS) {
                    DatasetRevision.accept(tableDTO);

                    String tableName = tableDTO.getName();
                    String tableType = tableDTO.getType();

                    namedDimTableEntitys.add(new NamedDimTable(
                            datasetId, modelName, tableName,
                            Utils.nullToEmptyStr(tableDTO.getAlias()),
                            Utils.nullToEmptyStr(tableType),
                            tableDTO.getActualTable(),
                            modelDTO.getFactTable(),
                            tableDTO.getTranslation()
                    ));

                    //collect named col
                    List<DimensionColDTO> dimColDTOs = tableDTO.getDimCols();
                    if (!Utils.isCollectionEmpty(dimColDTOs)) {
                        for (DimensionColDTO colDTO : dimColDTOs) {
                            if (colDTO.getSubfolder() != null && !colDTO.getSubfolder().matches(FOLDER_NAME_FORMAT)) {
                                throw new SemanticException(ERROR_FOLDER_NAME.replace("TYPE", "Dimension").replace("xxx", colDTO.getAlias()), ErrorCode.FOLDER_NAME_FORMAT_ERROR);
                            }
                            namedDimColEntitys.add(
                                    new NamedDimCol(
                                            datasetId, modelName, tableName,
                                            colDTO.getName(), Utils.nullToEmptyStr(colDTO.getAlias()),
                                            Utils.nullToDefaultInteger(colDTO.getType(), 0),
                                            colDTO.getDataType(), colDTO.getVisible(), colDTO.getInvisible(), colDTO.getDesc(),
                                            colDTO.getVisibleFlag(), colDTO.getNameColumn(), colDTO.getValueColumn(),
                                            colDTO.getProperties(), colDTO.getTranslation(), colDTO.getSubfolder(),
                                            colDTO.getDefaultMember()));
                        }
                    }

                    //collect custom hierarchy
                    List<HierarchyDTO> hierarchyDTOs = tableDTO.getHierarchys();
                    if (!Utils.isCollectionEmpty(hierarchyDTOs)) {
                        for (HierarchyDTO hierarchyDTO : hierarchyDTOs) {
                            for (int i = 0; i < hierarchyDTO.getDimCols().size(); i++) {
                                String dimColName = hierarchyDTO.getDimCols().get(i);
                                String weightColName = hierarchyDTO.getWeightCols() == null ? null : hierarchyDTO.getWeightCols().get(i);
                                customHierarchyEntitys.add(new CustomHierarchy(
                                        datasetId, modelName, tableName, hierarchyDTO.getName(),
                                        dimColName, weightColName,
                                        Utils.nullToEmptyStr(hierarchyDTO.getDesc()), hierarchyDTO.getTranslation()));
                            }
                        }
                    }
                }
            }

            //collect namedmeasure
            List<MeasureDTO> measureDTOs = modelDTO.getMeasures();
            if (!Utils.isCollectionEmpty(measureDTOs)) {
                for (MeasureDTO measureDTO : measureDTOs) {
                    if (StringUtils.isBlank(measureDTO.getName()) || StringUtils.isBlank(measureDTO.getAlias())) {
                        continue;
                    }
                    if (measureDTO.getSubfolder() != null && !measureDTO.getSubfolder().matches(FOLDER_NAME_FORMAT)) {
                        throw new SemanticException(ERROR_FOLDER_NAME.replace("TYPE", "measure").replace("xxx", measureDTO.getAlias()), ErrorCode.FOLDER_NAME_FORMAT_ERROR);
                    }
                    namedMeasureEntitys.add(
                            new NamedMeasure(datasetId, modelName, measureDTO.getName(), measureDTO.getAlias(),
                                    measureDTO.getExpression(), measureDTO.getFormat(), measureDTO.getFormatType(), measureDTO.getDataType(), measureDTO.getDimColumn(),
                                    measureDTO.getVisible(), measureDTO.getInvisible(), measureDTO.getDesc(), measureDTO.getVisibleFlag(), measureDTO.getTranslation(), measureDTO.getSubfolder()));
                }
            }
        }

        datasetService.insertNamedCols(namedDimColEntitys);
        datasetService.insertCustomHierarchies(customHierarchyEntitys);
        datasetService.insertNamedMeasure(namedMeasureEntitys);
        datasetService.insertNamedTable(namedDimTableEntitys);
    }


    public List<DimTableModelRel> convert2DimTableModelRelation(Integer datasetId, List<DimTableModelRelationDTO> dimTableModelRelationDTOs) throws SemanticException {
        List<DimTableModelRel> dimTableModelRelEntitys = new LinkedList<>();

        for (DimTableModelRelationDTO relDTO : dimTableModelRelationDTOs) {
            String modelName = relDTO.getModelName();

            List<DimTableModelRelationDTO.TableRelation> tableRelationDTOs = relDTO.getTableRelations();

            for (DimTableModelRelationDTO.TableRelation tableRelationDTO : tableRelationDTOs) {
                dimTableModelRelEntitys.add(
                        new DimTableModelRel(
                                datasetId, modelName,
                                tableRelationDTO.getTableName(), tableRelationDTO.getRelationType(),
                                Utils.nullToEmptyStr(tableRelationDTO.getRelationBridgeTableName()),
                                Utils.nullToEmptyStr(tableRelationDTO.getRelationFactKey())
                        )
                );
            }
        }

        return dimTableModelRelEntitys;
    }

    public List<CalculateMeasure> convert2CalculateMeasureEntities(Integer datasetId, List<CalculationMeasureDTO> calculateMeasureDTOs) throws SemanticException {
        List<CalculateMeasure> calculateMeasureEntities = new LinkedList<>();

        for (CalculationMeasureDTO cmDTO : calculateMeasureDTOs) {
            if (cmDTO.getSubfolder() != null && !cmDTO.getSubfolder().matches(FOLDER_NAME_FORMAT)) {
                throw new SemanticException(ERROR_FOLDER_NAME.replace("TYPE", "Calculation Measure").replace("xxx", cmDTO.getName()), ErrorCode.FOLDER_NAME_FORMAT_ERROR);
            }
            calculateMeasureEntities.add(new CalculateMeasure(datasetId, cmDTO.getName(), cmDTO.getExpression(),
                    cmDTO.getFormat(), cmDTO.getFormatType(), cmDTO.getFolder(), cmDTO.getVisible(), cmDTO.getInvisible(), cmDTO.getDesc(), cmDTO.getVisibleFlag(),
                    cmDTO.getTranslation(), cmDTO.getSubfolder(), cmDTO.getNonEmptyBehaviorMeasures()));

        }

        return calculateMeasureEntities;
    }

    public List<CommonDimRelation> convert2CommonDimRelation(Integer datasetId, List<CommonDimModelDTO> modelRelations) {
        if (Utils.isCollectionEmpty(modelRelations)) {
            return new ArrayList<>(0);
        }

        List<CommonDimRelation> commonDimEntitys = new ArrayList<>(4);

        for (CommonDimModelDTO commonDimDTO : modelRelations) {
            StringBuilder relationBuilder = new StringBuilder();

            if (StringUtils.isNotBlank(commonDimDTO.getModelRight())) {
                for (int i = 0; i < commonDimDTO.getRelation().size(); i++) {
                    CommonDimModelDTO.CommonDimRel rel = commonDimDTO.getRelation().get(i);

                    if (i != 0) {
                        relationBuilder.append(SemanticConstants.COMMA);
                    }

                    relationBuilder.append(rel.getLeft()).append(SemanticConstants.EQUAL).append(rel.getRight());
                }
            }

            commonDimEntitys.add(
                    new CommonDimRelation(
                            datasetId, commonDimDTO.getModelLeft(),
                            Utils.nullToEmptyStr(commonDimDTO.getModelRight()),
                            relationBuilder.toString())
            );

        }

        return commonDimEntitys;
    }


    public Response<DatasetIdVO> updateDataset(Integer datasetId, DatasetEntity datasetEntity, DatasetDTO datasetDTO, HttpServletRequest httpServletRequest) throws SemanticException, PwdDecryptException {

        try {
            if (validateDataset(datasetDTO, true).getStatus() == Response.Status.FAIL.ordinal()) {
                return validateDataset(datasetDTO, true);
            }
        } catch (CalculateMeasureValidateException e) {
            // handle exception and save the exception when insert dataset
        }

        datasetService.deleteOneDataset(datasetId);

        datasetDTO.setDatasetId(datasetId);
        String basicAuth = httpServletRequest.getHeader(SemanticConstants.BASIC_AUTH_HEADER_KEY);
        Response<DatasetIdVO> response;
        if (basicAuth != null) {
            response = doInsert(datasetDTO, basicAuth,
                    true, datasetEntity.getCreateTime(), datasetEntity.getCreateUser());
        } else {
            String user = authService.getCurrentUser();
            response = doInsert(datasetDTO, datasetService.getUserPwd(user),
                    true, datasetEntity.getCreateTime(), datasetEntity.getCreateUser());
        }
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public void checkConnectDataset(Integer datasetId, DatasetDTO originDataset, DatasetEntity datasetEntity, String project, String datasetName, HttpServletRequest httpServletRequest, String basicAuth) throws SemanticException, PwdDecryptException {

        if (!SemanticConfig.getInstance().isWhetherCheckDatasetConnect()) {
            return;
        }
        XmlaRequestContext context = new XmlaRequestContext();
        String url = SemanticConfig.getInstance().getDiscoverCatalogUrl(project);
        HttpEntity<String> requestEntity = getCheckDatasetHttpEntity(basicAuth, false);

        if (SemanticConfig.getInstance().isConvertorMock()) {
            context.clear();
            return;
        }

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
        try {
            if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
                throw new SemanticException("[CONNECT_MDX_ERROR]Please check the MDX server、user or password is normal.", ErrorCode.DATASET_CONNECT_CHECK_ERROR);
            }
            if (Optional.ofNullable(responseEntity.getBody()).orElse("").contains(ErrorCode.NO_AVAILABLE_DATASET.getCode())) {
                throw new SemanticException(ErrorCode.NO_AVAILABLE_DATASET, project);
            }
            calcMemberParserImpl.checkConnect(project, datasetName, authService.getCurrentUser());
        } catch (Exception e) {
            String errorMsg = collectCmParserErrorMessage(e);
            log.error(errorMsg);
            if (originDataset == null) {
                datasetService.deleteOneDataset(datasetId);
            } else {
                updateDataset(datasetId, datasetEntity, originDataset, httpServletRequest);
            }
            restTemplate.postForEntity(url, requestEntity, String.class);
            throw new SemanticException("[PARSE_ERROR]" + errorMsg, ErrorCode.DATASET_CONNECT_CHECK_ERROR);
        } finally {
            context.clear();
        }
    }

    public boolean isEmptyNameSet(String nameSet) {
        if (nameSet == null) {
            return true;
        }
        nameSet = nameSet.replaceAll("\\{", "").replaceAll("}", "").replaceAll(",", "");
        return nameSet.trim().isEmpty();
    }


    public static HttpEntity<String> getCheckDatasetHttpEntity(String basicAuth, boolean isHealthCheck) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Authorization", basicAuth);
        requestHeaders.add("Content-Type", "text/xml");
        requestHeaders.add("SOAPAction", "urn:schemas-microsoft-com:xml-analysis:Discover");
        requestHeaders.add("User-Agent", "MSOLAP 15.0 Client");
        if (isHealthCheck) {
            requestHeaders.add("Check-Type", "discover");
        }
        String requestBody = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "\t<soap:Header>\n" +
                "\t\t<Session xmlns=\"urn:schemas-microsoft-com:xml-analysis\" SessionId=\"no_session\" />\n" +
                "\t</soap:Header>\n" +
                "\t<soap:Body>\n" +
                "\t\t<Discover xmlns=\"urn:schemas-microsoft-com:xml-analysis\">\n" +
                "\t\t\t<RequestType>DISCOVER_PROPERTIES</RequestType>\n" +
                "\t\t\t<Restrictions>\n" +
                "\t\t\t\t<RestrictionList>\n" +
                "\t\t\t\t\t<PropertyName>Catalog</PropertyName>\n" +
                "\t\t\t\t</RestrictionList>\n" +
                "\t\t\t</Restrictions>\n" +
                "\t\t\t<Properties>\n" +
                "\t\t\t\t<PropertyList>\n" +
                "\t\t\t\t\t<DbpropMsmdOptimizeResponse>9</DbpropMsmdOptimizeResponse>\n" +
                "\t\t\t\t\t<DbpropMsmdActivityID>7592EAA7-C793-416F-96BF-0749BFB18C25</DbpropMsmdActivityID>\n" +
                "\t\t\t\t\t<DbpropMsmdRequestID>58A9982A-9561-4E46-904B-8F0AA4F6849D</DbpropMsmdRequestID>\n" +
                "\t\t\t\t\t<LocaleIdentifier>2052</LocaleIdentifier>\n" +
                "\t\t\t\t</PropertyList>\n" +
                "\t\t\t</Properties>\n" +
                "\t\t</Discover>\n" +
                "\t</soap:Body>\n" +
                "</soap:Envelope>";
        return new HttpEntity<>(requestBody, requestHeaders);
    }

    public DatasetDTO buildDatasetDTO(DatasetEntity datasetEntity) throws SemanticException {
        Integer datasetId = datasetEntity.getId();

        return new DatasetDTOBuilder(datasetEntity)
                .withCommonDimRelations(datasetService.selectCommonDimRelsByDatasetId(datasetId))
                .withModelDetails(
                        datasetEntity,
                        datasetService.selectDimTablesByDatasetId(datasetId),
                        datasetService.selectDimColsByDatasetId(datasetId),
                        datasetService.selectHierarchiesByDatasetId(datasetId),
                        datasetService.selectMeasuresByDatasetId(datasetId)
                )
                .calculateMeasures(datasetService.selectCMsByDatasetId(datasetId))
                .namedSets(datasetService.selectNamedSetsByDatasetId(datasetId))
                .dimTableModelRels(datasetService.selectDimTblModelRelsByDatasetId(datasetId))
                .build();
    }

    public List<DatasetContrastDTO> contrastDatasets(List<DatasetDTO> uploadDatasetDTOS, String projectName) {
        List<DatasetContrastDTO> datasetContrastDTOS = uploadDatasetDTOS.stream().map(datasetDTO -> {
            if(globleDatasetId.get() >= Integer.MAX_VALUE) {
                globleDatasetId.set(0);
            }
            String currentId = String.valueOf(globleDatasetId.getAndIncrement());
            String datasetName = datasetDTO.getDatasetName();
            DatasetEntity datasetEntityLocal = datasetService.getDataSetEntityByNameAndProjectName(projectName, datasetName);
            if (datasetEntityLocal != null) {
                DatasetDTO datasetDTOByLocal = buildDatasetDTO(datasetEntityLocal);
                importDatasetMap.put(currentId, Pair.of(datasetDTOByLocal, datasetDTO));
                return compareLocalDatasetAndUploadDataset(datasetDTOByLocal, datasetDTO, currentId);
            } else {
                importDatasetMap.put(currentId, Pair.of(null, datasetDTO));
                DatasetContrastDTO datasetContrastDTO = new DatasetContrastDTO();
                datasetContrastDTO.setId(currentId);
                datasetContrastDTO.setDataset(datasetDTO.getDatasetName());
                datasetContrastDTO.setExisted(false);
                List<DatasetItemDTO> brifeItems = setBrifeItems(datasetDTO);
                datasetContrastDTO.setBrifeItems(brifeItems);
                return datasetContrastDTO;
            }
        }).collect(Collectors.toList());
        return datasetContrastDTOS;
    }

    public List<DatasetItemDTO> compareDatasetItems(List<DatasetItemDTO> listA, List<DatasetItemDTO> listB) {
        List<DatasetItemDTO> result = new ArrayList(listA);
        result.removeAll(listB);
        return result;
    }

    public List<DatasetItemDTO> setBrifeItems(DatasetDTO datasetDTO) {
        List<DatasetItemDTO> uploadDatasetModels = collectModels(datasetDTO);
        List<DatasetItemDTO> uploadDatasetRelationship = collectRelationship(datasetDTO);
        uploadDatasetModels.addAll(uploadDatasetRelationship);
        return uploadDatasetModels;
    }


    public DatasetContrastDTO compareLocalDatasetAndUploadDataset(DatasetDTO datasetDTOByLocal, DatasetDTO datasetDTO, String currentId) {
        DatasetContrastDTO datasetContrastDTO = new DatasetContrastDTO();
        List<DatasetItemDTO> newItems = new ArrayList<>();
        List<DatasetItemDTO> reduceItems = new ArrayList<>();

        datasetContrastDTO.setId(currentId);
        datasetContrastDTO.setDataset(datasetDTO.getDatasetName());
        datasetContrastDTO.setExisted(true);


        List<DatasetItemDTO> brifeItems = setBrifeItems(datasetDTO);
        datasetContrastDTO.setBrifeItems(brifeItems);

        List<DatasetItemDTO> updateModels = collectModels(datasetDTO);
        List<DatasetItemDTO> updateRelationship = collectRelationship(datasetDTO);
        List<DatasetItemDTO> updateTables = collectTable(datasetDTO);
        List<DatasetItemDTO> updateCalculateMeasures = collectCalculateMeasures(datasetDTO);
        List<DatasetItemDTO> updateNamesets = collectNamesets(datasetDTO);
        List<DatasetItemDTO> updateDimColumns = collectDimCol(datasetDTO);
        List<DatasetItemDTO> updateHierarchy = collectHierarchy(datasetDTO);
        List<DatasetItemDTO> updateMeasure = collectMeasures(datasetDTO);


        List<DatasetItemDTO> localModels = collectModels(datasetDTOByLocal);
        List<DatasetItemDTO> localrelationship = collectRelationship(datasetDTOByLocal);
        List<DatasetItemDTO> localtables = collectTable(datasetDTOByLocal);
        List<DatasetItemDTO> localmeasures = collectCalculateMeasures(datasetDTOByLocal);
        List<DatasetItemDTO> localnamesets = collectNamesets(datasetDTOByLocal);
        List<DatasetItemDTO> localdimColumns = collectDimCol(datasetDTOByLocal);
        List<DatasetItemDTO> localhierarchy = collectHierarchy(datasetDTOByLocal);
        List<DatasetItemDTO> localMeasure = collectMeasures(datasetDTOByLocal);

        newItems.addAll(compareDatasetItems(updateModels, localModels));
        newItems.addAll(compareDatasetItems(updateRelationship, localrelationship));
        newItems.addAll(compareDatasetItems(updateTables, localtables));
        newItems.addAll(compareDatasetItems(updateCalculateMeasures, localmeasures));
        newItems.addAll(compareDatasetItems(updateNamesets, localnamesets));
        newItems.addAll(compareDatasetItems(updateDimColumns, localdimColumns));
        newItems.addAll(compareDatasetItems(updateHierarchy, localhierarchy));
        newItems.addAll(compareDatasetItems(updateMeasure, localMeasure));

        reduceItems.addAll(compareDatasetItems(localModels, updateModels));
        reduceItems.addAll(compareDatasetItems(localrelationship, updateRelationship));
        reduceItems.addAll(compareDatasetItems(localtables, updateTables));
        reduceItems.addAll(compareDatasetItems(localmeasures, updateCalculateMeasures));
        reduceItems.addAll(compareDatasetItems(localnamesets, updateNamesets));
        reduceItems.addAll(compareDatasetItems(localdimColumns, updateDimColumns));
        reduceItems.addAll(compareDatasetItems(localhierarchy, updateHierarchy));
        reduceItems.addAll(compareDatasetItems(localMeasure, updateMeasure));
        datasetContrastDTO.setNewItems(newItems);
        datasetContrastDTO.setReduceItems(reduceItems);

        List<DatasetItemDTO> updateItems = new ArrayList<>();
        if (CollectionUtils.isEqualCollection(datasetDTOByLocal.getDimTableModelRelations(), datasetDTO.getDimTableModelRelations())) {
            updateItems.add(new DatasetItemDTO("usage", "false"));
        } else {
            updateItems.add(new DatasetItemDTO("usage", "true"));
        }

        List<TranslationEntity> translationEntities = checkTranslation(datasetDTOByLocal);
        List<TranslationEntity> translationEntities1 = checkTranslation(datasetDTO);
        if (CollectionUtils.isEqualCollection(translationEntities,translationEntities1)) {
            updateItems.add(new DatasetItemDTO("translate", "false"));
        } else {
            updateItems.add(new DatasetItemDTO("translate", "true"));
        }
        datasetContrastDTO.setUpdateItems(updateItems);
        return datasetContrastDTO;
    }

    public List<DatasetItemDTO> collectRelationship(DatasetDTO datasetDTO) {
        return datasetDTO.getModelRelations().stream().flatMap(
                commonDimModel -> commonDimModel.getRelation().stream().map(dimTable -> {
                    if (commonDimModel.getModelRight().isEmpty()) {
                        return new DatasetItemDTO("relationship", commonDimModel.getModelLeft() + "." + dimTable.getLeft());
                    } else {
                        return new DatasetItemDTO("relationship", commonDimModel.getModelLeft() + "." + dimTable.getLeft() + "-" + commonDimModel.getModelRight() + "." + dimTable.getRight());
                    }
                })).collect(Collectors.toList());
    }

    public List<DatasetItemDTO> collectModels(DatasetDTO datasetDTO) {
        return datasetDTO.getModels().stream()
                .map(model -> new DatasetItemDTO("model", model.getModelName())).collect(Collectors.toList());
    }

    public List<TranslationEntity> checkTranslation(DatasetDTO datasetDTO) {
        return datasetDTO.getModels().stream().flatMap(va -> va.getDimensionTables().stream().flatMap(value -> value.getDimCols().stream().map(DimensionColDTO::getTranslation))).collect(Collectors.toList());
    }

    public List<DatasetItemDTO> collectTable(DatasetDTO datasetDTO) {
        return datasetDTO.getModels().stream().flatMap(model ->
                model.getDimensionTables().stream().map(table -> new DatasetItemDTO("table", model.getModelName() + "." + table.getAlias()))).collect(Collectors.toList());
    }

    public List<DatasetItemDTO> collectNamesets(DatasetDTO datasetDTO) {
        return datasetDTO.getNamedSets().stream().map(value -> new DatasetItemDTO("namedSet", value.getLocation() + "." + value.getName())).collect(Collectors.toList());
    }

    public List<DatasetItemDTO> collectCalculateMeasures(DatasetDTO datasetDTO) {
        return datasetDTO.getCalculateMeasures().stream().map(value -> new DatasetItemDTO("calculateMeasure", value.getFolder() + "." + value.getName())).collect(Collectors.toList());
    }

    public List<DatasetItemDTO> collectMeasures(DatasetDTO datasetDTO) {
        return datasetDTO.getModels().stream().flatMap(model -> model.getMeasures().stream().map(measureDTO -> new DatasetItemDTO("measure", model.getModelName() + "." + measureDTO.getAlias()))).collect(Collectors.toList());
    }

    public List<DatasetItemDTO> collectDimCol(DatasetDTO datasetDTO) {
        return datasetDTO.getModels().stream().flatMap(model -> model.getDimensionTables().stream().flatMap(value ->
                value.getDimCols().stream().map(t -> new DatasetItemDTO("dimColumn", model.getModelName() + "." + value.getAlias() + "." + t.getAlias())))).collect(Collectors.toList());
    }

    public List<DatasetItemDTO> collectHierarchy(DatasetDTO datasetDTO) {
        return datasetDTO.getModels().stream().flatMap(model -> model.getDimensionTables().stream().flatMap(value -> value.getHierarchys().stream().map(t -> new DatasetItemDTO("hierarchy", model.getModelName() + "." + value.getAlias() + "." + t.getName())))).collect(Collectors.toList());
    }

    public void uploadFilesParser(List<DatasetDTO> datasetDTOS, MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("upload", null);
        file.transferTo(tempFile);
        try (ZipFile zipFile = new ZipFile(tempFile)) {
            int dirNum = 0;
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    if (dirNum > 1) {
                        throw new SemanticException("datasets structure errors.", ErrorCode.ZIP_PACKAGE_ERROR);
                    }
                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
                        String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                        JSON.parse(result);
                        DatasetDTO datasetDTO = JacksonSerDeUtils.readString(result, DatasetDTO.class);
                        datasetDTOS.add(datasetDTO);
                    }
                } else {
                    dirNum++;
                }
            }
        } catch (Exception e) {
            throw new SemanticException("dataset package parse error.", ErrorCode.ZIP_PACKAGE_ERROR);
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public void importDatasetToInternal(DatasetImportResponseDTO datasetImportResponse, DatasetImportRequestDTO datasetImportRequest, String finalBasicAuth) {
        List<DatasetImportDetailsDTO> successInfo = new ArrayList<>();
        List<DatasetImportDetailsDTO> failedInfo = new ArrayList<>();
        datasetImportRequest.getDatasets().forEach(datasetImportInfos -> {
            Pair<DatasetDTO, DatasetDTO> datasets = importDatasetMap.get(datasetImportInfos.getId());
            DatasetDTO datasetDTO = datasets.getRight();
            datasetDTO.setDatasetName(datasetImportInfos.getName());
            datasetDTO.setProject(datasetImportRequest.getProjectName());

            if (Objects.equals(datasetImportInfos.getType(), "ADD_NEW")) {
                if (!datasetImportInfos.isAcl()) {
                    List<VisibleAttr> visibleAttrs = new ArrayList<>();
                    visibleAttrs.add(new VisibleAttr("role", "Admin"));
                    visibleAttrs.add(new VisibleAttr("user", authService.getCurrentUser()));
                    datasetDTO.getModels().forEach(model -> model.getDimensionTables().forEach(
                            value -> value.getDimCols().forEach(
                                    result -> {
                                        result.setVisible(visibleAttrs);
                                    }
                            )));
                    datasetDTO.getModels().forEach(model -> model.getMeasures().forEach(result -> {
                        result.setVisible(visibleAttrs);
                    }));
                    datasetDTO.getCalculateMeasures().forEach(result -> {
                        result.setVisible(visibleAttrs);
                    });
                    datasetDTO.getNamedSets().forEach(result -> {
                        result.setVisible(visibleAttrs);
                    });
                }
                try {
                    datasetDTO.setDatasetId(null);
                    doInsert(datasetDTO, finalBasicAuth, false, Utils.currentTimeStamp(),
                            authService.getCurrentUser());
                    successInfo.add(new DatasetImportDetailsDTO(datasetImportInfos.getId(), datasetDTO.getDatasetName()));
                } catch (Exception e) {
                    failedInfo.add(new DatasetImportDetailsDTO(datasetImportInfos.getId(), datasetDTO.getDatasetName()));
                }

            } else if (Objects.equals(datasetImportInfos.getType(), "OVERRIDE")) {
                DatasetDTO datasetDTOLocal = datasets.getLeft();
                try {
                    datasetService.deleteOneDataset(datasetDTOLocal.getDatasetId());
                    if (!datasetImportInfos.isAcl()) {
                        HashMap<String, List<VisibleAttr>> visibleMap = new HashMap<>();
                        HashMap<String, List<VisibleAttr>> invisibleMap = new HashMap<>();
                        HashMap<String, Boolean> visibleMapFlag = new HashMap<>();

                        datasetDTOLocal.getModels().forEach(model -> model.getDimensionTables().forEach(
                                value -> value.getDimCols().forEach(
                                        result -> {
                                            visibleMap.put(result.getName(), result.getVisible());
                                            invisibleMap.put(result.getName(), result.getInvisible());
                                            visibleMapFlag.put(result.getName(), result.getVisibleFlag());
                                        }
                                )));
                        datasetDTOLocal.getModels().forEach(model -> model.getMeasures().forEach(result -> {
                            visibleMap.put(result.getName(), result.getVisible());
                            invisibleMap.put(result.getName(), result.getInvisible());
                            visibleMapFlag.put(result.getName(), result.getVisibleFlag());
                        }));
                        datasetDTOLocal.getCalculateMeasures().forEach(result -> {
                            visibleMap.put(result.getName(), result.getVisible());
                            invisibleMap.put(result.getName(), result.getInvisible());
                            visibleMapFlag.put(result.getName(), result.getVisibleFlag());
                        });
                        datasetDTOLocal.getNamedSets().forEach(result -> {
                            visibleMap.put(result.getName(), result.getVisible());
                            invisibleMap.put(result.getName(), result.getInvisible());
                            visibleMapFlag.put(result.getName(), result.getVisibleFlag());
                        });

                        datasetDTO.getCalculateMeasures().forEach(result -> {
                            result.setVisible(visibleMap.getOrDefault(result.getName(), null));
                            result.setInvisible(invisibleMap.getOrDefault(result.getName(), null));
                            result.setVisibleFlag(visibleMapFlag.getOrDefault(result.getName(), !result.getVisibleFlag()));
                        });
                        datasetDTO.getNamedSets().forEach(result -> {
                            result.setVisible(visibleMap.getOrDefault(result.getName(), null));
                            result.setInvisible(invisibleMap.getOrDefault(result.getName(), null));
                            result.setVisibleFlag(visibleMapFlag.getOrDefault(result.getName(), !result.getVisibleFlag()));
                        });
                        datasetDTO.getModels().forEach(model -> model.getMeasures().forEach(result -> {
                            result.setVisible(visibleMap.getOrDefault(result.getName(), null));
                            result.setInvisible(invisibleMap.getOrDefault(result.getName(), null));
                            result.setVisibleFlag(visibleMapFlag.getOrDefault(result.getName(), !result.getVisibleFlag()));
                        }));

                        datasetDTO.getModels().forEach(model -> model.getDimensionTables().forEach(
                                value -> value.getDimCols().forEach(
                                        result -> {
                                            result.setVisible(visibleMap.getOrDefault(result.getName(), null));
                                            result.setInvisible(invisibleMap.getOrDefault(result.getName(), null));
                                            result.setVisibleFlag(visibleMapFlag.getOrDefault(result.getName(), !result.getVisibleFlag()));
                                        }
                                )));
                    }
                    datasetDTO.setDatasetId(datasetDTOLocal.getDatasetId());
                    doInsert(datasetDTO, finalBasicAuth, true, Utils.currentTimeStamp(),
                            authService.getCurrentUser());
                    successInfo.add(new DatasetImportDetailsDTO(datasetImportInfos.getId(), datasetDTO.getDatasetName()));
                } catch (Exception e) {
                    failedInfo.add(new DatasetImportDetailsDTO(datasetImportInfos.getId(), datasetDTO.getDatasetName()));
                }
            }
        });
        datasetImportResponse.setSuccess(successInfo);
        datasetImportResponse.setFailed(failedInfo);
    }
}
