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
