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
import { createSelector } from 'reselect';

import { configs } from '../../constants';

const { nodeTypes: { CALCULATE_MEASURE, NAMEDSET, HIERARCHY } } = configs;

export function getDefaultState() {
  return {
    isShow: false,
    callback: () => {},
    errorList: [],
    dataset: null,
    form: {
      selectedKeys: [],
    },
    filter: {
      nodeType: [],
    },
  };
}

export const getSelectedCount = createSelector(
  state => state.datasetKeyMap,
  state => state.selectedKeys,
  (datasetKeyMap, selectedKeys) => {
    const selectedItems = selectedKeys
      .map(key => datasetKeyMap[key])
      .filter(item => !!item);

    const count = {
      [CALCULATE_MEASURE]: 0,
      [NAMEDSET]: 0,
      [HIERARCHY]: 0,
    };

    for (const item of selectedItems) {
      count[item.nodeType] += 1;
    }

    return count;
  },
);
