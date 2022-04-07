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
