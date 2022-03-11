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
import React, { Fragment } from 'react';
import classnames from 'classnames';
import { Tooltip } from 'kyligence-ui-react';
import { createSelector } from 'reselect';

import { configs, strings } from '../../constants';
import { dataHelper, datasetHelper } from '../../utils';

function sortOptions(options = []) {
  return options.sort(({ label: labelA }, { label: labelB }) => (
    labelA.toLowerCase() < labelB.toLowerCase() ? -1 : 1
  ));
}

function filterUsedOptions(usedOptions = [], options = []) {
  return options.filter(option => (
    !usedOptions.find(usedOption => option.value === usedOption.value)
  ));
}

export function getDefaultState() {
  return {
    isShow: false,
    isEdit: false,
    isLoading: false,
    nodeType: 'column',
    pageOffset: 0,
    pageSize: 5,
    selectedItem: null,
    filterAccess: '',
    filterRestrict: '',
    callback: () => {},
    usedRestricts: [],
    allUsers: [],
    allRoles: [],
    accessMapping: {},
    form: {
      key: '',
      name: '',
      type: 'user',
      items: [],
    },
  };
}

export function getRestrictTypes({ intl }) {
  return {
    user: intl.formatMessage(strings.USER),
    group: intl.formatMessage(strings.USER_GROUP),
    role: intl.formatMessage(strings.ROLE),
  };
}

export function getRestrictTypeOptions({ intl }) {
  return Object.entries(getRestrictTypes({ intl }))
    .map(([value, label]) => ({ label, value }));
}

export const getRoleOptions = createSelector(
  state => state.roleList.data,
  roleList => sortOptions([
    ...configs.inbuildDatasetRoles.map(roleName => ({ label: roleName, value: roleName })),
    ...roleList.map(role => ({ label: role.name, value: role.name })),
  ]),
);

export const getUserOptions = createSelector(
  state => state.userList.data,
  userList => sortOptions(
    userList.map(user => ({ label: user.name, value: user.name })),
  ),
);

export const getUserGroupOptions = createSelector(
  state => state.userGroupList.data,
  groupList => sortOptions(
    groupList.map(group => ({ label: group.name, value: group.name })),
  ),
);

export const getUsedOptions = createSelector(
  state => state.restrictType,
  state => state.usedRestricts,
  (restrictType, usedRestricts) => usedRestricts
    .filter(restrict => restrict.type === restrictType)
    .map(restrict => ({ label: restrict.name, value: restrict.name })),
);

export const getRestrictOptions = createSelector(
  state => state.restrictType,
  getUsedOptions,
  getRoleOptions,
  getUserOptions,
  getUserGroupOptions,
  (restrictType, usedOptions, roleOptions, userOptions, groupOptions) => {
    switch (restrictType) {
      case 'user': return filterUsedOptions(usedOptions, userOptions);
      case 'role': return filterUsedOptions(usedOptions, roleOptions);
      case 'group': return filterUsedOptions(usedOptions, groupOptions);
      default: return [];
    }
  },
);

export const getFilteredRestrictOptions = createSelector(
  getRestrictOptions,
  state => state.filterRestrict,
  (restrictOptions, filterRestrict) => restrictOptions.filter(
    option => option.label.toLowerCase().includes(filterRestrict.toLowerCase()),
  ),
);

/* eslint-disable react/jsx-filename-extension */
function renderSelectionHeader(column, renderData, dataset) {
  const { isChecked, isDisabled, hasSelection, handleSelection } = renderData;
  const isVisibleMode = !dataset.access;
  const className = isVisibleMode
    ? classnames(
      'table-header-icon',
      !hasSelection && !isChecked && 'icon-superset-eye_close',
      isChecked && 'icon-superset-eye',
      hasSelection && !isChecked && 'icon-superset-half_eye',
      isDisabled && 'disabled',
    )
    : classnames(
      'table-header-icon',
      isChecked && 'icon-superset-eye_close',
      !hasSelection && !isChecked && 'icon-superset-eye',
      hasSelection && !isChecked && 'icon-superset-half_eye',
      isDisabled && 'disabled',
    );
  return (
    <div className="visibility-selection" onClick={handleSelection}>
      <i className={className} />
    </div>
  );
}

function renderSelection(row, column, index, renderData, dataset) {
  const { isSelected, isDisabled, handleToggleRowSelection } = renderData;
  const isVisibleMode = !dataset.access;
  const className = isVisibleMode
    // 白名单模式下，未选中或者禁用的列为不可见
    ? classnames((!isSelected || isDisabled) ? 'icon-superset-eye_close' : 'icon-superset-eye', isDisabled && 'disabled')
    // 黑名单模式下，选中或者禁用的列为不可见
    : classnames((isSelected || isDisabled) ? 'icon-superset-eye_close' : 'icon-superset-eye', isDisabled && 'disabled');
  return (
    <div className="visibility-selection" onClick={!isDisabled ? handleToggleRowSelection : undefined}>
      <i className={className} />
    </div>
  );
}

function selectable(row, accessMapping) {
  const isAccessInKE = datasetHelper.getIsAccessInKE(row.key, accessMapping);

  return row.isVisible && isAccessInKE;
}

/* eslint-disable max-len */
export function getDialogConfigs({ intl, accessMapping, dataset }) {
  const render = (...args) => renderSelection(...args, dataset);
  const renderHeader = (...args) => renderSelectionHeader(...args, dataset);
  return {
    column: {
      dialogTitle: (
        <Fragment>
          <span className="restrict-title">
            {intl.formatMessage(strings.GRANT_ACCESS_FOR_ENTITY, { entity: intl.formatMessage(strings.DIMENSION) })}
          </span>
          <Tooltip
            className="restrict-tip-icon"
            content={intl.formatMessage(strings.ACCESS_LIST_TITLE_TIP)}
            placement="right"
            popperClass="restrict-tip"
          >
            <i className="icon-superset-what" />
          </Tooltip>
        </Fragment>
      ),
      entityName: intl.formatMessage(strings.DIMENSION),
      renderContentHeader(selectedItem, tableRows) {
        return selectedItem && tableRows.length
          ? dataHelper.translate(intl, selectedItem.alias || selectedItem.label)
          : [];
      },
      filterPlaceholder: intl.formatMessage(strings.SEARCH_DIMENSION_OR_COLUMN),
      columns: [
        { type: 'selection', renderHeader, render, selectable: row => selectable(row, accessMapping) },
        { label: intl.formatMessage(strings.DIMENSION_NAME), prop: 'alias' },
        { label: intl.formatMessage(strings.KEY_COLUMN), prop: 'name' },
        { label: intl.formatMessage(strings.TYPE), prop: 'dataType' },
      ],
      getFilters(filter) {
        return [{ name: filter }, { alias: filter }];
      },
    },
    measure: {
      dialogTitle: (
        <Fragment>
          <span className="restrict-title">
            {intl.formatMessage(strings.GRANT_ACCESS_FOR_ENTITY, { entity: intl.formatMessage(strings.MEASURE) })}
          </span>
          <Tooltip
            className="restrict-tip-icon"
            content={intl.formatMessage(strings.ACCESS_LIST_TITLE_TIP)}
            placement="right"
            popperClass="restrict-tip"
          >
            <i className="icon-superset-what" />
          </Tooltip>
        </Fragment>
      ),
      entityName: intl.formatMessage(strings.MEASURE),
      renderContentHeader(selectedItem, tableRows) {
        return selectedItem && tableRows.length
          ? dataHelper.translate(intl, selectedItem.alias || selectedItem.label)
          : [];
      },
      filterPlaceholder: intl.formatMessage(strings.SEARCH_MEASURE_NAME),
      columns: [
        { type: 'selection', renderHeader, render, selectable: row => selectable(row, accessMapping) },
        { label: intl.formatMessage(strings.MEASURE_NAME), prop: 'alias' },
        { label: intl.formatMessage(strings.EXPRESSION), prop: 'fullExpression' },
      ],
      getFilters(filter) {
        return [{ alias: filter }];
      },
    },
    calculateMeasure: {
      dialogTitle: (
        <Fragment>
          <span className="restrict-title">
            {intl.formatMessage(strings.GRANT_ACCESS_FOR_ENTITY, { entity: intl.formatMessage(strings.CALCULATED_MEASURE) })}
          </span>
          <Tooltip
            className="restrict-tip-icon"
            content={intl.formatMessage(strings.ACCESS_LIST_TITLE_TIP)}
            placement="right"
            popperClass="restrict-tip"
          >
            <i className="icon-superset-what" />
          </Tooltip>
        </Fragment>
      ),
      entityName: intl.formatMessage(strings.CALCULATED_MEASURE),
      renderContentHeader(selectedItem, tableRows) {
        return selectedItem && tableRows.length
          ? dataHelper.translate(intl, selectedItem.alias || selectedItem.label)
          : '';
      },
      filterPlaceholder: intl.formatMessage(strings.SEARCH_CMEASURE_NAME),
      columns: [
        { type: 'selection', renderHeader, render, selectable: row => selectable(row, accessMapping) },
        { label: intl.formatMessage(strings.CALCULATED_MEASURE_NAME), prop: 'name' },
        { label: intl.formatMessage(strings.EXPRESSION), prop: 'expression' },
      ],
      getFilters(filter) {
        return [{ name: filter }];
      },
    },
    namedSet: {
      dialogTitle: (
        <Fragment>
          <span className="restrict-title">
            {intl.formatMessage(strings.GRANT_ACCESS_FOR_ENTITY, { entity: intl.formatMessage(strings.NAMEDSET) })}
          </span>
          <Tooltip
            className="restrict-tip-icon"
            content={intl.formatMessage(strings.ACCESS_LIST_TITLE_TIP)}
            placement="right"
            popperClass="restrict-tip"
          >
            <i className="icon-superset-what" />
          </Tooltip>
        </Fragment>
      ),
      entityName: intl.formatMessage(strings.NAMEDSET),
      // 展示命名集所属的model或者全局命名集名称
      renderContentHeader(selectedItem, tableRows) {
        return selectedItem && tableRows.length
          ? dataHelper.translate(
            intl,
            (selectedItem.model && selectedItem.table && `${selectedItem.model}.${selectedItem.table}`) ||
            (selectedItem.label),
          )
          : '';
      },
      filterPlaceholder: intl.formatMessage(strings.SEARCH_NAMEDSET_NAME),
      columns: [
        { type: 'selection', renderHeader, render, selectable: row => selectable(row, accessMapping) },
        { label: intl.formatMessage(strings.NAMEDSET_NAME), prop: 'name' },
        { label: intl.formatMessage(strings.EXPRESSION), prop: 'expression' },
      ],
      getFilters(filter) {
        return [{ name: filter }];
      },
    },
  };
}
/* eslint-enable */

export const validator = {
  name({ intl, form }) {
    return (rule, value, callback) => {
      if (!value) {
        const entity = getRestrictTypes({ intl })[form.type].toLowerCase();
        const message = intl.formatMessage(strings.PLEASE_SELECT_ENTITY, { entity });
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
};

export function getFullVisibilityItemsWithAccess(dataset, nodeType, accessMapping) {
  const items = [];

  if (nodeType === 'column') {
    for (const model of dataset.models) {
      for (const table of model.dimensionTables) {
        for (const dimCol of table.dimCols) {
          const key = datasetHelper.getDatasetKey(nodeType, dimCol);
          if (datasetHelper.getIsAccessInKE(key, accessMapping)) {
            items.push(dimCol);
          }
        }
      }
    }
  }
  if (nodeType === 'measure') {
    for (const model of dataset.models) {
      for (const measure of model.measures) {
        const key = datasetHelper.getDatasetKey(nodeType, measure);
        if (datasetHelper.getIsAccessInKE(key, accessMapping)) {
          items.push(measure);
        }
      }
    }
  }
  if (nodeType === 'calculateMeasure') {
    for (const cMeasure of dataset.calculateMeasures) {
      const key = datasetHelper.getDatasetKey(nodeType, cMeasure);
      if (datasetHelper.getIsAccessInKE(key, accessMapping)) {
        items.push(cMeasure);
      }
    }
  }
  if (nodeType === 'namedSet') {
    for (const namedSet of dataset.namedSets) {
      const key = datasetHelper.getDatasetKey(nodeType, namedSet);
      if (datasetHelper.getIsAccessInKE(key, accessMapping)) {
        items.push(namedSet);
      }
    }
  }
  return items;
}
