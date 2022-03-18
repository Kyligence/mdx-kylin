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
