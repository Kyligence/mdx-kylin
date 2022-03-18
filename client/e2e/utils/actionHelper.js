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
const { By, until } = require('selenium-webdriver');
const domHelper = require('./domHelper');
const { getParams } = require('./globalHelper');

/**
 * JS辅助：清除表单输入框的值
 * @param {WebDriver} driver WebDriver对象
 * @param {String} cssSelector 样式选择器
 */
exports.clearFormInput = async function clearFormInput(driver, cssSelector) {
  // 有时会清除失败，sleep300ms
  await driver.sleep(300);
  await driver.executeScript(`
    var input = document.querySelector("${cssSelector}");
    input.value = "";
    // dispatchEvent触发的是原生的event，不是react event。此处有待出解决方案。
    // input.dispatchEvent(new Event("change"));
  `);
};

/* eslint-disable max-len */
/**
 * JS辅助：拖拽目标元素放置在容器上
 * @param {WebDriver} driver WebDriver对象
 * @param {String} dragCssSelector 拖拽元素的样式选择器
 * @param {String} dropCssSelector 放置拖拽元素容器的样式选择器
 */
exports.dragAndDrop = async function dragAndDrop(driver, dragCssSelector, dropCssSelector) {
  try {
    await domHelper.ensureElementVisiable(driver, By.css(dragCssSelector));
    await domHelper.ensureElementVisiable(driver, By.css(dropCssSelector));
  } catch (e) {}
  await driver.executeScript(`
    function customEvent(typeOfEvent) {
      var event = document.createEvent("CustomEvent");
      event.initCustomEvent(typeOfEvent, true, true, null);
      event.dataTransfer = {
          data: {},
          setData: function (key, value) {
              this.data[key] = value;
          },
          getData: function (key) {
              return this.data[key];
          }
      };
      return event;
    }
    function dispatchEvent(element, event, transferData) {
      if (transferData !== undefined) {
          event.dataTransfer = transferData;
      }
      if (element.dispatchEvent) {
          element.dispatchEvent(event);
      } else if (element.fireEvent) {
          element.fireEvent("on" + event.type, event);
      }
    }
    (function() {
      var dragEl = document.querySelector('${dragCssSelector}');
      var dropEl = document.querySelector('${dropCssSelector}');
      var dragStartEvent = customEvent('dragstart');
      dispatchEvent(dragEl, dragStartEvent);
      var dropEvent = customEvent('drop');
      dispatchEvent(dropEl, dropEvent, dragStartEvent.dataTransfer);
      var dragEndEvent = customEvent('dragend');
      dispatchEvent(dragEl, dragEndEvent, dropEvent.dataTransfer);
    })()
  `);
};

/**
 * Hover在某个元素上
 * @param {WebDriver} driver WebDriver对象
 * @param {Locator} locator By选择器
 */
exports.hoverOn = async function hoverOn(driver, locator) {
  await domHelper.ensureElementVisiable(driver, locator);

  const el = await driver.findElement(locator);
  // 有时会报错，所以sleep300毫秒
  // Failed to execute 'elementsFromPoint' on 'Document': The provided double value is non-finite.
  await driver.sleep(300);
  await driver.actions({ bridge: true }).move({ origin: el }).perform();
};

/**
 * Hover在下拉菜单上
 * @param {WebDriver} driver WebDriver对象
 * @param {Locator} locator By选择器
 */
exports.hoverOnDropdown = async function hoverOnDropdown(driver, cssSelector) {
  await exports.hoverOn(driver, By.css(cssSelector));
  await domHelper.ensureElementVisiable(driver, By.css(`${cssSelector} .el-dropdown-menu`));
};

/**
 * Click在某个元素上
 * @param {WebDriver} driver WebDriver对象
 * @param {Locator} locator By选择器
 */
exports.clickOn = async function clickOn(driver, locator) {
  await domHelper.ensureElementVisiable(driver, locator);

  await driver.findElement(locator).click();
  await driver.sleep(100);
};

async function selectOptionByIdx(cssSelector, options, selectIdx) {
  if (options[selectIdx]) {
    await options[selectIdx].click();
  } else {
    throw new Error(`Cannot find select '${cssSelector}' option idx '${selectIdx}.'`);
  }
}

async function selectOptionByValue(cssSelector, options, optionsTexts, selectValue) {
  const optionIdx = optionsTexts.findIndex(text => text === selectValue);
  if (optionIdx !== -1) {
    await options[optionIdx].click();
  } else {
    throw new Error(`Cannot find select '${cssSelector}' option value '${selectValue}.'`);
  }
}

function selectOption(cssSelector, options, optionsTexts, valueOrIdx) {
  switch (typeof valueOrIdx) {
    case 'string': return selectOptionByValue(cssSelector, options, optionsTexts, valueOrIdx);
    case 'number': return selectOptionByIdx(cssSelector, options, valueOrIdx);
    default: return null;
  }
}

/**
 * 点击下拉框中的选项
 * @param {WebDriver} driver WebDriver对象
 * @param {string} cssSelector 样式选择器
 * @param {number|Array} optionIdx 下拉框的索引id
 */
exports.changeFormSelect = async function changeFormSelect(driver, cssSelector, value, hasSearch = false) {
  await exports.clickOn(driver, By.css(`${cssSelector} .el-input`));

  await domHelper.ensureElementVisiable(driver, By.css(`${cssSelector} .el-select-dropdown__list`));
  await domHelper.ensureElementVisiable(driver, By.css(`${cssSelector} .el-select-dropdown__item:nth-child(1)`));

  const options = await driver.findElements(By.css(`${cssSelector} .el-select-dropdown__item`));
  const optionsTexts = await Promise.all(options.map(option => option.getText()));

  if (value instanceof Array) {
    for (const item of value) {
      await selectOption(cssSelector, options, optionsTexts, item);
    }
    await exports.clickOn(driver, By.css(`${cssSelector} .el-kylin-more`));
  } else {
    await selectOption(cssSelector, options, optionsTexts, value);
  }
};

/**
 * 改变输入框的内容
 * @param {WebDriver} driver WebDriver对象
 * @param {string} cssSelector 样式选择器
 * @param {string} value 改变的值
 */
exports.changeFormInput = async function changeFormInput(driver, cssSelector, value) {
  await domHelper.ensureElementVisiable(driver, By.css(`${cssSelector} input`));

  await exports.clearFormInput(driver, `${cssSelector} input`);
  await driver.findElement(By.css(`${cssSelector} input`)).sendKeys(value);
};

/**
 * 改变输入框的内容
 * @param {WebDriver} driver WebDriver对象
 * @param {string} cssSelector 样式选择器
 * @param {string} value 改变的值
 */
exports.changeFormTextarea = async function changeFormTextarea(driver, cssSelector, value) {
  await domHelper.ensureElementVisiable(driver, By.css(`${cssSelector} textarea`));

  await exports.clearFormInput(driver, `${cssSelector} textarea`);
  await driver.findElement(By.css(`${cssSelector} textarea`)).sendKeys(value);
};

/**
 * 改变代码输入框的内容
 * @param {WebDriver} driver WebDriver对象
 * @param {string} cssSelector 样式选择器
 * @param {string} value 改变的值
 */
exports.changeFormCodeEditor = async function changeFormCodeEditor(driver, cssSelector, value) {
  await domHelper.ensureElementVisiable(driver, By.css(`${cssSelector} textarea`), false);

  await exports.clearFormInput(driver, `${cssSelector} textarea`);
  await driver.findElement(By.css(`${cssSelector} textarea`)).sendKeys(value);
};

/**
 * 改变级联选择框的内容
 * @param {WebDriver} driver WebDriver对象
 * @param {string} cssSelector 样式选择器
 * @param {string} value 改变的值
 */
exports.changeFormCascader = async function changeFormCascader(driver, cssSelector, optionsIdx) {
  await exports.clickOn(driver, By.css(`${cssSelector} input`));

  for (let i = 0; i < optionsIdx.length; i += 1) {
    const optionIdx = optionsIdx[i];
    try {
      await driver.sleep(300);
      await driver.wait(until.elementLocated(By.css(`${cssSelector} .el-cascader-menu:nth-child(${i + 1}) .el-cascader-menu__item:nth-child(${optionIdx})`)), 1000);
    } catch (e) {}
    await driver.findElement(By.css(`${cssSelector} .el-cascader-menu:nth-child(${i + 1}) .el-cascader-menu__item:nth-child(${optionIdx})`)).click();
  }
};

/**
 * 改变可输入下拉框的内容
 * @param {WebDriver} driver WebDriver对象
 * @param {string} cssSelector 样式选择器
 * @param {string|number} value 改变的值
 */
exports.changeFormAutoComplete = async function changeFormAutoComplete(driver, cssSelector, value) {
  const optionIdx = typeof value === 'number' ? value : null;
  if (optionIdx) {
    await exports.clickOn(driver, By.css(`${cssSelector} input`));
    await exports.clickOn(driver, By.css(`${cssSelector} .el-autocomplete-suggestion__list li:nth-child(${optionIdx})`));
  } else {
    await exports.changeFormInput(driver, By.css(`${cssSelector} input`), value);
  }
};

async function wrapperChrome45Option({ x = 0, y = 0, ...options }) {
  const newOptions = { ...options };

  if (options.origin) {
    const { width, height } = await options.origin.getRect();
    newOptions.x = x + Math.round(width / 2);
    newOptions.y = y + Math.round(height / 2);
  }
  return newOptions;
}

async function wrapperFirefoxOption({ x = 0, y = 0, ...options }) {
  const newOptions = { ...options };

  newOptions.x = x / 2;
  newOptions.y = y / 2;

  return newOptions;
}

exports.wrapperActionOptions = async function wrapperActionOptions(driver, options) {
  const { browser, version } = await getParams(driver);

  switch (browser) {
    case 'chrome': {
      if (version <= 45) {
        return wrapperChrome45Option(options);
      }
      return options;
    }
    case 'firefox': {
      return wrapperFirefoxOption(options);
    }
    default: return options;
  }
};
