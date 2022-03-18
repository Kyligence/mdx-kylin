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
import { Form, Input, Button, Select, Switch, MessageBox } from 'kyligence-ui-react';

import { createMountComponent, randomString, delayMs } from '../../../__test__/utils';
import { getDefaultStore } from '../../../__test__/mocks';
import { getMockStoreDataset } from '../../../store/reducers/workspace/__test__/dataset/mocks';
import { getInitPorps } from './handler';
import DimensionColumnBlock from '../DimensionColumnBlock';

describe('<DimensionColumnBlock /> 维度列界面测试', () => {
  const JestComponent = DimensionColumnBlock.jest;
  const props = getInitPorps();
  const store = getDefaultStore();
  store.workspace.dataset = getMockStoreDataset();
  const component = createMountComponent({ JestComponent, props, store });

  beforeAll(() => {
    jest.spyOn(MessageBox, 'confirm').mockImplementation(() => true);
  });

  afterAll(() => {
    MessageBox.confirm.mockClear();
  });

  it('正常渲染', () => {
    // 正常渲染
    expect(!!component).toBeTruthy();

    // 标题是否正确
    expect(component.find('.block-header .pull-left').text()).toBe('Show Dimension');

    // 信息展示是否正确
    const columnRows = component.find('table tr');
    const columnNameRow = columnRows.at(0);
    expect(columnNameRow.find('td').at(0).text()).toBe('Column Name (Key Column)');
    expect(columnNameRow.find('td').at(1).text()).toBe('维度列A');

    const columnAliasRow = columnRows.at(1);
    expect(columnAliasRow.find('td').at(0).text()).toBe('Dimension Name');
    expect(columnAliasRow.find('td').at(1).text()).toBe('维度列A');

    const columnTypeRow = columnRows.at(2);
    expect(columnTypeRow.find('td').at(0).text()).toBe('Data Type');
    expect(columnTypeRow.find('td').at(1).text()).toBe('date');

    const columnAttributeRow = columnRows.at(3);
    expect(columnAttributeRow.find('td').at(0).text()).toBe('Type');
    expect(columnAttributeRow.find('td').at(1).text()).toBe('Regular');

    const columnNameColumnRow = columnRows.at(4);
    expect(columnNameColumnRow.find('td').at(0).text()).toBe('Name Column');
    expect(columnNameColumnRow.find('td').at(1).text()).toBe('None');

    const columnValueColumnRow = columnRows.at(5);
    expect(columnValueColumnRow.find('td').at(0).text()).toBe('Value Column');
    expect(columnValueColumnRow.find('td').at(1).text()).toBe('None');

    const columnPropertyRow = columnRows.at(6);
    expect(columnPropertyRow.find('td').at(0).text()).toBe('Properties');
    expect(columnPropertyRow.find('td').at(1).text()).toBe('None');

    const columnFolderRow = columnRows.at(7);
    expect(columnFolderRow.find('td').at(0).text()).toBe('Folder');
    expect(columnFolderRow.find('td').at(1).text()).toBe('None');

    const columnVisibleRow = columnRows.at(8);
    expect(columnVisibleRow.find('td').at(0).text()).toBe('Visible');
    expect(columnVisibleRow.find('td').at(1).text()).toBe('Yes');

    // 是否只有一个编辑按钮
    expect(component.find(Button)).toHaveLength(1);
    expect(component.find(Button).find('button').text()).toBe('Edit');
  });

  it('进入编辑', async () => {
    // 触发编辑按钮，切换状态
    const editButton = component.find(Button).find('button').at(0);
    await editButton.simulate('click');
    await delayMs(0);

    // 表单展示是否正确
    const columnNameInput = component.find(Form.Item).at(0).find(Input);
    expect(columnNameInput.get(0).props.value).toBe('维度列A');
    expect(columnNameInput.get(0).props.disabled).toBeTruthy();

    const columnAliasInput = component.find(Form.Item).at(1).find(Input);
    expect(columnAliasInput.get(0).props.value).toBe('维度列A');
    expect(columnAliasInput.get(0).props.disabled).toBeUndefined();

    const columnTypeInput = component.find(Form.Item).at(2).find(Input);
    expect(columnTypeInput.get(0).props.value).toBe('date');
    expect(columnTypeInput.get(0).props.disabled).toBeTruthy();

    const columnAttributeSelect = component.find(Form.Item).at(3).find(Select);
    expect(columnAttributeSelect.get(0).props.value).toBe(0);
    expect(columnAttributeSelect.get(0).props.disabled).toBeUndefined();

    const columnNameColumnSelect = component.find(Form.Item).at(4).find(Select);
    expect(columnNameColumnSelect.get(0).props.value).toBeNull();
    expect(columnNameColumnSelect.get(0).props.disabled).toBeUndefined();

    const columnValueColumnSelect = component.find(Form.Item).at(5).find(Select);
    expect(columnValueColumnSelect.get(0).props.value).toBeNull();
    expect(columnValueColumnSelect.get(0).props.disabled).toBeUndefined();

    const columnPropertySelect = component.find(Form.Item).at(6).find(Select);
    expect(columnPropertySelect.get(0).props.value).toEqual([]);
    expect(columnPropertySelect.get(0).props.disabled).toBeUndefined();

    const columnFolderSwitch = component.find(Form.Item).at(7).find(Input);
    expect(columnFolderSwitch.get(0).props.value).toBe('');
    expect(columnFolderSwitch.get(0).props.disabled).toBeFalsy();

    const columnVisibleSwitch = component.find(Form.Item).at(8).find(Switch);
    expect(columnVisibleSwitch.get(0).props.value).toBeTruthy();
    expect(columnVisibleSwitch.get(0).props.disabled).toBeFalsy();

    // 是否有递交和取消按钮
    expect(component.find(Button)).toHaveLength(2);
    expect(component.find(Button).at(0).find('button').text()).toBe('Cancel');
    expect(component.find(Button).at(1).find('button').text()).toBe('Save');
  });

  it('编辑错误 - 别名', async () => {
    const emptyAlias = '';
    const invalidAlias = '特殊符号错误!@#$%^&*()_+';
    const duplicateAlias = '维度列B';
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
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('Please enter name.');
    // 重名验证
    await component.find(Form.Item).at(1).find(Input).find('input')
      .simulate('change', { target: { value: duplicateAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('The dimension named [维度列B] already exists.');
    // 名称太长验证
    await component.find(Form.Item).at(1).find(Input).find('input')
      .simulate('change', { target: { value: tooLongAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('The dimension name length cannot be greater than 300.');
  });

  it('输入错误数据 - 文件夹', async () => {
    const tooLongFolders = randomString(101, undefined, '\\');
    const invalidFolders = '特殊符号错误!@#$%^&*()_+';
    // 名称太长验证
    await component.find(Form.Item).at(7).find(Input).find('input')
      .simulate('change', { target: { value: invalidFolders } })
      .simulate('blur');
    expect(component.find(Form.Item).at(7).find('.el-form-item__error').text()).toBe('Display folder name only supports Chinese, English, numbers and spaces.');
    // 名称太长验证
    await component.find(Form.Item).at(7).find(Input).find('input')
      .simulate('change', { target: { value: tooLongFolders } })
      .simulate('blur');
    expect(component.find(Form.Item).at(7).find('.el-form-item__error').text()).toBe('Display folder cannot exceed 100 characters.');
    // 恢复成正常值
    await component.find(Form.Item).at(7).find(Input).find('input')
      .simulate('change', { target: { value: '' } })
      .simulate('blur');
  });

  /* eslint-disable no-await-in-loop */
  it('编辑正确', async () => {
    // 修改表单
    const validAlias = ' 新 维度列A  ';
    await component.find(Form.Item).at(1).find(Input).find('input')
      .simulate('change', { target: { value: validAlias } })
      .simulate('blur');
    // 维度列类型设置为1
    await component.find(Form.Item).at(3).find(Select.Option).at(1)
      .find('li')
      .simulate('click');
    // 设置Named Column为维度列B
    await component.find(Form.Item).at(4).find(Select.Option).at(2)
      .find('li')
      .simulate('click');
    // 设置Value Column为维度C
    await component.find(Form.Item).at(5).find(Select.Option).at(3)
      .find('li')
      .simulate('click');
    // 设置属性成员为维度列B、维度列C
    for (let i = 0; i < 2; i += 1) {
      await component.find(Form.Item).at(6).find(Select.Option).at(i)
        .find('li')
        .simulate('click');
    }
    // 关闭可见性限制
    await component.find(Form.Item).at(8).find(Switch).at(0)
      .find('input')
      .simulate('change', { target: { checked: false } });

    // 表单无报错，且数据正确
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('');
    expect(component.find('DimensionColumnBlock').state().form.alias).toBe(' 新 维度列A  ');
    expect(component.find('DimensionColumnBlock').state().form.type).toBe(1);
    expect(component.find('DimensionColumnBlock').state().form.nameColumn).toBe('维度列B');
    expect(component.find('DimensionColumnBlock').state().form.valueColumn).toBe('维度C');
    expect(component.find('DimensionColumnBlock').state().form.isVisible).toBeFalsy();
    expect(component.find('DimensionColumnBlock').state().form.subfolder).toBe('');
    expect(component.find('DimensionColumnBlock').state().form.properties).toEqual([
      { name: '维度列B', columnName: '维度列B', columnAlias: '维度列B' },
      { name: '维度列C', columnName: '维度C', columnAlias: '维度列C' },
    ]);
  });

  it('提交表单', async () => {
    // 提交表单
    await component.find(Button).at(1).find('button').simulate('click');
    // 是否调用action方法
    expect(props.boundDatasetActions.setDimColumn.callCount).toBe(1);
    // 是否只有一个编辑按钮
    expect(component.find(Button)).toHaveLength(1);
    expect(component.find(Button).find('button').contains('Edit')).toBe(true);

    await delayMs(0);
    expect(props.onSubmit.callCount).toBe(1);
  });
});
