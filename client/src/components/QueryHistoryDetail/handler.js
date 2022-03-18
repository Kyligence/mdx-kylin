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
