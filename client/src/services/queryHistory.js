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

export function fetchQueryHistory(params, data, { callback }) {
  const url = templateUrl(apiUrls.GET_QUERY_HISTORY, params);

  // 生成可取消的请求
  const source = axios.CancelToken.source();
  const { token: cancelToken } = source;
  callback(source);

  return axios.post(url, data, { cancelToken });
}

export function fetchQueryHistoryCluster(options) {
  const url = templateUrl(apiUrls.GET_QUERY_HISTORY_CLUSTER, null, options);
  return axios.get(url);
}

export function getQuerySqlByQueryId(params) {
  const url = templateUrl(apiUrls.GET_QUERY_SQLS, params);
  return axios.get(url);
}
