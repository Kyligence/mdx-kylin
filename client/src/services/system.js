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
