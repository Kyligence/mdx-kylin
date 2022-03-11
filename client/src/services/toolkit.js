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
