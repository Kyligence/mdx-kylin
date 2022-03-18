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
/* eslint-disable react/jsx-filename-extension */
import React from 'react';
import { shallow, mount } from 'enzyme';
import { IntlProvider, createIntl, createIntlCache } from 'react-intl';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import { getDefaultStore } from '../mocks';
import messages from '../../locale/en.json';

// 此处@Connect必须在@InjectIntl之前注入
export function createShallowComponent(options) {
  const {
    JestComponent,
    props = {},
    store = getDefaultStore(),
  } = options;

  const mockStore = configureStore([]);
  const storeData = mockStore(store);

  return shallow(
    <Provider store={storeData}>
      <IntlProvider locale="en" messages={messages}>
        <JestComponent {...props} />
      </IntlProvider>
    </Provider>,
  ).dive();
}

// 此处@Connect必须在@InjectIntl之前注入
export function createMountComponent(options) {
  const {
    JestComponent,
    props = {},
    store = getDefaultStore(),
  } = options;

  const mockStore = configureStore([]);
  const storeData = mockStore(store);

  return mount(
    <Provider store={storeData}>
      <IntlProvider locale="en" messages={messages}>
        <JestComponent {...props} />
      </IntlProvider>
    </Provider>,
  );
}

export function delayMs(ms) {
  return new Promise(resolve => {
    setTimeout(() => resolve(), ms);
  });
}

export async function executeDispatch(action, dispatch, getState) {
  if (typeof action === 'function') {
    try {
      // 新版的jest.spyOn()虽然能成功mock参数，但是同时也会返回
      // connect ECONNREFUSED 127.0.0.1:80
      // 最终导致UT挂掉，此处只报出错误，阻止UT被中断
      await action(curAction => executeDispatch(curAction, dispatch, getState), getState);
    } catch (e) {
      console.warn(e);
    }
  } else {
    await dispatch(action);
  }
}

export function randomChar(wordSet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789', specialSet = '_') {
  const randomCharset = wordSet + specialSet;
  const charAt = Math.floor(Math.random() * randomCharset.length);
  return randomCharset[charAt];
}

export function randomString(length, wordSet, specialSet) {
  let string = '';
  for (let i = 0; i < length; i += 1) {
    string += randomChar(wordSet, specialSet);
  }
  return string;
}

export const intlCache = createIntlCache();

export const intl = createIntl(
  {
    locale: 'en',
    defaultLocale: 'en',
    messages,
  },
  intlCache,
);

export function createIntlShallowComponent(component) {
  return shallow(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
}
