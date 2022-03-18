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
const { createDriver } = require('../utils/startup');
const { login, logout, changeProject } = require('../utils/businessHelper');
const { clickOn, changeFormSelect } = require('../utils/actionHelper');
const { waitingForLoading, cancelTooltipAndPopover, scrollTo } = require('../utils/domHelper');

const {
  USERNAME_ADMIN,
  PASSWORD_ADMIN,
  PROJECT_NAME,
} = process.env;

/* eslint-disable newline-per-chained-call */
describe('查询历史界面展示', async function () {
  this.timeout(300000);
  let driver;

  before(async () => {
    driver = await createDriver();

    await login(driver, USERNAME_ADMIN, PASSWORD_ADMIN);
    await driver.wait(until.urlContains('/dataset/list'));
    await changeProject(driver, PROJECT_NAME);
  });

  after(async () => {
    await logout(driver);

    await driver.quit();
  });

  it('点击查询历史菜单，展示界面', async () => {
    await clickOn(driver, By.css('.ant-menu li[link="/query-history"]'));
    await waitingForLoading(driver, '.layout-table');

    const historyRows = await driver.findElements(By.css('.query-history-row'));
    assert.strictEqual(historyRows.length, 10);
  });

  it('分页增加到20条，展示界面', async () => {
    await scrollTo(driver, '.query-history-pagination', '.layout-content');
    await changeFormSelect(driver, '.query-history-pagination .el-pagination__sizes .el-select', '20 /page');
    await waitingForLoading(driver, '.layout-table');
    await cancelTooltipAndPopover(driver);

    const historyRows = await driver.findElements(By.css('.query-history-row'));
    assert.strictEqual(historyRows.length, 20);
  });

  it('切到第二页，展示界面', async () => {
    await scrollTo(driver, '.query-history-pagination', '.layout-content');
    await clickOn(driver, By.css('.query-history-pagination .number:nth-child(2)'));
    await waitingForLoading(driver, '.layout-table');

    const historyRows = await driver.findElements(By.css('.query-history-row'));
    assert.strictEqual(historyRows.length, 11);
  });

  it('恢复到初始状态', async () => {
    await scrollTo(driver, '.query-history-pagination', '.layout-content');
    await changeFormSelect(driver, '.query-history-pagination .el-pagination__sizes .el-select', '10 /page');
    await waitingForLoading(driver, '.layout-table');
    await cancelTooltipAndPopover(driver);

    await scrollTo(driver, '.query-history-pagination', '.layout-content');
    await clickOn(driver, By.css('.query-history-pagination .number:nth-child(1)'));
    await waitingForLoading(driver, '.layout-table');

    const historyRows = await driver.findElements(By.css('.query-history-row'));
    assert.strictEqual(historyRows.length, 10);
  });

  it('表头按执行时间排序', async () => {
    let firstRowExcuteTime = null;

    await clickOn(driver, By.css('.table-header-execute-time'));
    await waitingForLoading(driver, '.layout-table');

    firstRowExcuteTime = await driver.findElement(By.css('.query-history-row:nth-child(1) .table-body-execute-time'));
    assert.strictEqual(await firstRowExcuteTime.getText(), '< 1s');

    await clickOn(driver, By.css('.table-header-execute-time'));
    await waitingForLoading(driver, '.layout-table');

    firstRowExcuteTime = await driver.findElement(By.css('.query-history-row:nth-child(1) .table-body-execute-time'));
    assert.strictEqual(await firstRowExcuteTime.getText(), '4 s');
  });

  it('展开第一行表格，展示界面', async () => {
    await clickOn(driver, By.css('.query-history-row:nth-child(1) .el-table__expand-column'));
    await waitingForLoading(driver, '.query-list');

    const queryItemRows = await driver.findElements(By.css('.table-body-query-item'));
    assert.strictEqual(queryItemRows.length, 2);
  });
});
