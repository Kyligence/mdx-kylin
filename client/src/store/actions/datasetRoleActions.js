/*
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
import * as actionTypes from '../types';
import { configs } from '../../constants';
import { storeHelper, businessHelper } from '../../utils';
import { DatasetRoleService } from '../../services';

let fetchAllData = null;

export function getAllRoles() {
  if (!fetchAllData) {
    fetchAllData = storeHelper.createFetchAllData(configs.batchDataCount.datasetRole);
  }
  return dispatch => (
    dispatch(fetchAllData({
      fetchApi: DatasetRoleService.fetchDatasetRoles,
      initListAction: actionTypes.INIT_DATASET_ROLE_LIST,
      pushDataAction: actionTypes.PUSH_DATASET_ROLE_LIST,
      setLoadingAction: actionTypes.SET_DATASET_ROLE_LIST_LOADING,
      setFullLoadingAction: actionTypes.SET_DATASET_ROLE_LIST_FULL_LOADING,
    }))
  );
}

export function initDatasetRole() {
  return { type: actionTypes.INIT_DATASET_ROLE };
}

export function setDatasetRole(datasetRole) {
  return { type: actionTypes.SET_DATASET_ROLE, datasetRole };
}

export function getRoleDetail(params) {
  return async dispatch => {
    let response = await DatasetRoleService.fetchRoleDetail(params);
    response = businessHelper.formatDatasetRoleDetail(response);

    const datasetRole = { ...response, id: params.datasetRoleId };
    dispatch(setDatasetRole(datasetRole));
    return datasetRole;
  };
}

/**
 * 创建数据集角色
 *
 * @param {*} params
 * @param {*} form
 */
export function createRole(params, form) {
  return async dispatch => {
    const response = await DatasetRoleService.createRole(params, form);
    const { role_id: roleId } = response;

    await dispatch(getAllRoles());

    return roleId;
  };
}

/**
 * 编辑数据集角色
 *
 * @param {*} params
 * @param {*} form
 */
export function editRole(params, form) {
  return async dispatch => {
    await DatasetRoleService.editRole(params, form);
    await dispatch(getAllRoles());
  };
}

/**
 * 通过Id删除某一数据集角色
 *
 * @param {*} datasetRoleId
 */
export function deleteRole(datasetRoleId) {
  return async dispatch => {
    await DatasetRoleService.deleteRole({ datasetRoleId });
    await dispatch(getAllRoles());
  };
}
