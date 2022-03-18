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
import { business, strings, configs } from '../../constants';

export const validator = {
  name({ intl }, isHierarchyDuplicate) {
    return (rule, inputValue = '', callback) => {
      const value = inputValue.trim();
      // 空值校验报错
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_NAME);
        callback(new Error(message));
        // 别名占用关键字报错
      } else if (configs.blackListKeyWords.includes(value.toLowerCase())) {
        const name = value;
        const nodeType = intl.formatMessage(strings.HIERARCHY).toLowerCase();
        const message = intl.formatMessage(strings.INVALID_NAME_IS_KEY_WORD, { name, nodeType });
        callback(new Error(message));
        // 别名重名报错
      } else if (isHierarchyDuplicate) {
        const params = { hierarchyName: value };
        const message = intl.formatMessage(strings.HIERARCHY_DUPLICATE, params);
        callback(new Error(message));
        // 名称不符合规则报错
      } else if (!business.nameRegExpInDataset.defaultRule.test(value)) {
        const message = intl.formatMessage(strings.INVALID_NAME_IN_DATASET);
        callback(new Error(message));
        // 超过最大长度报错
      } else if (value.length > configs.datasetMaxLength.hierarchyName) {
        const maxLength = configs.datasetMaxLength.hierarchyName;
        const params = { maxLength };
        const message = intl.formatMessage(strings.HIERARCHY_NAME_TOO_LONG, params);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
  // desc({ intl }) {
  //   return (rule, value = '', callback) => {
  //     // 超过最大长度报错
  //     if (value.length > configs.datasetMaxLength.hierarchyDesc) {
  //       const maxLength = configs.datasetMaxLength.hierarchyDesc;
  //       const params = { maxLength };
  //       const message = intl.formatMessage(strings.DESCRIPTION_TOO_LONG, params);
  //       callback(new Error(message));
  //     } else {
  //       callback();
  //     }
  //   };
  // },
  tablePath({ intl }) {
    return (rule, value, callback) => {
      if (!value.length) {
        const message = intl.formatMessage(strings.PLEASE_SELECT_MODEL_TABLE);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
  dimCols({ intl }, errorLevels = [], columnsOptions = []) {
    return (rule, value = [], callback) => {
      const currentErrorLevels = errorLevels.filter(
        errorLevel => value.includes(errorLevel),
      );

      if (value.length < 2) {
        const message = intl.formatMessage(strings.TOW_MORE_DIMENSIONS);
        callback(new Error(message));
      } else if (currentErrorLevels.length) {
        const getColumnsAlias = errorLevel => (
          columnsOptions.find(option => option.value === errorLevel).label
        );

        const deletedCols = currentErrorLevels.map(errorLevel => `[${getColumnsAlias(errorLevel)}]`).join(', ');
        const params = { deletedCols };
        const message = intl.formatMessage(strings.HIERARCHY_COLUMNS_DELETED, params);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
};

export function getModelTableOptions(dataset) {
  return dataset.models.map(model => ({
    label: model.name,
    value: model.name,
    children: model.dimensionTables ? model.dimensionTables
      // 至少拥有两个维度的维度表，才可以作为层级维度的候选项
      .filter(table => table.dimCols.length > 1)
      .map(table => ({
        label: table.alias,
        value: table.name,
      })) : [],
  }));
}

export function getColumnsOptions(dataset, [model, table]) {
  const currentModel = dataset.models.find(item => item.name === model) || {};
  const currnetTables = currentModel.dimensionTables || [];
  const currentTable = currnetTables.find(item => item.name === table) || {};
  const currnetColumns = currentTable.dimCols || [];

  return currnetColumns.map(column => ({
    label: column.alias,
    value: column.name,
    dataType: column.dataType,
  }));
}

export const getColumnAliasDict = createSelector(
  state => state.columns,
  columns => {
    const dict = {};
    for (const column of columns) {
      dict[column.value] = column.label;
    }
    return dict;
  },
);

export const getErrorWeights = createSelector(
  state => state.errors,
  errors => errors.reduce((columns, error) => {
    const [, , , weightName] = error.obj.split('.');

    if (error.type === 'HIERARCHY_WEIGHT_COL_DELETED') {
      columns.push(weightName);
    } else if (error.type === 'HIERARCHY_DIM_WEIGHT_COL_DELETED') {
      columns.push(weightName);
    }
    return columns;
  }, []),
);

export const getErrorLevels = createSelector(
  state => state.errors,
  errors => errors.reduce((columns, error) => {
    const [, , levelName] = error.obj.split('.');

    if (error.type === 'HIERARCHY_DIM_COL_DELETED') {
      columns.push(levelName);
    } else if (error.type === 'HIERARCHY_DIM_WEIGHT_COL_DELETED') {
      columns.push(levelName);
    }
    return columns;
  }, []),
);

export const getErrorColumns = createSelector(
  getErrorLevels,
  getErrorWeights,
  (errorLevels, errorWeights) => [...errorLevels, ...errorWeights],
);
