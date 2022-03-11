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
const { createDriver, getLaunchUrl } = require('../utils/startup');
const { waitingForStable, waitingForLoading } = require('../utils/domHelper');
const { clickOn, changeFormInput, changeFormTextarea, clearFormInput } = require('../utils/actionHelper');
const { isCorrectText } = require('../utils/assertHelper');
const { login, logout } = require('../utils/businessHelper');

const {
  USERNAME_ADMIN,
  PASSWORD_ADMIN,
  USERNAME_PROJECT_ADMIN,
  USERNAME_PROJECT_MANAGEMENT,
} = process.env;

/* eslint-disable newline-per-chained-call */
describe('数据集角色界面', async function () {
  this.timeout(300000);
  let driver;

  before(async () => {
    driver = await createDriver();
  });

  after(async () => {
    await driver.quit();
  });

  it('进入数据集角色页面', async () => {
    await login(driver, USERNAME_ADMIN, PASSWORD_ADMIN);

    await clickOn(driver, By.css('.mdx-it-management-button'));
    await driver.wait(until.stalenessOf(await driver.findElement(By.css('.mdx-it-global-admin-mask'))), 10000);
    await driver.wait(until.elementLocated(By.css('.mdx-it-dataset-roles-page')), 10000);

    assert.strictEqual(await driver.getCurrentUrl(), `${getLaunchUrl()}/dataset-role/list`);
  });

  it('默认选中一个角色', async () => {
    await driver.wait(until.elementLocated(By.css('.custom-roles .collapse-item.active span')), 10000);
  });

  it('选中Admin角色', async () => {
    await clickOn(driver, By.css('.mdx-it-default-roles .collapse-item span'));
    await waitingForLoading(driver, '.layout-right-content .form-content');

    await isCorrectText(driver, By.css('.mdx-it-default-roles .collapse-item.active span'), 'Admin');
    await isCorrectText(driver, By.css('.mdx-it-current-dataset-role'), 'Dataset Role:Admin');
  });

  it('创建数据集角色', async () => {
    await clickOn(driver, By.css('.mdx-it-add-role-button'));
    await isCorrectText(driver, By.css('.el-tabs__item.is-active'), 'User');

    await changeFormInput(driver, '.mdx-it-role-name-input', 'IT_TEST_ROLE');

    await changeFormInput(driver, '.el-transfer-panel:nth-child(1) .el-transfer-panel__filter', USERNAME_PROJECT_ADMIN);
    await clickOn(driver, By.css('.el-transfer-panel:nth-child(1) .el-checkbox:nth-child(1) .el-checkbox__inner'));
    await clickOn(driver, By.css('.el-transfer .el-icon-arrow-right'));

    await changeFormInput(driver, '.el-transfer-panel:nth-child(1) .el-transfer-panel__filter', USERNAME_PROJECT_MANAGEMENT);
    await clickOn(driver, By.css('.el-transfer-panel:nth-child(1) .el-checkbox:nth-child(1) .el-checkbox__inner'));
    await clickOn(driver, By.css('.el-transfer .el-icon-arrow-right'));

    await clickOn(driver, By.css('.el-tabs__item[name="description"]'));
    await changeFormTextarea(driver, '.mdx-it-dataset-role-description', 'Some descriptions');

    await clickOn(driver, By.css('.layout-footer .el-button--primary'));
  });

  it('展示数据集角色', async () => {
    await driver.wait(until.elementLocated(By.css('.custom-roles .collapse-item.active span')), 10000);
    assert.strictEqual(await driver.findElement(By.css('.custom-roles .collapse-item.active span')).getText(), 'IT_TEST_ROLE');
    assert.strictEqual(await driver.findElement(By.css('.layout-right-content .title-sm')).getText(), 'Dataset Role:IT_TEST_ROLE');

    await driver.wait(until.elementLocated(By.css('.el-tabs__item[name="users"]')), 10000);
    await driver.findElement(By.css('.el-tabs__item[name="users"]')).click();

    const webElements = await driver.findElements(By.css('.layout-right-content .el-table .el-table__row'));
    assert.strictEqual(await webElements[0].getText(), USERNAME_PROJECT_MANAGEMENT);
    assert.strictEqual(await webElements[1].getText(), USERNAME_PROJECT_ADMIN);

    await driver.findElement(By.css('.el-tabs__item[name="description"]')).click();
    assert.strictEqual(await driver.findElement(By.css('textarea')).getText(), 'Some descriptions');
  });

  it('修改数据集角色', async () => {
    await driver.findElement(By.css('.layout-right-content .actions .el-button:nth-child(1)')).click();

    await driver.findElement(By.css('.el-tabs__item[name="users"]')).click();

    await driver.wait(until.elementLocated(By.css('.layout-right-content .el-transfer')), 10000);

    await clearFormInput(driver, '.el-transfer-panel:nth-child(3) .el-transfer-panel__filter input');
    await driver.findElement(By.css('.el-transfer-panel:nth-child(3) .el-transfer-panel__filter input')).sendKeys(USERNAME_PROJECT_ADMIN);
    await driver.findElement(By.css('.el-transfer-panel:nth-child(3) .el-checkbox:nth-child(1) .el-checkbox__inner')).click();
    await driver.findElement(By.css('.el-transfer .el-icon-arrow-left')).click();

    await clearFormInput(driver, '.el-transfer-panel:nth-child(3) .el-transfer-panel__filter input');
    await driver.findElement(By.css('.el-transfer-panel:nth-child(3) .el-transfer-panel__filter input')).sendKeys(USERNAME_PROJECT_MANAGEMENT);
    await driver.findElement(By.css('.el-transfer-panel:nth-child(3) .el-checkbox:nth-child(1) .el-checkbox__inner')).click();
    await driver.findElement(By.css('.el-transfer .el-icon-arrow-left')).click();

    await driver.findElement(By.css('.layout-footer .el-button--primary')).click();
  });

  it('删除数据集角色', async () => {
    await driver.wait(until.elementLocated(By.css('.layout-right-content .actions .el-button:nth-child(2)')), 10000);
    await waitingForStable(driver);
    await clickOn(driver, By.css('.layout-right-content .actions .el-button:nth-child(2)'));

    await driver.wait(until.elementLocated(By.css('.el-message-box')), 10000);
    await clickOn(driver, By.css('.el-message-box .el-button--primary'));

    await driver.wait(until.stalenessOf(await driver.findElement(By.css('.el-message-box'))), 10000);

    await logout(driver);
  });
});
