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
import axios from 'axios';
import { apiUrls } from '../constants';
import templateUrl from '../utils/templateUrl';

export function fetchDatasetRoles(params) {
  const url = templateUrl(apiUrls.GET_DATASET_ROLES, params);
  return axios.get(url);
}

export function fetchRoleDetail(params) {
  const url = templateUrl(apiUrls.GET_DATASET_ROLE, params);
  return axios.get(url);
}

export function createRole(params, data) {
  const url = templateUrl(apiUrls.CREATE_DATASET_ROLE, params);
  return axios.post(url, data);
}

export function editRole(params, data) {
  const url = templateUrl(apiUrls.EDIT_DATASET_ROLE, params);
  return axios.put(url, data);
}

export function deleteRole(params) {
  const url = templateUrl(apiUrls.DELETE_DATASET_ROLE, params);
  return axios.delete(url);
}
