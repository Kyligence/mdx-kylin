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
    await isCorrectText(driver, By.css('.mdx-it-dropdown-manual'), 'MDX for Kylin Manual');
    await isCorrectText(driver, By.css('.mdx-it-dropdown-about'), 'About MDX for Kylin');

    await isCorrectText(driver, By.css('.mdx-it-lang-zh'), '中');
    await isCorrectText(driver, By.css('.mdx-it-lang-en'), 'En');

    await hoverOnDropdown(driver, '.mdx-it-profile-button .el-dropdown-link');
    await isCorrectText(driver, By.css('.mdx-it-logout'), 'Logout');

    await logout(driver);
  });

  it('顶部栏对项目ADMIN可见按钮', async () => {
    await login(driver, USERNAME_PROJECT_ADMIN, PASSWORD_PROJECT_ADMIN);

    await hoverOnDropdown(driver, '.mdx-it-help-button');
    await isCorrectText(driver, By.css('.mdx-it-dropdown-manual'), 'MDX for Kylin Manual');
    await isCorrectText(driver, By.css('.mdx-it-dropdown-about'), 'About MDX for Kylin');

    await isCorrectText(driver, By.css('.mdx-it-lang-zh'), '中');
    await isCorrectText(driver, By.css('.mdx-it-lang-en'), 'En');

    await hoverOnDropdown(driver, '.mdx-it-profile-button .el-dropdown-link');
    await isCorrectText(driver, By.css('.mdx-it-logout'), 'Logout');

    await logout(driver);
  });
});
