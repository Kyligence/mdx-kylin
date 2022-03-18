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
  name({ intl }, isNamedSetDuplicate) {
    return (rule, inputValue = '', callback) => {
      const value = inputValue.trim();
      // 空值校验报错
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_NAME);
        callback(new Error(message));
        // 别名占用关键字报错
      } else if (configs.blackListKeyWords.includes(value.toLowerCase())) {
        const name = value;
        const nodeType = intl.formatMessage(strings.NAMEDSET).toLowerCase();
        const message = intl.formatMessage(strings.INVALID_NAME_IS_KEY_WORD, { name, nodeType });
        callback(new Error(message));
        // 别名重名报错
      } else if (isNamedSetDuplicate) {
        const params = { namedSetName: value };
        const message = intl.formatMessage(strings.NAMEDSET_DUPLICATE, params);
        callback(new Error(message));
        // 名称不符合规则报错
      } else if (!business.nameRegExpInDataset.namedSet.test(value)) {
        const message = intl.formatMessage(strings.INVALID_NAMED_SET_NAME_IN_DATASET);
        callback(new Error(message));
        // 超过最大长度报错
      } else if (value.length > configs.datasetMaxLength.namedSetName) {
        const maxLength = configs.datasetMaxLength.namedSetName;
        const params = { maxLength };
        const message = intl.formatMessage(strings.NAMEDSET_NAME_TOO_LONG, params);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
  expression(that) {
    const isExpressionValid = async expression => {
      const { boundDatasetActions, dataset } = that.props;
      const data = await boundDatasetActions.validateNamedSetExpressionForMDX(expression, dataset);
      const { location, error } = data[0];
      that.handleInput('location', location && location !== 'Measures' ? location : 'Named Set');
      return { isValid: !error, error };
    };

    return async (rule, value, callback) => {
      const { intl } = that.props;
      // 空值校验报错
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_EXPRESSION);
        callback(new Error(message));
      } else if (value.length > configs.datasetMaxLength.namedSetExpression) {
        const maxLength = configs.datasetMaxLength.namedSetExpression;
        const params = { maxLength };
        const message = intl.formatMessage(strings.NAMEDSET_EXPRESSION_TOO_LONG, params);
        callback(new Error(message));
      } else {
        // 表达式校验不正确报错
        try {
          that.setState({ isValidating: true });

          const { isValid, error } = await isExpressionValid(value);

          that.setState({ isValidating: false });

          if (!isValid) {
            callback(new Error(error));
          } else {
            callback();
          }
        } catch (e) {
          that.setState({ isValidating: false });
          callback();
        }
      }
    };
  },
};
