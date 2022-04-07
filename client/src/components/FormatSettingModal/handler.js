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
