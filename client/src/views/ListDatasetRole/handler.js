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
