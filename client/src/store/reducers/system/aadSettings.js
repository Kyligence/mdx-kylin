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
    'insight.semantic.enable.aad': false,
    'aad.login.url': '',
    'aad.logout.url': '',
  };
}

export default function aadSettings(state = getInitialState(), action) {
  switch (action.type) {
    case actionTypes.SET_AAD_SETTINGS: {
      const settingsMap = {};

      for (const [key, value] of Object.entries(action.settings)) {
        if (value === 'true') {
          settingsMap[key] = true;
        } else if (value === 'false') {
          settingsMap[key] = false;
        } else {
          settingsMap[key] = value;
        }
      }

      return { ...state, ...settingsMap };
    }
    default:
      return state;
  }
}
