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

export const TODAY = 'TODAY';
export const LAST_3_DAYS = 'LAST_3_DAYS';
export const LAST_7_DAYS = 'LAST_7_DAYS';
export const LAST_1_MONTH = 'LAST_1_MONTH';
export const CUSTOMIZE = 'CUSTOMIZE';

export const dateTypes = [TODAY, LAST_3_DAYS, LAST_7_DAYS, LAST_1_MONTH, CUSTOMIZE];

export const validator = {
  dateRange({ intl }) {
    return (rule, inputValue = [], callback) => {
      const [startAt, endAt] = inputValue;

      if (!startAt || !endAt) {
        const message = intl.formatMessage(strings.PLEASE_SELECT_DATE_RANGE);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
  clusters({ intl }) {
    return (rule, inputValue = [], callback) => {
      if (!inputValue.length) {
        const message = intl.formatMessage(strings.PLEASE_SELECT_SERVER);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
};
