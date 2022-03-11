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


package io.kylin.mdx.insight.server.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.JacksonSerDeUtils;
import io.kylin.mdx.insight.common.util.NetworkUtils;
import io.kylin.mdx.insight.core.entity.KylinPermission;
import io.kylin.mdx.insight.core.service.AuthService;
import org.apache.commons.lang3.StringUtils;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.meta.SemanticAdapter;
import io.kylin.mdx.insight.core.service.BrokenService;
import io.kylin.mdx.insight.core.manager.ProjectManager;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.core.model.generic.KylinGenericModel;
import io.kylin.mdx.insight.core.model.semantic.DatasetStatus;
import io.kylin.mdx.insight.core.model.semantic.SemanticProject;
import io.kylin.mdx.insight.core.service.DatasetService;
import io.kylin.mdx.insight.core.service.ModelService;
import io.kylin.mdx.insight.core.service.SemanticContext;
import io.kylin.mdx.insight.core.sync.*;
import io.kylin.mdx.insight.core.sync.DatasetEventObject.DatasetChangedSource;
import io.kylin.mdx.insight.core.sync.DatasetEventObject.DatasetEventType;
import io.kylin.mdx.insight.core.sync.DatasetValidateResult.DatasetValidateType;
import io.kylin.mdx.insight.engine.bean.SimpleSchema;
import io.kylin.mdx.insight.engine.service.parser.CalcMemberParserImpl;
import io.kylin.mdx.insight.engine.service.parser.DefaultMemberValidatorImpl;
import io.kylin.mdx.insight.engine.service.parser.NamedSetParserImpl;
import io.kylin.mdx.insight.engine.manager.SyncManager;
import io.kylin.mdx.insight.server.bean.Page;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.bean.dto.*;
import io.kylin.mdx.insight.server.bean.dto.DatasetBrokenInfoDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetBrokenResponseDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetContrastDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetImportDetailsResponseDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetImportRequestDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetImportResponseDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetPackageResponseDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetRequestDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetWithBrokenDTO;
import io.kylin.mdx.insight.server.bean.dto.DefaultMemberValidateDTO;
import io.kylin.mdx.insight.server.bean.dto.FormatSampleDTO;
import io.kylin.mdx.insight.server.bean.dto.MdxExprValidationDTO;
import io.kylin.mdx.insight.server.bean.dto.ProjectDatasetDTO;
import io.kylin.mdx.insight.server.bean.dto.SimpleDatasetDTO;
import io.kylin.mdx.insight.server.bean.vo.DatasetIdVO;
import io.kylin.mdx.insight.server.service.BatchDatasetService;
import io.kylin.mdx.insight.server.support.Permission;
import io.kylin.mdx.ErrorCode;
import io.kylin.mdx.core.MdxConfig;
import lombok.extern.slf4j.Slf4j;
import mondrian.util.Format;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Slf4j
@Validated
@RestController
@RequestMapping("api")
public class DatasetController {

    private final DatasetService datasetService;

    private final ProjectManager projectManager;

    private final BrokenService brokenService;

    private final CalcMemberParserImpl calcMemberParserImpl;

    private final NamedSetParserImpl namedSetParserImpl;

    private final AuthService authService;

    private final DefaultMemberValidatorImpl defaultMemberValidatorImpl;

    public static final Map<String, List<DatasetDTO>> exportMap = new HashMap<>();

    public static final Set<String> importToken = new HashSet<>();

    private static final MdxConfig MDX_CONFIG = MdxConfig.getInstance();

    @Autowired
    private SemanticContext semanticContext;

    @Autowired
    private BatchDatasetService batchDatasetService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private SyncManager syncManager;


    @Autowired
    public DatasetController(DatasetService datasetService, ProjectManager projectManager,
                             BrokenService brokenService, CalcMemberParserImpl calcMemberParserImpl,
                             NamedSetParserImpl namedSetParserImpl, AuthService authService,
                             DefaultMemberValidatorImpl defaultMemberValidatorImpl) {
        this.datasetService = datasetService;
        this.projectManager = projectManager;
        this.brokenService = brokenService;
        this.calcMemberParserImpl = calcMemberParserImpl;
        this.namedSetParserImpl = namedSetParserImpl;
        this.authService = authService;
        this.defaultMemberValidatorImpl = defaultMemberValidatorImpl;
    }

    public static final String NAME_SET_CAN_NOT_BE_EMPTY = "NAME_SET_CAN_NOT_BE_EMPTY";


    /**
     * 查询所有项目名
     */
    @GetMapping("projects")
    @Permission
    public Response<Set<String>> getAllProjects(HttpServletRequest httpServletRequest) throws SemanticException, PwdDecryptException {
        log.info("user:{} enter API:GET [/api/projects]", authService.getCurrentUser());
        String basicAuth = httpServletRequest.getHeader(SemanticConstants.BASIC_AUTH_HEADER_KEY);
        if (basicAuth != null) {
            return new Response<Set<String>>(Response.Status.SUCCESS)
                    .data(projectManager.getActualProjectSet(new ConnectionInfo(basicAuth)));
        }
        String user = authService.getCurrentUser();
        return new Response<Set<String>>(Response.Status.SUCCESS)
                .data(projectManager.getActualProjectSet(new ConnectionInfo(datasetService.getUserPwd(user))));
    }

    /**
     * 查询数据集列表(分页)
     */
    @GetMapping("datasets")
    @Permission
    public Response<Page<SimpleDatasetDTO>> getDatasetsByPage(@RequestParam("pageNum") @Min(0) @Max(1000) Integer pageNum,
                                                              @RequestParam("pageSize") @NotNull Integer pageSize,
                                                              @RequestParam(value = "projectName", required = false) String projectName,
                                                              @RequestParam(value = "datasetName", required = false) String datasetName,
                                                              @RequestParam(value = "orderBy", required = false) String orderBy,
                                                              @RequestParam(value = "direction", required = false) String direction) {
        log.info("user:{} enter API:GET [/api/datasets], pageNum:{}, pageSize:{}, orderBy:{}, direction:{}",
                authService.getCurrentUser(), pageNum, pageSize, orderBy, direction);

        if (datasetName != null) {
            datasetName = datasetName.replace("\\\\", "\\\\\\\\");
            datasetName = datasetName.replace("_", "\\_");
            datasetName = datasetName.replace("%", "\\\\%");
        }
        List<String> orderByClause = new ArrayList<>();
        if (orderBy == null) {
            orderByClause.add("create_time desc");
        } else {
            String[] orderList = orderBy.split(",");
            String[] directionList = direction.split(",");
            if (orderList.length != directionList.length) {
                throw new SemanticException("dataset can not match orderBy to direction.", ErrorCode.DATASET_ORDER_ERROR);
            }
            Iterator<String> orders = Arrays.stream(orderList).iterator();
            Iterator<String> directions = Arrays.stream(directionList).iterator();
            while (orders.hasNext() && directions.hasNext()) {
                orderByClause.add(orders.next() + " " + directions.next());
            }
        }

        List<DatasetEntity> datasetEntities = datasetService.getDataSetEntitysByPage(projectName, datasetName, pageNum + 1, pageSize, String.join(", ", orderByClause));
        List<SimpleDatasetDTO> simpleDatasets = datasetEntities.stream().map(SimpleDatasetDTO::new).collect(Collectors.toList());

        Page<SimpleDatasetDTO> simpleDatasetPage = new Page<>(simpleDatasets);
        simpleDatasetPage.setPageInfo(datasetEntities);

        return new Response<Page<SimpleDatasetDTO>>(Response.Status.SUCCESS)
                .data(simpleDatasetPage);
    }

    /**
     * 返回符合要求不可用的数据集
     */
    @PostMapping("datasets/validation/broken")
    @Permission
    public Response<DatasetBrokenResponseDTO> getAllBrokenDatasets(@RequestBody @Validated DatasetRequestDTO datasetBrokenRequestDTO) {
        log.info("user:{} enter API:POST [/api/datasets/validation/broken]", authService.getCurrentUser());
        DatasetBrokenResponseDTO datasetBrokenResponseDTO = new DatasetBrokenResponseDTO();
        List<DatasetEntity> datasetEntities = datasetService.getDataSetEntitiesBySelectAll(
                datasetBrokenRequestDTO.getProjectName(),
                datasetBrokenRequestDTO.getSelectAll(),
                datasetBrokenRequestDTO.getExcludes(),
                datasetBrokenRequestDTO.getIncludes(),
                datasetBrokenRequestDTO.getSearchName(),
                true);
        List<DatasetBrokenInfoDTO> datasetBrokenInfoDTOS = datasetEntities.stream().map(
                        datasetEntity -> new DatasetBrokenInfoDTO(datasetEntity.getId().toString(), datasetEntity.getDataset()))
                .collect(Collectors.toList());
        datasetBrokenResponseDTO.setBroken(datasetBrokenInfoDTOS);
        return new Response<DatasetBrokenResponseDTO>(Response.Status.SUCCESS).data(datasetBrokenResponseDTO);
    }

    /**
     * 返回打包数据集信息
     */
    @PostMapping("datasets/package")
    @Permission
    public Response<DatasetPackageResponseDTO> exportDatasetZipPackage(@RequestBody @Validated DatasetRequestDTO datasetRequestDTO, HttpServletResponse response) {
        log.info("user:{} enter API:POST [/api/datasets/package]", authService.getCurrentUser());
        DatasetPackageResponseDTO datasetPackageResponseDTO = new DatasetPackageResponseDTO();
        List<DatasetEntity> datasetEntities = datasetService.getDataSetEntitiesBySelectAll(
                datasetRequestDTO.getProjectName(),
                datasetRequestDTO.getSelectAll(),
                datasetRequestDTO.getExcludes(),
                datasetRequestDTO.getIncludes(),
                datasetRequestDTO.getSearchName(),
                false);
        if (datasetEntities.isEmpty()) {
            throw new SemanticException("The dataset required doesn't exist.", ErrorCode.DATASET_NOT_EXISTS);
        }
        List<DatasetDTO> datasetDTOS = datasetEntities.stream().map(datasetEntity -> batchDatasetService.buildDatasetDTO(datasetEntity)).collect(Collectors.toList());
        AtomicLong filesSize = new AtomicLong();
        datasetDTOS.forEach(datasetDTO -> {
                byte buf[] = JacksonSerDeUtils.writeJsonAsByte(datasetDTO);
                filesSize.addAndGet(buf.length);
        });
        String token = String.valueOf(System.currentTimeMillis());
        exportMap.put(token, datasetDTOS);
        datasetPackageResponseDTO.setToken(token);
        datasetPackageResponseDTO.setSize(filesSize.get());
        String mdxHost = MDX_CONFIG.getMdxHost();
        if (StringUtils.isNotBlank(mdxHost)) {
            if (SemanticConstants.LOCAL_IP.equals(mdxHost) || SemanticConstants.LOCAL_HOST.equals(mdxHost)) {
                mdxHost = NetworkUtils.getLocalIP();
            }
        }
        response.setHeader("Mdx-Execute-Node", mdxHost + ":" + MDX_CONFIG.getMdxPort());
        return new Response<DatasetPackageResponseDTO>(Response.Status.SUCCESS).data(datasetPackageResponseDTO);
    }

    /**
     * 下载数据集zip包
     *
     * @return
     */
    @GetMapping("datasets/package/{token}")
    @Permission
    public ResponseEntity downloadDatasetsZipPackage(@PathVariable("token") String token,
                                                     @RequestParam("projectName") String projectName,
                                                     HttpServletResponse response) {
        log.info("user:{} enter API:GET [/api/dataset/package/{}", authService.getCurrentUser(), token);
        if (!exportMap.containsKey(token) || exportMap.get(token).isEmpty() || convertDay(Long.parseLong(token), System.currentTimeMillis()) > 1) {
            throw new SemanticException("The dataset required doesn't exist or timeout.", ErrorCode.DATASET_NOT_EXISTS);
        }
        List<DatasetDTO> datasetDTOS = exportMap.get(token);
        exportMap.remove(token);
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String zipName = "MDX_" + datasetDTOS.get(0).getProject() + "_" + timeStamp + ".zip";
        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(zipName, "UTF-8") + "\"");
            datasetDTOS.forEach(
                    datasetDTO -> {
                        String jsonName = datasetDTO.getProject() + "_" + datasetDTO.getDatasetName() + "_" + timeStamp + ".json";
                        try {
                            InputStream fileInputStream = new ByteArrayInputStream(JacksonSerDeUtils.writeJsonAsByte(datasetDTO));
                            ZipEntry zipEntry = new ZipEntry(jsonName);
                            zipOut.putNextEntry(zipEntry);
                            IOUtils.copy(fileInputStream, zipOut);
                        } catch (Exception e) {
                            log.error("{} file error happened.", jsonName);
                        }
                    }
            );
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            log.error("{} package error happened.", zipName);
            throw new SemanticException("package error happened.", ErrorCode.ZIP_PACKAGE_ERROR);
        }
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * 创建zip包中数据集的diff信息
     */
    @PostMapping("datasets")
    @Permission
    public Response<DatasetImportDetailsResponseDTO> importDatasetInfo(@RequestParam("type") String type,
                                                                       @RequestParam("projectName") String projectName,
                                                                       @RequestPart MultipartFile file,
                                                                       HttpServletResponse response) throws IOException {
        log.info("user:{} enter API:POST [/api/dataset?type={}", authService.getCurrentUser(), type);
        if (!type.equals("import")) {
            throw new SemanticException("datasets import type not found.", ErrorCode.IMPORT_TYPE_NOT_FOUNT);
        }
        List<DatasetDTO> uploadDatasetDTOS = new ArrayList<>();
        String contentType = file.getContentType();
        if (contentType != null && contentType.contains("json")) {
            DatasetDTO datasetDTO = JacksonSerDeUtils.readInputStream(file.getInputStream(), DatasetDTO.class);
            uploadDatasetDTOS.add(datasetDTO);
        }
        if (contentType != null && contentType.contains("zip")) {
            batchDatasetService.uploadFilesParser(uploadDatasetDTOS, file);
        }
        if (uploadDatasetDTOS.isEmpty()) {
            throw new SemanticException("The dataset required doesn't exist.", ErrorCode.DATASET_NOT_EXISTS);
        }
        DatasetImportDetailsResponseDTO datasetImportDetailsResponse = new DatasetImportDetailsResponseDTO();
        List<DatasetContrastDTO> datasetContrastDTOS = batchDatasetService.contrastDatasets(uploadDatasetDTOS, projectName);
        String token = String.valueOf(System.currentTimeMillis());
        importToken.add(token);
        datasetImportDetailsResponse.setToken(token);
        datasetImportDetailsResponse.setDataDiffList(datasetContrastDTOS);
        String mdxHost = MDX_CONFIG.getMdxHost();
        if (StringUtils.isNotBlank(mdxHost)) {
            if (SemanticConstants.LOCAL_IP.equals(mdxHost) || SemanticConstants.LOCAL_HOST.equals(mdxHost)) {
                mdxHost = NetworkUtils.getLocalIP();
            }
        }
        response.setHeader("Mdx-Execute-Node", mdxHost + ":" + MDX_CONFIG.getMdxPort());
        return new Response<DatasetImportDetailsResponseDTO>(Response.Status.SUCCESS).data(datasetImportDetailsResponse);
    }


    /**
     * 创建zip包中数据集
     */
    @PutMapping("datasets")
    @Permission
    public Response<DatasetImportResponseDTO> importDataset(@RequestParam("type") String type,
                                                            @RequestBody @Validated DatasetImportRequestDTO datasetImportRequest,
                                                            HttpServletRequest httpServletRequest) throws PwdDecryptException {
        log.info("user:{} enter API:PUT [/api/dataset?type={}", authService.getCurrentUser(), type);
        if (!type.equals("import")) {
            throw new SemanticException("datasets import type not found.", ErrorCode.IMPORT_TYPE_NOT_FOUNT);
        }
        String token = datasetImportRequest.getToken();
        if (!importToken.contains(token) || convertDay(Long.parseLong(token), System.currentTimeMillis()) > 1) {
            throw new SemanticException("The dataset required doesn't exist or timeout.", ErrorCode.DATASET_NOT_EXISTS);
        }
        importToken.remove(token);
        String basicAuth = httpServletRequest.getHeader(SemanticConstants.BASIC_AUTH_HEADER_KEY);
        if (basicAuth == null) {
            String user = authService.getCurrentUser();
            basicAuth = datasetService.getUserPwd(user);
        }
        DatasetImportResponseDTO datasetImportResponse = new DatasetImportResponseDTO();
        batchDatasetService.importDatasetToInternal(datasetImportResponse, datasetImportRequest, basicAuth);
        return new Response<DatasetImportResponseDTO>(Response.Status.SUCCESS)
                .data(datasetImportResponse);
    }

    /**
     * 查询是否存在某个数据集
     */
    @GetMapping("/dataset/{project}/{dataset}/{type}")
    @Permission
    public Response<Boolean> existDataset(@PathVariable("project") String project,
                                          @PathVariable("dataset") String dataset,
                                          @PathVariable("type") String type) {
        log.info("user:{} enter API:GET [/api/dataset/{}/{}/{}", authService.getCurrentUser(), project, dataset, type);
        DatasetEntity datasetEntity = datasetService.getDatasetEntity(project, dataset);
        return new Response<Boolean>(Response.Status.SUCCESS)
                .data(datasetEntity == null ? Boolean.FALSE : Boolean.TRUE);
    }

    /**
     * 删除一个dataset
     */
    @DeleteMapping("dataset/{datasetId}")
    @Permission
    public Response<String> deleteOneDataset(@PathVariable("datasetId") @NotNull Integer datasetId, HttpServletRequest httpServletRequest) throws SemanticException {
        log.info("user:{} enter API:DELETE [/api/dataset/{}", authService.getCurrentUser(), datasetId);
        DatasetEntity datasetEntity = datasetService.selectDatasetById(datasetId);
        if (datasetEntity == null) {
            return new Response<String>(Response.Status.SUCCESS)
                    .data(Utils.formatStr(SemanticConstants.DATASET_NOT_FOUND, datasetId));
        }

        if (!checkPermission(httpServletRequest, datasetEntity.getProject())) {
            throw new SemanticException(SemanticConstants.ACCESS_DENIED, ErrorCode.ACCESS_DENIED);
        }

        log.info("Deleting dataset, id:{}, dataset entity content: {}", datasetId, JSON.toJSONString(datasetEntity));

        datasetService.deleteOneDataset(datasetId);

        syncManager.asyncNotify(
                new DatasetEventObject(
                        new DatasetChangedSource(datasetEntity.getProject(), datasetEntity.getDataset()),
                        DatasetEventType.DATASET_DELETED)
        );

        return new Response<String>(Response.Status.SUCCESS)
                .data(SemanticConstants.RESP_SUC);
    }

    /**
     * 更新一个dataset
     */
    @PutMapping("dataset/{datasetId}")
    @Permission
    public Response<DatasetIdVO> updateOneDataset(HttpServletRequest httpServletRequest,
                                                  @PathVariable("datasetId") @NotNull Integer datasetId,
                                                  @RequestParam(value = "update_type", required = false, defaultValue = "") String updateType,
                                                  @RequestBody @Validated DatasetDTO datasetDTO) throws SemanticException, PwdDecryptException {
        log.info("user:{} enter API:PUT [/api/dataset/{}], datasetDTO:{}", authService.getCurrentUser(), datasetId, JSON.toJSONString(datasetDTO));

        if (!checkPermission(httpServletRequest, datasetDTO.getProject())) {
            throw new SemanticException(SemanticConstants.ACCESS_DENIED, ErrorCode.ACCESS_DENIED);
        }
        DatasetEntity datasetEntity = datasetService.selectDatasetById(datasetId);
        if (datasetEntity == null) {
            return new Response<DatasetIdVO>(Response.Status.FAIL)
                    .errorMsg(Utils.formatStr(SemanticConstants.DATASET_NOT_FOUND, datasetId));
        }
        boolean checkDataset = !"acl".equals(updateType);
        return updateDataset0(httpServletRequest, datasetId, datasetDTO, datasetEntity, checkDataset);
    }

    /**
     * 更新一个dataset
     */
    @PutMapping("dataset")
    @Permission
    public Response<DatasetIdVO> updateDataset(
            HttpServletRequest httpServletRequest,
            @RequestParam(value = "update_type", required = false, defaultValue = "") String updateType,
            @RequestBody @Validated DatasetDTO datasetDTO) throws SemanticException, PwdDecryptException {
        log.info("user:{} enter API:PUT [/api/dataset], datasetDTO:{}", authService.getCurrentUser(), JSON.toJSONString(datasetDTO));

        if (!checkPermission(httpServletRequest, datasetDTO.getProject())) {
            throw new SemanticException(SemanticConstants.ACCESS_DENIED, ErrorCode.ACCESS_DENIED);
        }
        DatasetEntity datasetEntity = datasetService.getDatasetEntity(datasetDTO.getProject(), datasetDTO.getDatasetName());
        if (datasetEntity == null) {
            return new Response<DatasetIdVO>(Response.Status.FAIL)
                    .errorMsg(Utils.formatStr(SemanticConstants.DATASET_NAME_NOT_FOUND, datasetDTO.getDatasetName()));
        }
        boolean checkDataset = !"acl".equals(updateType);
        return updateDataset0(httpServletRequest, datasetEntity.getId(), datasetDTO, datasetEntity, checkDataset);
    }

    private Response<DatasetIdVO> updateDataset0(HttpServletRequest httpServletRequest,
                                                 Integer datasetId,
                                                 DatasetDTO datasetDTO,
                                                 DatasetEntity datasetEntity,
                                                 boolean checkDataset) throws PwdDecryptException {
        DatasetDTO originDataset = batchDatasetService.buildDatasetDTO(datasetEntity);

        Response<DatasetIdVO> response = batchDatasetService.updateDataset(datasetId, datasetEntity, datasetDTO, httpServletRequest);

        String basicAuth = httpServletRequest.getHeader(SemanticConstants.BASIC_AUTH_HEADER_KEY);
        if (basicAuth == null) {
            String user = authService.getCurrentUser();
            basicAuth = datasetService.getUserPwd(user);
        }

        if (checkDataset && Response.Status.SUCCESS.ordinal() == (response.getStatus())) {
            batchDatasetService.checkConnectDataset(response.getData().getDatasetId(), originDataset, datasetEntity,
                    datasetDTO.getProject(), datasetDTO.getDatasetName(), httpServletRequest, basicAuth);
        }

        // if dataset is updated, it should be verified whether dataset turn to be normal or broken
        try {
            String project = datasetEntity.getProject();
            String datasetName = datasetEntity.getDataset();

            if (SemanticConfig.getInstance().isDatasetVerifyEnable() &&
                    DatasetStatus.BROKEN.name().equals(datasetEntity.getStatus())) {
                List<KylinGenericModel> noCacheModels = SemanticAdapter.INSTANCE.getNocacheGenericModels(project);
                DatasetEntity datasetVerify = DatasetEntity.builder().id(datasetId).project(project).dataset(datasetName).build();
                DatasetValidator datasetValidator = new DatasetValidator(datasetVerify, datasetService, noCacheModels, modelService);
                DatasetValidateResult result = datasetValidator.validate();
                if (result.getDatasetValidateType() == DatasetValidateType.NORMAL
                        || result.getDatasetValidateType() == DatasetValidateType.SELF_FIX) {
                    brokenService.recoverOneDatasetNormal(datasetId);
                    log.info("[UI update dataset] verify dataset turn to be normal.");
                } else if (result.getDatasetValidateType() == DatasetValidateType.BROKEN) {
                    DatasetBrokenInfo brokenInfo = result.getBrokenInfo();
                    brokenService.setOneDatasetBroken(datasetId, brokenInfo);
                    log.info("[UI update dataset] verify dataset has still been broken.");
                }
                modelService.refreshGenericModels(project, noCacheModels);
            }
        } catch (Exception e) {
            log.error("Update dataset[{}] : verify dataset throw exception", datasetId, e);
        }

        syncManager.asyncNotify(new DatasetEventObject(
                new DatasetChangedSource(datasetEntity.getProject(), datasetEntity.getDataset()),
                DatasetEventType.DATASET_CHANGED)
        );
        return response;
    }

    /**
     * 导出一个dataset (Deprecated)
     */
    @Deprecated
    @PostMapping("dataset/export/{datasetType}")
    @Permission
    public DatasetDTO exportOneDataset(@PathVariable("datasetType") String datasetType,
                                       @RequestBody @Validated ProjectDatasetDTO projectDatasetDTO,
                                       HttpServletRequest httpServletRequest) throws SemanticException {
        log.info("user:{} enter API:GET [/api/dataset/export/{}], body:{}", authService.getCurrentUser(), datasetType, JSON.toJSONString(projectDatasetDTO));

        if (!checkPermission(httpServletRequest, projectDatasetDTO.getProject())) {
            throw new SemanticException(SemanticConstants.ACCESS_DENIED, ErrorCode.ACCESS_DENIED);
        }
        DatasetEntity datasetEntity = datasetService.getDatasetEntity(projectDatasetDTO.getProject(), projectDatasetDTO.getDatasetName());

        if (datasetEntity == null) {
            throw new SemanticException("The dataset required doesn't exist.", ErrorCode.DATASET_NOT_EXISTS);
        }
        //TODO: 当dataset为broken状态的时候，是否要导出
        return batchDatasetService.buildDatasetDTO(datasetEntity);
    }

    /**
     * 导出一个dataset
     */
    @GetMapping("dataset")
    @Permission
    public DatasetDTO exportOneDataset(@RequestParam("datasetType") String datasetType,
                                       @RequestParam("project") String project,
                                       @RequestParam("datasetName") String datasetName,
                                       HttpServletRequest httpServletRequest) throws SemanticException {
        log.info("user:{} enter API:GET [/api/dataset/export/{}], datasetName:{}", authService.getCurrentUser(), datasetType, datasetName);

        if (!checkPermission(httpServletRequest, project)) {
            throw new SemanticException(SemanticConstants.ACCESS_DENIED, ErrorCode.ACCESS_DENIED);
        }
        DatasetEntity datasetEntity = datasetService.getDatasetEntity(project, datasetName);

        if (datasetEntity == null) {
            throw new SemanticException("The dataset required doesn't exist.", ErrorCode.DATASET_NOT_EXISTS);
        }

        //TODO: 当dataset为broken状态的时候，是否要导出

        return batchDatasetService.buildDatasetDTO(datasetEntity);
    }

    /**
     * 获取一个项目下的数据集信息，其包含丰富的语义层信息，以便导出诊断
     */
    @GetMapping("datasets/{datasetType}/{project}")
    @Permission
    public SemanticProject getDatasets(@PathVariable("datasetType") String datasetType, @PathVariable("project") String project) throws SemanticException {
        // TODO: remove useless `datasetType`
        log.info("user:{} enter API: GET [/api/datasets/{}/{}]", authService.getCurrentUser(), datasetType, project);

        return semanticContext.createSemanticProject(project);
    }

    /**
     * 查询一个dataset
     */
    @GetMapping("dataset/{datasetId}")
    @Permission
    public Response<DatasetWithBrokenDTO> getOneDataset(@PathVariable("datasetId") Integer datasetId) throws SemanticException {

        log.info("user:{} enter API:GET [/api/dataset/{}]", authService.getCurrentUser(), datasetId);

        DatasetEntity datasetEntity = datasetService.selectDatasetById(datasetId);
        if (datasetEntity == null) {
            return new Response<DatasetWithBrokenDTO>(Response.Status.SUCCESS)
                    .data(null);
        }

        DatasetWithBrokenDTO datasetWithBrokenDTO = new DatasetWithBrokenDTO(batchDatasetService.buildDatasetDTO(datasetEntity));

        datasetWithBrokenDTO.setStatus(datasetEntity.getStatus());
        datasetWithBrokenDTO.setDatasetBrokenInfo(JSON.parseObject(datasetEntity.getBrokenMsg(), DatasetBrokenInfo.class));

        return new Response<DatasetWithBrokenDTO>(Response.Status.SUCCESS)
                .data(datasetWithBrokenDTO);
    }



    /**
     * 插入/导入一个dataset
     */
    @PostMapping("dataset")
    @Permission
    public Response<DatasetIdVO> insertOneDataset(@RequestBody @Validated DatasetDTO datasetDTO,
                                                  HttpServletRequest httpServletRequest,
                                                  @RequestParam String createType)
            throws SemanticException, PwdDecryptException {
        log.info("user:{} enter API:POST [/api/dataset], insert dataset, createType:{}, datasetDTO:{}", authService.getCurrentUser(), createType, JSON.toJSONString(datasetDTO));

        datasetDTO.setDatasetId(null);
        String basicAuth = httpServletRequest.getHeader(SemanticConstants.BASIC_AUTH_HEADER_KEY);
        Response<DatasetIdVO> response;
        if (basicAuth == null) {
            String user = authService.getCurrentUser();
            basicAuth = datasetService.getUserPwd(user);
        }
        response = batchDatasetService.doInsert(datasetDTO, basicAuth, false, Utils.currentTimeStamp(), null);
        if (Response.Status.SUCCESS.ordinal() == (response.getStatus())) {
            batchDatasetService.checkConnectDataset(response.getData().getDatasetId(), null, null, datasetDTO.getProject(), datasetDTO.getDatasetName(), httpServletRequest, basicAuth);
        }
        return response;
    }

    /**
     * 导入一个dataset (Deprecated)
     */
    @Deprecated
    @PostMapping("dataset/import")
    @Permission
    public Response<DatasetIdVO> importOneDataset(@RequestBody @Validated DatasetDTO datasetDTO,
                                                  HttpServletRequest httpServletRequest) throws SemanticException, PwdDecryptException {
        log.info("user:{} enter API:POST [/api/dataset/import], import dataset, datasetDTO:{}", authService.getCurrentUser(), JSON.toJSONString(datasetDTO));
        if (!checkPermission(httpServletRequest, datasetDTO.getProject())) {
            throw new SemanticException(SemanticConstants.ACCESS_DENIED, ErrorCode.ACCESS_DENIED);
        }
        return insertOneDataset(datasetDTO, httpServletRequest, "import");
    }

    /**
     * 校验 calculate member
     */
    @PostMapping("/semantic/cm/list/check")
    public Response<List<String>> validateCM(@RequestBody @Validated MdxExprValidationDTO mdxExprValidationDTO) {

        log.info("user:{} enter API:POST [/api/semantic/cm/list/check], check calculate member, MDXExprValidationDTO:{}", authService.getCurrentUser(), JSON.toJSONString(mdxExprValidationDTO));
        if (Utils.isCollectionEmpty(mdxExprValidationDTO.getCalcMemberSnippets())) {
            return new Response<List<String>>(Response.Status.SUCCESS)
                    .data(Collections.emptyList());
        }

        List<String> result = new ArrayList<>();
        for (String calcMemberSnippet : mdxExprValidationDTO.getCalcMemberSnippets()) {
            try {
                calcMemberParserImpl.parse(calcMemberSnippet, mdxExprValidationDTO.getSimpleSchema());
                //empty string means no error
                result.add("");
            } catch (Throwable e) {
                result.add(BatchDatasetService.collectCmParserErrorMessage(e));
            }
        }

        return new Response<List<String>>(Response.Status.SUCCESS)
                .data(result);
    }

    /**
     * 校验 named set
     */
    @PostMapping("/semantic/namedset/list/check")
    public Response<List<JSONObject>> validateNamedSets(@RequestBody @Validated MdxExprValidationDTO mdxExprValidationDTO) {

        log.info("user:{} enter API:POST [/api/semantic/namedset/list/check], check named set, MDXExprValidationDTO:{}", authService.getCurrentUser(), JSON.toJSONString(mdxExprValidationDTO));
        if (Utils.isCollectionEmpty(mdxExprValidationDTO.getNamedSetSnippets())) {
            return new Response<List<JSONObject>>(Response.Status.SUCCESS)
                    .data(Collections.emptyList());
        }

        List<JSONObject> result = new ArrayList<>();
        for (String namedSetSnippet : mdxExprValidationDTO.getNamedSetSnippets()) {
            JSONObject object = new JSONObject();
            try {

                namedSetParserImpl.parse(namedSetSnippet, mdxExprValidationDTO.getSimpleSchema());

                if (batchDatasetService.isEmptyNameSet(namedSetSnippet)) {
                    object.put("error", NAME_SET_CAN_NOT_BE_EMPTY);
                    object.put("location", "");
                    result.add(object);
                    continue;
                }

                List<String> dimensionsForLocation = namedSetParserImpl.getDimensionForLocation();
                List<String> pruneDimensionsForLocation = dimensionsForLocation.stream()
                        .distinct().collect(Collectors.toList());
                if (pruneDimensionsForLocation.size() != 1) {
                    object.put("location", "Named Set");
                } else {
                    String location = pruneDimensionsForLocation.iterator().next();
                    if ("Measures".equals(location)) {
                        object.put("location", "Named Set");
                    } else {
                        object.put("location", location);
                    }
                }
                dimensionsForLocation.clear();
                object.put("error", "");
                result.add(object);
            } catch (Throwable e) {
                object.put("error", BatchDatasetService.collectCmParserErrorMessage(e));
                object.put("location", "");
                result.add(object);
            }
        }
        namedSetParserImpl.clearMapNameSetForLocation();

        return new Response<List<JSONObject>>(Response.Status.SUCCESS)
                .data(result);
    }

    /**
     * 校验 default member
     */
    @PostMapping("/semantic/default-member/list/check")
    public Response<List<String>> validateDefaultMembers(@RequestBody @Validated DefaultMemberValidateDTO defaultMemberValidateDTO) {
        // only check constant member, and only the member structure, not include data
        List<String> defaultMemberList = defaultMemberValidateDTO.getDefaultMemberList();
        List<String> defaultMemberPathList = defaultMemberValidateDTO.getDefaultMemberPathList();
        SimpleSchema simpleSchema = defaultMemberValidateDTO.getSimpleSchema();
        if (defaultMemberList.size() != defaultMemberPathList.size()) {
            return new Response<List<String>>(Response.Status.FAIL)
                    .errorMsg("Check default member failed, different length of default member and default member path");
        }

        List<String> checkResults = new ArrayList<>();
        for (int i = 0; i < defaultMemberList.size(); i++) {
            try {
                defaultMemberValidatorImpl.validateDefaultMember(
                        defaultMemberList.get(i), defaultMemberPathList.get(i), simpleSchema);
                checkResults.add("");
            } catch (Throwable e) {
                checkResults.add(BatchDatasetService.collectCmParserErrorMessage(e));
            }
        }
        return new Response<>(Response.Status.SUCCESS, checkResults);
    }

    /**
     * 单元格格式化预览
     */
    @PostMapping("/dataset/format/preview")
    public Response<String> formatPreview(@RequestBody @Validated FormatSampleDTO formatSampleDTO){
        String format = formatSampleDTO.getFormat();
        String result;
        Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        Format formatter = new Format(format, locale);
        result = formatter.format(formatSampleDTO.getValue());
        return new Response<>(Response.Status.SUCCESS, result);
    }

    private boolean checkPermission(HttpServletRequest httpServletRequest, String project) throws SemanticException {
        if (SemanticConfig.getInstance().isConvertorMock()) {
            return true;
        }
        String basicAuth;
        basicAuth = httpServletRequest.getHeader(SemanticConstants.BASIC_AUTH_HEADER_KEY);
        if (basicAuth == null) {
            try {
                basicAuth = datasetService.getUserPwd(authService.getCurrentUser());
            } catch (PwdDecryptException e) {
                throw new SemanticException(e);
            }
        }
        String userAccessInfo = SemanticAdapter.INSTANCE.getAccessInfo(new ConnectionInfo(basicAuth, project));
        return KylinPermission.GLOBAL_ADMIN.name().equalsIgnoreCase(userAccessInfo)
                || KylinPermission.ADMINISTRATION.name().equalsIgnoreCase(userAccessInfo);
    }


    private long convertDay(long pre, long cur) {
        return (cur - pre) / 86400000;
    }
}
