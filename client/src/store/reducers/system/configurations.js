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
import * as actionTypes from '../../types';

function getInitialState() {
  return {
    'insight.kylin.host': '',
    'insight.kylin.port': '',
    'insight.kylin.status': '',
    'insight.kylin.last_updated': '',
    'insight.kylin.username': '',
    'insight.dataset.allow-access-by-default': false,
    'insight.dataset.export-file-limit': 0,
  };
}

export default function configurations(state = getInitialState(), action) {
  switch (action.type) {
    case actionTypes.SET_CONFIGURATIONS: {
      const configurationMap = {};

      for (const [key, value] of Object.entries(action.configurations)) {
        if (value === 'true') {
          configurationMap[key] = true;
        } else if (value === 'false') {
          configurationMap[key] = false;
        } else if (['insight.dataset.export-file-limit'].includes(key)) {
          configurationMap[key] = +value;
        } else {
          configurationMap[key] = value;
        }
      }

      return { ...state, ...configurationMap };
    }
    default:
      return state;
  }
}
