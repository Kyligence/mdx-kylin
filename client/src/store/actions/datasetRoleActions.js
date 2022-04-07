/*
  Copyright (C) 2021 Kyligence Inc. All rights reserved.

  http://kyligence.io

  This software is the confidential and proprietary information of
  Kyligence Inc. ("Confidential Information"). You shall not disclose
  such Confidential Information and shall use it only in accordance
  with the terms of the license agreement you entered into with
  Kyligence Inc.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
