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
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.SemanticOmitDetailException;
import io.kylin.mdx.insight.core.entity.CalculateMeasure;
import io.kylin.mdx.insight.core.entity.DatasetEntity;
import io.kylin.mdx.insight.core.entity.NamedDimCol;
import io.kylin.mdx.insight.core.entity.NamedDimTable;
import io.kylin.mdx.insight.core.entity.NamedMeasure;
import io.kylin.mdx.insight.core.entity.NamedSet;
import io.kylin.mdx.insight.core.entity.RoleInfo;
import io.kylin.mdx.insight.core.manager.ProjectManager;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.core.service.AuthService;
import io.kylin.mdx.insight.core.service.DatasetService;
import io.kylin.mdx.insight.engine.service.RoleService;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.bean.dto.RoleInfoDTO;
import io.kylin.mdx.insight.server.bean.dto.UserInvisibilityDTO;
import io.kylin.mdx.insight.server.support.Permission;
import io.kylin.mdx.insight.server.support.WebUtils;
import io.kylin.mdx.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("api")
public class VisibilityController {

    @Autowired
    private ProjectManager projectManager;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private AuthService authService;

    /**
     * 新增某个用户对于 dimension/measure/cm/namedset 的不可见性
     */
    @PostMapping("/dataset/user/invisibility")
    @Permission
    public Response<String> addUserInvisibility(
            @RequestBody @Validated UserInvisibilityDTO userInvisibilityDTO) throws SemanticException {

        log.info("user:{} enter API:POST [/api/dataset/user/invisibility], userInvisibilityDTO:{} ", authService.getCurrentUser(), JSON.toJSONString(userInvisibilityDTO));
        Integer datasetId = validateRequest(userInvisibilityDTO);

        NamedDimCol targetNamedDimCol = getNamedDimCol(datasetId, userInvisibilityDTO.getDimensionCol());
        NamedMeasure targetNamedMeasure = getNamedMeasure(datasetId, userInvisibilityDTO.getMeasure());
        CalculateMeasure targetCalculateMeasure = getCalcMeasure(datasetId, userInvisibilityDTO.getCalculateMeasure());
        NamedSet targetNamedSet = getNamedSet(datasetId, userInvisibilityDTO.getNamedSet());

        datasetService.addUserInvisibleThings(userInvisibilityDTO.getTargetUser(), targetNamedDimCol, targetNamedMeasure, targetCalculateMeasure, targetNamedSet);

        return new Response<>(SemanticConstants.RESP_SUC);
    }

    /**
     * 删除某个用户对于 dimension/measure/cm/namedset 的不可见性
     */
    @DeleteMapping("/dataset/user/invisibility")
    @Permission
    public Response<String> deleteUserInvisibility(
            @RequestBody @Validated UserInvisibilityDTO userInvisibilityDTO) throws SemanticException {

        log.info("user:{} enter API:DELETE [/api/dataset/user/invisibility], userInvisibilityDTO:{} ", authService.getCurrentUser(), JSON.toJSONString(userInvisibilityDTO));
        Integer datasetId = validateRequest(userInvisibilityDTO);

        NamedDimCol targetNamedDimCol = getNamedDimCol(datasetId, userInvisibilityDTO.getDimensionCol());
        NamedMeasure targetNamedMeasure = getNamedMeasure(datasetId, userInvisibilityDTO.getMeasure());
        CalculateMeasure targetCalculateMeasure = getCalcMeasure(datasetId, userInvisibilityDTO.getCalculateMeasure());
        NamedSet targetNamedSet = getNamedSet(datasetId, userInvisibilityDTO.getNamedSet());

        datasetService.removeUserInvisibleThings(userInvisibilityDTO.getTargetUser(), targetNamedDimCol, targetNamedMeasure, targetCalculateMeasure, targetNamedSet);

        return new Response<>(SemanticConstants.RESP_SUC);
    }

    /**
     * 向数据集角色添加单个用户
     */
    @PostMapping("/role/user/visibility/{roleId}")
    @Permission
    public Response<String> addUserToRole(
            @RequestBody @Validated RoleInfoDTO roleInfoDTO, @PathVariable("roleId") @NotNull Integer roleId) throws SemanticException {
        authService.hasAdminPermission();
        log.info("user:{} enter API:POST [/api/dataset/user/invisibility/{}], roleInfoDTO:{} ", authService.getCurrentUser(), roleId, JSON.toJSONString(roleInfoDTO));
        RoleInfo roleInfo = new RoleInfo(roleInfoDTO.getName(), roleInfoDTO.getVisible(), roleInfoDTO.getDescription());
        String status = roleService.addUserToRole(roleInfo, roleId);
        if (!status.equalsIgnoreCase(SemanticConstants.RESP_SUC)) {
            return new Response<String>(Response.Status.FAIL).data(status);
        }
        return new Response<>(SemanticConstants.RESP_SUC);
    }

    /**
     * 从数据集角色删除单个用户
     */
    @DeleteMapping("/role/user/visibility/{roleId}")
    @Permission
    public Response<String> deleteUserFromRole(
            @RequestBody @Validated RoleInfoDTO roleInfoDTO, @PathVariable("roleId") @NotNull Integer roleId) throws SemanticException {
        authService.hasAdminPermission();
        log.info("user:{} enter API:DELETE [/api/dataset/user/invisibility/{}], roleInfoDTO:{} ", authService.getCurrentUser(), roleId, JSON.toJSONString(roleInfoDTO));
        RoleInfo roleInfo = new RoleInfo(roleInfoDTO.getName(), roleInfoDTO.getVisible(), roleInfoDTO.getDescription());
        String status = roleService.deleteUserFromRole(roleInfo, roleId);
        if (!status.equalsIgnoreCase(SemanticConstants.RESP_SUC)) {
            return new Response<String>(Response.Status.FAIL).data(status);
        }
        return new Response<String>(Response.Status.SUCCESS).data(SemanticConstants.RESP_SUC);
    }

    private NamedDimCol getNamedDimCol(Integer datasetId, UserInvisibilityDTO.DimensionCol dimColDTO) {
        if (dimColDTO == null) {
            return null;
        }
        NamedDimTable namedDimTable = datasetService.selectOneDimTableBySearch(datasetId, dimColDTO.getModel(), dimColDTO.getTableAlias());
        if (namedDimTable == null) {
            throw new SemanticOmitDetailException(String.format("This dimension tableAlias[%s:%s] doesn't exist", dimColDTO.getModel(), dimColDTO.getTableAlias()), ErrorCode.DIMENSION_NOT_FOUND);
        }

        //use tableAlias as one condition to search namedDimCol
        NamedDimCol namedDimCol = datasetService.selectOneDimColsBySearch(datasetId, dimColDTO.getModel(), namedDimTable.getDimTable(), dimColDTO.getDimColAlias());
        if (namedDimCol == null) {
            throw new SemanticOmitDetailException(String.format("This dimension[%s:%s:%s] column doesn't exist", dimColDTO.getModel(), dimColDTO.getTableAlias(), dimColDTO.getDimColAlias()), ErrorCode.DIMENSION_COL_NOT_FOUND);
        }
        return namedDimCol;
    }

    private NamedMeasure getNamedMeasure(Integer datasetId, UserInvisibilityDTO.Measure measureDTO) {
        if (measureDTO == null) {
            return null;
        }
        NamedMeasure namedMeasure = datasetService.selectOneMeasureBySearch(datasetId, measureDTO.getModel(), measureDTO.getMeasure());
        if (namedMeasure == null) {
            throw new SemanticOmitDetailException(String.format("This measure[%s:%s] doesn't exist", measureDTO.getModel(), measureDTO.getMeasure()), ErrorCode.MEASURE_NOT_FOUND);
        }
        return namedMeasure;
    }

    private CalculateMeasure getCalcMeasure(Integer datasetId, String calcMeasureDTO) {
        if (StringUtils.isBlank(calcMeasureDTO)) {
            return null;
        }
        CalculateMeasure calculateMeasure = datasetService.selectOneCalcMeasureBySearch(datasetId, calcMeasureDTO);
        if (calculateMeasure == null) {
            throw new SemanticOmitDetailException(String.format("This calculate measure[%s] doesn't exist", calcMeasureDTO), ErrorCode.CALCULATED_MEASURE_NOT_FOUND);
        }
        return calculateMeasure;
    }

    private NamedSet getNamedSet(Integer datasetId, String namedSetDTO) {
        if (StringUtils.isBlank(namedSetDTO)) {
            return null;
        }

        NamedSet namedSet = datasetService.selectOneNamedSetBySearch(datasetId, namedSetDTO);
        if (namedSet == null) {
            throw new SemanticOmitDetailException(String.format("This named set[%s] doesn't exist", namedSetDTO), ErrorCode.NAMED_SET_NOT_FOUND);
        }
        return namedSet;
    }

    private Integer validateRequest(UserInvisibilityDTO userInvisibilityDTO) throws SemanticException {

        //check whether the user have right to access this project
        ConnectionInfo connInfo = ConnectionInfo.builder()
                .user(WebUtils.getCurrentLoginUser())
                .password(WebUtils.getCurrentUserPwd())
                .build();
        Set<String> accessProjects = projectManager.getActualProjectSet(connInfo);
        if (!accessProjects.contains(userInvisibilityDTO.getProject())) {
            throw new SemanticOmitDetailException(
                    String.format("User[%s] has no right to access the project[%s].", connInfo.getUser(), userInvisibilityDTO.getProject()));
        }

        //check whether dataset exist
        DatasetEntity datasetEntity = datasetService.getDatasetEntity(userInvisibilityDTO.getProject(), userInvisibilityDTO.getDatasetName());
        if (datasetEntity == null) {
            throw new SemanticOmitDetailException("The dataset doesn't exist!");
        }

        return datasetEntity.getId();
    }
}
