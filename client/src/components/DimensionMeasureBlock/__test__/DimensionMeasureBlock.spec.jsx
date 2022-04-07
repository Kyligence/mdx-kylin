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
import { Form, Input, Button, Switch, MessageBox, AutoComplete } from 'kyligence-ui-react';

import { createMountComponent, randomString, delayMs } from '../../../__test__/utils';
import { getDefaultStore } from '../../../__test__/mocks';
import { getMockStoreDataset } from '../../../store/reducers/workspace/__test__/dataset/mocks';
import { getInitPorps } from './handler';
import DimensionMeasureBlock from '../DimensionMeasureBlock';
import FormatInput from '../../FormatInput/FormatInput';

/* eslint-disable newline-per-chained-call */
describe('<DimensionMeasureBlock /> 度量界面测试', () => {
  const JestComponent = DimensionMeasureBlock.jest;
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
    expect(component.find('.block-header .pull-left').text()).toBe('Show Measure');

    // 信息展示是否正确
    const columnRows = component.find('table tr');
    const nameRow = columnRows.at(0);
    expect(nameRow.find('td').at(0).text()).toBe('Measure Name');
    expect(nameRow.find('td').at(1).text()).toBe('度量A');

    const formatRow = columnRows.at(1);
    expect(formatRow.find('td').at(0).text()).toBe('Format');
    expect(formatRow.find('td').at(1).text()).toBe('ing_16.svg');

    const folderRow = columnRows.at(2);
    expect(folderRow.find('td').at(0).text()).toBe('Folder');
    expect(folderRow.find('td').at(1).text()).toBe('None');

    const metricTypeRow = columnRows.at(3);
    expect(metricTypeRow.find('td').at(0).text()).toBe('Metric Type');
    expect(metricTypeRow.find('td').at(1).text()).toBe('COUNT');

    const expressionRow = columnRows.at(4);
    expect(expressionRow.find('td').at(0).text()).toBe('Expression');
    expect(expressionRow.find('td').at(1).text()).toBe('COUNT(*)');

    const visibleRow = columnRows.at(5);
    expect(visibleRow.find('td').at(0).text()).toBe('Visible');
    expect(visibleRow.find('td').at(1).text()).toBe('Yes');

    // 是否只有一个编辑按钮
    expect(component.find(Button)).toHaveLength(1);
    expect(component.find(Button).find('button').text()).toBe('Edit');
  });

  it('进入编辑', async () => {
    // 触发编辑按钮，切换状态
    const editButton = component.find(Button).find('button').at(0);
    await editButton.simulate('click');

    // 表单展示是否正确
    const aliasInput = component.find(Form.Item).at(0).find(Input);
    expect(aliasInput.get(0).props.value).toBe('度量A');
    expect(aliasInput.get(0).props.disabled).toBeFalsy();

    const formatInput = component.find(FormatInput).at(0);
    expect(formatInput.get(0).props.format).toBe('regular');
    expect(formatInput.get(0).props.formatType).toBe('No Format');

    const folderInput = component.find(Form.Item).at(2).find(AutoComplete).find('input');
    expect(folderInput.get(0).props.value).toBe('');
    expect(folderInput.get(0).props.disabled).toBeUndefined();

    const columnAliasInput = component.find(Form.Item).at(3).find(Input);
    expect(columnAliasInput.get(0).props.value).toBe('COUNT');
    expect(columnAliasInput.get(0).props.disabled).toBeTruthy();

    const columnTypeInput = component.find(Form.Item).at(4).find(Input);
    expect(columnTypeInput.get(0).props.value).toBe('COUNT(*)');
    expect(columnTypeInput.get(0).props.disabled).toBeTruthy();

    const columnVisibleSwitch = component.find(Form.Item).at(5).find(Switch);
    expect(columnVisibleSwitch.get(0).props.value).toBeTruthy();
    expect(columnVisibleSwitch.get(0).props.disabled).toBeFalsy();

    // 是否有递交和取消按钮
    expect(component.find(Button)).toHaveLength(3);
    expect(component.find(Button).at(0).find('button').text()).toBe('Cancel');
    expect(component.find(Button).at(1).find('button').text()).toBe('Save');
    expect(component.find(Button).at(2).find('button').text()).toBe('edit_page_16.svgEdit');
  });

  it('编辑错误 - 别名', async () => {
    const emptyAlias = '';
    const invalidAlias = '特殊符号错误!@#$%^&*()_+';
    const duplicateAlias = '度量B';
    const tooLongAlias = randomString(301);
    // 非法字符验证
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: invalidAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('Alias only supports Chinese, English, numbers, spaces, _, -, %, (, ),?.');
    // 空值验证
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: emptyAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('Please enter name.');
    // 重名验证
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: duplicateAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('The measure named [度量B] already exists.');
    // 名称太长验证
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: tooLongAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('The measure name length cannot be greater than 300.');
  });

  it('输入错误数据 - 文件夹', async () => {
    const tooLongFolders = randomString(101, undefined, '\\');
    const invalidFolders = '特殊符号错误!@#$%^&*()_+';
    // 名称太长验证
    await component.find(Form.Item).at(2).find('input')
      .simulate('change', { target: { value: invalidFolders } })
      .simulate('blur');
    expect(component.find(Form.Item).at(2).find('.el-form-item__error').text()).toBe('Display folder name only supports Chinese, English, numbers and spaces.');
    // 名称太长验证
    await component.find(Form.Item).at(2).find('input')
      .simulate('change', { target: { value: tooLongFolders } })
      .simulate('blur');
    expect(component.find(Form.Item).at(2).find('.el-form-item__error').text()).toBe('Display folder cannot exceed 100 characters.');
    // 恢复成正常值
    await component.find(Form.Item).at(2).find('input')
      .simulate('change', { target: { value: '' } })
      .simulate('blur');
  });

  /* eslint-disable no-await-in-loop */
  it('编辑正确', async () => {
    // 修改表单
    const validAlias = ' 新 度量A  ';
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: validAlias } })
      .simulate('blur');
    // 添加文件夹位置
    await component.find(Form.Item).at(2).find('input')
      .simulate('change', { target: { value: ' folder \\ folder ' } })
      .simulate('blur');
    // 关闭可见性限制
    await component.find(Form.Item).at(5).find(Switch).at(0)
      .find('input')
      .simulate('change', { target: { checked: false } });

    // 表单无报错，且数据正确
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('');
    expect(component.find('DimensionMeasureBlock').state().form.alias).toBe(' 新 度量A  ');
    expect(component.find('DimensionMeasureBlock').state().form.subfolder).toBe(' folder \\ folder ');
    expect(component.find('DimensionMeasureBlock').state().form.isVisible).toBeFalsy();
  });

  it('提交表单', async () => {
    // 提交表单
    await component.find(Button).at(1).find('button').simulate('click');
    // 是否调用action方法
    expect(props.boundDatasetActions.setDimMeasure.callCount).toBe(1);
    // 是否只有一个编辑按钮
    expect(component.find(Button)).toHaveLength(1);
    expect(component.find(Button).find('button').contains('Edit')).toBe(true);

    await delayMs(0);
    expect(props.onSubmit.callCount).toBe(1);
  });
});
