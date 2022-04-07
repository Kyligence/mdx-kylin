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
