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
