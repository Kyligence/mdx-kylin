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
