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

export function getInvisibleItemKey({ type, name }) {
  return `${type}-${name}`;
}

/* eslint-disable no-param-reassign, max-len */
function pushInvisibleItem(invisibleMap = {}, invisibleItem = {}, datasetItem) {
  const key = getInvisibleItemKey(invisibleItem);

  if (!invisibleMap[key]) {
    invisibleMap[key] = { key, name: invisibleItem.name, type: invisibleItem.type, items: [datasetItem] };
  } else {
    invisibleMap[key].items.push(datasetItem);
  }
}
/* eslint-enable */

function getIsAccess(invisibleMapping, accessMapping, datasetItem) {
  const { key: itemKey, isVisible: isItemVisiable } = datasetItem;

  return !invisibleMapping[itemKey] &&
    datasetHelper.getIsAccessInKE(itemKey, accessMapping) &&
    isItemVisiable;
}

function filterAndMapDatasetItems(invisibleMapping, accessMapping, datasetItems, filterMethod) {
  return datasetItems.reduce((results, datasetItem) => {
    if (filterMethod(datasetItem)) {
      const access = getIsAccess(invisibleMapping, accessMapping, datasetItem);
      return [...results, { ...datasetItem, access }];
    }
    return results;
  }, []);
}

function filterAndSortInvisibleItems(invisibleList = [], filter) {
  return invisibleList
    .filter(item => item.name.toLowerCase().includes(filter.toLowerCase()))
    .sort((itemA, itemB) => (itemA.name.toLowerCase() < itemB.name.toLowerCase() ? -1 : 1));
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
export const getInvisibleRestrictListForColumn = createSelector(
  getFlatDatasetItems,
  state => state.filterRestrict,
  (datasetItems, filterRestrict) => {
    const invisibleMap = {};

    for (const dimCol of datasetItems.column) {
      for (const invisibleItem of dimCol.invisible) {
        pushInvisibleItem(invisibleMap, invisibleItem, dimCol);
      }
    }
    return filterAndSortInvisibleItems(
      Object.values(polyfillAdminRestrict(invisibleMap)), filterRestrict,
    );
  },
);

export const getInvisibleRestrictListForMeasure = createSelector(
  getFlatDatasetItems,
  state => state.filterRestrict,
  (datasetItems, filterRestrict) => {
    const invisibleMap = {};

    for (const measure of datasetItems.measure) {
      for (const invisibleItem of measure.invisible) {
        pushInvisibleItem(invisibleMap, invisibleItem, measure);
      }
    }
    return filterAndSortInvisibleItems(
      Object.values(polyfillAdminRestrict(invisibleMap)), filterRestrict,
    );
  },
);

export const getInvisibleRestrictListForCMeasure = createSelector(
  getFlatDatasetItems,
  state => state.filterRestrict,
  (datasetItems, filterRestrict) => {
    const invisibleMap = {};

    for (const cMeasure of datasetItems.calculateMeasure) {
      for (const invisibleItem of cMeasure.invisible) {
        pushInvisibleItem(invisibleMap, invisibleItem, cMeasure);
      }
    }
    return filterAndSortInvisibleItems(
      Object.values(polyfillAdminRestrict(invisibleMap)), filterRestrict,
    );
  },
);

export const getInvisibleRestrictListForNamedSet = createSelector(
  getFlatDatasetItems,
  state => state.filterRestrict,
  (datasetItems, filterRestrict) => {
    const invisibleMap = {};

    for (const namedSet of datasetItems.namedSet) {
      for (const invisibleItem of namedSet.invisible) {
        pushInvisibleItem(invisibleMap, invisibleItem, namedSet);
      }
    }
    return filterAndSortInvisibleItems(
      Object.values(polyfillAdminRestrict(invisibleMap)), filterRestrict,
    );
  },
);

/* eslint-disable max-len */
export const getAccessListFromInvisible = createSelector(
  getFlatDatasetItems,
  state => state.invisibleList,
  state => state.tabType,
  state => state.accessMapping,
  state => state.filterAccess,
  (datasetItems, accessList, tabType, accessMapping, filterAccess) => {
    const invisibleMapping = accessList.reduce((mapping, access) => ({
      ...mapping, [access.key]: true,
    }), {});

    switch (tabType) {
      // 当维度列不在黑名单中，KE中有权限，且不为不可见时
      case nodeTypes.COLUMN: return filterAndMapDatasetItems(
        invisibleMapping,
        accessMapping,
        datasetItems.column,
        column => filterColumn(column, filterAccess),
      );
      // 当度量不在黑名单中，KE中有权限，且不为不可见时
      case nodeTypes.MEASURE: return filterAndMapDatasetItems(
        invisibleMapping,
        accessMapping,
        datasetItems.measure,
        measure => filterMeasure(measure, filterAccess),
      );
      // 当计算度量不在黑名单中，KE中有权限，且不为不可见时
      case nodeTypes.CALCULATE_MEASURE: return filterAndMapDatasetItems(
        invisibleMapping,
        accessMapping,
        datasetItems.calculateMeasure,
        calculateMeasure => filterCMeasure(calculateMeasure, filterAccess),
      );
      // 当命名集不在黑名单中，KE中有权限，且不为不可见时
      case nodeTypes.NAMEDSET: return filterAndMapDatasetItems(
        invisibleMapping,
        accessMapping,
        datasetItems.namedSet,
        namedSet => filterNamedSet(namedSet, filterAccess),
      );
      default: return [];
    }
  },
);
/* eslint-enable */
