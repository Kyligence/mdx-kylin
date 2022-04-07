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
import React from 'react';
import { createSelector } from 'reselect';
import classnames from 'classnames';

import { strings, configs } from '../../constants';
import { dataHelper, datasetHelper, businessHelper } from '../../utils';

export function getInitState() {
  return {
    filterRestrict: {
      column: '',
      measure: '',
      calculateMeasure: '',
      namedSet: '',
    },
    filterAccess: {
      column: '',
      measure: '',
      calculateMeasure: '',
      namedSet: '',
    },
    isLoading: false,
    accessMapping: {},
    isKeAccessLoading: false,
    selectedRestrictKey: null,
    pageOffset: 0,
    pageSize: 10,
  };
}

/* eslint-disable react/jsx-filename-extension */
function renderDatasetItemAccess(row) {
  const accessIconClass = classnames(row.access ? 'icon-superset-eye' : 'icon-superset-eye_close');
  return <i className={accessIconClass} />;
}
/* eslint-enable */

function getAccessRowClassName(row) {
  return classnames(row.access ? 'access-visible' : 'access-invisible');
}

export const getTabsConfig = createSelector(
  state => state.intl,
  datasetHelper.getDatasetAliasMap,
  (intl, datasetAliasMap) => ({
    column: {
      tabType: configs.nodeTypes.COLUMN,
      tabText: intl.formatMessage(strings.DIMENSION),
      tabTexts: intl.formatMessage(strings.DIMENSIONS),
      headerText: intl.formatMessage(strings.DIMENSIONS_ACCESS_LIST),
      placeholder: intl.formatMessage(strings.SEARCH_BY) + dataHelper.getHumanizeJoinString(intl, [
        intl.formatMessage(strings.DIMENSION).toLowerCase(),
        intl.formatMessage(strings.TABLE).toLowerCase(),
      ], strings.BETWEEN_OR),
      rowClassName: getAccessRowClassName,
      columns: [
        { label: '', prop: 'access', render: renderDatasetItemAccess, width: '35px' },
        { label: intl.formatMessage(strings.DIMENSION_NAME), prop: 'alias' },
        { label: intl.formatMessage(strings.TABLE_NAME), render: row => datasetAliasMap && datasetAliasMap[`${row.model}.${row.table}`] },
      ],
    },
    measure: {
      tabType: configs.nodeTypes.MEASURE,
      tabText: intl.formatMessage(strings.MEASURE),
      tabTexts: intl.formatMessage(strings.MEASURES),
      headerText: intl.formatMessage(strings.MEASURES_ACCESS_LIST),
      placeholder: intl.formatMessage(strings.SEARCH_BY) +
        intl.formatMessage(strings.MEASURE).toLowerCase(),
      rowClassName: getAccessRowClassName,
      columns: [
        { label: '', prop: 'access', render: renderDatasetItemAccess, width: '35px' },
        { label: intl.formatMessage(strings.MEASURE_NAME), prop: 'alias' },
      ],
    },
    calculateMeasure: {
      tabType: configs.nodeTypes.CALCULATE_MEASURE,
      tabText: intl.formatMessage(strings.CALCULATED_MEASURE),
      tabTexts: intl.formatMessage(strings.CALCULATED_MEASURES),
      headerText: intl.formatMessage(strings.CMEASURES_ACCESS_LIST),
      placeholder: intl.formatMessage(strings.SEARCH_BY) +
        intl.formatMessage(strings.CALCULATED_MEASURE).toLowerCase(),
      rowClassName: getAccessRowClassName,
      columns: [
        { label: '', prop: 'access', render: renderDatasetItemAccess, width: '35px' },
        { label: intl.formatMessage(strings.CALCULATED_MEASURE_NAME), prop: 'name' },
      ],
    },
    namedSet: {
      tabType: configs.nodeTypes.NAMEDSET,
      tabText: intl.formatMessage(strings.NAMEDSET),
      tabTexts: intl.formatMessage(strings.NAMEDSETS),
      headerText: intl.formatMessage(strings.NAMED_SET_ACCESS_LIST),
      placeholder: intl.formatMessage(strings.SEARCH_BY) +
        intl.formatMessage(strings.NAMEDSET).toLowerCase(),
      rowClassName: getAccessRowClassName,
      columns: [
        { label: '', prop: 'access', render: renderDatasetItemAccess, width: '35px' },
        { label: intl.formatMessage(strings.NAMEDSET_NAME), prop: 'name' },
      ],
    },
  }),
);

export const restrictTypes = {
  USER: 'user',
  ROLE: 'role',
  GROUP: 'group',
};

export const getFlatDatasetItems = createSelector(
  state => state.dataset,
  dataset => {
    const allColumns = [];
    const allMeasures = [];
    const allCMeasures = [];
    const allNamedSets = [];

    for (const model of dataset.models) {
      for (const table of model.dimensionTables) {
        for (const dimCol of table.dimCols) {
          allColumns.push(dimCol);
        }
      }
      for (const measure of model.measures) {
        allMeasures.push(measure);
      }
    }

    for (const cMeasure of dataset.calculateMeasures) {
      allCMeasures.push(cMeasure);
    }
    for (const namedSet of dataset.namedSets) {
      allNamedSets.push(namedSet);
    }

    return {
      column: allColumns,
      measure: allMeasures,
      calculateMeasure: allCMeasures,
      namedSet: allNamedSets,
    };
  },
);

export const listUrl = businessHelper.findSiteByName('listDataset', configs.sitemap).url;

export function filterColumn(column = [], filterAccess) {
  return column.alias.toLowerCase().includes(filterAccess.toLowerCase());
}

export function filterMeasure(measure = [], filterAccess) {
  return measure.alias.toLowerCase().includes(filterAccess.toLowerCase());
}

export function filterCMeasure(cMeasure = [], filterAccess) {
  return cMeasure.name.toLowerCase().includes(filterAccess.toLowerCase());
}

export function filterNamedSet(namedSet = [], filterAccess) {
  return namedSet.name.toLowerCase().includes(filterAccess.toLowerCase());
}
