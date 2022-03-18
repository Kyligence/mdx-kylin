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
  name({ intl }) {
    return (rule, value, callback) => {
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_ROLE_NAME);
        callback(new Error(message));
      } else if (!business.nameRegExpWithChineseAndSpace.test(value)) {
        const message = intl.formatMessage(strings.INVALID_NAME_WITH_CHINESE_AND_SPACE);
        callback(new Error(message));
      } else if (value.length && value.length > 20) {
        const params = { maxLength: 20 };
        const message = intl.formatMessage(strings.DATASET_ROLE_NAME_TOO_LONG, params);
        callback(new Error(message));
      }
      callback();
    };
  },
  description({ intl }) {
    return (rule, value, callback) => {
      const maxLength = configs.datasetRoleMaxLength.description;

      if (value.length && value.length > maxLength) {
        const params = { maxLength };
        const message = intl.formatMessage(strings.DESCRIPTION_TOO_LONG, params);
        callback(new Error(message));
      }
      callback();
    };
  },
};

export function getInitialForm() {
  return {
    id: '',
    name: '',
    description: '',
    contains: [],
  };
}
