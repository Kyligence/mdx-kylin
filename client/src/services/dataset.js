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
import { download } from '../utils/domHelper';

export function fetchDatasets(params) {
  const url = templateUrl(apiUrls.GET_DATASETS, params);
  return axios.get(url);
}

export function fetchSemanticModelInfo(params, data, options) {
  const url = templateUrl(apiUrls.GET_SEMANTIC_MODEL_INFO, params, options);
  return axios.get(url);
}

export function fetchProjectModels(params, data, options) {
  const url = templateUrl(apiUrls.GET_PROJECT_MODELS, params, options);
  return axios.get(url);
}

export function fetchModelTables(params) {
  const url = templateUrl(apiUrls.GET_MODEL_TABLES, params);
  return axios.get(url);
}

export function checkDatasetName(params) {
  const url = templateUrl(apiUrls.CHECK_DATASET_NAME, params);
  return axios.get(url);
}

export function saveDatasetJson(params, data) {
  const url = templateUrl(apiUrls.SAVE_DATASET, params);
  return axios.post(url, data);
}

export function updateDatasetJson(params, data) {
  const url = templateUrl(apiUrls.UPDATE_DATASET, params);
  return axios.put(url, data);
}

export function deleteDataset(params) {
  const url = templateUrl(apiUrls.DELETE_DATASET, params);
  return axios.delete(url);
}

export function fetchDatasetJson(params) {
  const url = templateUrl(apiUrls.GET_DATASET, params);
  return axios.get(url);
}

export function validateCMeasureExpressionsForMDX(params, data) {
  const url = templateUrl(apiUrls.VALIDATE_ALL_CMEASURE_EXPRESSION, params);
  return axios.post(url, data);
}

export function validateNamedSetExpressionForMDX(params, data) {
  const url = templateUrl(apiUrls.VALIDATE_ALL_NAMEDSET_EXPRESSION, params);
  return axios.post(url, data);
}

export function validateDefaultMemberForMDX(params, data) {
  const url = templateUrl(apiUrls.VALIDATE_ALL_DEFAULT_MEMBER, params);
  return axios.post(url, data);
}

export function fetchDatasetKeAccesses(params, data, { getState }) {
  const url = templateUrl(apiUrls.GET_KE_ACCESS_FROM_DATASET, params, {
    getState,
  });
  return axios.post(url, data);
}

export function previewFormatSample(data) {
  const url = templateUrl(apiUrls.PREVIEW_FORMAT_SAMPLE);
  return axios.post(url, data);
}

export function generatePackage(data) {
  const url = templateUrl(apiUrls.GENERATE_DATASET_PACKAGE);
  return axios.post(url, data);
}

export async function downloadPackage(token, projectName, size, host, port, onDownload) {
  return new Promise((resolve, reject) => {
    function onDownloadProgress(event) {
      if (onDownload) {
        onDownload(event, event.loaded / event.total);
      }
      if (event.loaded >= event.total) {
        resolve();
      }
    }

    async function process() {
      try {
        const url = templateUrl(apiUrls.DOWNLOAD_DATASET_PACKAGE, { token, projectName });
        const blob = await axios.get(url, {
          responseType: 'blob',
          onDownloadProgress,
          headers: {
            'X-Host': host,
            'X-Port': port,
          },
        });

        let fileName;

        // eslint-disable-next-line no-underscore-dangle
        for (const [header, value] of Object.entries(blob.__headers())) {
          if (header.toLocaleLowerCase() === 'content-disposition') {
            fileName = value
              .replace(/^attachment; *filename="/, '')
              .replace(/"$/, '');
            fileName = decodeURIComponent(fileName);
          }
        }

        if ('msSaveBlob' in window.navigator) {
          window.navigator.msSaveBlob(blob, fileName);
        } else {
          download.get(window.URL.createObjectURL(blob), fileName);
        }
      } catch (e) {
        reject(e);
      }
    }

    process();
  });
}

export function validateBroken(data) {
  const url = templateUrl(apiUrls.VALIDATE_DATASET_BROKEN);
  return axios.post(url, data);
}

export function uploadDatasetPackage(data) {
  const url = templateUrl(apiUrls.UPLOAD_DATASET_PACKAGE);
  return axios.post(url, data);
}

export function importDatasetPackage(data, host, port) {
  const url = templateUrl(apiUrls.IMPORT_DATASET_BY_PACKAGE);
  const headers = { 'X-Host': host, 'X-Port': port };
  return axios.put(url, data, { headers });
}
