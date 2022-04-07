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
