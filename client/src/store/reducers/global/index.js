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
