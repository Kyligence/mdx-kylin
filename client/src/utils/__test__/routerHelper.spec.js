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
import sinon from 'sinon';
import { routerHook } from '../routerHelper';

function getDefaultArgs() {
  return {
    prevProps: {
      currentUser: {
        username: '',
      },
    },
    nextProps: {
      currentUser: {
        username: '',
      },
      currentSite: {
        name: '',
      },
      menus: [
        { url: '/dataset/list' },
      ],
      history: {
        push: sinon.spy(),
      },
    },
    nextStates: {
      isSystemInited: true,
      isDataInited: true,
    },
    redirectToLogin: sinon.spy(),
  };
}

describe('路由钩子功能测试', () => {
  describe('未登录的用户', () => {
    it('进入登录页面', () => {
      const { prevProps, prevState, nextProps, nextStates, redirectToLogin } = getDefaultArgs();

      prevProps.currentUser.username = '';
      nextProps.currentUser.username = '';
      nextProps.currentSite.name = 'login';

      routerHook({ prevProps, prevState }, { nextProps, nextStates, redirectToLogin });
      expect(nextProps.history.push.called).toBeFalsy();
      expect(redirectToLogin.called).toBeFalsy();
    });

    it('进入不可进入的页面', () => {
      const { prevProps, prevState, nextProps, nextStates, redirectToLogin } = getDefaultArgs();

      prevProps.currentUser.username = '';
      nextProps.currentUser.username = '';
      nextProps.currentSite = null;

      routerHook({ prevProps, prevState }, { nextProps, nextStates, redirectToLogin });
      expect(redirectToLogin.called).toBeTruthy();
    });

    it('系统进入登录用户', () => {
      const { prevProps, prevState, nextProps, nextStates, redirectToLogin } = getDefaultArgs();

      prevProps.currentUser.username = '';
      nextProps.currentUser.username = 'ADMIN';
      nextProps.currentSite.name = 'login';

      routerHook({ prevProps, prevState }, { nextProps, nextStates, redirectToLogin });
      expect(nextProps.history.push.args[0][0]).toBe('/dataset/list');
    });
  });

  describe('已登录的用户', () => {
    it('进入登录页面', () => {
      const { prevProps, prevState, nextProps, nextStates, redirectToLogin } = getDefaultArgs();

      prevProps.currentUser.username = 'ADMIN';
      nextProps.currentUser.username = 'ADMIN';
      nextProps.currentSite.name = 'login';

      routerHook({ prevProps, prevState }, { nextProps, nextStates, redirectToLogin });
      expect(nextProps.history.push.args[0][0]).toBe('/dataset/list');
    });

    it('进入不存在的页面', () => {
      const { prevProps, prevState, nextProps, nextStates, redirectToLogin } = getDefaultArgs();

      prevProps.currentUser.username = 'ADMIN';
      nextProps.currentUser.username = 'ADMIN';
      nextProps.currentSite = null;

      routerHook({ prevProps, prevState }, { nextProps, nextStates, redirectToLogin });
      expect(nextProps.history.push.args[0][0]).toBe('/dataset/list');
    });

    it('系统退出登录用户', () => {
      const { prevProps, prevState, nextProps, nextStates, redirectToLogin } = getDefaultArgs();

      prevProps.currentUser.username = 'ADMIN';
      nextProps.currentUser.username = '';
      nextProps.currentSite = 'datasetList';

      routerHook({ prevProps, prevState }, { nextProps, nextStates, redirectToLogin });
      expect(redirectToLogin.called).toBeTruthy();
    });
  });
});
