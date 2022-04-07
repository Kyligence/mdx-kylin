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
import Login from '../Login';
import { createMountComponent } from '../../../__test__/utils';

describe('<Login /> 登录界面测试', () => {
  let component = null;

  it('正常渲染', () => {
    component = createMountComponent({
      JestComponent: Login.jest,
    });
    expect(!!component).toBeTruthy();
    expect(component.find('.main-header').text()).toBe('Welcome to Kylin MDX');
    expect(component.find('.sub-header').text()).toBe('A Unified Business Semantic Layer');

    const menuItemTexts = component.find('.login-menu-item .link').map(menuItem => menuItem.text());
    const menuItemLinks = component.find('.login-menu-item .link').map(menuItem => menuItem.getDOMNode().getAttribute('href'));
    expect(menuItemTexts).toEqual(['Introduction', 'Manual', 'Contact Us']);
    expect(menuItemLinks).toEqual(['https://kyligence.io/', 'https://docs.kyligence.io/books/mdx/v1.3/en/index.html', 'mailto:info@Kyligence.io']);
  });
});
