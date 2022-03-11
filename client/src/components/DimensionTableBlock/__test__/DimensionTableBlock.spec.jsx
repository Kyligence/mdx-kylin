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
import { Form, Input, Button, Select } from 'kyligence-ui-react';

import { createMountComponent, randomString } from '../../../__test__/utils';
import { getDefaultStore } from '../../../__test__/mocks';
import { getMockStoreDataset } from '../../../store/reducers/workspace/__test__/dataset/mocks';
import { getInitPorps } from './handler';
import DimensionTableBlock from '../DimensionTableBlock';

describe('<DimensionTableBlock /> 维度表界面测试', () => {
  const JestComponent = DimensionTableBlock.jest;
  const props = getInitPorps();
  const store = getDefaultStore();
  store.workspace.dataset = getMockStoreDataset();
  const component = createMountComponent({ JestComponent, props, store });

  it('正常渲染', () => {
    // 正常渲染
    expect(!!component).toBeTruthy();

    // 标题是否正确
    expect(component.find('.block-header .pull-left').text()).toBe('Show Dimension Table Properties');

    // 信息展示是否正确
    const tableRows = component.find('table tr');
    const tableNameRow = tableRows.at(0);
    expect(tableNameRow.find('td').at(0).contains('Table Name')).toBe(true);
    expect(tableNameRow.find('td').at(1).contains('维度表A')).toBe(true);

    const tableAliasRow = tableRows.at(1);
    expect(tableAliasRow.find('td').at(0).contains('Dimension Table Name')).toBe(true);
    expect(tableAliasRow.find('td').at(1).contains('维度表A')).toBe(true);

    const tableAttributeRow = tableRows.at(2);
    expect(tableAttributeRow.find('td').at(0).contains('Type')).toBe(true);
    expect(tableAttributeRow.find('td').at(1).contains('Regular')).toBe(true);

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
    expect(tableNameInput.get(0).props.value).toBe('维度表A');
    expect(tableNameInput.get(0).props.disabled).toBe(true);

    const tableAliasInput = component.find(Form.Item).at(1).find(Input);
    expect(tableAliasInput.get(0).props.value).toBe('维度表A');
    expect(tableAliasInput.get(0).props.disabled).toBeUndefined();

    const tableAttributeSelect = component.find(Form.Item).at(2).find(Select);
    expect(tableAttributeSelect.get(0).props.value).toBe('regular');
    expect(tableAttributeSelect.get(0).props.disabled).toBeUndefined();

    // 是否有递交和取消按钮
    expect(component.find(Button)).toHaveLength(2);
    expect(component.find(Button).at(0).find('button').contains('Cancel')).toBe(true);
    expect(component.find(Button).at(1).find('button').contains('Save')).toBe(true);
  });

  it('编辑错误', async () => {
    const emptyAlias = '';
    const invalidAlias = '特殊符号错误!@#$%^&*()_+';
    const duplicateAlias = '事实表A';
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
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('Please enter table alias.');
    // 重名验证
    await component.find(Form.Item).at(1).find(Input).find('input')
      .simulate('change', { target: { value: duplicateAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('The dimension table named [事实表A] already exists.');
    // 名称太长验证
    await component.find(Form.Item).at(1).find(Input).find('input')
      .simulate('change', { target: { value: tooLongAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('Dimension table name length cannot be greater than 300.');
  });

  it('编辑正确', async () => {
    // 修改表单
    const validAlias = ' 新 维度表A  ';
    await component.find(Form.Item).at(1).find(Input).find('input')
      .simulate('change', { target: { value: validAlias } })
      .simulate('blur');
    await component.find(Form.Item).at(2).find(Select.Option).at(1)
      .find('li')
      .simulate('click');
    // 表单无报错，且数据正确
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('');
    expect(component.find('DimensionTableBlock').state().form.alias).toBe(' 新 维度表A  ');
    expect(component.find('DimensionTableBlock').state().form.type).toBe('time');
  });

  it('提交表单', async () => {
    // 提交表单
    await component.find(Button).at(1).find('button').simulate('click');
    // 是否调用action方法
    expect(props.boundDatasetActions.setDimTable.callCount).toBe(1);
    // 是否只有一个编辑按钮
    expect(component.find(Button)).toHaveLength(1);
    expect(component.find(Button).find('button').contains('Edit')).toBe(true);
  });

  // TODO: 目前无法对高阶组件进行setProps，具体原因有待调查
  // it('切成维度表B来展示', async () => {
  //   props.data = {
  //     name: '维度表B',
  //     alias: '维度表B',
  //     type: 'regular',
  //   };
  //   component.find('DimensionTableBlock').setProps(props);
  //   // 信息展示是否正确
  //   const tableRows = component.find('table tr');
  //   const tableNameRow = tableRows.at(0);
  //   expect(tableNameRow.find('td').at(0).contains('Table Name')).toBe(true);
  //   expect(tableNameRow.find('td').at(1).contains('维度表B')).toBe(true);

  //   const tableAliasRow = tableRows.at(1);
  //   expect(tableAliasRow.find('td').at(0).contains('Dimension Table Name')).toBe(true);
  //   expect(tableAliasRow.find('td').at(1).contains('维度表B')).toBe(true);

  //   const tableAttributeRow = tableRows.at(2);
  //   expect(tableAttributeRow.find('td').at(0).contains('Attribute')).toBe(true);
  //   expect(tableAttributeRow.find('td').at(1).contains('Time')).toBe(true);
  // });
});
