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
