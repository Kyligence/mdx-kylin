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
import sinon from 'sinon';
import { Form, Input, Button, Switch, Cascader, MessageBox } from 'kyligence-ui-react';

import { createMountComponent, randomString, delayMs } from '../../../__test__/utils';
import CodeEditor from '../../CodeEditor/CodeEditor';
import { getDefaultStore } from '../../../__test__/mocks';
import { getMockStoreDataset } from '../../../store/reducers/workspace/__test__/dataset/mocks';
import { getInitPorps } from './handler';
import NamedSetBlock from '../NamedSetBlock';

/* eslint-disable newline-per-chained-call */
describe('<NamedSetBlock /> 命名集界面测试', () => {
  const JestComponent = NamedSetBlock.jest;
  const props = getInitPorps();
  const store = getDefaultStore();
  store.workspace.dataset = getMockStoreDataset();
  store.workspace.dataset.namedSets.push({
    name: '命名集_全局重名',
    location: 'Named Set',
    expression: '1',
    isVisible: true,
    invisible: [],
    visible: [],
    translation: {},
  });
  let component = createMountComponent({ JestComponent, props, store });

  // Popper.js issue: https://github.com/jsdom/jsdom/issues/317
  beforeAll(() => {
    global.document.createRange = () => ({
      setStart: () => {},
      setEnd: () => {},
      commonAncestorContainer: {
        nodeName: 'BODY',
        ownerDocument: document,
      },
    });
    jest.spyOn(MessageBox, 'confirm').mockImplementation(() => true);
  });

  afterAll(() => {
    MessageBox.confirm.mockClear();
  });

  it('创建状态', () => {
    // 正常渲染
    expect(!!component).toBeTruthy();
    // 标题是否正确
    expect(component.find('.block-header .pull-left').text()).toBe('Add Named Set');
    // 是否有递交和取消按钮
    expect(component.find(Button)).toHaveLength(2);
    expect(component.find(Button).at(0).find('button').text()).toBe('Cancel');
    expect(component.find(Button).at(1).find('button').text()).toBe('Save');
    // 是否有名字输入框渲染
    const nameInput = component.find(Form.Item).at(0).find(Input);
    expect(nameInput.get(0).props.value).toBe('');
    expect(nameInput.get(0).props.disabled).toBeUndefined();
    // 是否有表达式输入框
    const expressionInput = component.find(Form.Item).at(1).find(CodeEditor);
    expect(expressionInput.get(0).props.value).toBe('');
    expect(expressionInput.get(0).props.disabled).toBeUndefined();
    // 是否有位置展示框
    const locationCascader = component.find(Form.Item).at(2).find(Cascader);
    expect(locationCascader.get(0).props.value).toEqual(['Named Set']);
    expect(locationCascader.get(0).props.disabled).toBeTruthy();
    // 是否有可见性开关
    const visibleSwitch = component.find(Form.Item).at(3).find(Switch);
    expect(visibleSwitch.get(0).props.value).toBeTruthy();
    expect(visibleSwitch.get(0).props.disabled).toBeFalsy();
  });

  it('创建空表单报错', async () => {
    await component.find(Button).at(1).find('button').simulate('click');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('Please enter name.');
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('Expression could not be empty.');
  });

  it('输入正确的创建数据', async () => {
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: '命名集A' } })
      .simulate('blur');

    component.find(Form.Item).at(1).find(CodeEditor).props().onChange('1');
    component.find(Form.Item).at(1).find('#ace-editor').simulate('blur');

    await component.find(Form.Item).at(3).find(Switch).at(0)
      .find('input')
      .simulate('change', { target: { checked: false } });

    await delayMs(0);

    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('');
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('');

    expect(component.find('NamedSetBlock').state().form.name).toBe('命名集A');
    expect(component.find('NamedSetBlock').state().form.expression).toBe('1');
    expect(component.find('NamedSetBlock').state().form.isVisible).toBeFalsy();
  });

  it('输入错误数据 - 名称', async () => {
    const emptyAlias = '';
    const invalidAlias = '特殊符号错误!@#$%^&*()_+';
    const duplicateAlias = '命名集_全局重名';
    const tooLongAlias = randomString(301);
    // 非法字符验证
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: invalidAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('Name only support Chinese, English, numbers, spaces, _, -, %, (, ).');
    // 空值验证
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: emptyAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('Please enter name.');
    // 重名验证
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: duplicateAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('The named set named [命名集_全局重名] already exists in whole dataset.');
    // 名称太长验证
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: tooLongAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('The named set name length cannot be greater than 300.');

    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: '命名集A' } })
      .simulate('blur');
  });

  it('输入错误数据 - 表达式', async () => {
    const tooLongExpression = randomString(5001);
    // 表达式太长验证
    component.find(Form.Item).at(1).find(CodeEditor).props().onChange(tooLongExpression);
    await component.find(Form.Item).at(1).find('#ace-editor').simulate('blur');
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('The named set expression length cannot be greater than 5000.');
    // 恢复正常值
    component.find(Form.Item).at(1).find(CodeEditor).props().onChange('1');
    await component.find(Form.Item).at(1).find('#ace-editor').simulate('blur');
    // 如果不延时，ace-editor会崩掉
    await delayMs(0);
  });

  it('递交创建', async () => {
    await component.find(Button).at(1).find('button').simulate('click');
    await delayMs(500);

    expect(props.onSubmit.callCount).toBe(1);
    expect(props.boundDatasetActions.deleteNamedSet.called).toBeFalsy();
    expect(props.boundDatasetActions.setNamedSet.args[0][0]).toBe(null);
    expect(props.boundDatasetActions.setNamedSet.args[0][1]).toEqual({
      name: '命名集A',
      location: 'Named Set',
      expression: '1',
      isVisible: false,
      invisible: [],
      visible: [],
      translation: {},
      error: '',
    });
  });

  it('阅览状态', async () => {
    props.data.name = '命名集A';
    props.data.location = '模型A.维度表A';
    props.data.expression = '1';
    props.data.isVisible = true;
    props.key = '重新渲染';
    props.boundDatasetActions.validateNamedSetExpressionForMDX = sinon.spy(sinon.fake.returns([{
      location: '模型A.维度表A', error: '',
    }]));
    component = createMountComponent({ JestComponent, props, store });

    // 标题是否正确
    expect(component.find('.block-header .pull-left').text()).toBe('Show Named Set');

    const tableRows = component.find('table tr');
    // 名称是否正确
    const nameRow = tableRows.at(0);
    expect(nameRow.find('td').at(0).text()).toBe('Name');
    expect(nameRow.find('td').at(1).text()).toBe('命名集A');
    // 表达式是否正确
    const expressionRow = tableRows.at(1);
    expect(expressionRow.find('td').at(0).text()).toBe('Expression');
    expect(expressionRow.find('td').at(1).text()).toBe('1');
    // 位置是否正确
    const locationRow = tableRows.at(2);
    expect(locationRow.find('td').at(0).text()).toBe('Location');
    expect(locationRow.find('td').at(1).text()).toBe('模型A / 维度表A');
    // 可见性是否正确
    const visibleRow = tableRows.at(3);
    expect(visibleRow.find('td').at(0).text()).toBe('Visible');
    expect(visibleRow.find('td').at(1).text()).toBe('Yes');
    // 按钮是否正确
    expect(component.find(Button).at(0).find('button').text()).toBe('Delete');
    expect(component.find(Button).at(1).find('button').text()).toBe('Edit');
  });

  it('取消编辑', async () => {
    // 进入编辑
    await component.find(Button).at(1).find('button').simulate('click');
    // 如果不延时，ace-editor会崩掉
    await delayMs(0);
    // 取消编辑
    await component.find(Button).at(0).find('button').simulate('click');
    // 是否在阅览状态
    expect(component.find('NamedSetBlock').state().isEditMode).toBeFalsy();
  });

  it('编辑状态', async () => {
    await component.find(Button).at(1).find('button').simulate('click');

    // 标题是否正确
    expect(component.find('.block-header .pull-left').text()).toBe('Edit Named Set');
    // 是否有递交和取消按钮
    expect(component.find(Button)).toHaveLength(2);
    expect(component.find(Button).at(0).find('button').text()).toBe('Cancel');
    expect(component.find(Button).at(1).find('button').text()).toBe('Save');
    // 是否有名字输入框渲染
    const nameInput = component.find(Form.Item).at(0).find(Input);
    expect(nameInput.get(0).props.value).toBe('命名集A');
    expect(nameInput.get(0).props.disabled).toBeUndefined();
    // 是否有模型/表级联选择框
    const expressionInput = component.find(Form.Item).at(1).find(CodeEditor);
    expect(expressionInput.get(0).props.value).toBe('1');
    expect(expressionInput.get(0).props.disabled).toBeUndefined();
    // 是否有位置展示框
    const locationCascader = component.find(Form.Item).at(2).find(Cascader);
    expect(locationCascader.get(0).props.value).toEqual(['模型A', '维度表A']);
    expect(locationCascader.get(0).props.disabled).toBeTruthy();
    // 是否有可见性开关
    const visibleSwitch = component.find(Form.Item).at(3).find(Switch);
    expect(visibleSwitch.get(0).props.value).toBeTruthy();
    expect(visibleSwitch.get(0).props.disabled).toBeFalsy();
    // 如果不延时，ace-editor会崩掉
    await delayMs(0);
  });

  it('递交编辑', async () => {
    await component.update();
    await component.find(Button).at(1).find('button').simulate('click');
    await delayMs(500);

    expect(props.onSubmit.callCount).toBe(2);
    expect(props.boundDatasetActions.setNamedSet.args[1]).toEqual([
      '命名集A', {
        name: '命名集A',
        location: '模型A.维度表A',
        expression: '1',
        isVisible: true,
        invisible: [],
        visible: [],
        translation: {},
        error: '',
      },
    ]);
  });

  it('删除命名集', async () => {
    // 智障框架不渲染了，需要执行setProps。
    component.setProps({});
    // 点击删除按钮
    await component.find(Button).at(0).find('button').simulate('click');
    await delayMs(0);

    expect(props.boundDatasetActions.deleteNamedSet.args[0]).toEqual([{
      name: '命名集A',
      location: '模型A.维度表A',
      expression: '1',
      isVisible: true,
      invisible: [],
      visible: [],
      translation: {},
    }]);
    expect(props.onDelete.callCount).toBe(1);
  });
});
