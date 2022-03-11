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
import { strings } from '../../constants';
import { dataHelper } from '../../utils';

export function warpperNone(intl, value) {
  return value !== undefined ? value : intl.formatMessage(strings.NONE);
}

export const detailLabelMap = {
  queryId: strings.QUERY_ID,
  executeTime: strings.EXECUTE_TIME,
  isCached: strings.USE_MDX_CACHE,
  isOtherQueryEngine: strings.USE_OTHER_ENGINE,
  networkSize: strings.NETWORK_PACKAGE,
  isTimeout: strings.TIMEOUT,
  multiDimDatasetTime: strings.MULTI_DIM_DATASET_TIME,
  transferTime: strings.TIME_FOR_MARSHALL_SOAP_MESSAGE,
  datasetName: strings.DATASET_NAME,
  isGateway: strings.USE_GATEWAY,

  id: strings.SYSTEM_ID,
  keQueryId: strings.KYLIN_QUERY_ID,
  sqlCacheUsed: strings.USE_SQL_CACHE,
  sqlExecutionTime: strings.SQL_EXECUTION_TIME,
  status: strings.SQL_QUERY_STATUS,
};

export const detailValueMap = {
  queryId: (value, intl) => warpperNone(intl, value),
  executeTime: (value, intl) => warpperNone(intl, dataHelper.getDurationTime(intl, value)),
  isCached: (value, intl) => intl.formatMessage(value ? strings.TRUE : strings.FALSE),
  isOtherQueryEngine: (value, intl) => intl.formatMessage(value ? strings.TRUE : strings.FALSE),
  networkSize: (value, intl) => warpperNone(intl, value),
  isTimeout: (value, intl) => intl.formatMessage(value ? strings.TRUE : strings.FALSE),
  multiDimDatasetTime: (value, intl) => warpperNone(intl, dataHelper.getDurationTime(intl, value)),
  transferTime: (value, intl) => warpperNone(intl, dataHelper.getDurationTime(intl, value)),
  datasetName: (value, intl) => warpperNone(intl, value),
  isGateway: (value, intl) => intl.formatMessage(value ? strings.TRUE : strings.FALSE),

  id: value => value,
  keQueryId: (value, intl) => warpperNone(intl, value),
  sqlCacheUsed: (value, intl) => intl.formatMessage(value ? strings.TRUE : strings.FALSE),
  sqlExecutionTime: (value, intl) => warpperNone(intl, dataHelper.getDurationTime(intl, value)),
  status: (value, intl) => intl.formatMessage(value ? strings.SUCCESS : strings.FAILURE),
};
