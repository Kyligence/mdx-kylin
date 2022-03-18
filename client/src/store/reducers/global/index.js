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
import { configs, storagePath } from '../../../constants';
import { browserHelper } from '../../../utils';
import { Polling } from '../../../classes';

const { pollingDelayMs } = configs;

function getInitialState() {
  const { IS_MENU_COLLAPSED } = storagePath;
  const isMenuCollapsed = !!browserHelper.getStorage(IS_MENU_COLLAPSED);

  return {
    isSemanticEdit: false,
    isDatasetEdit: false,
    isDashboardEdit: false,
    isExporterEdit: false,
    isShowEnterAdmin: false,
    isShowLeaveAdmin: false,
    isShowGlobalMask: false,
    isAutoPopConnectionUser: false,
    isNoPageHeader: JSON.parse(browserHelper.getQueryFromLocation('isNoPageHeader')) || false,
    isNoPageFooter: JSON.parse(browserHelper.getQueryFromLocation('isNoPageFooter')) || false,
    isNoMenubar: JSON.parse(browserHelper.getQueryFromLocation('isNoMenubar')) || false,
    isNoBreadCrumb: JSON.parse(browserHelper.getQueryFromLocation('isNoBreadCrumb')) || false,
    isMenuCollapsed,
    pollings: {
      configuration: new Polling({ duringMs: pollingDelayMs.configuration }),
    },
    globalMaskMessage: null,
    globalMaskData: null,
  };
}

export function getGlobalReducer() {
  return function global(state = getInitialState(), action) {
    switch (action.type) {
      case actionTypes.TOGGLE_FLAG: {
        const { key, value } = action;
        return { ...state, [key]: value };
      }
      case actionTypes.DISABLE_MENU_PREVENT: {
        const flags = configs.preventMenuFlagList.reduce(
          (allFlags, flagName) => ({ ...allFlags, [flagName]: false }), {},
        );
        return { ...state, ...flags };
      }
      case actionTypes.SET_MENU_COLLAPSED: {
        return { ...state, isMenuCollapsed: action.collapsed };
      }
      case actionTypes.SET_GLOBAL_MASK_MESSAGE: {
        return { ...state, globalMaskMessage: action.message, globalMaskData: action.data };
      }
      default:
        return state;
    }
  };
}
