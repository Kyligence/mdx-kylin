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
  alias({ intl }, isModelAliasDuplicate) {
    return (rule, inputValue = '', callback) => {
      const value = inputValue.trim();
      // 空值校验报错
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_MEASURE_GROUP_NAME);
        callback(new Error(message));
        // 别名重名报错
      } else if (isModelAliasDuplicate) {
        const params = { measureGroupName: value };
        const message = intl.formatMessage(strings.MEASURE_GROUP_NAME_DUPLICATE, params);
        callback(new Error(message));
        // 名称不符合规则报错
      } else if (!business.nameRegExpInDataset.defaultRule.test(value)) {
        const message = intl.formatMessage(strings.INVALID_ALIAS_IN_DATASET);
        callback(new Error(message));
      } else if (value.length > configs.datasetMaxLength.measureGroupName) {
        const maxLength = configs.datasetMaxLength.measureGroupName;
        const params = { maxLength };
        const message = intl.formatMessage(strings.MEASURE_GROUP_NAME_TOO_LONG, params);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
};
