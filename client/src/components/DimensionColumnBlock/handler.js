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
import { business, strings, configs } from '../../constants';

export const validator = {
  // desc({ intl }) {
  //   return (rule, value = '', callback) => {
  //     const maxLength = configs.datasetMaxLength.columnDesc;
  //     // 超过最大长度报错
  //     if (value.length > configs.datasetMaxLength.columnDesc) {
  //       const params = { maxLength };
  //       const message = intl.formatMessage(strings.DESCRIPTION_TOO_LONG, params);
  //       callback(new Error(message));
  //     } else {
  //       callback();
  //     }
  //   };
  // },
  alias({ intl }, isColumnAliasDuplicate) {
    return (rule, inputValue = '', callback) => {
      const value = inputValue.trim();
      // 空值校验报错
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_NAME);
        callback(new Error(message));
        // 别名占用关键字报错
      } else if (configs.blackListKeyWords.includes(value.toLowerCase())) {
        const name = value;
        const nodeType = intl.formatMessage(strings.DIMENSION).toLowerCase();
        const message = intl.formatMessage(strings.INVALID_NAME_IS_KEY_WORD, { name, nodeType });
        callback(new Error(message));
        // 别名重名报错
      } else if (isColumnAliasDuplicate) {
        const params = { columnAlias: value };
        const message = intl.formatMessage(strings.DIMENSION_NAME_DUPLICATE, params);
        callback(new Error(message));
        // 名称不符合规则报错
      } else if (!business.nameRegExpInDataset.defaultRule.test(value)) {
        const message = intl.formatMessage(strings.INVALID_ALIAS_IN_DATASET);
        callback(new Error(message));
        // 超过最大长度报错
      } else if (value.length > configs.datasetMaxLength.columnAlias) {
        const maxLength = configs.datasetMaxLength.columnAlias;
        const params = { maxLength };
        const message = intl.formatMessage(strings.DIMENSION_NAME_TOO_LONG, params);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
  nameColumn(that) {
    return (rule, inputValue = '', callback) => {
      const { data } = that.props;
      const { nameColumnError } = data;

      if (nameColumnError) {
        const columnName = nameColumnError.obj.split('.')[2];
        if (inputValue === columnName) {
          const message = that.nameColumnMsg;
          callback(new Error(message));
        } else {
          callback();
        }
      } else {
        callback();
      }
    };
  },
  valueColumn(that) {
    return (rule, inputValue = '', callback) => {
      const { data } = that.props;
      const { valueColumnError } = data;

      if (valueColumnError) {
        const columnName = valueColumnError.obj.split('.')[2];
        if (inputValue === columnName) {
          const message = that.valueColumnMsg;
          callback(new Error(message));
        } else {
          callback();
        }
      } else {
        callback();
      }
    };
  },
  properties(that) {
    return (rule, inputValue = [], callback) => {
      const { isInvalidProperty, getInvalidPropertyMsg } = that;
      const invalidProperty = inputValue.find(property => isInvalidProperty(property.columnName));

      if (invalidProperty) {
        const message = getInvalidPropertyMsg(invalidProperty.columnName);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
  subfolder({ intl }) {
    return (rule, value, callback) => {
      if (!business.subfolderRegExpInDataset.test(value)) {
        const message = intl.formatMessage(strings.INVALID_FOLDER_IN_DATASET);
        callback(new Error(message));
      } else if (value.length > configs.datasetMaxLength.folderName) {
        const maxLength = configs.datasetMaxLength.folderName;
        const message = intl.formatMessage(strings.FOLDER_NAME_TOO_LONG, { maxLength });
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
  defaultMember(that) {
    const isExpressionValid = async expression => {
      const { boundDatasetActions, data } = that.props;
      const { currentTable } = that;
      const result = await boundDatasetActions.validateDefaultMemberForMDX(
        expression, data.model, currentTable.alias, data.alias,
      );
      return { isValid: !result[0], error: result[0] };
    };

    return async (rule, value, callback) => {
      const { intl } = that.props;
      // 超长报错
      if (value && (value.length > configs.datasetMaxLength.columnDefaultMember)) {
        const maxLength = configs.datasetMaxLength.columnDefaultMember;
        const params = { maxLength };
        const message = intl.formatMessage(strings.DEFAULT_MEMBER_TOO_LONG, params);
        callback(new Error(message));
      } else if (value) {
        try {
          that.setState({ isValidating: true });

          const { isValid, error } = await isExpressionValid(value);
          if (!isValid) {
            const message = error === 'error' ? intl.formatMessage(strings.INVALID_DEFAULT_MEMBER_EXPRESSION) : error;
            callback(new Error(message));
          } else {
            callback();
          }
        } catch (e) {
          callback();
        } finally {
          that.setState({ isValidating: false });
        }
      } else {
        callback();
      }
    };
  },
};

export const dimColumnStrings = {
  0: 'REGULAR',
  1: 'LEVEL_YEAR',
  2: 'LEVEL_QUARTERS',
  3: 'LEVEL_MONTHS',
  4: 'LEVEL_WEEKS',
  5: 'LEVEL_DAYS',
};
