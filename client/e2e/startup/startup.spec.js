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
/* eslint-disable newline-per-chained-call */
const { By, until } = require('selenium-webdriver');
const { createDriver, getLaunchUrl } = require('../utils/startup');
const { login, logout } = require('../utils/businessHelper');

const {
  USERNAME_ADMIN,
  PASSWORD_ADMIN,
} = process.env;

/* eslint-disable newline-per-chained-call */
describe('测试是否需要启动设置', async function () {
  this.timeout(300000);
  let driver;

  before(async () => {
    driver = await createDriver();
  });

  after(async () => {
    await driver.quit();
  });

  it('测试是否跳转至配置页', async () => {
    await login(driver, USERNAME_ADMIN, PASSWORD_ADMIN);

    await driver.wait(until.urlMatches(/\/configuration|\/dataset\/list/));
    if (await driver.getCurrentUrl() === `${getLaunchUrl()}/configuration`) {
      await driver.wait(until.elementLocated(By.css('.connection-user-modal')), 10000);

      await driver.findElement(By.css('.connection-user-modal .input-username input')).sendKeys(USERNAME_ADMIN);
      await driver.findElement(By.css('.connection-user-modal .input-password input')).sendKeys(PASSWORD_ADMIN);
      await driver.findElement(By.css('.connection-user-modal .dialog-footer .el-button--primary')).click();

      await driver.wait(until.elementIsNotVisible(await driver.findElement(By.css('.connection-user-modal'))), 10000);
    }
  });

  it('登出系统', async () => {
    await logout(driver);
  });
});
