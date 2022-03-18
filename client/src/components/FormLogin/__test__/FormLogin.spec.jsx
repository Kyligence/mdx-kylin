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
import { Form, Input, Button } from 'kyligence-ui-react';

import { createMountComponent, randomString } from '../../../__test__/utils';
import FormLogin from '../FormLogin';
import { getInitPorps } from './handler';

describe('<FormLogin /> 登录界面测试', () => {
  const JestComponent = FormLogin.jest;
  const props = getInitPorps();
  const component = createMountComponent({ JestComponent, props });

  it('正常渲染', () => {
    // 正常渲染
    expect(!!component).toBeTruthy();
    // 用户名输入框启用，且值为空
    expect(component.find(Form.Item).at(0).find(Input).get(0).props.value).toBe('');
    expect(component.find(Form.Item).at(0).find(Input).get(0).props.placeholder).toBe('Username');
    expect(component.find(Form.Item).at(0).find(Input).get(0).props.disabled).toBeUndefined();
    // 密码输入框启用，且值为空
    expect(component.find(Form.Item).at(1).find(Input).get(0).props.value).toBe('');
    expect(component.find(Form.Item).at(1).find(Input).get(0).props.placeholder).toBe('Password');
    expect(component.find(Form.Item).at(1).find(Input).get(0).props.disabled).toBeUndefined();
    // 忘记密码提示
    expect(component.find(Button).at(0).text()).toBe('Forget Password');
    // 确认按钮
    expect(component.find(Button).at(1).text()).toBe('Sign In');
    expect(component.find(Button).get(1).props.loading).toBe(false);
  });

  it('表单报错提示', async () => {
    // 表单为空报错
    await component.find(Button).at(1).find('button').simulate('click');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('Please enter username.');
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('Please enter password.');
    // 用户名超长报错
    await component.find(Form.Item).at(0).find('input').simulate('change', { target: { value: randomString(257) } });
    await component.find(Form.Item).at(0).find('input').simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('The username length cannot be greater than 256.');
  });

  it('执行登录操作', async () => {
    await component.find(Form.Item).at(0).find('input').simulate('change', { target: { value: 'USERNAME' } });
    await component.find(Form.Item).at(1).find('input').simulate('change', { target: { value: 'PASSWORD' } });
    await component.find(Button).at(1).find('button').simulate('click');

    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('');
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('');
    expect(props.boundSystemActions.login.callCount).toBe(1);
    expect(props.boundSystemActions.login.args[0][0]).toEqual({
      username: 'USERNAME',
      password: 'PASSWORD',
    });
  });
});
