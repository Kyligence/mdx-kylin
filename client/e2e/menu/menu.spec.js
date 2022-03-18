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
/* eslint-disable newline-per-chained-call */
const { By } = require('selenium-webdriver');
const { createDriver } = require('../utils/startup');
const { hoverOnDropdown } = require('../utils/actionHelper');
const { login, logout } = require('../utils/businessHelper');
const { isCorrectText } = require('../utils/assertHelper');

const {
  USERNAME_ADMIN,
  PASSWORD_ADMIN,
  USERNAME_PROJECT_ADMIN,
  PASSWORD_PROJECT_ADMIN,
} = process.env;

/* eslint-disable newline-per-chained-call */
describe('菜单访问权限', async function () {
  this.timeout(300000);
  let driver;

  before(async () => {
    driver = await createDriver();
  });

  after(async () => {
    await driver.quit();
  });

  it('顶部栏对ADMIN可见按钮', async () => {
    await login(driver, USERNAME_ADMIN, PASSWORD_ADMIN);
    await isCorrectText(driver, By.css('.mdx-it-management-button'), 'Enter Management');

    await hoverOnDropdown(driver, '.mdx-it-help-button');
    await isCorrectText(driver, By.css('.mdx-it-dropdown-manual'), 'Kylin MDX Manual');
    await isCorrectText(driver, By.css('.mdx-it-dropdown-about'), 'About Kylin MDX');

    await isCorrectText(driver, By.css('.mdx-it-lang-zh'), '中');
    await isCorrectText(driver, By.css('.mdx-it-lang-en'), 'En');

    await hoverOnDropdown(driver, '.mdx-it-profile-button .el-dropdown-link');
    await isCorrectText(driver, By.css('.mdx-it-logout'), 'Logout');

    await logout(driver);
  });

  it('顶部栏对项目ADMIN可见按钮', async () => {
    await login(driver, USERNAME_PROJECT_ADMIN, PASSWORD_PROJECT_ADMIN);

    await hoverOnDropdown(driver, '.mdx-it-help-button');
    await isCorrectText(driver, By.css('.mdx-it-dropdown-manual'), 'Kylin MDX Manual');
    await isCorrectText(driver, By.css('.mdx-it-dropdown-about'), 'About Kylin MDX');

    await isCorrectText(driver, By.css('.mdx-it-lang-zh'), '中');
    await isCorrectText(driver, By.css('.mdx-it-lang-en'), 'En');

    await hoverOnDropdown(driver, '.mdx-it-profile-button .el-dropdown-link');
    await isCorrectText(driver, By.css('.mdx-it-logout'), 'Logout');

    await logout(driver);
  });
});
