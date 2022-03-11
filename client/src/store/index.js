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
import { compose, createStore, combineReducers, applyMiddleware } from 'redux';
import { composeWithDevTools } from 'redux-devtools-extension';
import thunk from 'redux-thunk';
import { connectRouter, routerMiddleware } from 'connected-react-router';
import * as actionTypes from './types';
import { Connect } from './connect';
import { InjectIntl } from './injectIntl';
import { getHistory } from './history';

import { getSystemReducer } from './reducers/system';
import { getGlobalReducer } from './reducers/global';
import { getDataReducer } from './reducers/data';
import { getModalReducer } from './reducers/modal';
import { getWorkspaceReducer } from './reducers/workspace';

let store = null;

function getAppStore() {
  if (!store) {
    const isProduction = process.env.NODE_ENV === 'production';
    const composeFunc = isProduction ? compose : composeWithDevTools;

    const rootReducer = combineReducers({
      system: getSystemReducer(),
      global: getGlobalReducer(),
      data: getDataReducer(),
      modal: getModalReducer(),
      workspace: getWorkspaceReducer(),
      router: connectRouter(getHistory()),
    });

    store = createStore(
      rootReducer,
      composeFunc(
        applyMiddleware(
          thunk,
          routerMiddleware(getHistory()),
        ),
      ),
    );
  }
  return store;
}

export {
  Connect,
  InjectIntl,
  actionTypes,
  getHistory,
  getAppStore,
};
