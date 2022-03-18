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

export function generatePackage(params, data, headers) {
  const url = templateUrl(apiUrls.GENERATE_DIAGNOSIS_PACKAGE, params);
  return axios.post(url, data, { headers });
}

export function fetchPackageState(params, headers) {
  const url = templateUrl(apiUrls.GET_DIAGNOSIS_PACKAGE_STATE, params);
  return axios.get(url, { headers });
}

export function stopPackage(params, headers) {
  const url = templateUrl(apiUrls.STOP_DIAGNOSIS_PACKAGE, params);
  return axios.get(url, { headers });
}

export function downloadPackage(params, headers) {
  const url = templateUrl(apiUrls.DOWNLOAD_DIAGNOSIS_PACKAGE, params);
  return axios.get(url, { headers, responseType: 'blob' });
}
