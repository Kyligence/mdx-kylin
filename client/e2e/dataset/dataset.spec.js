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
/* eslint-disable newline-per-chained-call, max-len */
const { By, until } = require('selenium-webdriver');
const assert = require('assert');
const { createDriver } = require('../utils/startup');
const { login, logout, changeProject } = require('../utils/businessHelper');
const { waitingForStable, waitingForLoading, waitingForPageClean, waitingForButtonLoading } = require('../utils/domHelper');
const { changeFormInput, dragAndDrop, changeFormSelect, changeFormCodeEditor, clickOn, hoverOn, changeFormCascader, changeFormAutoComplete, wrapperActionOptions } = require('../utils/actionHelper');
const { isCorrectText } = require('../utils/assertHelper');

const {
  USERNAME_ADMIN,
  PASSWORD_ADMIN,
  PROJECT_NAME,
  DATASET_MODEL_NAME_1,
  DATASET_MODEL_NAME_2,
} = process.env;

describe('数据集编辑界面', async function () {
  this.timeout(300000);
  let driver;

  before(async () => {
    driver = await createDriver();

    await login(driver, USERNAME_ADMIN, PASSWORD_ADMIN);
    await driver.wait(until.urlContains('/dataset/list'));
    await changeProject(driver, PROJECT_NAME);
    await waitingForStable(driver);
  });

  after(async () => {
    await driver.quit();
  });

  it('进入创建数据集页面', async () => {
    await clickOn(driver, By.css('.layout-actions .pull-left .el-button:nth-child(1)'));
    await driver.wait(until.elementLocated(By.css('.steps-form-wrapper')), 10000);
  });

  it('输入数据集基本信息', async () => {
    await changeFormInput(driver, '.steps-form-wrapper .el-form-item:nth-child(4)', 'IT_TEST_DATASET');
    await clickOn(driver, By.css('.steps-actions .el-button--primary'));
  });

  it('添加模型关系', async () => {
    await waitingForLoading(driver, '.model-list-wrapper');
    // 搜索模型1，并且加入画布
    await changeFormInput(driver, '.model-filter-wrapper .el-input', DATASET_MODEL_NAME_1);
    await dragAndDrop(driver, '.dragable-model-item:nth-child(2)', '.drop-model-container');
    // 搜索模型2，并且加入画布
    await changeFormInput(driver, '.model-filter-wrapper .el-input', DATASET_MODEL_NAME_2);
    await dragAndDrop(driver, '.dragable-model-item:nth-child(2)', '.drop-model-container');

    const action1 = await driver.actions({ bridge: true });
    const action2 = await driver.actions({ bridge: true });
    const action3 = await driver.actions({ bridge: true });
    const model1 = await driver.findElement(By.css('.draggable-element:nth-child(1)'));
    const model2 = await driver.findElement(By.css('.draggable-element:nth-child(2)'));
    const model1Right = await driver.findElement(By.css('.operation-area .jtk-endpoint:nth-child(3)'));

    // 在画布中移动模型1的位置
    await action1.move(await wrapperActionOptions(driver, { origin: model2 })).perform();
    await action1.press().perform();
    await action1.move(await wrapperActionOptions(driver, { origin: model2, x: 300, y: 0 })).perform();
    await action1.release().perform();

    // 在画布中移动模型2的位置
    await action2.move(await wrapperActionOptions(driver, { origin: model1 })).perform();
    await action2.press().perform();
    await action2.move(await wrapperActionOptions(driver, { origin: model1, x: 10, y: 0 })).perform();
    await action2.release().perform();

    // 模型1连接到模型2
    await action3.move(await wrapperActionOptions(driver, { origin: model1Right })).perform();
    await action3.press().perform();
    await action3.move(await wrapperActionOptions(driver, { origin: model1Right, x: 25, y: 0 })).perform();
    await action3.release().perform();

    // 判断模型关系弹框是否弹出
    await driver.wait(until.elementIsVisible(await driver.findElement(By.css('.create-relationship-modal'))), 10000);
    // 检查模型是否正确
    const leftModel = await driver.findElement(By.css('.model-select-wrapper .el-form-item:nth-child(2) input')).getAttribute('value');
    const rightModel = await driver.findElement(By.css('.model-select-wrapper .el-form-item:nth-child(4) input')).getAttribute('value');
    assert.equal(leftModel, DATASET_MODEL_NAME_1);
    assert.equal(rightModel, DATASET_MODEL_NAME_2);

    // 选定一组公共维表
    await changeFormSelect(driver, '.table-select-wrapper .double-selector-wrapper:nth-child(1) .el-form-item:nth-child(2)', 'BUYER_ACCOUNT');
    await changeFormSelect(driver, '.table-select-wrapper .double-selector-wrapper:nth-child(1) .el-form-item:nth-child(4)', 'BUYER_ACCOUNT');
    // 添加一组公共维表
    await clickOn(driver, By.css('.table-select-wrapper .double-selector-wrapper:nth-child(1) .actions-bar .el-button:nth-child(1)'));
    // 选定一组公共维表
    await changeFormSelect(driver, '.table-select-wrapper .double-selector-wrapper:nth-child(2) .el-form-item:nth-child(2)', 'SELLER_ACCOUNT');
    await changeFormSelect(driver, '.table-select-wrapper .double-selector-wrapper:nth-child(2) .el-form-item:nth-child(4)', 'SELLER_ACCOUNT');
    // 保存公共维表关系
    await driver.findElement(By.css('.create-relationship-modal .dialog-footer .el-button--primary')).click();

    await waitingForPageClean(driver);
    // 进入下一步
    await driver.findElement(By.css('.steps-actions .el-button--primary')).click();
  });

  it('跳转数据集语义', async () => {
    // 等待loading结束
    await waitingForLoading(driver, '.steps-content');
    const model1El = await driver.findElement(By.id(DATASET_MODEL_NAME_1));
    const model2El = await driver.findElement(By.id(DATASET_MODEL_NAME_2));
    // 展开模型1、模型2
    await driver.actions({ bridge: true }).move({ origin: model1El }).click().perform();
    await driver.sleep(1000);
    await driver.actions({ bridge: true }).move({ origin: model2El }).click().perform();
    // 判断模型1中公共维表是否正确
    let commonTableClass = await driver
      .findElement(By.id(`${DATASET_MODEL_NAME_1}-BUYER_ACCOUNT`))
      .findElement(By.css('.perfix-icon'))
      .getAttribute('class');
    assert.equal(commonTableClass.includes('icon-superset-common_dim'), true);
    // 判断模型1中公共维表是否正确
    commonTableClass = await driver
      .findElement(By.id(`${DATASET_MODEL_NAME_1}-SELLER_ACCOUNT`))
      .findElement(By.css('.perfix-icon'))
      .getAttribute('class');
    assert.equal(commonTableClass.includes('icon-superset-common_dim'), true);
  });

  it('修改维度表', async () => {
    // 过滤类型选择维度表、过滤名称输入维度表
    await changeFormSelect(driver, '.mdx-it-dimension-bar-type-filter', 0);
    await changeFormInput(driver, '.mdx-it-dimension-bar-name-filter', 'KYLIN_CAL_DT');
    // 编辑维度表
    await hoverOn(driver, By.id(`${DATASET_MODEL_NAME_1}-KYLIN_CAL_DT`));
    await clickOn(driver, By.css(`span#${DATASET_MODEL_NAME_1}-KYLIN_CAL_DT + i`));
    // 输入信息
    await changeFormInput(driver, '.dimension-table-block .el-form-item:nth-child(3)', '  Date Table  ');
    await changeFormSelect(driver, '.dimension-table-block .el-form-item:nth-child(4)', 'Time');
    // 保存
    await clickOn(driver, By.css('.block-footer .el-button--primary'));
    // 保存内容是否正确
    await isCorrectText(driver, By.id(`${DATASET_MODEL_NAME_1}-KYLIN_CAL_DT`), 'Date Table');
    await isCorrectText(driver, By.css('.dimension-table-block tr:nth-child(1) td:nth-child(2)'), 'KYLIN_CAL_DT');
    await isCorrectText(driver, By.css('.dimension-table-block tr:nth-child(2) td:nth-child(2)'), 'Date Table');
    await isCorrectText(driver, By.css('.dimension-table-block tr:nth-child(3) td:nth-child(2)'), 'Time');
  });

  it('修改维度', async () => {
    // 过滤类型选择维度列、过滤名称输入维度列
    await changeFormSelect(driver, '.mdx-it-dimension-bar-type-filter', 1);
    await changeFormInput(driver, '.mdx-it-dimension-bar-name-filter', 'YEAR');
    // 编辑维度列
    await hoverOn(driver, By.id(`${DATASET_MODEL_NAME_1}-KYLIN_CAL_DT-c-YEAR_BEG_DT`));
    await clickOn(driver, By.css(`span#${DATASET_MODEL_NAME_1}-KYLIN_CAL_DT-c-YEAR_BEG_DT + i`));
    // 输入信息
    await changeFormInput(driver, '.dimension-column-block .el-form-item:nth-child(3)', '  Year  ');
    await changeFormSelect(driver, '.dimension-column-block .el-form-item:nth-child(5)', 'Year');
    // 保存
    await clickOn(driver, By.css('.block-footer .el-button--primary'));
    // 保存内容是否正确
    await isCorrectText(driver, By.id(`${DATASET_MODEL_NAME_1}-KYLIN_CAL_DT-c-YEAR_BEG_DT`), 'Year');
    await isCorrectText(driver, By.css('.dimension-column-block tr:nth-child(1) td:nth-child(2)'), 'YEAR_BEG_DT');
    await isCorrectText(driver, By.css('.dimension-column-block tr:nth-child(2) td:nth-child(2)'), 'Year');
    await isCorrectText(driver, By.css('.dimension-column-block tr:nth-child(4) td:nth-child(2)'), 'Year');
  });

  it('创建层级维度', async () => {
    await clickOn(driver, By.css('.dimension-bar .card-action:nth-child(1)'));
    await changeFormInput(driver, '.hierarchy-block .el-form-item:nth-child(2)', '  Date Hierarchy  ');
    await changeFormCascader(driver, '.hierarchy-block .el-form-item:nth-child(3)', [1, 3]);
    await changeFormSelect(driver, '.hierarchy-block .el-form-item:nth-child(4)', ['Year', 'QTR_BEG_DT', 'MONTH_BEG_DT']);
    await clickOn(driver, By.css('.block-footer .el-button--primary'));
  });

  it('修改层级维度', async () => {
    // 过滤类型选择层级维度、过滤名称输入层级维度
    await changeFormSelect(driver, '.mdx-it-dimension-bar-type-filter', 2);
    await changeFormInput(driver, '.mdx-it-dimension-bar-name-filter', 'Date Hierarchy');

    await hoverOn(driver, By.id(`${DATASET_MODEL_NAME_1}-KYLIN_CAL_DT-h-Date Hierarchy`));
    await clickOn(driver, By.css(`span[id='${DATASET_MODEL_NAME_1}-KYLIN_CAL_DT-h-Date Hierarchy'] + i`));

    await changeFormInput(driver, '.hierarchy-block .el-form-item:nth-child(2)', '  Date Hierarchy_1  ');
    await clickOn(driver, By.css('.block-footer .el-button--primary'));

    await isCorrectText(driver, By.css('.mdx-it-hierarchy-display-name'), 'Date Hierarchy_1');
  });

  it('创建命名集', async () => {
    await clickOn(driver, By.css('.mdx-it-add-namedset'));
    await changeFormInput(driver, '.mdx-it-namedset-name-input', '  Date Namedset  ');
    await changeFormCodeEditor(driver, '.mdx-it-namedset-expression-input', '[Date Table].[Year].[Year].Members');

    await clickOn(driver, By.css('.mdx-tip-content'));
    await waitingForLoading(driver, '.mdx-it-namedset-expression-validate-loading');

    await clickOn(driver, By.css('.mdx-it-namedset-submit'));
    await waitingForButtonLoading(driver, '.mdx-it-namedset-submit');
  });

  it('编辑命名集', async () => {
    // 过滤类型选择层级维度、过滤名称输入层级维度
    await changeFormSelect(driver, '.mdx-it-dimension-bar-type-filter', 3);
    await changeFormInput(driver, '.mdx-it-dimension-bar-name-filter', 'Date Namedset');

    await hoverOn(driver, By.id('namedSetRoot-Date Namedset'));
    await clickOn(driver, By.css('span[id=\'namedSetRoot-Date Namedset\'] + i'));

    await changeFormInput(driver, '.mdx-it-namedset-name-input', '  Date Namedset_1  ');
    await clickOn(driver, By.css('.mdx-it-namedset-submit'));

    await waitingForButtonLoading(driver, '.mdx-it-namedset-submit');

    await isCorrectText(driver, By.css('.mdx-it-namedset-display-name'), 'Date Namedset_1');
  });

  it('修改度量', async () => {
    // 过滤类型选择度量、过滤名称输入度量
    await changeFormSelect(driver, '.mdx-it-measure-bar-type-filter', 0);
    await changeFormInput(driver, '.mdx-it-measure-bar-name-filter', 'COUNT_ALL');
    // 编辑度量
    await hoverOn(driver, By.id(`${DATASET_MODEL_NAME_1}-COUNT_ALL`));
    await clickOn(driver, By.css(`span[id='${DATASET_MODEL_NAME_1}-COUNT_ALL'] + i`));
    // 输入信息
    await changeFormInput(driver, '.mdx-it-measure-alias-input', '  总数 COUNT  ');
    await changeFormAutoComplete(driver, '.mdx-it-measure-format-input', 2);
    await changeFormInput(driver, '.mdx-it-measure-folder-input', 'folder1\\folder2\\folder3');
    // 保存
    await clickOn(driver, By.css('.mdx-it-measure-submit'));
    // 保存内容是否正确
    await isCorrectText(driver, By.id(`${DATASET_MODEL_NAME_1}-COUNT_ALL`), '总数 COUNT');
    await isCorrectText(driver, By.css('.mdx-it-measure-display-name'), '总数 COUNT');
    await isCorrectText(driver, By.css('.mdx-it-measure-display-format'), '####');
    await isCorrectText(driver, By.css('.mdx-it-measure-display-folder'), 'folder1\\folder2\\folder3');
  });

  it('创建计算度量', async () => {
    await clickOn(driver, By.css('.mdx-it-add-cMeasure'));
    await changeFormInput(driver, '.mdx-it-cMeasure-name-input', '  Date CMeasure  ');
    await changeFormAutoComplete(driver, '.mdx-it-cMeasure-format-input', 2);
    await changeFormInput(driver, '.mdx-it-cMeasure-folder-input', 'folder1\\folder2\\folder3');
    await changeFormCodeEditor(driver, '.mdx-it-cMeasure-expression-input', '1');

    await clickOn(driver, By.css('.mdx-tip-content'));
    await waitingForLoading(driver, '.mdx-it-cMeasure-expression-validate-loading');

    await clickOn(driver, By.css('.mdx-it-cMeasure-submit'));
    await waitingForButtonLoading(driver, '.mdx-it-cMeasure-submit');
    // 保存内容是否正确
    await isCorrectText(driver, By.id('calculateMeasureRoot-Date CMeasure'), 'Date CMeasure');
    await isCorrectText(driver, By.css('.mdx-it-cMeasure-display-name'), 'Date CMeasure');
    await isCorrectText(driver, By.css('.mdx-it-cMeasure-display-format'), '####');
    await isCorrectText(driver, By.css('.mdx-it-cMeasure-display-folder'), 'folder1\\folder2\\folder3');
    await isCorrectText(driver, By.css('.mdx-it-cMeasure-display-expression'), '1');
  });

  it('编辑计算度量', async () => {
    // 过滤类型选择计算度量、过滤名称输入计算度量
    await changeFormSelect(driver, '.mdx-it-measure-bar-type-filter', 1);
    await changeFormInput(driver, '.mdx-it-measure-bar-name-filter', 'Date CMeasure');

    await hoverOn(driver, By.id('calculateMeasureRoot-Date CMeasure'));
    await clickOn(driver, By.css('span[id=\'calculateMeasureRoot-Date CMeasure\'] + i'));

    await changeFormInput(driver, '.mdx-it-cMeasure-name-input', '  Date CMeasure_1  ');
    await clickOn(driver, By.css('.mdx-it-cMeasure-submit'));
    await waitingForButtonLoading(driver, '.mdx-it-cMeasure-submit');

    await isCorrectText(driver, By.css('.mdx-it-cMeasure-display-name'), 'Date CMeasure_1');
  });

  it('进入翻译界面', async () => {
    await driver.findElement(By.css('.steps-actions .el-button--primary')).click();
    await driver.sleep(1000);
  });

  it('进入维度用法界面', async () => {
    await driver.findElement(By.css('.steps-actions .el-button--primary')).click();
    await driver.sleep(1000);
  });

  it('进入权限界面', async () => {
    await driver.findElement(By.css('.steps-actions .el-button--primary')).click();
    await driver.sleep(1000);
  });

  it('保存数据集', async () => {
    await driver.findElement(By.css('.steps-actions .el-button--primary')).click();
    await driver.wait(until.elementLocated(By.css('.mdx-it-datasets-page')));
  });

  it('删除数据集', async () => {
    await changeFormInput(driver, '.mdx-it-search-dataset', 'IT_TEST_DATASET');
    waitingForLoading(driver, '.mdx-it-dataset-search-loading');
    clickOn(driver, By.css('.el-table__row:first-child td:last-child .icon-superset-table_others'));
    clickOn(driver, By.css('.el-table__row:first-child td:last-child .mdx-it-delete'));

    clickOn(driver, By.css('.el-message-box .el-button--primary'));

    await changeFormInput(driver, '.mdx-it-search-dataset', '');
    await driver.sleep(1000);
    await logout(driver);
  });
});
