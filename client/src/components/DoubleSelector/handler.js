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
