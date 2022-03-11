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
