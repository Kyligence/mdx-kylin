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
