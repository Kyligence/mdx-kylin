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

export const CALCULATED_MEASURE = 'Calculated Measure';

export function isErrorMeasure(behavior, nonEmptyBehaviorErrors) {
  return nonEmptyBehaviorErrors &&
    nonEmptyBehaviorErrors.some(nonEmptyBehaviorError => (
      nonEmptyBehaviorError.obj === `${behavior.model}.${behavior.name}`
    ));
}

export const validator = {
  name({ intl }, isCMeasureDuplicate) {
    return (rule, inputValue = '', callback) => {
      const value = inputValue.trim();
      // 空值校验报错
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_NAME);
        callback(new Error(message));
        // 别名占用关键字报错
      } else if (configs.blackListKeyWords.includes(value.toLowerCase())) {
        const name = value;
        const nodeType = intl.formatMessage(strings.CALCULATED_MEASURE).toLowerCase();
        const message = intl.formatMessage(strings.INVALID_NAME_IS_KEY_WORD, { name, nodeType });
        callback(new Error(message));
        // 别名重名报错
      } else if (isCMeasureDuplicate) {
        const params = { cMeasureName: value };
        const message = intl.formatMessage(strings.CALCULATE_MEASURE_DUPLICATE, params);
        callback(new Error(message));
        // 名称不符合规则报错
      } else if (!business.nameRegExpInDataset.defaultRule.test(value)) {
        const message = intl.formatMessage(strings.INVALID_NAME_IN_DATASET);
        callback(new Error(message));
        // 超过最大长度报错
      } else if (value.length > configs.datasetMaxLength.cMeasureName) {
        const maxLength = configs.datasetMaxLength.cMeasureName;
        const params = { maxLength };
        const message = intl.formatMessage(strings.CMEASURE_NAME_TOO_LONG, params);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
  format({ intl }) {
    return (rule, value, callback) => {
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_FORMAT);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
  folder({ intl }) {
    return (rule, value, callback) => {
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_SELECT_MEASURE_GROUP);
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
  expression(that) {
    const isExpressionValid = async expression => {
      const { boundDatasetActions, dataset } = that.props;
      const data = await boundDatasetActions.validateCMeasureExpressionForMDX(expression, dataset);
      return { isValid: !data[0], error: data[0] };
    };

    return async (rule, value, callback) => {
      const { intl } = that.props;
      // 空值校验报错
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_EXPRESSION);
        callback(new Error(message));
        // 超过最大长度报错
      } else if (value.length > configs.datasetMaxLength.cMeasureExpression) {
        const maxLength = configs.datasetMaxLength.cMeasureExpression;
        const params = { maxLength };
        const message = intl.formatMessage(strings.CMEASURE_EXPRESSION_TOO_LONG, params);
        callback(new Error(message));
      } else {
        // 表达式校验不正确报错
        try {
          that.setState({ isValidating: true });

          const { isValid, error } = await isExpressionValid(value);
          if (!isValid) {
            callback(new Error(error));
          } else {
            callback();
          }
        } catch (e) {
          callback();
        } finally {
          that.setState({ isValidating: false });
        }
      }
    };
  },
  nonEmptyBehavior(that) {
    return async (rule, value, callback) => {
      const { intl, data: { nonEmptyBehaviorErrors } } = that.props;
      const errorMeasure = value.find(item => isErrorMeasure(item, nonEmptyBehaviorErrors));

      if (value && value.length && !!errorMeasure) {
        const { alias } = errorMeasure;
        const message = intl.formatMessage(strings.NON_EMPTY_BEHAVIOR_DELETED, { alias });
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
  // desc({ intl }) {
  //   return (rule, value = '', callback) => {
  //     if (value.length > configs.datasetMaxLength.cMeasureDesc) {
  //       const maxLength = configs.datasetMaxLength.cMeasureDesc;
  //       const params = { maxLength };
  //       const message = intl.formatMessage(strings.DESCRIPTION_TOO_LONG, params);
  //       callback(new Error(message));
  //     } else {
  //       callback();
  //     }
  //   };
  // },
};

export const calcMeasureFormats = [
  'regular',
  '####',
  '#,###.00',
  '0.00%',
  '$#,###',
  '$#,###.00',
];

export const getAllMeasureOptions = createSelector(
  state => state.dataset,
  state => state.form,
  state => state.nonEmptyBehaviorErrors,
  (dataset, form, nonEmptyBehaviorErrors) => {
    const allMeasureOptions = [];

    for (const model of dataset.models) {
      for (const measure of model.measures) {
        allMeasureOptions.push({
          label: `[Measures].[${measure.alias}]`,
          value: measure.alias,
          data: measure,
        });
      }
    }

    // 此处逻辑，等后端加入nonEmptyBehaviorErrors后需要移除，不然影响性能
    if (!nonEmptyBehaviorErrors) {
      for (const nonEmptyBehavior of form.nonEmptyBehavior) {
        const isNotInOptions = !allMeasureOptions.some(option => (
          option.value === nonEmptyBehavior.alias
        ));

        if (isNotInOptions) {
          allMeasureOptions.push({
            label: `[Measures].[${nonEmptyBehavior.alias}]`,
            value: nonEmptyBehavior.alias,
            data: nonEmptyBehavior,
            disabled: true,
          });
        }
      }
    } else {
      for (const nonEmptyBehavior of form.nonEmptyBehavior) {
        if (isErrorMeasure(nonEmptyBehavior, nonEmptyBehaviorErrors)) {
          allMeasureOptions.push({
            label: `[Measures].[${nonEmptyBehavior.alias}]`,
            value: nonEmptyBehavior.alias,
            data: nonEmptyBehavior,
            disabled: true,
          });
        }
      }
    }

    return allMeasureOptions;
  },
);
