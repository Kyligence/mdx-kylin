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

/**
 * 等待页面上所有的对话框和消息框消失
 * @param {WebDriver} driver WebDriver对象
 */
exports.waitingForPageClean = async function waitingForPageClean(driver) {
  try {
    const messageBoxWrappers = await driver.findElements(By.css('.el-message-box__wrapper'));
    for (const messageBoxWrapper of messageBoxWrappers) {
      try {
        await driver.wait(until.elementIsNotVisible(messageBoxWrapper), 1000);
      } catch {}
    }
  } catch {}

  try {
    const dialogWrappers = await driver.findElements(By.css('.el-dialog__wrapper'));
    for (const dialogWrapper of dialogWrappers) {
      try {
        await driver.wait(until.elementIsNotVisible(dialogWrapper), 1000);
      } catch {}
    }
  } catch {}
};

/**
 * 等待页面稳定500ms
 * @param {WebDriver} driver WebDriver对象
 */
exports.waitingForStable = async function waitingForStable(driver) {
  await driver.sleep(500);
};

/**
 * 等待容器的loading消失
 * @param {WebDriver} driver WebDriver对象
 * @param {String} loadingParentClass loading容器类字符串
 */
exports.waitingForLoading = async function waitingForLoading(driver, loadingParentClass) {
  try {
    await driver.wait(until.elementLocated(By.css(`${loadingParentClass} > div > div > div.el-loading-spinner`)), 1000);
  } catch {}
  try {
    await driver.wait(until.stalenessOf(await driver.findElement(By.css(`${loadingParentClass} > div > div > div.el-loading-spinner`))));
  } catch {}
};

exports.cancelTooltipAndPopover = async function waitingForLoading(driver) {
  const actions = await driver.actions({ bridge: true });
  const body = await driver.findElement(By.css('body'));
  // actions.move是以中点为起点移动
  const bodySize = await body.getRect();
  const bodyCenterX = 0;
  const bodyTopY = Math.floor(-bodySize.height / 2 + 1);
  await actions.move({ origin: body, x: bodyCenterX, y: bodyTopY }).click().perform();

  const popovers = await driver.findElements(By.css('.el-popover'));

  try {
    for (const popover of popovers) {
      await driver.wait(until.elementIsNotVisible(popover), 10000);
    }
  } catch {}
};

exports.waitingForButtonLoading = async function waitingForButtonLoading(driver, loadingButtonClass = '') {
  try {
    await driver.wait(until.elementLocated(By.css(`${loadingButtonClass}.el-button.is-loading`)), 1000);
    await driver.wait(until.stalenessOf(await driver.findElement(By.css(`${loadingButtonClass}.el-button.is-loading`))));
  } catch {}
};

exports.scrollTo = async function scrollTo(driver, scrollElementClass, scrollParentClass = '') {
  await driver.executeScript(`
    var rectSize = document.querySelector('${scrollElementClass}').getBoundingClientRect();
    var scrollParent = '${scrollParentClass}' ? document.querySelector('${scrollParentClass}') : window;
    if (scrollParent.scrollTo) {
      scrollParent.scrollTo(rectSize.x, rectSize.y + rectSize.height);
    } else {
      scrollParent.scrollTop = rectSize.y + rectSize.height;
      scrollParent.scrollLeft = rectSize.x;
    }
  `);
};

/* eslint-disable max-len */
exports.ensureElementVisiable = async function ensureElementVisiable(driver, locator, visible = true) {
  try {
    await driver.wait(until.elementLocated(locator), 10000);
    if (visible) {
      await driver.wait(until.elementIsVisible(await driver.findElement(locator)), 10000);
    }
  } catch {}
};

/**
 * JS辅助：在浏览器端打印内容
 * @param {WebDriver} driver WebDriver对象
 * @param  {...any} args 打印内容
 */
exports.logInBrowser = async function logInBrowser(driver, ...args) {
  const messages = args.map(arg => {
    switch (typeof arg) {
      case 'object': return `JSON.parse(JSON.stringify(${arg}))`;
      case 'string': return `'${arg}'`;
      default: return arg.toString();
    }
  });
  const scripts = messages.join(', ');
  await driver.executeScript(`
    console.log(${scripts});
  `);
};

/**
 * JS辅助：在浏览器端监听打印鼠标按下、移动、抬起事件
 * @param {WebDriver} driver WebDriver对象
 */
exports.traceMouseEvents = async function traceMouseEvents(driver) {
  await driver.executeScript(`
    function traceMouse(x, y) {
      var pointer = document.querySelector('.selenium-trace-pointer');

      if (!pointer) {
        pointer = document.createElement('div');
        pointer.className = 'selenium-trace-pointer';
        pointer.style.position = 'fixed';
        pointer.style.left = x + 'px';
        pointer.style.top = y + 'px';
        pointer.style.width = '2px';
        pointer.style.height = '2px';
        pointer.style.zIndex = '99999999';
        pointer.style.pointerEvents = 'none';
        pointer.style.backgroundColor = 'red';
        document.body.appendChild(pointer);
      } else {
        pointer.style.left = x + 'px';
        pointer.style.top = y + 'px';
      }
    }

    if (!handleMouseDown) {
      var handleMouseDown = function handleMouseDown(e) {
        console.log('mousedown', e.clientX, e.clientY);
      }
    }

    if (!handleMouseMove) {
      var handleMouseMove = function handleMouseMove(e) {
        traceMouse(e.clientX, e.clientY);
      }
    }

    if (!handleMouseUp) {
      var handleMouseUp = function handleMouseUp(e) {
        console.log('mouseup', e.clientX, e.clientY);
      }
    }

    window.removeEventListener('mousedown', handleMouseDown);
    window.removeEventListener('mousemove', handleMouseMove);
    window.removeEventListener('mouseup', handleMouseUp);

    window.addEventListener('mousedown', handleMouseDown);
    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
  `);
};

/**
 * JS辅助：在浏览器端停止监听鼠标按下、移动、抬起事件
 * @param {WebDriver} driver WebDriver对象
 */
exports.stopMouseEvents = async function stopMouseEvents(driver) {
  await driver.executeScript(`
    try {
      if (!handleMouseDown) {
        var handleMouseDown = function handleMouseDown(e) {
          console.log('mousedown', e.clientX, e.clientY);
        }
      }
    } catch (e) {
      var handleMouseDown = function handleMouseDown(e) {
        console.log('mousedown', e.clientX, e.clientY);
      }
    }
    try {
      if (!handleMouseMove) {
        var handleMouseDown = function handleMouseDown(e) {
          console.log('mousedown', e.clientX, e.clientY);
        }
      }
    } catch (e) {
      var handleMouseMove = function handleMouseMove(e) {
        console.log('mousedown', e.clientX, e.clientY);
      }
    }
    try {
      if (!handleMouseUp) {
        var handleMouseDown = function handleMouseDown(e) {
          console.log('mousedown', e.clientX, e.clientY);
        }
      }
    } catch (e) {
      var handleMouseUp = function handleMouseUp(e) {
        console.log('mousedown', e.clientX, e.clientY);
      }
    }

    window.removeEventListener('mousedown', handleMouseDown);
    window.removeEventListener('mousemove', handleMouseMove);
    window.removeEventListener('mouseup', handleMouseUp);
  `);
};
