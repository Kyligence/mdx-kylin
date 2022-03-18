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
import { Form, Input, Button, Switch, AutoComplete, MessageBox, Select } from 'kyligence-ui-react';

import { createMountComponent, randomString, delayMs } from '../../../__test__/utils';
import CodeEditor from '../../CodeEditor/CodeEditor';
import { getDefaultStore } from '../../../__test__/mocks';
import { getMockStoreDataset } from '../../../store/reducers/workspace/__test__/dataset/mocks';
import { getInitPorps } from './handler';
import CalculateMeasureBlock from '../CalculateMeasureBlock';
import FormatInput from '../../FormatInput/FormatInput';

/* eslint-disable newline-per-chained-call */
describe('<CalculateMeasureBlock /> 计算度量界面测试', () => {
  const JestComponent = CalculateMeasureBlock.jest;
  const props = getInitPorps();
  const store = getDefaultStore();
  store.workspace.dataset = getMockStoreDataset();
  store.workspace.dataset.calculateMeasures.push({
    name: '计算度量_全局重名',
    format: '',
    formatType: '',
    desc: '',
    folder: 'Calculated Measure',
    subfolder: '',
    expression: '',
    isVisible: true,
    invisible: [],
    visible: [],
    translation: {},
  });
  store.workspace.dataset.models[0].measures.push({
    name: '度量A',
    alias: '度量_全局重名',
    format: '',
    formatType: '',
    desc: '',
    subfolder: '',
    expression: 'COUNT',
    expressionParams: '*',
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
    expect(component.find('.block-header .pull-left').text()).toBe('Add Calculated Measure');
    // 是否有递交、取消和创建向导按钮
    expect(component.find(Button)).toHaveLength(4);
    expect(component.find(Button).at(0).find('button').text()).toBe('Cancel');
    expect(component.find(Button).at(1).find('button').text()).toBe('Save');
    expect(component.find(Button).at(2).find('button').text()).toBe('edit_page_16.svgEdit');
    expect(component.find(Button).at(3).find('button').text()).toBe('Use Templates');
    // 是否有名字输入框渲染
    const nameInput = component.find(Form.Item).at(0).find(Input);
    expect(nameInput.get(0).props.value).toBe('');
    expect(nameInput.get(0).props.disabled).toBeUndefined();
    // 是否有格式输入框
    const formatInput = component.find(FormatInput).at(0);
    expect(formatInput.get(0).props.format).toBe('regular');
    expect(formatInput.get(0).props.formatType).toBe('No Format');
    // 是否有度量组选择框
    const measureGroupSelect = component.find(Form.Item).at(2).find(Select);
    expect(measureGroupSelect.get(0).props.value).toBe('Calculated Measure');
    expect(measureGroupSelect.get(0).props.disabled).toBeUndefined();
    // 是否有文件夹输入框
    const folderInput = component.find(Form.Item).at(3).find(AutoComplete);
    expect(folderInput.get(0).props.value).toBe('');
    // 是否有表达式输入框
    const expressionInput = component.find(Form.Item).at(4).find(CodeEditor);
    expect(expressionInput.get(0).props.value).toBe('');
    expect(expressionInput.get(0).props.disabled).toBeUndefined();
    // 是否有可见性开关
    const visibleSwitch = component.find(Form.Item).at(6).find(Switch);
    expect(visibleSwitch.get(0).props.value).toBeTruthy();
    expect(visibleSwitch.get(0).props.disabled).toBeFalsy();
  });

  it('创建空表单报错', async () => {
    await component.find(Button).at(1).find('button').simulate('click');
    expect(component.find(Form.Item).at(4).find('.el-form-item__error').text()).toBe('Expression could not be empty.');
  });

  it('输入正确的创建数据', async () => {
    // 输入名字
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: '计算度量A' } })
      .simulate('blur');
    // 选择度量组
    await component.find(Form.Item).at(2).find(Select).find('.el-select-dropdown__item').at(1)
      .simulate('click');
    // 输入表达式
    component.find(Form.Item).at(4).find(CodeEditor).props().onChange('1');
    component.find(Form.Item).at(4).find('#ace-editor').simulate('blur');
    // 关闭可见性
    await component.find(Form.Item).at(6).find(Switch).at(0)
      .find('input')
      .simulate('change', { target: { checked: false } });

    await delayMs(0);

    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('');
    expect(component.find(Form.Item).at(4).find('.el-form-item__error').text()).toBe('');

    expect(component.find('CalculateMeasureBlock').state().form.name).toBe('计算度量A');
    expect(component.find('CalculateMeasureBlock').state().form.format).toBe('regular');
    expect(component.find('CalculateMeasureBlock').state().form.folder).toBe('模型A');
    expect(component.find('CalculateMeasureBlock').state().form.subfolder).toBe('');
    expect(component.find('CalculateMeasureBlock').state().form.expression).toBe('1');
    expect(component.find('CalculateMeasureBlock').state().form.isVisible).toBeFalsy();
  });

  it('输入错误数据 - 名称', async () => {
    const emptyAlias = '';
    const invalidAlias = '特殊符号错误!@#$%^&*()_+';
    const duplicateAlias = '计算度量_全局重名';
    const duplicateMeasureAlias = '度量_全局重名';
    const tooLongAlias = randomString(301);
    // 非法字符验证
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: invalidAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('Name only support Chinese, English, numbers, spaces, _, -, %, (, ),?.');
    // 空值验证
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: emptyAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('Please enter name.');
    // 计算度量重名验证
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: duplicateAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('The calculated measure named [计算度量_全局重名] already exists in whole dataset.');
    // 度量重名验证
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: duplicateMeasureAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('The calculated measure named [度量_全局重名] already exists in whole dataset.');
    // 名称太长验证
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: tooLongAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('The calculated measure name length cannot be greater than 300.');
    // 恢复成正常值
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: '计算度量A' } })
      .simulate('blur');
  });

  it('输入错误数据 - 文件夹', async () => {
    const tooLongFolders = randomString(101, undefined, '\\');
    const invalidFolders = '特殊符号错误!@#$%^&*()_+';
    // 名称太长验证
    await component.find(Form.Item).at(3).find('input')
      .simulate('change', { target: { value: invalidFolders } })
      .simulate('blur');
    expect(component.find(Form.Item).at(3).find('.el-form-item__error').text()).toBe('Display folder name only supports Chinese, English, numbers and spaces.');
    // 名称太长验证
    await component.find(Form.Item).at(3).find('input')
      .simulate('change', { target: { value: tooLongFolders } })
      .simulate('blur');
    expect(component.find(Form.Item).at(3).find('.el-form-item__error').text()).toBe('Display folder cannot exceed 100 characters.');
    // 恢复成正常值
    await component.find(Form.Item).at(3).find('input')
      .simulate('change', { target: { value: ' folder \\ folder ' } })
      .simulate('blur');
  });

  it('输入错误数据 - 表达式', async () => {
    const tooLongExpression = randomString(5001);
    // 表达式太长验证
    component.find(Form.Item).at(4).find(CodeEditor).props().onChange(tooLongExpression);
    await component.find(Form.Item).at(4).find('#ace-editor').simulate('blur');
    expect(component.find(Form.Item).at(4).find('.el-form-item__error').text()).toBe('The calculated measure expression length cannot be greater than 5000.');
    // 恢复正常值
    component.find(Form.Item).at(4).find(CodeEditor).props().onChange('1');
    await component.find(Form.Item).at(4).find('#ace-editor').simulate('blur');
    // 如果不延时，ace-editor会崩掉
    await delayMs(0);
  });

  it('递交创建', async () => {
    // 防止自动完成输入框的Suggestion报错
    await delayMs(500);
    await component.find(Button).at(1).find('button').simulate('click');
    await delayMs(500);

    expect(props.onSubmit.callCount).toBe(1);
    expect(props.boundDatasetActions.deleteCalculatedMeasure.called).toBeFalsy();
    expect(props.boundDatasetActions.setCalculatedMeasure.args[0][0]).toBe(null);
    expect(props.boundDatasetActions.setCalculatedMeasure.args[0][1]).toEqual({
      name: '计算度量A',
      format: 'regular',
      formatType: 'No Format',
      desc: '',
      folder: '模型A',
      subfolder: ' folder \\ folder ',
      expression: '1',
      isVisible: false,
      invisible: [],
      visible: [],
      translation: {},
      nonEmptyBehavior: [],
      error: '',
    });
  });

  it('阅览状态', async () => {
    props.data.name = '计算度量A';
    props.data.expression = '1';
    props.data.isVisible = true;
    props.data.subfolder = ' folder \\ folder ';
    props.key = '重新渲染';
    component = createMountComponent({ JestComponent, props, store });

    // 标题是否正确
    expect(component.find('.block-header .pull-left').text()).toBe('Show Calculated Measure');

    const tableRows = component.find('table tr');
    // 名称是否正确
    const nameRow = tableRows.at(0);
    expect(nameRow.find('td').at(0).text()).toBe('Calculated Measure Name');
    expect(nameRow.find('td').at(1).text()).toBe('计算度量A');
    // 格式是否正确
    const formatRow = tableRows.at(1);
    expect(formatRow.find('td').at(0).text()).toBe('Format');
    expect(formatRow.find('td').at(1).text()).toBe('ing_16.svg');
    // 度量组是否正确
    const measureGroupRow = tableRows.at(2);
    expect(measureGroupRow.find('td').at(0).text()).toBe('Measure Group');
    expect(measureGroupRow.find('td').at(1).text()).toBe('Calculated Measure');
    // 文件夹是否正确
    const folderRow = tableRows.at(3);
    expect(folderRow.find('td').at(0).text()).toBe('Folder');
    expect(folderRow.find('td').at(1).text()).toBe(' folder \\ folder ');
    // 表达式是否正确
    const expressionRow = tableRows.at(4);
    expect(expressionRow.find('td').at(0).text()).toBe('Expression');
    expect(expressionRow.find('td').at(1).text()).toBe('1');
    // 可见性是否正确
    const visibleRow = tableRows.at(6);
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
    expect(component.find('CalculateMeasureBlock').state().isEditMode).toBeFalsy();
  });

  it('编辑状态', async () => {
    await component.find(Button).at(1).find('button').simulate('click');

    // 标题是否正确
    expect(component.find('.block-header .pull-left').text()).toBe('Edit Calculated Measure');
    // 是否有递交和取消按钮
    expect(component.find(Button)).toHaveLength(4);
    expect(component.find(Button).at(0).find('button').text()).toBe('Cancel');
    expect(component.find(Button).at(1).find('button').text()).toBe('Save');
    expect(component.find(Button).at(2).find('button').text()).toBe('edit_page_16.svgEdit');
    expect(component.find(Button).at(3).find('button').text()).toBe('Use Templates');
    // 是否有名字输入框
    const nameInput = component.find(Form.Item).at(0).find(Input);
    expect(nameInput.get(0).props.value).toBe('计算度量A');
    expect(nameInput.get(0).props.disabled).toBeUndefined();
    // 是否有格式输入框
    const formatInput = component.find(FormatInput).at(0);
    expect(formatInput.get(0).props.format).toBe('regular');
    expect(formatInput.get(0).props.formatType).toBe('No Format');
    // 是否有位置选择框
    const folderSelect = component.find(Form.Item).at(2).find(Select);
    expect(folderSelect.get(0).props.value).toBe('Calculated Measure');
    expect(folderSelect.get(0).props.disabled).toBeUndefined();
    // 是否有文件夹输入框
    const FolderInput = component.find(Form.Item).at(3).find(Input);
    expect(FolderInput.get(0).props.value).toBe(' folder \\ folder ');
    expect(FolderInput.get(0).props.disabled).toBeUndefined();
    // 是否有表达式输入框
    const expressionInput = component.find(Form.Item).at(4).find(CodeEditor);
    expect(expressionInput.get(0).props.value).toBe('1');
    expect(expressionInput.get(0).props.disabled).toBeUndefined();
    // 是否有可见性开关
    const visibleSwitch = component.find(Form.Item).at(6).find(Switch);
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
    expect(props.boundDatasetActions.setCalculatedMeasure.args[1]).toEqual([
      '计算度量A', {
        name: '计算度量A',
        format: 'regular',
        formatType: 'No Format',
        desc: '',
        folder: 'Calculated Measure',
        subfolder: ' folder \\ folder ',
        expression: '1',
        isVisible: true,
        invisible: [],
        visible: [],
        nonEmptyBehavior: [],
        translation: {},
        error: '',
      },
    ]);
  });

  it('删除计算度量', async () => {
    // 智障框架不渲染了，需要执行setProps。
    component.setProps({});
    // 点击删除按钮
    await component.find(Button).at(0).find('button').simulate('click');
    await delayMs(0);

    expect(props.boundDatasetActions.deleteCalculatedMeasure.args[0]).toEqual([{
      name: '计算度量A',
      format: 'regular',
      formatType: 'No Format',
      desc: '',
      folder: 'Calculated Measure',
      subfolder: ' folder \\ folder ',
      expression: '1',
      isVisible: true,
      invisible: [],
      visible: [],
      nonEmptyBehavior: [],
      translation: {},
    }]);
    expect(props.onDelete.callCount).toBe(1);
  });
});
