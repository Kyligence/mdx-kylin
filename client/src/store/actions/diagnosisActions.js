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
import { DiagnosisService } from '../../services';
import { domHelper } from '../../utils';

function getPackageInfo(startAt, endAt, logType) {
  return {
    start_at: Math.round(startAt / 1000),
    end_at: Math.round(endAt / 1000),
    log_type: logType,
  };
}

export function generatePackages({ dateRange, logType = 0, clusters }) {
  return () => {
    const data = getPackageInfo(dateRange[0], dateRange[1], logType);

    return Promise.all(clusters.map(({ host, port }) => {
      const headers = { 'X-Host': host, 'X-Port': port };
      return DiagnosisService.generatePackage(null, data, headers);
    }));
  };
}

export function getPackageState({ host, port }) {
  return async () => {
    const headers = { 'X-Host': host, 'X-Port': port };
    return DiagnosisService.fetchPackageState(null, headers);
  };
}

export function retryPackage({ host, port, startAt, endAt, logType = 0 }) {
  return async () => {
    const headers = { 'X-Host': host, 'X-Port': port };
    const data = getPackageInfo(startAt, endAt, logType);

    return DiagnosisService.generatePackage(null, data, headers);
  };
}

export function downloadPackage({ host, port, fileName }) {
  return async () => {
    const headers = { 'X-Host': host, 'X-Port': port };
    const result = await DiagnosisService.downloadPackage({ fileName }, headers);
    const url = window.URL.createObjectURL(new Blob([result]));
    domHelper.download.get(url, fileName);
  };
}
