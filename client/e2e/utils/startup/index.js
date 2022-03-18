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
