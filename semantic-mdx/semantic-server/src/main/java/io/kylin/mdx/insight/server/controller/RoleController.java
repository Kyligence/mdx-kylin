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
import io.kylin.mdx.insight.common.PwdDecryptException;
import io.kylin.mdx.insight.common.SemanticConfig;
import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.AESWithECBEncryptor;
import io.kylin.mdx.insight.core.service.AuthService;
import org.apache.commons.lang3.StringUtils;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.entity.RoleInfo;
import io.kylin.mdx.insight.core.entity.UserInfo;
import io.kylin.mdx.insight.core.meta.ConnectionInfo;
import io.kylin.mdx.insight.core.service.UserService;
import io.kylin.mdx.insight.engine.service.RoleService;
import io.kylin.mdx.insight.server.bean.Page;
import io.kylin.mdx.insight.server.bean.Response;
import io.kylin.mdx.insight.server.bean.dto.RoleInfoDTO;
import io.kylin.mdx.insight.server.bean.vo.RoleIdVo;
import io.kylin.mdx.insight.server.support.Permission;
import io.kylin.mdx.insight.server.support.WebUtils;
import io.kylin.mdx.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Validated
@RestController
@RequestMapping("api")
public class RoleController {

    private final RoleService roleService;

    private final AuthService authService;

    private final UserService userService;

    @Autowired
    public RoleController(AuthService authService, UserService userService, RoleService roleService) {
        this.authService = authService;
        this.userService = userService;
        this.roleService = roleService;
    }

    /**
     * 插入数据集角色
     */
    @PostMapping("/role")
    @Permission
    public Response<RoleIdVo> insertRole(@RequestBody @Validated RoleInfoDTO roleInfoDTO) {
        log.info("user:{} enter API:POST [/api/role], roleInfoDTO:{} ", authService.getCurrentUser(), JSON.toJSONString(roleInfoDTO));
        checkPermission();
        roleInfoDTO.setVisible(roleInfoDTO.getVisible().stream().distinct().collect(Collectors.toList()));
        RoleInfo roleInfo = new RoleInfo(roleInfoDTO.getName(), roleInfoDTO.getVisible(), roleInfoDTO.getDescription());
        RoleIdVo roleIdVo = new RoleIdVo();
        String status = roleService.insertRole(roleInfo);
        if (!status.startsWith(SemanticConstants.RESP_SUC)) {
            roleIdVo.setStatus(status);
            return new Response<RoleIdVo>(Response.Status.FAIL).data(roleIdVo);
        }
        roleIdVo.setStatus(status.split(":")[0]);
        roleIdVo.setRoleId(Integer.valueOf(status.split(":")[1]));
        return new Response<RoleIdVo>(Response.Status.SUCCESS).data(roleIdVo);
    }

    /**
     * 查询单个数据集角色信息
     */
    @GetMapping("/role/{roleId}")
    @Permission
    public Response<RoleInfoDTO> selectRoleInfo(@PathVariable("roleId") @NotNull Integer roleId) {
        log.info("user:{} enter API:GET [/api/role/{}]", authService.getCurrentUser(), roleId);
        RoleInfo roleInfo = roleService.selectRole(roleId);
        if (roleInfo == null) {
            return new Response<RoleInfoDTO>(Response.Status.SUCCESS).data(null);
        }
        RoleInfoDTO roleInfoDTO = new RoleInfoDTO(roleInfo);
        return new Response<RoleInfoDTO>(Response.Status.SUCCESS).data(roleInfoDTO);
    }

    /**
     * 更新单个数据集角色信息
     */
    @PutMapping("/role/{roleId}")
    @Permission
    public Response<String> updateRoleInfo(@PathVariable("roleId") @NotNull Integer roleId,
                                           @RequestBody @Validated RoleInfoDTO roleInfoDTO) {
        log.info("user:{} enter API:PUT [/api/role/{}], role info DTO content: {}",
                authService.getCurrentUser(), roleId, JSON.toJSONString(roleInfoDTO));
        checkPermission();
        roleInfoDTO.setVisible(roleInfoDTO.getVisible().stream().distinct().collect(Collectors.toList()));
        RoleInfo roleInfo = new RoleInfo(roleInfoDTO.getName(), roleInfoDTO.getVisible(), roleInfoDTO.getDescription());
        String status = roleService.updateRole(roleInfo, roleId);
        if (!status.equalsIgnoreCase(SemanticConstants.RESP_SUC)) {
            return new Response<String>(Response.Status.FAIL).data(status);
        }
        return new Response<String>(Response.Status.SUCCESS).data(SemanticConstants.RESP_SUC);
    }

    /**
     * 删除单个数据集角色
     */
    @DeleteMapping("/role/{roleId}")
    @Permission
    public Response<String> deleteRole(@PathVariable("roleId") @NotNull Integer roleId) {
        log.info("user:{} enter API:DELETE [/api/role/{}]", authService.getCurrentUser(), roleId);
        checkPermission();

        RoleInfo roleInfo = roleService.selectRole(roleId);
        log.info("Deleting role: id:{}, role info entity content: {}", roleId, JSON.toJSONString(roleInfo));

        String status = roleService.deleteRole(roleId);
        if (!status.equals(SemanticConstants.RESP_SUC)) {
            return new Response<String>(Response.Status.FAIL).data(status);
        }
        return new Response<String>(Response.Status.SUCCESS).data(SemanticConstants.RESP_SUC);
    }

    /**
     * 查询数据集角色列表
     */
    @GetMapping("/roles")
    @Permission
    public Response<Page<RoleInfoDTO>> getRoles(@RequestParam("pageNum") @Min(0) @Max(1000) Integer pageNum,
                                                @RequestParam("pageSize") @NotNull Integer pageSize,
                                                @RequestParam(value = "RoleName", required = false) String roleName,
                                                @RequestParam("containsDesc") @NotNull boolean containsDesc) {
        log.info("user:{} enter API:GET [/api/roles], pageNum:{}, pageSize:{}", authService.getCurrentUser(), pageNum, pageSize);
        RoleInfo search = new RoleInfo(roleName);
        List<RoleInfo> roleInfos = roleService.getRoleInfoByPage(search, pageNum + 1, pageSize);
        List<RoleInfoDTO> roleInfoDTOS = new LinkedList<>();
        if (containsDesc) {
            roleInfos.forEach(roleInfo ->
                    roleInfoDTOS.add(new RoleInfoDTO(roleInfo.getId(), roleInfo.getName(), roleInfo.getDescription()))
            );
        } else {
            roleInfos.forEach(roleInfo ->
                    roleInfoDTOS.add(new RoleInfoDTO(roleInfo.getId(), roleInfo.getName()))
            );
        }
        Page<RoleInfoDTO> roleInfoDTOPage = new Page<>(roleInfoDTOS);
        roleInfoDTOPage.setPageInfo(roleInfos);
        return new Response<Page<RoleInfoDTO>>(Response.Status.SUCCESS).data(roleInfoDTOPage);
    }

    /**
     * 检验用户是否具有系统管理员权限
     */
    public void checkPermission() {
        String user = authService.getCurrentUser();
        if (StringUtils.isEmpty(user)) {
            HttpServletRequest request = WebUtils.getRequest();
            String basicAuth = request.getHeader(SemanticConstants.BASIC_AUTH_HEADER_KEY);
            if (StringUtils.isNotBlank(basicAuth)) {
                String[] userPwd = Utils.decodeBasicAuth(basicAuth);
                user = userPwd[0];
            }
        }
        UserInfo userInfo = userService.selectOne(user);
        if (userInfo == null) {
            throw new SemanticException(SemanticConstants.NO_ACCESS, ErrorCode.MISSING_AUTH_INFO);
        }

        String userPwd;
        try {
            userPwd = Utils.buildBasicAuth(user, AESWithECBEncryptor.decrypt(userInfo.getPassword()));
        } catch (PwdDecryptException p) {
            throw new SemanticException("decrypt user's password throw an exception:" + p.getMessage(), ErrorCode.ENCRYPT_PARAMETER_LENGTH_ERROR);
        }
        if (SemanticConfig.getInstance().isConvertorMock()) {
            return;
        }

        boolean accessFlag = userService.hasAdminPermission(new ConnectionInfo(userPwd), true);
        if (!accessFlag) {
            throw new SemanticException(SemanticConstants.NO_ACCESS, ErrorCode.NOT_ADMIN_USER);
        }
    }

}
