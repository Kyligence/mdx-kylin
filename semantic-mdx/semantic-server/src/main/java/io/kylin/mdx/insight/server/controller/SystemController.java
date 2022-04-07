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
import io.kylin.mdx.insight.common.MdxContext;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.SemanticUserAndPwd;
import io.kylin.mdx.insight.common.constants.ConfigConstants;
import io.kylin.mdx.insight.common.util.AESWithECBEncryptor;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.GroupInfo;
import io.kylin.mdx.insight.core.entity.UserInfo;
import io.kylin.mdx.insight.core.service.*;
import io.kylin.mdx.insight.core.support.KILicenseInfo;
import io.kylin.mdx.insight.core.support.UserOperResult;
import io.kylin.mdx.insight.server.bean.Page;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.bean.dto.AADInfoDTO;
import io.kylin.mdx.insight.server.bean.dto.GroupInfoDTO;
import io.kylin.mdx.insight.server.bean.dto.LicenseUpdateDTO;
import io.kylin.mdx.insight.server.bean.dto.MdxHealthDTO;
import io.kylin.mdx.insight.server.bean.dto.MdxLoadDTO;
import io.kylin.mdx.insight.server.bean.dto.UserInfoDTO;
import io.kylin.mdx.insight.server.service.BatchDatasetService;
import io.kylin.mdx.insight.server.support.Permission;
import lombok.extern.slf4j.Slf4j;
import mondrian.xmla.XmlaRequestContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Validated
@RestController
@RequestMapping("api")
public class SystemController {

    @Autowired
    private InitService initService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleController roleController;

    @Autowired
    private AuthService authService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private BatchDatasetService batchDatasetService;


    @Autowired
    private MetadataService metadataService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private MetaSyncService metaSyncService;

    private final SemanticConfig config = SemanticConfig.getInstance();

    /**
     * 刷新某个项目的 cube cache
     */
    @GetMapping("/system/refresh/{project}")
    @Permission
    public Response<String> refreshProjectModels(@PathVariable("project") String project) throws SemanticException {
        log.info("user:{} enter API:GET [/api/system/refresh/{}]", authService.getCurrentUser(), project);
        modelService.loadGenericModels(project);

        return new Response<String>(Response.Status.SUCCESS)
                .data(SemanticConstants.RESP_SUC);
    }

    /**
     * 同步KYLIN的一些元信息
     */
    @GetMapping("/system/sync")
    public Response<String> syncKeMetaData(HttpServletResponse httpServletResponse) throws SemanticException {

        log.info("user:{} enter API:GET [/api/system/sync]", authService.getCurrentUser());
        if (!initService.sync()) {
            httpServletResponse.setStatus(UserOperResult.NO_CONFIG_USER.getHttpCode());
            return new Response<String>(UserOperResult.NO_CONFIG_USER.getCode())
                    .data(UserOperResult.NO_CONFIG_USER.getMessage());
        }
        return new Response<String>(Response.Status.SUCCESS)
                .data(SemanticConstants.RESP_SUC);
    }

    /**
     * 配置详情
     */
    @GetMapping("/system/configurations")
    @Permission
    public Response<Map<String, String>> getConfigurations(
            @RequestParam(required = false, value = "key", defaultValue = "") String key) throws SemanticException {
        log.info("user:{} enter API:GET [/api/system/configurations]", authService.getCurrentUser());
        Map<String, String> confMap = initService.getConfigurations();
        if (StringUtils.isNotBlank(key)) {
            Set<String> keyList = new HashSet<>(Arrays.asList(key.split(",")));
            confMap = Utils.filterMapByKey(confMap, keyList);
        }
        return new Response<>(confMap);
    }

    /**
     * 配置用户信息更改
     */
    @PutMapping("/system/configurations")
    @Permission
    public Response<String> updateConfigurations(@RequestBody Map<String, String> confMap) throws SemanticException {

        log.info("user:{} enter API:PUT [/api/system/configurations]", authService.getCurrentUser());
        roleController.checkPermission();
        String data = initService.updateConfigurations(confMap);
        if (!SemanticConstants.RESP_SUC.equals(data)) {
            return new Response<String>(Response.Status.FAIL)
                    .data(data);
        }
        return new Response<String>(Response.Status.SUCCESS)
                .data(SemanticConstants.RESP_SUC);
    }

    /**
     * 同步任务重启
     */
    @PostMapping("/system/sync/jobs")
    @Permission
    public Response<String> syncJob() throws SemanticException {

        log.info("user:{} enter API:POST [/api/system/sync/jobs]", authService.getCurrentUser());
        roleController.checkPermission();
        String result = initService.restartSync();
        if (!SemanticConstants.RESP_SUC.equals(result)) {
            return new Response<String>(Response.Status.FAIL)
                    .data(result);
        }
        return new Response<String>(Response.Status.SUCCESS)
                .data(SemanticConstants.RESP_SUC);
    }

    // TODO: remove this api
    /**
     * 同步KYLIN License
     */
    @PostMapping("/system/sync/license")
    public Response<String> syncLicense() throws SemanticException {
        return new Response<String>(Response.Status.SUCCESS).data(SemanticConstants.RESP_SUC);
    }

    /**
     * MDX 健康检测接口
     */
    @GetMapping("/system/health")
    public MdxHealthDTO checkHealth(HttpServletResponse response, @RequestParam(required = false) String projectName) {

        MdxHealthDTO mdxHealthDTO = new MdxHealthDTO();
        List<String> projects = null;
        String basicAuth = "";
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        try {
            projects = datasetService.getProjectsRelatedDataset();
            basicAuth = Utils.buildBasicAuth(SemanticUserAndPwd.getUser(), SemanticUserAndPwd.getDecodedPassword());
        } catch (Exception e) {
            mdxHealthDTO.setMsg(e.toString());
            return mdxHealthDTO;
        }

        if (!MdxContext.isSyncStatus()) {
            mdxHealthDTO.setMsg("KYLIN meta synchronization job has stopped...");
            if (!SemanticConfig.getInstance().isConvertorMock()) {
                return mdxHealthDTO;
            }
        }

        if (projects.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_OK);
            mdxHealthDTO.setCode(SemanticConstants.CODE_SUCCESS);
            mdxHealthDTO.setData(SemanticConstants.DATA_NORMAL);
            return mdxHealthDTO;
        }
        String project;
        if (StringUtils.isNotBlank(projectName)) {
            project = projectName;
        } else {
            project = projects.get(0);
        }
        String url = SemanticConfig.getInstance().getDiscoverCatalogUrl(project);
        HttpEntity<String> requestEntity = BatchDatasetService.getCheckDatasetHttpEntity(basicAuth, true);
        XmlaRequestContext context = new XmlaRequestContext();
        ResponseEntity<String> responseEntity;
        if (!SemanticConfig.getInstance().isConvertorMock()) {
            responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
        } else {
            responseEntity = new ResponseEntity<>(HttpStatus.OK);
        }
        context.clear();
        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            mdxHealthDTO.setMsg("Execute MDX occur error: " + responseEntity.getBody());
            return mdxHealthDTO;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        mdxHealthDTO.setMsg("");
        mdxHealthDTO.setCode(SemanticConstants.CODE_SUCCESS);
        mdxHealthDTO.setData(SemanticConstants.DATA_NORMAL);
        return mdxHealthDTO;
    }

    /**
     * 更新KI的license
     */
    @PostMapping("/system/user")
    @Permission
    public Response<String> updateLicense(
            HttpServletResponse httpServletResponse,
            @RequestBody @Validated LicenseUpdateDTO licenseUpdateDTO) {

        log.info("user:{} enter API:POST [/api/system/user], LicenseUpdateDTO:{}", authService.getCurrentUser(), JSON.toJSONString(licenseUpdateDTO));
        UserOperResult userOperResult = userService.changeUserLicense(licenseUpdateDTO.getUsername(), licenseUpdateDTO.getLicenseAuth());
        httpServletResponse.setStatus(userOperResult.getHttpCode());

        log.info("user:{} change license auth to [{}], result:{}.", licenseUpdateDTO.getUsername(), licenseUpdateDTO.getLicenseAuth(),
                JSON.toJSONString(userOperResult));
        return new Response<String>(userOperResult.getCode()).data(userOperResult.getMessage());
    }

    /**
     * 获取KI的license
     */
    @GetMapping("/system/license")
    public Response<KILicenseInfo> getKILicense() {
        log.info("user:{} enter API:GET [/api/system/license]", authService.getCurrentUser());
        KILicenseInfo kiLicenseInfo = userService.getKiLicenseInfo();
        return new Response<>(kiLicenseInfo);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/system/users")
    @Permission
    public Response<?> selectUserInfo(@RequestParam(value = "username", required = false) String username,
                                      @RequestParam(value = "project", required = false, defaultValue = "") String project) {
        log.info("user:{} enter API:GET [/api/system/users], project:{}",
                authService.getCurrentUser(), project);
        if (StringUtils.isNotBlank(username)) {
            return new Response<>(userService.selectOne(username));
        }
        List<UserInfo> userInfos = userService.selectAll();
        if (StringUtils.isNotBlank(project)) {
            List<String> filters = userService.getAllUserByProject(project, 0, Integer.valueOf(SemanticConfig.getInstance().getUserPageSize()))
                    .stream().map(UserInfo::getUsername).collect(Collectors.toList());
            userInfos = userInfos.stream()
                    .filter(userInfo -> filters.contains(userInfo.getUsername()))
                    .collect(Collectors.toList());
        }
        return new Response<>(userInfos);
    }

    /**
     * 获取用户列表
     */
    @GetMapping("/system/allUsers")
    @Permission
    public Response<Page<UserInfoDTO>> getUsers(@RequestParam("pageNum") @Min(0) @Max(1000) Integer pageNum,
                                                @RequestParam("pageSize") @NotNull Integer pageSize,
                                                @RequestParam(value = "project", required = false, defaultValue = "") String project) {
        log.info("user:{} enter API:GET [/api/system/allUsers], project:{}, pageNum:{}, pageSize:{}",
                authService.getCurrentUser(), project, pageNum, pageSize);
        List<UserInfo> userInfos;
        if (StringUtils.isNotBlank(project)) {
            userInfos = userService.getAllUserByProject(project, pageNum, pageSize);
        } else {
            userInfos = userService.getAllUsers(pageNum + 1, pageSize);
        }
        List<UserInfoDTO> userInfoDTOs = new LinkedList<>();
        userInfos.forEach(userInfo -> {
            UserInfoDTO userInfoDTO = new UserInfoDTO(userInfo);
            userInfoDTOs.add(userInfoDTO);
        });
        Page<UserInfoDTO> userInfoDTOPage = new Page<>(userInfoDTOs);
        userInfoDTOPage.setPageInfo(userInfos);
        return new Response<Page<UserInfoDTO>>(Response.Status.SUCCESS).data(userInfoDTOPage);
    }

    /**
     * 获取用户组列表
     */
    @GetMapping("/system/groups")
    @Permission
    public Response<Page<GroupInfoDTO>> getGroups(
            @RequestParam(value = "page_num", required = false, defaultValue = "0") @Min(0) @Max(1000) Integer pageNum,
            @RequestParam(value = "page_size", required = false, defaultValue = "100000") @NotNull Integer pageSize,
            @RequestParam(value = "project", required = false, defaultValue = "") String project) {
        log.info("user:{} enter API:GET [/api/system/groups], page_num:{}, page_size:{}", authService.getCurrentUser(), pageNum, pageSize);
        List<GroupInfo> groupInfos;
        if (StringUtils.isBlank(project)) {
            groupInfos = userService.getAllGroup(pageNum, pageSize);
        } else {
            groupInfos = userService.getAllGroupByProject(project, pageNum, pageSize);
        }
        List<GroupInfoDTO> groupInfoDTOS = new ArrayList<>(groupInfos.size());
        groupInfos.forEach(groupInfo -> {
            GroupInfoDTO groupInfoDTO = new GroupInfoDTO(groupInfo);
            groupInfoDTOS.add(groupInfoDTO);
        });
        Page<GroupInfoDTO> page = new Page<>(groupInfoDTOS);
        page.setPageInfo(groupInfos);
        return new Response<Page<GroupInfoDTO>>(Response.Status.SUCCESS).data(page);
    }

    /**
     * 获取维度基数信息
     */
    @GetMapping("/system/metadata/cardinality")
    @Permission
    public Response<Map<String, Map<String, Long>>> getCardinality(@RequestParam(value = "project") String project) {
        log.info("user:{} enter API:GET [/api/system/metadata/cardinality], project:{}",
                authService.getCurrentUser(), project);
        return new Response<>(metadataService.getCardinalityMap(project));
    }

    /**
     * MDX 负载信息
     */
    @GetMapping("/system/load")
    public MdxLoadDTO checkLoad(HttpServletResponse response) {
        MemoryMXBean mxb = ManagementFactory.getMemoryMXBean();
        double memoryRatio = mxb.getHeapMemoryUsage().getUsed() * 1D / mxb.getHeapMemoryUsage().getMax();
        response.setStatus(HttpServletResponse.SC_OK);
        DecimalFormat df = new DecimalFormat("#.000");
        String res = df.format(memoryRatio);
        return new MdxLoadDTO(SemanticConstants.CODE_SUCCESS, "", res);
    }

    /**
     * 获取 AAD 信息, 供 MDX Gateway 调用
     */
    @GetMapping("/system/aadInfo")
    public Response<AADInfoDTO> getAADInfo() {
        AADInfoDTO aadInfoDTO = new AADInfoDTO();
        String clientSecret = config.getAADClientSecret();
        try {
            if (StringUtils.isNotBlank(clientSecret)) {
                clientSecret = AESWithECBEncryptor.encrypt(clientSecret);
            }
        } catch (Exception e) {
            log.error("encrypt clientSecret catch exception", e);
        }
        aadInfoDTO.setClientId(config.getAADClientId());
        aadInfoDTO.setTenantId(config.getAADTenantId());
        aadInfoDTO.setClientSecret(clientSecret);
        aadInfoDTO.setServerUrl(config.getAADServerUrl());
        return new Response<>(aadInfoDTO);
    }

    /**
     * AAD 设置信息, 供前端调用
     */
    @GetMapping("/system/aad-settings")
    public Response<Map<String, String>> getAADSettings() {
        Map<String, String> confMap = new HashMap<>();
        confMap.put(ConfigConstants.IS_ENABLE_AAD, String.valueOf(config.isEnableAAD()));
        if (config.isEnableAAD()) {
            confMap.put(ConfigConstants.AAD_LOGOUT_URL, config.getAADLogoutUrl());
            confMap.put(ConfigConstants.AAD_LOGIN_URL, config.getAADLoginUrl());
        }
        return new Response<>(confMap);
    }

}
