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
const { By, until } = require('selenium-webdriver');
const { waitingForStable } = require('./domHelper');
const { changeFormInput, hoverOn, clickOn, changeFormSelect } = require('./actionHelper');
const { isCorrectText } = require('./assertHelper');

exports.login = async function login(driver, username, password) {
  await changeFormInput(driver, '.mdx-it-username-input', username);
  await changeFormInput(driver, '.mdx-it-password-input', password);
  await clickOn(driver, By.css('.mdx-it-login-button'));

  await driver.wait(until.urlMatches(/\/configuration|\/dataset\/list/));
  await isCorrectText(driver, By.css('.mdx-it-logged-in-username'), username);
};

exports.logout = async function logout(driver) {
  await hoverOn(driver, By.css('.mdx-it-logged-in-username'));
  await waitingForStable(driver);
  await clickOn(driver, By.css('.mdx-it-logout'));

  await driver.wait(until.elementLocated(By.css('.mdx-it-login-page')), 10000);
};

exports.changeProject = async function changeProject(driver, projectName) {
  await changeFormSelect(driver, '.project-selector', projectName);
};
