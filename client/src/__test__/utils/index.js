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
