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
import { Base64 } from 'js-base64';
import { strings, configs } from '../../constants';

export function getDefaultState() {
  return {
    isShow: false,
    callback: () => {},
    errorMessage: strings.EDIT_CONNECTION_USER_TIP,
    errorData: {},
    form: {
      username: '',
      password: '',
    },
  };
}

export function formatPostForm(form) {
  return {
    'insight.kylin.username': form.username,
    'insight.kylin.password': Base64.encode(form.password),
  };
}

export const validator = {
  username({ intl }) {
    return (rule, value = '', callback) => {
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_USERNAME);
        callback(new Error(message));
      } else if (value.length >= configs.userMaxLength.username) {
        const maxLength = configs.userMaxLength.username;
        const params = { maxLength };
        const message = intl.formatMessage(strings.USERNAME_TOO_LONG, params);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
  password({ intl }) {
    return (rule, value = '', callback) => {
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_PASSWORD);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
};
