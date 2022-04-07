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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.SemanticOmitDetailException;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.entity.DatasetType;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.core.service.DatasetService;
import io.kylin.mdx.insight.core.service.DiagnosisService;
import io.kylin.mdx.insight.core.support.PackageThread;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.bean.dto.ClusterInfoDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetDTO;
import io.kylin.mdx.insight.server.bean.dto.DatasetDTOBuilder;
import io.kylin.mdx.insight.server.bean.dto.PackageStateDTO;
import io.kylin.mdx.insight.server.support.Permission;
import io.kylin.mdx.ErrorCode;
import io.kylin.mdx.rolap.util.QueryEngineUtils;
import lombok.extern.slf4j.Slf4j;
import mondrian.olap.MondrianException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Validated
@RestController
@RequestMapping("api")
public class DiagnosisController {

    private static final String TEMP_PATH = "diagnosis_tmp";

    private final DiagnosisService diagnosisService;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private RoleController roleController;

    @Autowired
    private AuthService authService;

    private PackageThread localThread;

    private final SimpleDateFormat queryDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private final String rootPath = System.getProperty("user.dir") + File.separator + "..";

    @Autowired
    public DiagnosisController(DiagnosisService diagnosisService){
        this.diagnosisService = diagnosisService;
    }

    /**
     * Return cluster information
     */
    @GetMapping("/system/clusters")
    @Permission
    public Response<List<ClusterInfoDTO.ClusterDTO>> getClusterInfo(HttpServletRequest request) throws SemanticException {
        log.info("user:{} enter API:GET [/api/system/clusters]", authService.getCurrentUser());
        roleController.checkPermission();
        ClusterInfoDTO clusterInfoDTO = new ClusterInfoDTO();
        clusterInfoDTO.setClusterNodes(new LinkedList<>());
        Map<String, String> nodeStatusMap = new HashMap<>();
        // get cluster from property file
        SemanticConfig.getInstance().loadConfig();
        if (diagnosisService.retrieveClusterInfo(nodeStatusMap)) {
            for (String ipAndPort : nodeStatusMap.keySet()) {
                String ip = ipAndPort.split(":")[0];
                String port = ipAndPort.split(":")[1];
                String status = nodeStatusMap.get(ipAndPort);
                ClusterInfoDTO.ClusterDTO currentNode = new ClusterInfoDTO.ClusterDTO();
                currentNode.setHost(ip);
                currentNode.setPort(port);
                currentNode.setStatus(status);
                clusterInfoDTO.getClusterNodes().add(currentNode);
            }
        }
        return new Response<List<ClusterInfoDTO.ClusterDTO>>(Response.Status.SUCCESS).data(clusterInfoDTO.getClusterNodes());
    }

    /**
     * Response:
     * <p>
     * {
     * "status": 0,
     * "data": [
     * {
     * "host": "10.1.2.32",
     * "port": "8080",
     * "status": "active"
     * },
     * {
     * "host": "10.1.2.31",
     * "port": "8080",
     * "status": "inactive"
     * }
     * ]
     * }
     */
    /**
     * Generate package
     */
    @PostMapping("/system/diagnose")
    @Permission
    public Response<String> generatePackage(HttpServletRequest httpServletRequest, @RequestBody @Validated ClusterInfoDTO clusterInfoDTO) throws SemanticException {
        log.info("user:{} enter API:POST [/api/system/diagnose]", authService.getCurrentUser());
        roleController.checkPermission();
        Long startTimeStamp = Long.parseLong(clusterInfoDTO.getStartAt());
        Long endTimeStamp = Long.parseLong(clusterInfoDTO.getEndAt());
        Date date = new Date(System.currentTimeMillis());
        String queryTime = queryDateFormat.format(date);
        String ip = httpServletRequest.getHeader("X-Host");
        String port = httpServletRequest.getHeader("X-Port");
        try {
            if (!QueryEngineUtils.isCurrentNode(ip, port)) {
                throw new SemanticOmitDetailException("The operation request is incorrect. It is recommended to check the configuration of Nginx.", ErrorCode.REQUEST_NODE_INCONSISTENCY_ERROR);
            }
        } catch (SocketException e) {
            throw new SemanticOmitDetailException(e.getMessage());
        }
        Map<String, String> mapDatasetName2Json = new HashMap<>();
        try {
            List<DatasetEntity> datasets = datasetService.selectDatasetBySearch(
                    DatasetType.MDX.ordinal(), 0L, System.currentTimeMillis() / 1000);
            for (DatasetEntity datasetEntity : datasets) {
                if (datasetEntity == null) {
                    throw new SemanticException("The dataset required doesn't exist.", ErrorCode.DATASET_NOT_EXISTS);
                }

                DatasetDTO currentDataset = buildDatasetDTO(datasetEntity);
                ObjectMapper objectMapper = new ObjectMapper();
                String datasetStr = objectMapper.writeValueAsString(currentDataset);
                mapDatasetName2Json.put("Project-" + datasetEntity.getProject() + "_Dataset-" + datasetEntity.getDataset(), datasetStr);
            }
        } catch (Exception e) {
            throw new MondrianException(e);
        }
        localThread = diagnosisService.extractSpecifiedData(startTimeStamp, endTimeStamp, rootPath, TEMP_PATH,
                queryTime, ip, port, mapDatasetName2Json);
        return new Response<String>(Response.Status.SUCCESS).data(SemanticConstants.RESP_SUC);
    }

    private DatasetDTO buildDatasetDTO(DatasetEntity datasetEntity) throws SemanticException {
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

    /**
     * {
     * "start_at": UTC_TIMESTAMP,
     * "end_at": UTC_TIMESTAMP,
     * "log_type": 0, // 0: 基础诊断, 1: 查询诊断, 2: 全量诊断
     * "clusters": [
     * {
     * "host": "10.1.2.31",
     * "port": "8080"
     * },
     * {
     * "host": "10.1.2.32",
     * "port": "8080"
     * }
     * ]
     * }
     * Response:
     * <p>
     * {
     * "status": 0,
     * "data": "success"
     * }
     */
    /**
     * Return package state
     */
    @GetMapping("/system/diagnose")
    @Permission
    public Response<List<PackageStateDTO.PhaseDTO>> retrievePackageState(HttpServletRequest httpServletRequest) throws SemanticException {
        log.info("user:{} enter API:GET [/api/system/diagnose]", authService.getCurrentUser());
        roleController.checkPermission();

        String ip = httpServletRequest.getHeader("X-Host");
        String port = httpServletRequest.getHeader("X-Port");
        try {
            if (!QueryEngineUtils.isCurrentNode(ip, port)) {
                throw new SemanticOmitDetailException("The operation request is incorrect. It is recommended to check the configuration of Nginx.", ErrorCode.REQUEST_NODE_INCONSISTENCY_ERROR);
            }
        } catch (SocketException e) {
            throw new SemanticOmitDetailException(e.getMessage());
        }
        String result = diagnosisService.getPackageState(localThread);
        String[] phaseStates = result.split(",");
        List<PackageStateDTO.PhaseDTO> phaseDTOs = new LinkedList<PackageStateDTO.PhaseDTO>();
        for (int i = 0; i < phaseStates.length; i++) {
            String status = phaseStates[i].split(":")[0];
            String process = phaseStates[i].split(":")[1];
            String detail = phaseStates[i].split(":")[2];
            PackageStateDTO.PhaseDTO phaseDTO = new PackageStateDTO.PhaseDTO(status, process, detail);
            phaseDTOs.add(phaseDTO);
        }
        return new Response<List<PackageStateDTO.PhaseDTO>>(Response.Status.SUCCESS).data(phaseDTOs);
    }

    /**
     * Response:
     * <p>
     * {
     * "status": 0,
     * "data": [
     * {
     * "status": "pending", // pending, running, error, success
     * "process": "EXTRACT_PACKAGE_INFO",
     * "detail": "各种报错信息"
     * },
     * {
     * "status": "pending", // pending, running, error, success
     * "process": "START_COMPRESSION",
     * "detail": "各种报错信息"
     * },
     * {
     * "status": "pending", // pending, running, error, success
     * "process": "PACKAGE_COMPLETE",
     * "detail": "下载链接"
     * }
     * ]
     * }
     */
    /**
     * Stop packing
     */
    @DeleteMapping("/system/diagnose")
    @Permission
    public Response<String> stopPacking() throws SemanticException {
        log.info("user:{} enter API:DELETE [/api/system/diagnose]", authService.getCurrentUser());
        roleController.checkPermission();
        diagnosisService.stopPackaging(localThread, rootPath);
        return new Response<String>(Response.Status.SUCCESS).data(SemanticConstants.RESP_SUC);
    }

    /**
     * Response:
     * <p>
     * {
     * "status": 0,
     * "data": "success"
     * }
     */
    /**
     * Download target package
     */
    @GetMapping("/download/{fileName}")
    @Permission
    public Response<String> downloadFile(HttpServletRequest httpServletRequest, @PathVariable("fileName") String fileName,
                                         HttpServletResponse response) throws SemanticException {
        log.info("user:{} enter API:GET [/api/download/{}]", authService.getCurrentUser(), fileName);
        roleController.checkPermission();

        String ip = httpServletRequest.getHeader("X-Host");
        String port = httpServletRequest.getHeader("X-Port");

        try {
            if (!QueryEngineUtils.isCurrentNode(ip, port)) {
                throw new SemanticOmitDetailException("The operation request is incorrect. It is recommended to check the configuration of Nginx.", ErrorCode.REQUEST_NODE_INCONSISTENCY_ERROR);
            }
        } catch (SocketException e) {
            throw new SemanticOmitDetailException(e.getMessage());
        }

        String diagnoseDir = rootPath + File.separator +
                "semantic-mdx" + File.separator + "diagnose_packages";
        File targetFile = new File(diagnoseDir + File.separator + fileName);
        boolean result = true;
        if (!targetFile.exists()) {
            result = false;
        } else {
            try (FileInputStream in = new FileInputStream(targetFile);
                 OutputStream out = response.getOutputStream()) {
                //set header
                response.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
                //set buffer
                byte[] buffer = new byte[1024];
                int len = 0;
                //instream to buffer
                while((len = in.read(buffer)) > 0){
                    out.write(buffer, 0, len);
                }
            } catch (Exception e) {
                result = false;
            }
        }
        if (result) {
            return new Response<String>(Response.Status.SUCCESS).data(SemanticConstants.RESP_SUC);
        } else {
            return new Response<String>(Response.Status.FAIL).data(SemanticConstants.RESP_FAIL);
        }
    }

}
