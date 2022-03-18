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
export function routerHook(prev, next) {
  const { nextProps, nextStates, redirectToLogin } = next;
  const { prevProps } = prev;
  const { currentUser: prevUser } = prevProps;
  const { isSystemInited, isDataInited } = nextStates;
  const {
    currentUser: nextUser,
    currentSite,
    menus,
    history,
  } = nextProps;

  if (isSystemInited) {
    const { name: currentSiteName } = currentSite || {};

    const isInvalidSite = !currentSite || (nextUser.username && currentSiteName === 'login');
    const isNotLogin = !nextUser.username;
    const isNotInLoginPage = currentSiteName !== 'login';
    const isNotInToolkitPage = currentSiteName !== 'toolkit';
    const isLoginSuccess = currentSiteName === 'login' && !prevUser.username && !!nextUser.username;

    if (isNotLogin && isNotInLoginPage && isNotInToolkitPage) {
      redirectToLogin();
    }
    if (isDataInited) {
      if (isLoginSuccess || isInvalidSite) {
        const [defaultMenu] = menus;
        history.push(defaultMenu.url);
      }
    }
  }
}
