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
import { getDefaultStore } from '../../../__test__/mocks';
import { getMockStoreDataset } from '../../../store/reducers/workspace/__test__/dataset/mocks';
import { getInitPorps } from './handler';
import MeasureGroupBlock from '../MeasureGroupBlock';

describe('<MeasureGroupBlock /> 度量组界面测试', () => {
  const JestComponent = MeasureGroupBlock.jest;
  const props = getInitPorps();
  const store = getDefaultStore();
  store.workspace.dataset = getMockStoreDataset();
  const component = createMountComponent({ JestComponent, props, store });

  it('正常渲染', () => {
    // 正常渲染
    expect(!!component).toBeTruthy();

    // 标题是否正确
    expect(component.find('.block-header .pull-left').text()).toBe('Show Model');

    // 信息展示是否正确
    const tableRows = component.find('table tr');
    const nameRow = tableRows.at(0);
    expect(nameRow.find('td').at(0).text()).toBe('Model Name');
    expect(nameRow.find('td').at(1).text()).toBe('模型A');

    const aliasRow = tableRows.at(1);
    expect(aliasRow.find('td').at(0).text()).toBe('Measure Group');
    expect(aliasRow.find('td').at(1).text()).toBe('模型A');

    // 是否只有一个编辑按钮
    expect(component.find(Button)).toHaveLength(1);
    expect(component.find(Button).find('button').contains('Edit')).toBe(true);
  });

  it('进入编辑', async () => {
    // 触发编辑按钮，切换状态
    const editButton = component.find(Button).find('button').at(0);
    await editButton.simulate('click');

    // 表单展示是否正确
    const tableNameInput = component.find(Form.Item).at(0).find(Input);
    expect(tableNameInput.get(0).props.value).toBe('模型A');
    expect(tableNameInput.get(0).props.disabled).toBe(true);

    const tableAliasInput = component.find(Form.Item).at(1).find(Input);
    expect(tableAliasInput.get(0).props.value).toBe('模型A');
    expect(tableAliasInput.get(0).props.disabled).toBeUndefined();

    // 是否有递交和取消按钮
    expect(component.find(Button)).toHaveLength(2);
    expect(component.find(Button).at(0).find('button').contains('Cancel')).toBe(true);
    expect(component.find(Button).at(1).find('button').contains('Save')).toBe(true);
  });

  it('编辑错误', async () => {
    const emptyAlias = '';
    const invalidAlias = '特殊符号错误!@#$%^&*()_+';
    const duplicateAlias = '模型B';
    const tooLongAlias = randomString(301);
    // 非法字符验证
    await component.find(Form.Item).at(1).find(Input).find('input')
      .simulate('change', { target: { value: invalidAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('Alias only supports Chinese, English, numbers, spaces, _, -, %, (, ),?.');
    // 空值验证
    await component.find(Form.Item).at(1).find(Input).find('input')
      .simulate('change', { target: { value: emptyAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('Please enter measure group name.');
    // 重名验证
    await component.find(Form.Item).at(1).find(Input).find('input')
      .simulate('change', { target: { value: duplicateAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('The measure group named [模型B] already exists.');
    // 名称太长验证
    await component.find(Form.Item).at(1).find(Input).find('input')
      .simulate('change', { target: { value: tooLongAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('The measure group name length cannot be greater than 300.');
  });

  it('编辑正确', async () => {
    // 修改表单
    const validAlias = ' 新 模型A  ';
    await component.find(Form.Item).at(1).find(Input).find('input')
      .simulate('change', { target: { value: validAlias } })
      .simulate('blur');
    // 表单无报错，且数据正确
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('');
    expect(component.find('MeasureGroupBlock').state().form.alias).toBe(' 新 模型A  ');
  });

  it('提交表单', async () => {
    // 提交表单
    await component.find(Button).at(1).find('button').simulate('click');
    // 是否调用action方法
    expect(props.boundDatasetActions.setMeasureGroup.callCount).toBe(1);
    // 是否只有一个编辑按钮
    expect(component.find(Button)).toHaveLength(1);
    expect(component.find(Button).find('button').contains('Edit')).toBe(true);
  });
});
