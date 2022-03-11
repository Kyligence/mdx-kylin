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
import { datasetHelper } from '../../utils';
import { restrictTypes, getFlatDatasetItems, filterColumn, filterMeasure, filterCMeasure, filterNamedSet } from './handler';

const { inbuildDatasetRoles: [ADMIN], nodeTypes } = configs;

export function getVisibleItemKey({ type, name }) {
  return `${type}-${name}`;
}

/* eslint-disable no-param-reassign */
function pushVisibleItem(visibleMap = {}, visibleItem = {}, datasetItem) {
  const key = getVisibleItemKey(visibleItem);

  if (!visibleMap[key]) {
    visibleMap[key] = { key, name: visibleItem.name, type: visibleItem.type, items: [datasetItem] };
  } else {
    visibleMap[key].items.push(datasetItem);
  }
}
/* eslint-enable */

function getIsAccess(visibleMapping, accessMapping, datasetItem) {
  const { key: itemKey, isVisible: isItemVisiable } = datasetItem;

  return visibleMapping[itemKey] &&
    datasetHelper.getIsAccessInKE(itemKey, accessMapping) &&
    isItemVisiable;
}

function filterAndMapDatasetItems(visibleMapping, accessMapping, datasetItems, filterMethod) {
  return datasetItems.reduce((results, datasetItem) => {
    if (filterMethod(datasetItem)) {
      const access = getIsAccess(visibleMapping, accessMapping, datasetItem);
      return [...results, { ...datasetItem, access }];
    }
    return results;
  }, []);
}

function filterAndSortVisibleItems(visibleList = [], filter) {
  return visibleList
    .filter(item => item.name.toLowerCase().includes(filter.toLowerCase()))
    .sort((itemA, itemB) => {
      if (itemA.name === 'Admin' && itemA.type === 'role') {
        return -1;
      }
      if (itemB.name === 'Admin' && itemB.type === 'role') {
        return 1;
      }
      return itemA.name.toLowerCase() < itemB.name.toLowerCase() ? -1 : 1;
    });
}

function polyfillAdminRestrict(restrictMap) {
  const hasAdmin = restrictMap[`${restrictTypes.ROLE}-${ADMIN}`];
  const roleAdminKey = `${restrictTypes.ROLE}-${ADMIN}`;
  const adminRestrict = {
    [roleAdminKey]: {
      key: `${restrictTypes.ROLE}-${ADMIN}`,
      name: ADMIN,
      type: restrictTypes.ROLE,
      items: [],
    },
  };

  return !hasAdmin ? { ...adminRestrict, ...restrictMap } : restrictMap;
}

/**
 * visibleMap = {
 *    'user-admin': [columns],
 *    'role-ADMIN': [columns],
 * }
 */
export const getVisibleRestrictListForColumn = createSelector(
  getFlatDatasetItems,
  state => state.filterRestrict,
  (datasetItems, filterRestrict) => {
    const visibleMap = {};

    for (const dimCol of datasetItems.column) {
      for (const visibleItem of dimCol.visible) {
        pushVisibleItem(visibleMap, visibleItem, dimCol);
      }
    }
    return filterAndSortVisibleItems(
      Object.values(polyfillAdminRestrict(visibleMap)), filterRestrict,
    );
  },
);

export const getVisibleRestrictListForMeasure = createSelector(
  getFlatDatasetItems,
  state => state.filterRestrict,
  (datasetItems, filterRestrict) => {
    const visibleMap = {};

    for (const measure of datasetItems.measure) {
      for (const visibleItem of measure.visible) {
        pushVisibleItem(visibleMap, visibleItem, measure);
      }
    }
    return filterAndSortVisibleItems(
      Object.values(polyfillAdminRestrict(visibleMap)), filterRestrict,
    );
  },
);

export const getVisibleRestrictListForCMeasure = createSelector(
  getFlatDatasetItems,
  state => state.filterRestrict,
  (datasetItems, filterRestrict) => {
    const visibleMap = {};

    for (const cMeasure of datasetItems.calculateMeasure) {
      for (const visibleItem of cMeasure.visible) {
        pushVisibleItem(visibleMap, visibleItem, cMeasure);
      }
    }
    return filterAndSortVisibleItems(
      Object.values(polyfillAdminRestrict(visibleMap)), filterRestrict,
    );
  },
);

export const getVisibleRestrictListForNamedSet = createSelector(
  getFlatDatasetItems,
  state => state.filterRestrict,
  (datasetItems, filterRestrict) => {
    const visibleMap = {};

    for (const namedSet of datasetItems.namedSet) {
      for (const visibleItem of namedSet.visible) {
        pushVisibleItem(visibleMap, visibleItem, namedSet);
      }
    }
    return filterAndSortVisibleItems(
      Object.values(polyfillAdminRestrict(visibleMap)), filterRestrict,
    );
  },
);

/* eslint-disable max-len */
export const getAccessListFromVisible = createSelector(
  getFlatDatasetItems,
  state => state.visibleList,
  state => state.tabType,
  state => state.accessMapping,
  state => state.filterAccess,
  (datasetItems, accessList, tabType, accessMapping, filterAccess) => {
    const visibleMapping = accessList.reduce((mapping, access) => ({
      ...mapping, [access.key]: true,
    }), {});

    switch (tabType) {
      // 当维度列不在黑名单中，KE中有权限，且不为不可见时
      case nodeTypes.COLUMN: return filterAndMapDatasetItems(
        visibleMapping,
        accessMapping,
        datasetItems.column,
        column => filterColumn(column, filterAccess),
      );
      // 当度量不在黑名单中，KE中有权限，且不为不可见时
      case nodeTypes.MEASURE: return filterAndMapDatasetItems(
        visibleMapping,
        accessMapping,
        datasetItems.measure,
        measure => filterMeasure(measure, filterAccess),
      );
      // 当计算度量不在黑名单中，KE中有权限，且不为不可见时
      case nodeTypes.CALCULATE_MEASURE: return filterAndMapDatasetItems(
        visibleMapping,
        accessMapping,
        datasetItems.calculateMeasure,
        cMeasure => filterCMeasure(cMeasure, filterAccess),
      );
      // 当命名集不在黑名单中，KE中有权限，且不为不可见时
      case nodeTypes.NAMEDSET: return filterAndMapDatasetItems(
        visibleMapping,
        accessMapping,
        datasetItems.namedSet,
        namedSet => filterNamedSet(namedSet, filterAccess),
      );
      default: return [];
    }
  },
);
/* eslint-enable */
