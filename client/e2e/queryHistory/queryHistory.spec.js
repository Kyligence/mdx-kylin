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
