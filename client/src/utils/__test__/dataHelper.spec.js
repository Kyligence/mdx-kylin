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
import React from 'react';
import { shallow } from 'enzyme';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import duration from 'dayjs/plugin/duration';

import * as dataHelper from '../dataHelper';
import { strings } from '../../constants';
import { intl, createIntlShallowComponent } from '../../__test__/utils';

describe('dataHelper函数测试', () => {
  beforeAll(() => {
    dayjs.extend(relativeTime);
    dayjs.extend(duration);
  });

  it('isEmpty', () => {
    // 测试输入为空
    expect(dataHelper.isEmpty()).toBeTruthy();
    expect(dataHelper.isEmpty(undefined)).toBeTruthy();
    expect(dataHelper.isEmpty(null)).toBeTruthy();
    expect(dataHelper.isEmpty('')).toBeTruthy();
    // 测试输入非空
    expect(dataHelper.isEmpty('string')).toBeFalsy();
    expect(dataHelper.isEmpty(0)).toBeFalsy();
    expect(dataHelper.isEmpty({})).toBeFalsy();
    expect(dataHelper.isEmpty(true)).toBeFalsy();
    expect(dataHelper.isEmpty(false)).toBeFalsy();
    expect(dataHelper.isEmpty([])).toBeFalsy();
    expect(dataHelper.isEmpty(() => {})).toBeFalsy();
  });

  it('getDateString', () => {
    // 测试合法时间戳转日期
    expect(dataHelper.getDateString(1577808000000)).toBe('2020-01-01 00:00:00 GMT+8');
    expect(dataHelper.getDateString('1577808000000')).toBe('2020-01-01 00:00:00 GMT+8');
    // 测试非法时间戳转日期
    expect(dataHelper.getDateString('string')).toBe(null);
  });

  it('getActionInColumns', () => {
    // 不渲染操作按钮
    const [actionColumn] = dataHelper.getActionInColumns({ intl }, []);
    expect(actionColumn.label).toBe('Actions');
    expect(actionColumn.width).toBe(84);
    expect(!!createIntlShallowComponent(actionColumn.render({ name: 'row' }))).toBeTruthy();
  });

  it('getExcludeTypeValue', () => {
    const column = { name: 'CAL_DT', order: 1, children: [], data: {} };
    expect(dataHelper.getExcludeTypeValue(['object'], column)).toEqual({ name: 'CAL_DT', order: 1 });
  });

  /* eslint-disable react/jsx-filename-extension */
  it('renderStringWithBr', () => {
    // 检测回车被转成换行
    const component = shallow(<div>{dataHelper.renderStringWithBr('title\ndescription')}</div>);
    expect(component.text()).toBe('titledescription');
    expect(component.find('br')).toHaveLength(1);
  });

  it('translate', () => {
    // 翻译对象被正确翻译
    expect(dataHelper.translate(intl, strings.KYLIN_MDX)).toBe('Kylin MDX');
    // 翻译字符串被正确翻译
    expect(dataHelper.translate(intl, 'KYLIN_MDX')).toBe('Kylin MDX');
  });

  it('getPaginationTable', () => {
    // 空值检查
    let paginationData = dataHelper.getPaginationTable();
    expect(paginationData.data).toEqual([]);
    expect(paginationData.pageOffset).toEqual(0);
    expect(paginationData.pageSize).toEqual(10);
    expect(paginationData.totalCount).toEqual(0);
    expect(paginationData.columns).toEqual([]);

    const datas = [
      { name: 'CAL_DT', nodeType: 'column' },
      { name: 'SELLER_ACCOUNT', nodeType: 'column' },
      { name: 'BUYER_ACCOUNT', nodeType: 'column' },
      { name: 'DATE-HIERARCHY', nodeType: 'hierarchy' },
      { name: 'COUNTRY-HIERARCHY', nodeType: 'hierarchy' },
    ];
    // 正确分页成 2条/页，处在第1页
    paginationData = dataHelper.getPaginationTable({ datas, pageOffset: 0, pageSize: 2 });
    expect(paginationData.data).toEqual([
      { name: 'CAL_DT', nodeType: 'column' },
      { name: 'SELLER_ACCOUNT', nodeType: 'column' },
    ]);
    expect(paginationData.pageOffset).toEqual(0);
    expect(paginationData.pageSize).toEqual(2);
    expect(paginationData.totalCount).toEqual(5);
    expect(paginationData.columns).toEqual([
      { prop: 'name', label: { defaultMessage: 'Name', id: 'NAME' } },
      { prop: 'nodeType', label: 'NodeType' },
    ]);
    // 正确分页成 3条/页，处在第2页
    paginationData = dataHelper.getPaginationTable({ datas, pageOffset: 1, pageSize: 3 });
    expect(paginationData.data).toEqual([
      { name: 'DATE-HIERARCHY', nodeType: 'hierarchy' },
      { name: 'COUNTRY-HIERARCHY', nodeType: 'hierarchy' },
    ]);
    expect(paginationData.pageOffset).toEqual(1);
    expect(paginationData.pageSize).toEqual(3);
    expect(paginationData.totalCount).toEqual(5);
    expect(paginationData.columns).toEqual([
      { prop: 'name', label: { defaultMessage: 'Name', id: 'NAME' } },
      { prop: 'nodeType', label: 'NodeType' },
    ]);
    // 正确分页成 10条/页，处在第1页，名字过滤"hierarchy"
    paginationData = dataHelper.getPaginationTable({ datas, pageOffset: 0, pageSize: 10, filters: [{ name: 'hierarchy' }] });
    expect(paginationData.data).toEqual([
      { name: 'DATE-HIERARCHY', nodeType: 'hierarchy' },
      { name: 'COUNTRY-HIERARCHY', nodeType: 'hierarchy' },
    ]);
    expect(paginationData.pageOffset).toEqual(0);
    expect(paginationData.pageSize).toEqual(10);
    expect(paginationData.totalCount).toEqual(2);
    expect(paginationData.columns).toEqual([
      { prop: 'name', label: { defaultMessage: 'Name', id: 'NAME' } },
      { prop: 'nodeType', label: 'NodeType' },
    ]);
  });

  it('findLastIndex', () => {
    // 空值检查
    let list = [];
    expect(dataHelper.findLastIndex(list, () => {})).toBe(-1);
    // 从后往前找到"success"的index
    list = [{ status: 'success' }, { status: 'success' }, { status: 'failed' }];
    expect(dataHelper.findLastIndex(list, ({ status }) => status === 'success')).toBe(1);
    // 从后往前找到"failed"的index
    list = [{ status: 'success' }, { status: 'success' }, { status: 'failed' }];
    expect(dataHelper.findLastIndex(list, ({ status }) => status === 'failed')).toBe(2);
    // 从后往前找不到"unknow"的index
    list = [{ status: 'success' }, { status: 'success' }, { status: 'failed' }];
    expect(dataHelper.findLastIndex(list, ({ status }) => status === 'unknow')).toBe(-1);
  });

  it('isJsonParseError', () => {
    expect(dataHelper.isJsonParseError({ message: 'JSON.parse', stack: '' })).toBeTruthy();
    expect(dataHelper.isJsonParseError({ message: '', stack: 'JSON.parse' })).toBeTruthy();
    expect(dataHelper.isJsonParseError({ message: 'JSON Parse', stack: '' })).toBeTruthy();
    expect(dataHelper.isJsonParseError({ message: '', stack: 'JSON Parse' })).toBeTruthy();
  });

  it('getDurationTime', () => {
    const second1 = 1000;
    const minute1 = second1 * 60;
    const hour1 = minute1 * 60;
    const day1 = hour1 * 24;
    const month1 = dayjs.duration(1, 'M').asMilliseconds();
    const year1 = dayjs.duration(1, 'y').asMilliseconds();
    // 正确从毫秒数计算出时长
    expect(dataHelper.getDurationTime(intl, 10)).toBe('< 1s');
    expect(dataHelper.getDurationTime(intl, second1)).toBe('1 s');
    expect(dataHelper.getDurationTime(intl, minute1)).toBe('1 min');
    expect(dataHelper.getDurationTime(intl, minute1 + 1100)).toBe('1 min 1 s');
    expect(dataHelper.getDurationTime(intl, hour1)).toBe('1 hour');
    expect(dataHelper.getDurationTime(intl, hour1 + 1100)).toBe('1 hour 1 s');
    expect(dataHelper.getDurationTime(intl, day1)).toBe('1 day(s)');
    expect(dataHelper.getDurationTime(intl, day1 + minute1 + second1)).toBe('1 day(s) 1 min');
    expect(dataHelper.getDurationTime(intl, month1 + minute1 + second1)).toBe('1 month(s) 1 min');
    expect(dataHelper.getDurationTime(intl, year1 + hour1 + second1)).toBe('1 year(s) 1 hour');
  });
});
