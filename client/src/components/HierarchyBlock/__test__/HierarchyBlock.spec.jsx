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
import { Form, Input, Button, Select, Cascader, MessageBox } from 'kyligence-ui-react';

import { createMountComponent, randomString, delayMs } from '../../../__test__/utils';
import { getDefaultStore } from '../../../__test__/mocks';
import { getMockStoreDataset } from '../../../store/reducers/workspace/__test__/dataset/mocks';
import { getInitPorps } from './handler';
import HierarchyBlock from '../HierarchyBlock';

describe('<HierarchyBlock /> 层级维度界面测试', () => {
  const JestComponent = HierarchyBlock.jest;
  const props = getInitPorps();
  const store = getDefaultStore();
  store.workspace.dataset = getMockStoreDataset();
  store.workspace.dataset.models
    .find(m => m.name === '模型A').dimensionTables
    .find(t => t.name === '维度表A').hierarchys
    .push({
      name: '层级维度_同表重名',
      desc: '',
      tablePath: ['模型A', '维度表A'],
      dimCols: ['维度列A', '维度列B'],
      weightCols: [null, null],
      translation: {},
      errors: [],
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
    expect(component.find('.block-header .pull-left').text()).toBe('Add Hierarchy');
    // 是否有递交和取消按钮
    expect(component.find('.block-header').find(Button)).toHaveLength(2);
    expect(component.find(Button).at(0).find('button').text()).toBe('Cancel');
    expect(component.find(Button).at(1).find('button').text()).toBe('Save');
    // 是否有名字输入框渲染
    const nameInput = component.find(Form.Item).at(0).find(Input);
    expect(nameInput.get(0).props.value).toBe('');
    expect(nameInput.get(0).props.disabled).toBeUndefined();
    // 是否有模型/表级联选择框
    const belongCascader = component.find(Form.Item).at(1).find(Cascader);
    expect(belongCascader.get(0).props.value).toEqual([]);
    expect(belongCascader.get(0).props.disabled).toBeUndefined();
    // 是否有层级维度选择框
    const dimColSelect = component.find(Form.Item).at(2).find(Select);
    expect(dimColSelect.get(0).props.value).toEqual([]);
    expect(dimColSelect.get(0).props.disabled).toBeUndefined();
    // 是否有层级维度权重按钮
    expect(component.find(Form.Item).at(2).find(Button)).toHaveLength(1);
  });

  it('创建空表单报错', async () => {
    await component.find(Button).at(1).find('button').simulate('click');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('Please enter name.');
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('Please select the table where you need to create a hierarchy dimension.');
    expect(component.find(Form.Item).at(2).find('.el-form-item__error').text()).toBe('Please select 2 or more dimensions.');
  });

  it('输入正确的创建数据', async () => {
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: '层级维度A' } })
      .simulate('blur');
    // 设置 model为 模型A，设置 table为 维度表A
    await component.find(Form.Item).at(1).find('.el-cascader-menu').at(0)
      .find('li')
      .at(0)
      .simulate('mouseenter');
    await component.find(Form.Item).at(1).find('.el-cascader-menu').at(1)
      .find('li')
      .at(1)
      .simulate('click');

    const dimColOptions = component.find(Form.Item).at(2).find(Select.Option);
    await dimColOptions.at(0).find('li').simulate('click');
    await dimColOptions.at(1).find('li').simulate('click');
    await dimColOptions.at(2).find('li').simulate('click');

    // 级联选择器触发form.onFieldChange有setTimeout
    await delayMs(0);

    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('');
    expect(component.find(Form.Item).at(1).find('.el-form-item__error').text()).toBe('');
    expect(component.find(Form.Item).at(2).find('.el-form-item__error').text()).toBe('');

    expect(component.find('HierarchyBlock').state().form).toEqual({
      name: '层级维度A',
      desc: '',
      tablePath: ['模型A', '维度表A'],
      dimCols: ['维度列A', '维度列B', '维度C'],
      weightCols: [null, null, null],
      translation: {},
    });
  });

  it('输入错误数据', async () => {
    const emptyAlias = '';
    const invalidAlias = '特殊符号错误!@#$%^&*()_+';
    const duplicateAlias = '层级维度_同表重名';
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
    // 重名验证
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: duplicateAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('The hierarchy named [层级维度_同表重名] already exists.');
    // 名称太长验证
    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: tooLongAlias } })
      .simulate('blur');
    expect(component.find(Form.Item).at(0).find('.el-form-item__error').text()).toBe('Hierarchy name length cannot be greater than 300.');

    await component.find(Form.Item).at(0).find(Input).find('input')
      .simulate('change', { target: { value: '层级维度A' } })
      .simulate('blur');
  });

  it('递交创建', async () => {
    await component.find(Button).at(1).find('button').simulate('click');
    await delayMs(0);

    expect(props.onSubmit.callCount).toBe(1);
    expect(props.boundDatasetActions.deleteHierarchy.called).toBeFalsy();
    expect(props.boundDatasetActions.setHierarchy.args[0]).toEqual([
      '模型A', '维度表A', null, {
        name: '层级维度A',
        desc: '',
        tablePath: ['模型A', '维度表A'],
        dimCols: ['维度列A', '维度列B', '维度C'],
        weightCols: [null, null, null],
        translation: {},
        errors: [],
      },
    ]);
  });

  it('阅览状态', async () => {
    props.data.name = '层级维度A';
    props.data.model = '模型A';
    props.data.table = '维度表A';
    props.data.tablePath = ['模型A', '维度表A'];
    props.data.dimCols = ['维度列A', '维度列B', '维度C'];
    props.data.weightCols = [null, null, null];
    component = createMountComponent({ JestComponent, props, store });

    // 标题是否正确
    expect(component.find('.block-header .pull-left').text()).toBe('Show Hierarchy');
    // 信息展示是否正确
    const tableRows = component.find('table tr');
    // 名称是否正确
    const nameRow = tableRows.at(0);
    expect(nameRow.find('td').at(0).text()).toBe('Name');
    expect(nameRow.find('td').at(1).text()).toBe('层级维度A');

    const pathRow = tableRows.at(1);
    expect(pathRow.find('td').at(0).text()).toBe('Model / Table');
    expect(pathRow.find('td').at(1).text()).toBe('模型A/维度表A');

    const dimColRow = tableRows.at(2);
    expect(dimColRow.find('td').at(0).text()).toBe('Hierarchy');
    expect(dimColRow.find('td').at(1).text()).toBe('1维度列A2维度列B3维度列C');

    expect(component.find(Button).at(0).find('button').text()).toBe('Delete');
    expect(component.find(Button).at(1).find('button').text()).toBe('Edit');
  });

  it('取消编辑', async () => {
    // 进入编辑
    await component.find(Button).at(1).find('button').simulate('click');
    // 取消编辑
    await component.find(Button).at(0).find('button').simulate('click');
    // 是否在阅览状态
    expect(component.find('HierarchyBlock').state().isEditMode).toBeFalsy();
  });

  it('编辑状态', async () => {
    await component.find(Button).at(1).find('button').simulate('click');

    // 标题是否正确
    expect(component.find('.block-header .pull-left').text()).toBe('Edit Hierarchy');
    // 是否有递交和取消按钮
    expect(component.find('.block-header').find(Button)).toHaveLength(2);
    expect(component.find(Button).at(0).find('button').text()).toBe('Cancel');
    expect(component.find(Button).at(1).find('button').text()).toBe('Save');
    // 是否有名字输入框渲染
    const nameInput = component.find(Form.Item).at(0).find(Input);
    expect(nameInput.get(0).props.value).toBe('层级维度A');
    expect(nameInput.get(0).props.disabled).toBeUndefined();
    // 是否有模型/表级联选择框
    const belongCascader = component.find(Form.Item).at(1).find(Cascader);
    expect(belongCascader.get(0).props.value).toEqual(['模型A', '维度表A']);
    expect(belongCascader.get(0).props.disabled).toBeUndefined();
    // 是否有层级维度选择框
    const dimColSelect = component.find(Form.Item).at(2).find(Select);
    expect(dimColSelect.get(0).props.value).toEqual(['维度列A', '维度列B', '维度C']);
    expect(dimColSelect.get(0).props.disabled).toBeUndefined();

    // 是否有层级维度权重按钮
    expect(component.find(Form.Item).at(2).find(Button)).toHaveLength(1);

    // 打开模型/表选择菜单
    await component.find(Form.Item).at(1).find('.el-cascader > span').simulate('click');
    // 切换层级维度所属模型/表
    const cascaderMenu = component.find(Form.Item).at(1).find('.el-cascader-menu');
    await cascaderMenu.at(0).find('li').at(5).simulate('click');
    await cascaderMenu.at(1).find('li').at(2).simulate('click');
    expect(component.find('HierarchyBlock').state().form.tablePath).toEqual(['模型A', '维度表B']);
    expect(component.find('HierarchyBlock').state().form.dimCols).toEqual([]);

    // 编辑层级维度列
    const dimColOptions = component.find(Form.Item).at(2).find(Select.Option);
    await dimColOptions.at(1).find('li').simulate('click');
    await dimColOptions.at(2).find('li').simulate('click');
    expect(component.find('HierarchyBlock').state().form.dimCols).toEqual(['维度列B', '维度C']);

    await component.find(Form.Item).at(2).find(Button).simulate('click');
    expect(props.boundModalActions.setModalData.args[0]).toEqual([
      'HierarchyWeightModal',
      {
        modelName: '模型A',
        tableName: '维度表B',
        columns: [
          { label: '维度列A', value: '维度列A', dataType: 'integer' },
          { label: '维度列B', value: '维度列B', dataType: 'integer' },
          { label: '维度列C', value: '维度C', dataType: 'integer' },
        ],
        errors: [],
      },
    ]);
    expect(props.boundModalActions.showModal.args[0]).toEqual(['HierarchyWeightModal', {
      dimCols: ['维度列B', '维度C'],
      weightCols: [null, null],
    }]);
  });

  it('递交编辑', async () => {
    await component.update();
    await component.find(Button).at(1).find('button').simulate('click');
    await delayMs(500);

    expect(props.onSubmit.callCount).toBe(2);
    expect(props.boundDatasetActions.setHierarchy.args[1]).toEqual([
      '模型A', '维度表B', '层级维度A', {
        name: '层级维度A',
        desc: '',
        tablePath: ['模型A', '维度表B'],
        dimCols: ['维度列B', '维度C'],
        weightCols: [null, '权重列A'],
        translation: {},
        errors: [],
      },
    ]);
  });

  it('删除层级维度', async () => {
    component = createMountComponent({ JestComponent, props, store });

    await component.find('.block-header Button').at(0).find('button').simulate('click');
    // 此处由于有异步action，延时一定时间
    await delayMs(10);
    expect(props.boundDatasetActions.deleteHierarchy.args[0]).toEqual([
      '模型A', '维度表A', {
        name: '层级维度A',
        desc: '',
        model: '模型A',
        table: '维度表A',
        tablePath: ['模型A', '维度表A'],
        dimCols: ['维度列A', '维度列B', '维度C'],
        weightCols: [null, null, null],
        translation: {},
        errors: [],
      },
    ]);
    expect(props.onDelete.callCount).toBe(1);
  });
});
