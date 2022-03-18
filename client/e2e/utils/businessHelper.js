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
const { waitingForStable } = require('./domHelper');
const { changeFormInput, hoverOn, clickOn, changeFormSelect } = require('./actionHelper');
const { isCorrectText } = require('./assertHelper');

exports.login = async function login(driver, username, password) {
  await changeFormInput(driver, '.mdx-it-username-input', username);
  await changeFormInput(driver, '.mdx-it-password-input', password);
  await clickOn(driver, By.css('.mdx-it-login-button'));

  await driver.wait(until.urlMatches(/\/configuration|\/dataset\/list/));
  await isCorrectText(driver, By.css('.mdx-it-logged-in-username'), username);
};

exports.logout = async function logout(driver) {
  await hoverOn(driver, By.css('.mdx-it-logged-in-username'));
  await waitingForStable(driver);
  await clickOn(driver, By.css('.mdx-it-logout'));

  await driver.wait(until.elementLocated(By.css('.mdx-it-login-page')), 10000);
};

exports.changeProject = async function changeProject(driver, projectName) {
  await changeFormSelect(driver, '.project-selector', projectName);
};
