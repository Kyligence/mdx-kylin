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
import { download } from '../utils/domHelper';

export async function repairSchema(formData, onUpload, onDownload) {
  let fileName = 'repaired_file';

  const blob = await axios.post(
    apiUrls.REPAIR_SCHEMA, formData,
    { responseType: 'blob', onUploadProgress: onUpload, onDownloadProgress: onDownload },
  );

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
    const url = window.URL.createObjectURL(blob);
    download.get(url, fileName);
  }
}

export function getTookitConfigurations() {
  return axios.get(apiUrls.GET_TOOLKIT_CONFIGURATION);
}

export function checkSchemaSource(formData) {
  return axios.post(apiUrls.CHECK_SCHEMA_SOURCE, formData);
}
