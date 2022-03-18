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
import { Table } from 'kyligence-ui-react';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import duration from 'dayjs/plugin/duration';

import { createMountComponent, delayMs } from '../../../__test__/utils';
import { getDefaultStore } from '../../../__test__/mocks';
import Pagination from '../../../components/Pagination/Pagination';
import SuggestionFilterInput from '../../../components/SuggestionFilterInput/SuggestionFilterInput';
import QueryHistory from '../QueryHistory';
import { getInitProps, getListData } from './handler';

/* eslint-disable max-len */
describe('<QueryHistory /> 查询历史界面测试', () => {
  const JestComponent = QueryHistory.jest;
  const props = getInitProps();
  const store = getDefaultStore();
  store.system.currentProject = { name: '项目名称', permission: 'GLOBAL_ADMIN' };
  let component = createMountComponent({ JestComponent, store, props });

  beforeAll(() => {
    dayjs.extend(relativeTime);
    dayjs.extend(duration);

    global.document.createRange = () => ({
      setStart: () => {},
      setEnd: () => {},
      commonAncestorContainer: {
        nodeName: 'BODY',
        ownerDocument: document,
      },
    });
  });

  it('组件渲染', () => {
    expect(component.find(SuggestionFilterInput)).toHaveLength(1);
    expect(component.find(Table)).toHaveLength(1);
    expect(component.find('TableBody .el-table__row')).toHaveLength(0);
    expect(component.find(Pagination)).toHaveLength(1);

    expect(props.boundQueryHistoryActions.getQueryHistory.callCount).toBe(1);
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[0])
      .toEqual({ pageOffset: 0, pageSize: 10, orderBy: null, direction: null });
  });

  it('表格渲染', () => {
    store.data.queryHistory = getListData();
    store.system.currentProject.name = 'learn_kylin';
    component = createMountComponent({ JestComponent, store, props });

    expect(component.find('TableBody .el-table__row')).toHaveLength(10);

    const firstRowFields = component.find('TableBody .el-table__row').at(0).find('td');
    expect(firstRowFields.at(1).text()).toBe('2020-10-27 12:56:31 GMT+8');
    expect(firstRowFields.at(2).text()).toBe('< 1s');
    expect(firstRowFields.at(3).text()).toBe('SQL 1');
    expect(firstRowFields.at(4).text()).toBe('dataset');
    expect(firstRowFields.at(5).text()).toBe('Failure');
    expect(firstRowFields.at(6).text()).toBe('Excel');
    expect(firstRowFields.at(7).text()).toBe('localhost:7080');
    expect(firstRowFields.at(8).text()).toBe('user');

    const secondRowFields = component.find('TableBody .el-table__row').at(1).find('td');
    expect(secondRowFields.at(1).text()).toBe('2020-10-27 12:56:31 GMT+8');
    expect(secondRowFields.at(2).text()).toBe('2 s');
    expect(secondRowFields.at(3).text()).toBe('SQL 2');
    expect(secondRowFields.at(4).text()).toBe('dataset');
    expect(secondRowFields.at(5).text()).toBe('Success');
    expect(secondRowFields.at(6).text()).toBe('Excel');
    expect(secondRowFields.at(7).text()).toBe('localhost:7080');
    expect(secondRowFields.at(8).text()).toBe('user');
  });

  it('切换页码 1', async () => {
    component.find('Pagination .el-pager li.number').at(1).simulate('click');
    expect(component.find('QueryHistory').state().pageOffset).toBe(1);
    expect(component.find('QueryHistory').state().pageSize).toBe(10);
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[0])
      .toEqual({ pageOffset: 1, pageSize: 10, orderBy: null, direction: null });
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[1])
      .toEqual({ query_id: undefined, username: undefined, success: undefined, application: [], cluster: [], startTimeFrom: undefined, startTimeTo: undefined });
  });

  // 同时测试更改过滤条件，页面应该回归 0
  it('切换用户过滤条件', async () => {
    // 在输入框输入用户名
    component.find('SuggestionFilterInput input').simulate('focus');
    component.find('SuggestionFilterInput input').simulate('change', { target: { value: 'user' } });
    expect(component.find('SuggestionFilterInput li').at(0).text()).toBe('Search by Query ID:user');
    expect(component.find('SuggestionFilterInput li').at(1).text()).toBe('Search by User:user');
    expect(component.find('SuggestionFilterInput input').getDOMNode().getAttribute('value')).toBe('user');
    // 选择用户名作为筛选条件
    component.find('SuggestionFilterInput li').at(1).simulate('click');
    expect(component.find('SuggestionFilterInput input').getDOMNode().getAttribute('value')).toBe('');

    expect(component.find('QueryHistory').state().pageOffset).toBe(0);
    expect(component.find('QueryHistory').state().pageSize).toBe(10);
    expect(component.find('QueryHistory').state().filter.username).toBe('user');
    // 异步搜索会防抖1秒钟
    await delayMs(2000);
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[0])
      .toEqual({ pageOffset: 0, pageSize: 10, orderBy: null, direction: null });
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[1])
      .toEqual({ query_id: undefined, username: 'user', success: undefined, application: [], cluster: [], startTimeFrom: undefined, startTimeTo: undefined });
  });

  it('切换查询ID过滤条件', async () => {
    // 在输入框输入查询ID
    component.find('SuggestionFilterInput input').simulate('focus');
    component.find('SuggestionFilterInput input').simulate('change', { target: { value: '340bb08a-594f-4e2c-898a-fcca4eb8a221' } });
    expect(component.find('SuggestionFilterInput li').at(0).text()).toBe('Search by Query ID:340bb08a-594f-4e2c-898a-fcca4eb8a221');
    expect(component.find('SuggestionFilterInput li').at(1).text()).toBe('Search by User:340bb08a-594f-4e2c-898a-fcca4eb8a221');
    expect(component.find('SuggestionFilterInput input').getDOMNode().getAttribute('value')).toBe('340bb08a-594f-4e2c-898a-fcca4eb8a221');
    // 选择查询ID作为筛选条件
    component.find('SuggestionFilterInput li').at(0).simulate('click');
    expect(component.find('SuggestionFilterInput input').getDOMNode().getAttribute('value')).toBe('');

    expect(component.find('QueryHistory').state().pageOffset).toBe(0);
    expect(component.find('QueryHistory').state().pageSize).toBe(10);
    expect(component.find('QueryHistory').state().filter.queryId).toBe('340bb08a-594f-4e2c-898a-fcca4eb8a221');
    // 异步搜索会防抖1秒钟
    await delayMs(2000);
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[0])
      .toEqual({ pageOffset: 0, pageSize: 10, orderBy: null, direction: null });
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[1])
      .toEqual({ query_id: '340bb08a-594f-4e2c-898a-fcca4eb8a221', username: 'user', success: undefined, application: [], cluster: [], startTimeFrom: undefined, startTimeTo: undefined });
  });

  // 同时测试更改分页，过滤条件依旧还在
  it('切换页码 2', () => {
    component.find('Pagination .el-pager li.number').at(2).simulate('click');
    expect(component.find('QueryHistory').state().pageOffset).toBe(2);
    expect(component.find('QueryHistory').state().pageSize).toBe(10);

    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[0])
      .toEqual({ pageOffset: 2, pageSize: 10, orderBy: null, direction: null });
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[1])
      .toEqual({ query_id: '340bb08a-594f-4e2c-898a-fcca4eb8a221', username: 'user', success: undefined, application: [], cluster: [], startTimeFrom: undefined, startTimeTo: undefined });
  });

  // 同时测试更改过滤条件，页面应该回归 0
  it('切换MDX查询状态过滤条件为成功', async () => {
    const theader = component.find('Table TableHeader thead th').at(5);
    const checkbox = theader.find('TableFilterBox Checkbox').at(0);
    checkbox.find('label').simulate('click');
    expect(component.find('QueryHistory').state().pageOffset).toBe(0);
    expect(component.find('QueryHistory').state().pageSize).toBe(10);
    expect(component.find('QueryHistory').state().filter.status).toBe(true);

    // 异步搜索会防抖1秒钟
    await delayMs(2000);
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[0])
      .toEqual({ pageOffset: 0, pageSize: 10, orderBy: null, direction: null });
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[1])
      .toEqual({ query_id: '340bb08a-594f-4e2c-898a-fcca4eb8a221', username: 'user', success: true, application: [], cluster: [], startTimeFrom: undefined, startTimeTo: undefined });
  });

  it('切换MDX查询状态过滤条件为空', async () => {
    const theader = component.find('Table TableHeader thead th').at(5);
    const checkbox = theader.find('TableFilterBox Checkbox').at(0);
    checkbox.find('label').simulate('click');
    expect(component.find('QueryHistory').state().pageOffset).toBe(0);
    expect(component.find('QueryHistory').state().pageSize).toBe(10);
    expect(component.find('QueryHistory').state().filter.status).toBe(undefined);

    // 异步搜索会防抖1秒钟
    await delayMs(2000);
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[0])
      .toEqual({ pageOffset: 0, pageSize: 10, orderBy: null, direction: null });
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[1])
      .toEqual({ query_id: '340bb08a-594f-4e2c-898a-fcca4eb8a221', username: 'user', success: undefined, application: [], cluster: [], startTimeFrom: undefined, startTimeTo: undefined });
  });

  it('切换MDX应用程序过滤条件为Excel和Tableau', async () => {
    const theader = component.find('Table TableHeader thead th').at(6);
    theader.find('TableFilterBox Checkbox').at(0).find('label').simulate('click');
    theader.find('TableFilterBox Checkbox').at(1).find('label').simulate('click');
    expect(component.find('QueryHistory').state().pageOffset).toBe(0);
    expect(component.find('QueryHistory').state().pageSize).toBe(10);
    expect(component.find('QueryHistory').state().filter.application).toEqual(['Excel', 'Tableau']);

    // 异步搜索会防抖1秒钟
    await delayMs(2000);
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[0])
      .toEqual({ pageOffset: 0, pageSize: 10, orderBy: null, direction: null });
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[1])
      .toEqual({ query_id: '340bb08a-594f-4e2c-898a-fcca4eb8a221', username: 'user', success: undefined, application: ['Excel', 'Tableau'], cluster: [], startTimeFrom: undefined, startTimeTo: undefined });
  });

  it('切回页码 1', async () => {
    component.find('Pagination .el-pager li.number').at(1).simulate('click');
    expect(component.find('QueryHistory').state().pageOffset).toBe(1);
    expect(component.find('QueryHistory').state().pageSize).toBe(10);
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[0])
      .toEqual({ pageOffset: 1, pageSize: 10, orderBy: null, direction: null });
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[1])
      .toEqual({ query_id: '340bb08a-594f-4e2c-898a-fcca4eb8a221', username: 'user', success: undefined, application: ['Excel', 'Tableau'], cluster: [], startTimeFrom: undefined, startTimeTo: undefined });
  });

  it('对表头递增排序', async () => {
    component.find('Table TableHeader thead th').at(1).simulate('click');
    expect(component.find('QueryHistory').state().pageOffset).toBe(1);
    expect(component.find('QueryHistory').state().pageSize).toBe(10);
    expect(component.find('QueryHistory').state().orderBy).toBe('startTime');
    expect(component.find('QueryHistory').state().direction).toBe('ascending');
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[0])
      .toEqual({ pageOffset: 1, pageSize: 10, orderBy: 'start', direction: 'ascending' });
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[1])
      .toEqual({ query_id: '340bb08a-594f-4e2c-898a-fcca4eb8a221', username: 'user', success: undefined, application: ['Excel', 'Tableau'], cluster: [], startTimeFrom: undefined, startTimeTo: undefined });
  });

  it('删除MDX应用程序过滤条件为Tableau', async () => {
    component.find('.layout-filters Tag').at(3).find('.el-icon-close').simulate('click');
    expect(component.find('QueryHistory').state().pageOffset).toBe(0);
    expect(component.find('QueryHistory').state().pageSize).toBe(10);
    expect(component.find('QueryHistory').state().filter.application).toEqual(['Excel']);

    // 异步搜索会防抖1秒钟
    await delayMs(2000);
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[0])
      .toEqual({ pageOffset: 0, pageSize: 10, orderBy: 'start', direction: 'ascending' });
    expect(props.boundQueryHistoryActions.getQueryHistory.lastCall.args[1])
      .toEqual({ query_id: '340bb08a-594f-4e2c-898a-fcca4eb8a221', username: 'user', success: undefined, application: ['Excel'], cluster: [], startTimeFrom: undefined, startTimeTo: undefined });
  });

  it('展开某一行', async () => {
    // 暂时无法实现
    // component.find('QueryHistory').instance().renderExpandRow = sinon.spy();
    // const firstRowFields = component.find('TableBody .el-table__row').at(0).find('td');
    // firstRowFields.at(0).simulate('click');
    // console.log(component.find('QueryHistory').instance().renderExpandRow.lastCall.args);
  });
});
