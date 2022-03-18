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
import './assets/stylesheets/index.less';

import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import { Route } from 'react-router-dom';
import { ConnectedRouter } from 'connected-react-router';

import dayjs from 'dayjs';
import 'dayjs/locale/en';
import 'dayjs/locale/zh-cn';
import relativeTime from 'dayjs/plugin/relativeTime';
import duration from 'dayjs/plugin/duration';

import App from './App';
import { getAppStore, getHistory } from './store';

const rootElement = document.getElementById('main');

dayjs.extend(relativeTime);
dayjs.extend(duration);

// 初始化用户权限信息
if (!localStorage.getItem('system.project')) {
  localStorage.setItem('system.project', '{"name":"", "access":"EMPTY"}');
}

if (rootElement) {
  ReactDOM.render((
    <Provider store={getAppStore()}>
      <ConnectedRouter history={getHistory()}>
        <Route path="/" component={App} />
      </ConnectedRouter>
    </Provider>
  ), rootElement);
}
