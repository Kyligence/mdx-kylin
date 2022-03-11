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
import { business, strings, configs } from '../../constants';

export const validator = {
  alias({ intl }, isMeasureAliasDuplicate) {
    return (rule, inputValue = '', callback) => {
      const value = inputValue.trim();
      // 空值校验报错
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_NAME);
        callback(new Error(message));
        // 别名占用关键字报错
      } else if (configs.blackListKeyWords.includes(value.toLowerCase())) {
        const name = value;
        const nodeType = intl.formatMessage(strings.MEASURE).toLowerCase();
        const message = intl.formatMessage(strings.INVALID_NAME_IS_KEY_WORD, { name, nodeType });
        callback(new Error(message));
        // 别名重名报错
      } else if (isMeasureAliasDuplicate) {
        const params = { measureName: value };
        const message = intl.formatMessage(strings.MEASURE_DUPLICATE, params);
        callback(new Error(message));
        // 名称不符合规则报错
      } else if (!business.nameRegExpInDataset.defaultRule.test(value)) {
        const message = intl.formatMessage(strings.INVALID_ALIAS_IN_DATASET);
        callback(new Error(message));
        // 超过最大长度报错
      } else if (value.length > configs.datasetMaxLength.measureAlias) {
        const maxLength = configs.datasetMaxLength.measureAlias;
        const params = { maxLength };
        const message = intl.formatMessage(strings.MEASURE_NAME_TOO_LONG, params);
        callback(new Error(message));
      }
      callback();
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
  // desc({ intl }) {
  //   return (rule, value = '', callback) => {
  //     if (value.length > configs.datasetMaxLength.measureDesc) {
  //       const maxLength = configs.datasetMaxLength.measureDesc;
  //       const params = { maxLength };
  //       const message = intl.formatMessage(strings.DESCRIPTION_TOO_LONG, params);
  //       callback(new Error(message));
  //     } else {
  //       callback();
  //     }
  //   };
  // },
};

export const measureFormats = [
  'regular',
  '####',
  '#,###.00',
  '0.00%',
  '$#,###',
  '$#,###.00',
];
