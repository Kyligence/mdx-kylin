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
