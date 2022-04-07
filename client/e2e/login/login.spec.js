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
/* eslint-disable newline-per-chained-call */
const { By, until } = require('selenium-webdriver');
const { createDriver } = require('../utils/startup');
const { waitingForStable } = require('../utils/domHelper');
const { hoverOn, clickOn, changeFormInput } = require('../utils/actionHelper');
const { isCorrectText, isCorrectDialogError } = require('../utils/assertHelper');

const {
  USERNAME_ADMIN,
  PASSWORD_ADMIN,
  USERNAME_PROJECT_ADMIN,
  PASSWORD_PROJECT_ADMIN,
  USERNAME_PROJECT_MANAGEMENT,
  PASSWORD_PROJECT_MANAGEMENT,
  USERNAME_PROJECT_OPERATION,
  PASSWORD_PROJECT_OPERATION,
  USERNAME_PROJECT_QUERY,
  PASSWORD_PROJECT_QUERY,
} = process.env;

describe('登录登出', async function () {
  this.timeout(300000);
  let driver;

  before(async () => {
    driver = await createDriver();
  });

  after(async () => {
    await driver.quit();
  });

  it('空的表单登录', async () => {
    await changeFormInput(driver, '.mdx-it-username-input', '');
    await changeFormInput(driver, '.mdx-it-password-input', '');
    await clickOn(driver, By.css('.mdx-it-login-button'));

    await waitingForStable(driver);
    await isCorrectText(driver, By.css('.mdx-it-username-input + .el-form-item__error'), 'Please enter username.');
    await isCorrectText(driver, By.css('.mdx-it-password-input + .el-form-item__error'), 'Please enter password.');
  });

  it('用户管理员登录', async () => {
    await changeFormInput(driver, '.mdx-it-username-input', USERNAME_ADMIN);
    await changeFormInput(driver, '.mdx-it-password-input', PASSWORD_ADMIN);
    await clickOn(driver, By.css('.mdx-it-login-button'));

    await driver.wait(until.urlContains('/dataset/list'));
    await isCorrectText(driver, By.css('.mdx-it-logged-in-username'), USERNAME_ADMIN);
  });

  it('用户管理员登出', async () => {
    await hoverOn(driver, By.css('.mdx-it-logged-in-username'));
    await waitingForStable(driver);
    await clickOn(driver, By.css('.mdx-it-logout'));

    await driver.wait(until.urlContains('/login'));
    await driver.wait(until.elementLocated(By.css('.mdx-it-login-page')), 10000);
  });

  it('项目管理员登录', async () => {
    await changeFormInput(driver, '.mdx-it-username-input', USERNAME_PROJECT_ADMIN);
    await changeFormInput(driver, '.mdx-it-password-input', PASSWORD_PROJECT_ADMIN);
    await clickOn(driver, By.css('.mdx-it-login-button'));

    await driver.wait(until.urlContains('/dataset/list'));
    await isCorrectText(driver, By.css('.mdx-it-logged-in-username'), USERNAME_PROJECT_ADMIN);
  });

  it('项目管理员登出', async () => {
    await hoverOn(driver, By.css('.mdx-it-logged-in-username'));
    await waitingForStable(driver);
    await clickOn(driver, By.css('.mdx-it-logout'));

    await driver.wait(until.urlContains('/login'));
    await driver.wait(until.elementLocated(By.css('.mdx-it-login-page')), 10000);
  });

  it('Management登录不可', async () => {
    await changeFormInput(driver, '.mdx-it-username-input', USERNAME_PROJECT_MANAGEMENT);
    await changeFormInput(driver, '.mdx-it-password-input', PASSWORD_PROJECT_MANAGEMENT);
    await clickOn(driver, By.css('.mdx-it-login-button'));

    await isCorrectDialogError(driver, '[MDX-04010004] Access denied. Only Kylin system administrators or project administrators can access the Kylin MDX UI interface.');
  });

  it('Operation登录不可', async () => {
    await changeFormInput(driver, '.mdx-it-username-input', USERNAME_PROJECT_OPERATION);
    await changeFormInput(driver, '.mdx-it-password-input', PASSWORD_PROJECT_OPERATION);
    await clickOn(driver, By.css('.mdx-it-login-button'));

    await isCorrectDialogError(driver, '[MDX-04010004] Access denied. Only Kylin system administrators or project administrators can access the Kylin MDX UI interface.');
  });

  it('Query登录不可', async () => {
    await changeFormInput(driver, '.mdx-it-username-input', USERNAME_PROJECT_QUERY);
    await changeFormInput(driver, '.mdx-it-password-input', PASSWORD_PROJECT_QUERY);
    await clickOn(driver, By.css('.mdx-it-login-button'));

    await isCorrectDialogError(driver, '[MDX-04010004] Access denied. Only Kylin system administrators or project administrators can access the Kylin MDX UI interface.');
  });

  it('Query登录不可', async () => {
    await changeFormInput(driver, '.mdx-it-username-input', 'error_error_error');
    await changeFormInput(driver, '.mdx-it-password-input', 'error_error_error');
    await clickOn(driver, By.css('.mdx-it-login-button'));

    await isCorrectDialogError(driver, '[MDX-04010002] Invalid username or password.');
  });
});
