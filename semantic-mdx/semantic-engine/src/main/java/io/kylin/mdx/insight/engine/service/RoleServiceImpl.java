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

import io.kylin.mdx.insight.common.SemanticConstants;
import io.kylin.mdx.insight.common.SemanticException;
import io.kylin.mdx.insight.common.util.Utils;
import io.kylin.mdx.insight.core.dao.RoleInfoMapper;
import io.kylin.mdx.insight.core.dao.UserInfoMapper;
import io.kylin.mdx.insight.core.entity.EntityContains;
import io.kylin.mdx.insight.core.entity.RoleInfo;
import io.kylin.mdx.insight.core.entity.UserInfo;
import io.kylin.mdx.insight.core.entity.VisibleAttr;
import io.kylin.mdx.ErrorCode;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {
    @Autowired
    private RoleInfoMapper roleInfoMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public String insertRole(RoleInfo roleInfo) throws SemanticException {
        roleInfo.setName(roleInfo.getName().trim());
        if (!verifyRoleName(roleInfo.getName())) {
            return SemanticConstants.INVALID_NAME;
        }
        String checkResult = verifyVisibleAttr(roleInfo, true);
        if (!checkResult.isEmpty()) {
            if (SemanticConstants.FORMAT_ERROR.equalsIgnoreCase(checkResult)) {
                return checkResult;
            }
            String userNotExists = SemanticConstants.USER_NOT_EXISTS.replaceAll("xxx", checkResult);
            return userNotExists;
        }
        //check whether role duplicate
        RoleInfo query = new RoleInfo(roleInfo.getName());
        RoleInfo result = roleInfoMapper.selectOne(query);
        if (result != null) {
            String duplicateRoleName = SemanticConstants.DUPLICATE_ROLE_NAME.replaceAll("xxx", roleInfo.getName());
            throw new SemanticException(duplicateRoleName, ErrorCode.DUPLICATE_ROLE_NAME);
        }
        int r = roleInfoMapper.insertOneReturnId(roleInfo);
        if (r > 0 && roleInfo.getId() != null) {
            return SemanticConstants.RESP_SUC + ":" + roleInfo.getId();
        } else {
            throw new SemanticException(Utils.formatStr("Inserting role_info record gets a failure, r:%d, roleName:%s", r, roleInfo.getName()), ErrorCode.DB_OPERATION_ERROR);
        }
    }

    @Override
    public RoleInfo selectRole(Integer roleId) {
        RoleInfo queryRole = new RoleInfo(roleId);
        RoleInfo roleInfo = roleInfoMapper.selectOne(queryRole);
        return roleInfo;
    }
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public String updateRole(RoleInfo roleInfo, Integer roleId) throws SemanticException {
        if (!verifyRoleName(roleInfo.getName())) {
            return SemanticConstants.INVALID_NAME;
        }

        String checkResult = verifyVisibleAttr(roleInfo, false);
        if (!checkResult.isEmpty()) {
            return checkResult;
        }
        RoleInfo query = new RoleInfo(roleId);
        RoleInfo result = roleInfoMapper.selectOne(query);
        if (result == null) {
            roleInfo.setId(roleId);
            roleInfo.setCreateTime(Utils.currentTimeStamp());
            roleInfo.setModifyTime(Utils.currentTimeStamp());
            roleInfoMapper.insert(roleInfo);
            return SemanticConstants.RESP_SUC;
        }
        if (!result.getName().equalsIgnoreCase(roleInfo.getName())) {
            return SemanticConstants.ROLE_ID_NOT_MATCH_NAME;
        }

        int delete = roleInfoMapper.delete(query);
        if (delete < 0) {
            throw new SemanticException(Utils.formatStr("update role_info record gets a failure, delete:%d, id:%d", delete, roleInfo.getId()), ErrorCode.DB_OPERATION_ERROR);
        }
        if (SemanticConstants.DEFAULT_ROLE.equalsIgnoreCase(roleInfo.getName())) {
            result.setExtend(roleInfo.getExtend());
            result.setModifyTime(Utils.currentTimeStamp());
            roleInfoMapper.insert(result);
            return SemanticConstants.RESP_SUC;
        }
        result.setExtend(roleInfo.getExtend());
        result.setDescription(roleInfo.getDescription());
        result.setModifyTime(Utils.currentTimeStamp());
        roleInfoMapper.insert(result);
        return SemanticConstants.RESP_SUC;
    }

    @Override
    public String deleteRole(Integer roleId) {
        RoleInfo deleteRole = new RoleInfo(roleId);
        RoleInfo roleInfo = roleInfoMapper.selectOne(deleteRole);
        if (roleInfo == null) {
            return SemanticConstants.RESP_SUC;
        }
        if (roleInfo.getName().equals(SemanticConstants.DEFAULT_ROLE)) {
            return SemanticConstants.ADMIN_CAN_NOT_BE_DELETED;
        }
        List<VisibleAttr> visibleAttrs = roleInfo.extractVisibleFromExtend();
        if (!visibleAttrs.isEmpty()) {
            String roleNotEmpty = SemanticConstants.ROLE_NOT_EMPTY.replaceAll("xxx", roleInfo.getName());
            throw new SemanticException(roleNotEmpty, ErrorCode.ROLE_MUST_NOT_EMPTY);
        }
        roleInfoMapper.delete(deleteRole);
        return SemanticConstants.RESP_SUC;
    }
    @Override
    public List<RoleInfo> getRoleInfoByPage(RoleInfo search, int pageNum, int pageSize) {
        return roleInfoMapper.selectAllRolesByPage(search, new RowBounds(pageNum, pageSize));
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public String addUserToRole(RoleInfo roleInfo, Integer roleId) throws SemanticException {
        String roleName = roleInfo.getName();
        RoleInfo select = new RoleInfo(roleName);
        RoleInfo result = roleInfoMapper.selectOne(select);
        // check whether role exists
        if (result == null) {
            return SemanticConstants.ROLE_NOT_EXISTS.replace("xxx", roleName);
        }
        if (!roleId.equals(result.getId())) {
            return SemanticConstants.ROLE_ID_NOT_MATCH_NAME;
        }
        List<VisibleAttr> visibleAttrs = result.extractVisibleFromExtend();
        List<VisibleAttr> insertAttr = roleInfo.extractVisibleFromExtend();
        if (insertAttr.size() != 1) {
            return SemanticConstants.ROLE_CONTAINS_ONE_USER;
        }

        if (insertAttr.get(0).getName().isEmpty() || !SemanticConstants.USER.equalsIgnoreCase(insertAttr.get(0).getType())) {
            return SemanticConstants.FORMAT_ERROR;
        }
        //check whether user exists
        UserInfo userInfo = userInfoMapper.selectByUserName(insertAttr.get(0).getName().toUpperCase());
        if (userInfo == null) {
            String userNotExists = SemanticConstants.USER_NOT_EXISTS.replaceAll("xxx", insertAttr.get(0).getName());
            return userNotExists;
        }

        //check whether role already contains this user
        for (VisibleAttr visibleAttr : visibleAttrs) {
            if (visibleAttr.getName().equalsIgnoreCase(insertAttr.get(0).getName())) {
                return SemanticConstants.ROLE_ALREADY_CONTAINS_USER;
            }
        }
        if (visibleAttrs.isEmpty() || visibleAttrs == null) {
            visibleAttrs = new LinkedList<VisibleAttr>();
        }
        visibleAttrs.add(insertAttr.get(0));
        String entityContains = new EntityContains().withContains(visibleAttrs).take();
        result.setExtend(entityContains);
        return updateRole(result, result.getId());
    }
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public String deleteUserFromRole(RoleInfo roleInfo, Integer roleId) throws SemanticException {
        String roleName = roleInfo.getName();
        RoleInfo select = new RoleInfo(roleName);
        RoleInfo result = roleInfoMapper.selectOne(select);
        if (result == null) {
            return SemanticConstants.ROLE_NOT_EXISTS.replace("xxx", roleName);
        }
        if (!roleId.equals(result.getId())) {
            return SemanticConstants.ROLE_ID_NOT_MATCH_NAME;
        }

        List<VisibleAttr> visibleAttrs = result.extractVisibleFromExtend();
        List<VisibleAttr> deleteAttr = roleInfo.extractVisibleFromExtend();
        if (deleteAttr.size() != 1) {
            return SemanticConstants.ROLE_CONTAINS_ONE_USER;
        }
        if (deleteAttr.get(0).getName().isEmpty() || !SemanticConstants.USER.equalsIgnoreCase(deleteAttr.get(0).getType())) {
            return SemanticConstants.FORMAT_ERROR;
        }
        boolean containsUser = false;
        for (VisibleAttr visibleAttr : visibleAttrs) {
            if (visibleAttr.getName().equalsIgnoreCase(deleteAttr.get(0).getName())) {
                containsUser = true;
                break;
            }
        }
        if (!containsUser) {
            return SemanticConstants.ROLE_NOT_CONTAINS_THIS_USER;
        }
        visibleAttrs.remove(deleteAttr.get(0));
        String entityContains = new EntityContains().withContains(visibleAttrs).take();
        result.setExtend(entityContains);
        return updateRole(result, result.getId());
    }

    // check whether name only contains chinese、letters、 numbers and _
    public boolean verifyRoleName(String roleName) {
        roleName = roleName.replaceAll(" ", "");
        roleName = roleName.replaceAll("^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$", "");
        if (roleName.isEmpty()) {
            return true;
        }
        return false;
    }

    // check whether type correct and  user exists
    public String verifyVisibleAttr(RoleInfo roleInfo, boolean checkUserFlag) {
        StringBuffer result = new StringBuffer("");
        boolean addCommaFlag = false;
        List<VisibleAttr> insertAttr = roleInfo.extractVisibleFromExtend();
        for (VisibleAttr visibleAttr : insertAttr) {
            if (!visibleAttr.getType().equals(SemanticConstants.USER)) {
                return SemanticConstants.FORMAT_ERROR;
            }
            if (!checkUserFlag) {
                continue;
            }
            UserInfo userInfo = userInfoMapper.selectByUserName(visibleAttr.getName().toUpperCase());
            if (userInfo == null) {
                if (!addCommaFlag) {
                    result.append(visibleAttr.getName());
                    addCommaFlag = true;
                    continue;
                }
                result.append(SemanticConstants.COMMA);
                result.append(visibleAttr.getName());
            }
        }
        return result.toString();
    }
}
