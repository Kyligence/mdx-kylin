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
import { strings } from '../../constants';

const errorMap = {
  COMMON_TABLE_DELETED: strings.COMMON_TABLE_DELETED,
};

export const validator = {
  left({ intl }) {
    return (rule, value, callback, field, options) => {
      const { value: form = {} } = options;
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_SELECT_TABLE);
        callback(new Error(message));
      } else if (form.leftError) {
        const { name, type } = form.leftError;
        const table = name.split('.')[1];
        if (value === table) {
          const message = intl.formatMessage(errorMap[type](table));
          callback(new Error(message));
        } else {
          callback();
        }
      } else {
        callback();
      }
    };
  },
  right({ intl }) {
    return (rule, value, callback, field, options) => {
      const { value: form = {} } = options;
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_SELECT_TABLE);
        callback(new Error(message));
      } else if (form.rightError) {
        const { name, type } = form.rightError;
        const table = name.split('.')[1];
        if (value === table) {
          const message = intl.formatMessage(errorMap[type](table));
          callback(new Error(message));
        } else {
          callback();
        }
      } else {
        callback();
      }
    };
  },
};

export function getInitValues() {
  return [{ left: '', right: '' }];
}
