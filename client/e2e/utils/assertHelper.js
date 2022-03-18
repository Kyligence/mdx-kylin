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
const assert = require('assert');
const { By, until } = require('selenium-webdriver');
const { waitingForStable, waitingForPageClean, ensureElementVisiable } = require('./domHelper');
const { clickOn } = require('./actionHelper');

/**
 * 断言：元素中的文案是否正确
 * @param {*} driver WebDriver对象
 * @param {*} locator By选择器
 * @param {*} value 正确的文案
 */
exports.isCorrectText = async function isCorrectText(driver, locator, value) {
  await ensureElementVisiable(driver, locator);
  assert.strictEqual(await driver.findElement(locator).getText(), value);
};

/**
 * 断言：全局报错弹框的文案是否正确
 * @param {*} driver WebDriver对象
 * @param {*} value 正确的文案
 */
exports.isCorrectDialogError = async function isCorrectDialogError(driver, value) {
  // 等待弹框元素出现
  await driver.wait(until.elementIsVisible(driver.findElement(By.css('.mdx-it-global-message-modal'))), 10000);
  // 等待动画消失
  await waitingForStable(driver);
  // 验证文案是否正确
  await exports.isCorrectText(driver, By.css('.mdx-it-global-message-modal .dialog-message'), value);
  // 点击确认
  await clickOn(driver, By.css('.mdx-it-modal-confirm'));
  // 等待弹框消失
  await waitingForPageClean(driver);
};
