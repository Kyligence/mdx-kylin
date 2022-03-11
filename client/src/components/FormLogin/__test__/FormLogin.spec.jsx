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
