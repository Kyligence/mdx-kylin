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
const assert = require('assert');
const { createDriver, getLaunchUrl } = require('../utils/startup');
const { login, logout } = require('../utils/businessHelper');
const { waitingForLoading } = require('../utils/domHelper');
const { clickOn } = require('../utils/actionHelper');

const {
  USERNAME_ADMIN,
  PASSWORD_ADMIN,
} = process.env;

/* eslint-disable newline-per-chained-call */
describe('参数配置界面', async function () {
  this.timeout(300000);
  let driver;

  before(async () => {
    driver = await createDriver();
  });

  after(async () => {
    await driver.quit();
  });

  it('进入参数配置页面', async () => {
    await login(driver, USERNAME_ADMIN, PASSWORD_ADMIN);

    await driver.findElement(By.css('.page-header-right div:nth-child(1) button')).click();
    await driver.wait(until.stalenessOf(await driver.findElement(By.css('.global-admin-mask'))), 10000);
    await driver.findElement(By.css('.ant-menu li[link="/configuration"]')).click();
    await driver.wait(until.elementLocated(By.css('.configurations')), 10000);

    assert.equal(await driver.getCurrentUrl(), `${getLaunchUrl()}/configuration`);
  });

  it('重启同步任务', async () => {
    await driver.findElement(By.css('.configuration:nth-child(4) .el-button')).click();
    await waitingForLoading(driver, '.block-body');
  });

  it('修改连接用户', async () => {
    await clickOn(driver, By.css('.configuration:nth-child(5) .el-button'));
    await driver.wait(until.elementLocated(By.css('.connection-user-modal')), 10000);

    await driver.findElement(By.css('.connection-user-modal .input-username input')).sendKeys(USERNAME_ADMIN);
    await driver.findElement(By.css('.connection-user-modal .input-password input')).sendKeys(PASSWORD_ADMIN);
    await driver.findElement(By.css('.connection-user-modal .dialog-footer .el-button--primary')).click();

    await driver.wait(until.elementIsNotVisible(await driver.findElement(By.css('.connection-user-modal'))), 10000);

    await logout(driver);
  });
});
