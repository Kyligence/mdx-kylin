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
