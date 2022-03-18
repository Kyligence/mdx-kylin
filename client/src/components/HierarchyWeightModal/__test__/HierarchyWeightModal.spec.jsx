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
import sinon from 'sinon';
import { Layout, Input, Select, Dialog, Button } from 'kyligence-ui-react';

import { getInitProps } from './handler';
import { getDefaultStore } from '../../../__test__/mocks';
import { createMountComponent } from '../../../__test__/utils';
import HierarchyWeightModal from '../HierarchyWeightModal';

/* eslint-disable max-len, newline-per-chained-call */
describe('<HierarchyWeightModal /> 层级维度界面测试', () => {
  const JestComponent = HierarchyWeightModal.jest;
  const props = getInitProps();
  const store = getDefaultStore();
  let component = createMountComponent({ JestComponent, props, store });

  it('初始化弹窗', () => {
    expect(component).toHaveLength(1);
    expect(component.find('HierarchyWeightModal Dialog').get(0).props.visible).toBeFalsy();
  });

  // 用例原则：ColumnA/B保持不变，ColumnC/D切换取反
  it('展示弹窗', async () => {
    store.modal.HierarchyWeightModal = {
      isShow: true,
      callback: sinon.spy(),
      columns: [
        { label: 'Column A', value: 'ColumnA', dataType: 'varchar' },
        { label: 'Column B', value: 'ColumnB', dataType: 'int' },
        { label: 'Column C', value: 'ColumnC', dataType: 'int' },
        { label: 'Column D', value: 'ColumnD', dataType: 'int' },
        { label: 'Column E', value: 'ColumnE', dataType: 'int' },
        { label: 'Column F', value: 'ColumnF', dataType: 'int' },
        { label: 'Column G', value: 'ColumnG', dataType: 'int' },
        { label: 'Column H', value: 'ColumnH', dataType: 'varchar' },
      ],
      form: {
        dimCols: ['ColumnA', 'ColumnB', 'ColumnC', 'ColumnD'],
        weightCols: [null, 'ColumnE', null, 'ColumnF'],
      },
    };

    component = createMountComponent({ JestComponent, props, store });
    expect(component.find('HierarchyWeightModal Dialog').get(0).props.visible).toBeTruthy();

    // 验证DimCols展示是否都是别名，且都不可编辑
    const dimColInputs = component.find('.hierarchy-weight-item td:first-child Input');
    expect(dimColInputs.map(dimColInput => dimColInput.get(0).props.value)).toEqual(['Column A', 'Column B', 'Column C', 'Column D']);
    expect(dimColInputs.map(dimColInput => dimColInput.get(0).props.disabled)).toEqual([true, true, true, true]);

    // 验证WeightCols展示是否都是别名，且都可以编辑
    const weightColSelectors = component.find('.hierarchy-weight-item td + td Select');
    expect(weightColSelectors.map(weightColSelector => weightColSelector.find('input').getElement().props.value)).toEqual(['', 'Column E', '', 'Column F']);
    expect(weightColSelectors.map(weightColSelector => weightColSelector.get(0).props.disabled)).toEqual([undefined, undefined, undefined, undefined]);

    // 验证WeightCols渲染的选项是否正确
    const weightColOptions = weightColSelectors.at(0).find(Select.Option);
    expect(weightColOptions.map(weightColOption => weightColOption.text())).toEqual(['Column E', 'Column F', 'Column G']);
  });

  it('设置第三个维度列的权重', async () => {
    const options = component.find('.hierarchy-weight-item td + td Select').at(2).find(Select.Option);
    const optionIdx = options.map(option => option.text() === 'Column G').findIndex(isTrue => isTrue);
    options.at(optionIdx).find('li').simulate('click');

    expect(props.boundModalActions.setModalForm.lastCall.args).toEqual([
      'HierarchyWeightModal',
      { weightCols: [null, 'ColumnE', 'ColumnG', 'ColumnF'] },
    ]);
    store.modal.HierarchyWeightModal.form.weightCols[2] = 'ColumnG';
  });

  it('清空第四个维度列的权重', async () => {
    component = createMountComponent({ JestComponent, props, store });

    const selector = component.find('.hierarchy-weight-item td + td Select').at(3);
    selector.find(Input).simulate('mouseenter');

    component.find('.hierarchy-weight-item td + td Select').at(3).find('.el-icon-circle-close').simulate('click');

    expect(props.boundModalActions.setModalForm.lastCall.args).toEqual([
      'HierarchyWeightModal',
      { weightCols: [null, 'ColumnE', 'ColumnG', null] },
    ]);
    store.modal.HierarchyWeightModal.form.weightCols[3] = null;
  });

  it('点击保存层级结构', async () => {
    component = createMountComponent({ JestComponent, props, store });
    component.find(Dialog.Footer).find(Button).at(1).find('button').simulate('click');
    expect(store.modal.HierarchyWeightModal.callback.lastCall.args).toEqual([{
      isSubmit: true,
      dimCols: ['ColumnA', 'ColumnB', 'ColumnC', 'ColumnD'],
      weightCols: [null, 'ColumnE', 'ColumnG', null],
    }]);
  });
});
