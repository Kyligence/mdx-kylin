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
import * as actionTypes from '../types';
import { storeHelper } from '../../utils';
import { IndicatorService } from '../../services';

function formatIndicatorResponse(response) {
  const { list, totalCount } = typeof response === 'string' ? JSON.parse(response) : response;
  return { data: list.map((item, id) => ({ ...item, id })), totalCount };
}

export function getIndicators(options = {}, data) {
  return (dispatch, getState) => dispatch(
    storeHelper.fetchDataWithPagination({
      getState,
      options,
      fetchApi: params => IndicatorService.fetchIndicators(params, data),
      formatResponse: formatIndicatorResponse,
      setDataAction: actionTypes.SET_INDICATOR,
      setLoadingAction: actionTypes.SET_INDICATOR_LOADING,
    }),
  );
}
