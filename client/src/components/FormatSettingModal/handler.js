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

import { formatHelper } from '../../utils';
import { strings, configs } from '../../constants';

const { parseSettingsToFormat } = formatHelper;

export function getDefaultState() {
  return {
    isShow: false,
    callback: () => {},
    format: '',
    formatType: '',
    customFormats: [],
    form: {
      formatType: '',
      decimalCount: 2,
      isThousandSeparate: false,
      negativeType: configs.negativeTypes.NORMAL,
      currency: configs.currencyTypes.DOLLAR,
      customFormat: '',
    },
  };
}

export const getFormatBySettings = createSelector(
  state => state.form,
  form => parseSettingsToFormat(form),
);

export const getDecimalFormat = createSelector(
  state => state.decimalCount,
  decimalCount => formatHelper.getDecimalFormat(decimalCount),
);

export const getSampleDecimalFormat = createSelector(
  state => state.decimalCount,
  decimalCount => formatHelper.getSampleDecimalFormat(decimalCount),
);

export const validator = {
  customFormat({ intl }) {
    return (rule, value, callback) => {
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_FORMAT);
        callback(new Error(message));
      } else if (value.length > configs.datasetMaxLength.measureFormat) {
        const { measureFormat: maxLength } = configs.datasetMaxLength;
        const message = intl.formatMessage(strings.CUSTOMIZE_FORMAT_TOO_LONG, { maxLength });
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
};
