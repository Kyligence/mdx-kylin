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
import { strings } from '../../constants';

export function getDefaultState() {
  return {
    isShow: false,
    callback: () => {},
    columns: [],
    modelName: '',
    tableName: '',
    errors: [],
    form: {
      dimCols: [],
      weightCols: [],
    },
  };
}

export const getErrorColumns = createSelector(
  state => state.errors,
  errors => errors.reduce((columns, error) => {
    const [, , levelName, weightName] = error.obj.split('.');

    if (error.type === 'HIERARCHY_DIM_COL_DELETED') {
      columns.push(levelName);
    } else if (error.type === 'HIERARCHY_WEIGHT_COL_DELETED') {
      columns.push(weightName);
    } else if (error.type === 'HIERARCHY_DIM_WEIGHT_COL_DELETED') {
      columns.push(levelName);
      columns.push(weightName);
    }
    return columns;
  }, []),
);

export function isErrorWeight(column, errorColumns) {
  return errorColumns.includes(column);
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

export const validator = {
  weightCols({ intl, errorColumns, modelName, tableName }) {
    return (rule, value, callback) => {
      if (isErrorWeight(value, errorColumns)) {
        const params = {
          deletedCols: `[${modelName}].[${tableName}].[${value}]`,
        };
        const message = intl.formatMessage(strings.HIERARCHY_COLUMNS_DELETED, params);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
};
