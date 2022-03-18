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
  alias({ intl }, isTableAliasDuplicate) {
    return (rule, inputValue = '', callback) => {
      const value = inputValue.trim();
      // 空值校验报错
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_TABLE_ALIAS);
        callback(new Error(message));
        // 别名占用关键字报错
      } else if (configs.blackListKeyWords.includes(value.toLowerCase())) {
        const name = value;
        const nodeType = intl.formatMessage(strings.DIMENSION_TABLE).toLowerCase();
        const message = intl.formatMessage(strings.INVALID_NAME_IS_KEY_WORD, { name, nodeType });
        callback(new Error(message));
        // 别名重名报错
      } else if (isTableAliasDuplicate) {
        const params = { tableAlias: value };
        const message = intl.formatMessage(strings.TABLE_NAME_DUPLICATE, params);
        callback(new Error(message));
        // 名称不符合规则报错
      } else if (!business.nameRegExpInDataset.defaultRule.test(value)) {
        const message = intl.formatMessage(strings.INVALID_ALIAS_IN_DATASET);
        callback(new Error(message));
        // 超过最大长度报错
      } else if (value.length > configs.datasetMaxLength.tableAlias) {
        const maxLength = configs.datasetMaxLength.tableAlias;
        const params = { maxLength };
        const message = intl.formatMessage(strings.DIM_TABLE_NAME_TOO_LONG, params);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
  type({ intl }) {
    return (rule, value = '', callback) => {
      // 空值校验报错
      if (!value.length) {
        const message = intl.formatMessage(strings.PLEASE_SELECT_TYPE);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
};

export const getChildHierarchies = createSelector(
  state => state.children || [],
  children => children.filter(child => child.nodeType === configs.nodeTypes.HIERARCHY),
);

export const getHasWeightHierarchy = createSelector(
  getChildHierarchies,
  hierarchies => hierarchies.some(hierarchy => hierarchy.weightCols.filter(w => w).length),
);
