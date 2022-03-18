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
import { batch } from 'react-redux';

import { strings } from '../../../constants';
import * as GlobalActions from '../globalActions';
import * as DatasetActions from '../datasetActions';

export function getAllDatasets() {
  return async (dispatch, getState) => {
    const options = { pageOffset: 0, pageSize: 999999, withLocation: false, project: null };
    await dispatch(DatasetActions.getDatasets(options));
    return getState().data.datasetList;
  };
}

export function startDatasetUpgrading() {
  return dispatch => {
    batch(() => {
      dispatch(GlobalActions.toggleFlag('isShowGlobalMask', true));
      dispatch(GlobalActions.setGlobalMaskMessage(strings.UPGRADE_ALL_DATASETS_START));
    });
  };
}

export function finishDatasetUpgrading() {
  return dispatch => {
    batch(() => {
      dispatch(GlobalActions.toggleFlag('isShowGlobalMask', false));
      dispatch(GlobalActions.setGlobalMaskMessage(null));
    });
  };
}

export function showDatasetUpgradeSuccess() {
  return dispatch => new Promise(resolve => {
    dispatch(GlobalActions.setGlobalMaskMessage(strings.UPGRADE_ALL_DATASET_SUCCESS));
    setTimeout(() => resolve(), 3000);
  });
}

export function showDatasetUpgrading(currentCount, totalCount) {
  return dispatch => {
    const params = { currentCount, totalCount };
    return dispatch(GlobalActions.setGlobalMaskMessage(strings.UPGRADING_DATASET, params));
  };
}

export function upgradingDataset(datasetId, currentCount, totalCount) {
  return async dispatch => {
    await dispatch(showDatasetUpgrading(currentCount, totalCount));
    await dispatch(DatasetActions.setDatasetStore(datasetId));
    await dispatch(DatasetActions.updateDatasetJson(datasetId));
  };
}
