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
import {
  startDatasetUpgrading,
  finishDatasetUpgrading,
  showDatasetUpgradeSuccess,
  getAllDatasets,
  upgradingDataset,
} from './upgradeDatasets';
import { configs } from '../../../constants';

/* eslint-disable no-await-in-loop */
export function upgradeAllDatasets() {
  return async dispatch => {
    await dispatch(startDatasetUpgrading());

    const { data } = await dispatch(getAllDatasets());
    const lastedVersion = configs.getDatasetVersion();
    const datasets = data.filter(dataset => dataset.front_v !== lastedVersion);

    for (let i = 0; i < datasets.length; i += 1) {
      await dispatch(upgradingDataset(datasets[i].id, i + 1, datasets.length));
    }

    await dispatch(showDatasetUpgradeSuccess());
    await dispatch(finishDatasetUpgrading());
  };
}
