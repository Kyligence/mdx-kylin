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
import { ProjectService, SystemService } from '../../services';
import { storeHelper } from '../../utils';
import { configs } from '../../constants';

function formatProjectResponse(response = {}, permissionMap = {}) {
  const data = response.map(name => ({
    name, access: permissionMap[name],
  }));
  const totalCount = data.length;
  return { totalCount, data };
}

export function getProjects() {
  return async dispatch => {
    const permissionMap = await SystemService.fetchPermission();

    await dispatch(storeHelper.fetchDataWithPagination({
      options: {
        pageOffset: 0,
        pageSize: configs.pageCount.projectList,
        withLocation: false,
      },
      formatResponse: response => formatProjectResponse(response, permissionMap),
      fetchApi: ProjectService.fetchProjects,
      setDataAction: actionTypes.SET_PROJECT_LIST,
      setLoadingAction: actionTypes.SET_PROJECT_LIST_LOADING,
    }));
  };
}

export function clearProjects() {
  return { type: actionTypes.CLEAR_PROJECT_LIST };
}
