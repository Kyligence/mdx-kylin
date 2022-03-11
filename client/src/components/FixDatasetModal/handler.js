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
