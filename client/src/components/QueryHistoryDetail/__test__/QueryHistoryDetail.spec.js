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
import { Table, Checkbox, Tabs } from 'kyligence-ui-react';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import duration from 'dayjs/plugin/duration';

import { createMountComponent, delayMs } from '../../../__test__/utils';
import { getDefaultStore } from '../../../__test__/mocks';
import Pagination from '../../Pagination/Pagination';
import QueryHistoryDetail from '../QueryHistoryDetail';
import { getInitProps } from './handler';

describe('<QueryHistoryDetail /> 查询历史界面测试', () => {
  const JestComponent = QueryHistoryDetail.jest;
  const props = getInitProps();
  const store = getDefaultStore();
  let component;

  beforeAll(() => {
    dayjs.extend(relativeTime);
    dayjs.extend(duration);
  });

  it('组件渲染', async () => {
    component = createMountComponent({ JestComponent, store, props });
    await delayMs(0);
    component = component.find(Table).update();

    expect(component.find(Checkbox).at(0).state().checked).toBeFalsy();
    expect(component.find('Table TableBody .el-table__row.current-row').text()).toBe('MDXREFRESH CUBE [dataset]');
    expect(component.find('Table TableBody .el-table__row').at(1).text()).toBe('SQLselect count(*) from "DEFAULT"."KYLIN_SALES_1"');
    expect(component.find('Table TableBody .el-table__row').at(2).text()).toBe('SQLFailureselect count(*) from "DEFAULT"."KYLIN_SALES_2"');
    expect(component.find('Table TableBody .el-table__row').at(3).text()).toBe('SQLFailureselect count(*) from "DEFAULT"."KYLIN_SALES_3"');

    expect(component.find(Pagination)).toHaveLength(1);

    expect(component.find(Tabs).at(0).state().currentName).toBe('QUERY_CONTENT');
    expect(component.find('TabPane CodeEditor').at(0).prop('value')).toBe('REFRESH CUBE [dataset]');

    component.find('Tabs .el-tabs__item').at(1).simulate('click');
    expect(component.find(Tabs).at(0).state().currentName).toBe('QUERY_INFO');

    const queryDetail = component.find('.query-detail .query-detail-item');
    expect(queryDetail.at(0).text()).toBe('Query ID:7af63b7b-e0eb-4278-b486-7924e05d743b');
    expect(queryDetail.at(1).text()).toBe('Execution Time:< 1s');
    expect(queryDetail.at(2).text()).toBe('Use MDX Cache:False');
    expect(queryDetail.at(3).text()).toBe('Use Other Engine:True');
    expect(queryDetail.at(4).text()).toBe('Network Package:11556');
    expect(queryDetail.at(5).text()).toBe('Timeout:False');
    expect(queryDetail.at(6).text()).toBe('Time for Creating Multidimensional Dataset:1 min 1 s');
    expect(queryDetail.at(7).text()).toBe('Time for Marshall Soap Message:2 s');
    expect(queryDetail.at(8).text()).toBe('Dataset Name:dataset');
    expect(queryDetail.at(9).text()).toBe('Use Gateway:False');

    component.find('.copy-button button').simulate('click');
  });

  it('切换SQL查询记录1', () => {
    component.find('Table TableBody .el-table__row').at(1).simulate('click');
    // 高亮的一行变为选中的SQL
    expect(component.find('Table TableBody .el-table__row.current-row').text()).toBe('SQLselect count(*) from "DEFAULT"."KYLIN_SALES_1"');
    expect(component.find(Tabs).at(0).state().currentName).toBe('QUERY_INFO');

    const queryDetail = component.find('.query-detail .query-detail-item');
    expect(queryDetail.at(0).text()).toBe('System ID:1');
    expect(queryDetail.at(1).text()).toBe('Kyligence Query ID:None');
    expect(queryDetail.at(2).text()).toBe('Use SQL Cache:True');
    expect(queryDetail.at(3).text()).toBe('SQL Execution Time:< 1s');
    expect(queryDetail.at(4).text()).toBe('SQL Query Status:Success');
  });

  it('切换SQL查询记录2', () => {
    component.find('Table TableBody .el-table__row').at(2).simulate('click');
    // 高亮的一行变为选中的SQL
    expect(component.find('Table TableBody .el-table__row.current-row').text()).toBe('SQLFailureselect count(*) from "DEFAULT"."KYLIN_SALES_2"');

    const queryDetail = component.find('.query-detail .query-detail-item');
    expect(queryDetail.at(0).text()).toBe('System ID:2');
    expect(queryDetail.at(1).text()).toBe('Kyligence Query ID:None');
    expect(queryDetail.at(2).text()).toBe('Use SQL Cache:False');
    expect(queryDetail.at(3).text()).toBe('SQL Execution Time:None');
    expect(queryDetail.at(4).text()).toBe('SQL Query Status:Failure');
  });

  it('切换SQL查询记录3', () => {
    component.find('Table TableBody .el-table__row').at(3).simulate('click');
    // 高亮的一行变为选中的SQL
    expect(component.find('Table TableBody .el-table__row.current-row').text()).toBe('SQLFailureselect count(*) from "DEFAULT"."KYLIN_SALES_3"');

    const queryDetail = component.find('.query-detail .query-detail-item');
    expect(queryDetail.at(0).text()).toBe('System ID:3');
    expect(queryDetail.at(1).text()).toBe('Kyligence Query ID:None');
    expect(queryDetail.at(2).text()).toBe('Use SQL Cache:True');
    expect(queryDetail.at(3).text()).toBe('SQL Execution Time:1 hour 1 min');
    expect(queryDetail.at(4).text()).toBe('SQL Query Status:Failure');
  });

  it('只显示错误的SQL查询', () => {
    component.find('label.mdx-it-only-failure-query').simulate('click');
    expect(component.find('QueryHistoryDetail').state().filter.isOnlyFailure).toBeTruthy();
    expect(props.boundQueryHistoryActions.getQuerySqlByQueryId.lastCall.args[0]).toEqual({
      pageOffset: 0,
      pageSize: 5,
      onlyFailure: 0,
      queryId: '7af63b7b-e0eb-4278-b486-7924e05d743b',
    });
  });

  it('切回显示所有的SQL查询', () => {
    component.find('label.mdx-it-only-failure-query').simulate('click');
    expect(component.find('QueryHistoryDetail').state().filter.isOnlyFailure).toBeFalsy();
    expect(props.boundQueryHistoryActions.getQuerySqlByQueryId.lastCall.args[0]).toEqual({
      pageOffset: 0,
      pageSize: 5,
      onlyFailure: undefined,
      queryId: '7af63b7b-e0eb-4278-b486-7924e05d743b',
    });
  });

  it('切分页', () => {
    component.find('Pagination .el-pager li.number').at(1).simulate('click');
    expect(component.find('QueryHistoryDetail').state().pageOffset).toBe(1);
    expect(component.find('QueryHistoryDetail').state().pageSize).toBe(5);
    expect(props.boundQueryHistoryActions.getQuerySqlByQueryId.lastCall.args[0])
      .toEqual({
        pageOffset: 1,
        pageSize: 5,
        onlyFailure: undefined,
        queryId: '7af63b7b-e0eb-4278-b486-7924e05d743b',
      });
  });
});
