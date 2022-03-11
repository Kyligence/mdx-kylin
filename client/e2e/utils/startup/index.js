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
const { Builder, By } = require('selenium-webdriver');
const { clickOn } = require('../actionHelper');
const { traceMouseEvents } = require('../domHelper');
const optionMap = require('./optionMap');

const {
  REMOTE_MODE,
  BROWSER,
  VERSION = '',
  LAUNCH_URL,
  REMOTE_LAUNCH_URL,
  REMOTE_SERVER_URL,
} = process.env;

function getLaunchUrl() {
  return REMOTE_MODE === 'true' ? REMOTE_LAUNCH_URL : LAUNCH_URL;
}

exports.getLaunchUrl = getLaunchUrl;

exports.createDriver = async function createDriver() {
  let driver;

  if (REMOTE_MODE === 'true') {
    const { name, setOptions, options = {} } = optionMap[`${BROWSER}${VERSION}`] || {};

    driver = new Builder()
      .forBrowser(name)
      .usingServer(REMOTE_SERVER_URL)[setOptions](options)
      .build();

    await driver.get(getLaunchUrl());
    await driver.manage().window().setRect({ x: 0, y: 0, width: 1440, height: 880 });
    await clickOn(driver, By.css('.mdx-it-lang-en'));
  } else {
    driver = await new Builder().forBrowser(BROWSER).build();
    await driver.get(getLaunchUrl());
    await driver.manage().window().setRect({ x: 0, y: 0, width: 1440, height: 880 });
    await clickOn(driver, By.css('.mdx-it-lang-en'));
  }

  await traceMouseEvents(driver);

  return driver;
};
