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

export function fetchLocales(params) {
  switch (params.locale) {
    case 'zh': return import('../locale/zh.json');
    case 'en':
    default: return import('../locale/en.json');
  }
}

export function login(params, data) {
  const url = templateUrl(apiUrls.LOGIN, params);
  const { basicAuth } = data;
  const headers = { Authorization: `Basic ${basicAuth}` };
  return axios.get(url, { headers });
}

export function logout(params) {
  const url = templateUrl(apiUrls.LOGOUT, params);
  return axios.get(url);
}

export function logoutAAD(params) {
  return new Promise(resolve => {
    const logoutAADiFrame = document.createElement('iframe');

    const handleLogoutSuccess = () => {
      resolve();
      logoutAADiFrame.removeEventListener('load', handleLogoutSuccess);
      document.body.removeChild(logoutAADiFrame);
    };

    logoutAADiFrame.src = params.logoutUrl;
    logoutAADiFrame.addEventListener('load', handleLogoutSuccess);
    document.body.appendChild(logoutAADiFrame);
  });
}

export function fetchCurrentUser(params) {
  const url = templateUrl(apiUrls.GET_CURRENT_USER, params);
  return axios.get(url);
}

export function fetchAADSettings(params) {
  const url = templateUrl(apiUrls.GET_AAD_SETTINGS, params);
  return axios.get(url);
}

export function fetchLicense(params) {
  const url = templateUrl(apiUrls.GET_LICENSE, params);
  return axios.get(url);
}

export function fetchPermission(params) {
  const url = templateUrl(apiUrls.GET_PERMISSION, params);
  return axios.get(url);
}

export function fetchClustersInfo(params) {
  const url = templateUrl(apiUrls.GET_CLUSTERS_INFO, params);
  return axios.get(url);
}

export function fetchConfigurations(params) {
  const url = templateUrl(apiUrls.GET_CONFIGURATIONS, params);
  return axios.get(url);
}

export function updateConfigurations(params, data) {
  const url = templateUrl(apiUrls.UPDATE_CONFIGURATIONS, params);
  return axios.put(url, data, { headers: { 'X-Error-Silence': true } });
}

export function restartSyncTask(params) {
  const url = templateUrl(apiUrls.RESTART_SYNC_TASK, params);
  return axios.post(url, null);
}

// 未来如果需要计算"较昨日"，前端可以通过api去计算
// 统计API
export function fetchStatisticBasic(params, { getState }) {
  const url = templateUrl(apiUrls.GET_STATISTICS_BASIC, params, { getState });
  return axios.get(url);
}

export function fetchStatisticTrend(params, data, { getState, callback = () => {} }) {
  const url = templateUrl(apiUrls.GET_STATISTICS_TREND, params, { getState });

  // 生成可取消的请求
  const source = axios.CancelToken.source();
  const { token: cancelToken } = source;
  callback(source);

  return axios.post(url, data, { cancelToken });
}

export function fetchStatisticQueryCost(params, data, { getState }) {
  const url = templateUrl(apiUrls.GET_STATISTICS_QUERY_COST, params, { getState });
  return axios.post(url, data);
}

export function fetchStatisticRanking(params, { getState }) {
  const url = templateUrl(apiUrls.GET_STATISTICS_RANKING, params, { getState });
  return axios.get(url);
}
