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
const dayjs = require('dayjs');
const { createDriver, getLaunchUrl } = require('../utils/startup');
const { waitingForStable } = require('../utils/domHelper');
const { login, logout } = require('../utils/businessHelper');

const {
  USERNAME_ADMIN,
  PASSWORD_ADMIN,
} = process.env;

const endDate = dayjs().endOf('day').format('YYYY.MM.DD');
const startDate = dayjs().endOf('day').startOf('day').format('YYYY.MM.DD');
const startDate3Day = dayjs().endOf('day').add(-3, 'day').add(1, 'millisecond').startOf('day').format('YYYY.MM.DD');
const startDate7Day = dayjs().endOf('day').add(-7, 'day').add(1, 'millisecond').startOf('day').format('YYYY.MM.DD');
const startDate1Month = dayjs().endOf('day').add(-1, 'month').add(1, 'millisecond').startOf('day').format('YYYY.MM.DD');

/* eslint-disable newline-per-chained-call */
describe('诊断包界面', async function () {
  this.timeout(300000);
  let driver;

  before(async () => {
    driver = await createDriver();
  });

  after(async () => {
    await driver.quit();
  });

  it('进入诊断页面', async () => {
    await login(driver, USERNAME_ADMIN, PASSWORD_ADMIN);

    await driver.findElement(By.css('.page-header-right div:nth-child(1) button')).click();
    await driver.wait(until.stalenessOf(await driver.findElement(By.css('.global-admin-mask'))), 10000);
    await driver.findElement(By.css('.ant-menu li[link="/diagnosis"]')).click();
    await driver.wait(until.elementLocated(By.css('.diagnosis')), 10000);

    assert.equal(await driver.getCurrentUrl(), `${getLaunchUrl()}/diagnosis`);
  });

  it('默认选择当天诊断包', async () => {
    await driver.wait(until.elementLocated(By.css('.is-checked-label')), 10000);
    assert.equal(await driver.findElement(By.css('.is-checked-label')).getText(), 'Today');
    assert.equal(await driver.findElement(By.css('.date-range-field .el-form-item__content')).getText(), `${startDate} - ${endDate}`);
  });

  it('选择最近三日诊断包', async () => {
    await driver.findElement(By.css('.el-radio-group .el-radio:nth-child(2) .el-radio__inner')).click();
    assert.equal(await driver.findElement(By.css('.date-range-field .el-form-item__content')).getText(), `${startDate3Day} - ${endDate}`);
  });

  it('选择最近七日诊断包', async () => {
    await driver.findElement(By.css('.el-radio-group .el-radio:nth-child(3) .el-radio__inner')).click();
    assert.equal(await driver.findElement(By.css('.date-range-field .el-form-item__content')).getText(), `${startDate7Day} - ${endDate}`);
  });

  it('选择最近一月诊断包', async () => {
    await driver.findElement(By.css('.el-radio-group .el-radio:nth-child(4) .el-radio__inner')).click();
    assert.equal(await driver.findElement(By.css('.date-range-field .el-form-item__content')).getText(), `${startDate1Month} - ${endDate}`);
  });

  it('生成当天诊断包', async () => {
    await driver.findElement(By.css('.el-radio-group .el-radio:nth-child(1) .el-radio__inner')).click();
    assert.equal(await driver.findElement(By.css('.date-range-field .el-form-item__content')).getText(), `${startDate} - ${endDate}`);

    await driver.findElement(By.css('.el-select .el-input__inner')).click();
    await driver.wait(until.elementLocated(By.css('.el-select-dropdown .el-select-dropdown__item:first-child')), 10000);
    await waitingForStable(driver);
    await driver.findElement(By.css('.el-select-dropdown .el-select-dropdown__item:first-child')).click();
    await driver.findElement(By.css('.el-select .el-input__inner')).click();

    await driver.findElement(By.css('.package-actions .el-button:first-child')).click();

    await driver.wait(until.elementLocated(By.css('.diagnosis-progress')));
    await driver.wait(until.elementIsEnabled(await driver.findElement(By.css('.diagnosis-progress .el-button'))));
    await driver.findElement(By.css('.diagnosis-progress .el-button')).click();

    await logout(driver);
  });
});
