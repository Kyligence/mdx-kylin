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
import React, { Fragment } from 'react';
import dayjs from 'dayjs';
import isInteger from 'lodash/isInteger';
import template from 'lodash/template';

import { strings } from '../constants';
import * as calcHelper from './calcHelper';
import ActionButton from '../components/ActionButton/ActionButton';

export function isEmpty(value) {
  return value === undefined || value === null || value === '';
}

export function getDateString(timestamp) {
  if (!isInteger(parseInt(timestamp, 10))) {
    return null;
  }
  const date = dayjs(parseInt(timestamp, 10));
  const dateString = date.format('YYYY-MM-DD HH:mm:ss');
  const timeZone = date.format('ZZ');
  const prefix = timeZone.includes('+') ? '+' : '-';
  const zoneString = timeZone.replace(/^\+[0]*|^-[0]*|[0]*$/g, '');
  return `${dateString} GMT${prefix}${zoneString}`;
}

/* eslint-disable react/jsx-filename-extension */
export function getActionInColumns({ intl }, tableActions = []) {
  const width = calcHelper.calculateTableActionWidth(tableActions);
  const label = intl.formatMessage(strings.ACTIONS);
  const render = row => <ActionButton data={row} actions={tableActions} />;
  const actionColumn = { label, width, render };
  return [actionColumn];
}

export function getExcludeTypeValue(excludeTypes = [], data) {
  const newData = {};

  for (const key of Object.keys(data)) {
    if (!excludeTypes.includes(typeof data[key])) {
      newData[key] = data[key];
    }
  }

  return newData;
}

/* eslint-disable react/jsx-filename-extension */
export function renderStringWithBr(string) {
  const stringArray = string.split('\n');
  const hasBr = stringArray.length > 1;
  const result = hasBr ? [] : string;
  if (hasBr) {
    stringArray.forEach((stringItem, index) => {
      result.push(stringItem);
      if (index < stringArray.length - 1) {
        result.push(<br key={result.length} />);
      }
    });
  }
  return <Fragment>{result}</Fragment>;
}

export function translate(intl, input, params) {
  const { formatMessage } = intl;
  const isDescriptor = typeof input !== 'string';

  let string = '';
  if (isDescriptor) {
    string = formatMessage(input, params);
  } else if (strings[input]) {
    string = formatMessage(strings[input], params);
  } else {
    string = input;
  }

  return params ? template(string)(params) : string;
}

export function getPaginationTable(options = {}) {
  const { datas = [], pageOffset = 0, pageSize = 10, filters = [] } = options;
  const columnsMap = {};

  const filteredDatas = datas.filter(data => {
    let isMatchedData = false;

    if (filters.length === 0) {
      isMatchedData = true;
    }

    for (const filter of filters) {
      let isFilterMatch = true;
      for (const [key, filterString] of Object.entries(filter)) {
        const dataValue = String(data[key]).toLowerCase();
        const filterValue = String(filterString).toLowerCase();

        if (!dataValue.includes(filterValue)) {
          isFilterMatch = false;
          break;
        }
      }
      isMatchedData = isMatchedData || isFilterMatch;
    }

    if (isMatchedData) {
      for (const column of Object.keys(data)) {
        if (!(column in columnsMap)) {
          const prop = column;

          const labelKey = column.toUpperCase();
          const defaultLabel = `${column[0].toUpperCase()}${column.substr(1, column.length - 1)}`;
          const label = strings[labelKey] ? strings[labelKey] : defaultLabel;

          columnsMap[column] = { prop, label };
        }
      }
    }

    return isMatchedData;
  });

  const data = filteredDatas.slice(pageOffset * pageSize, pageOffset * pageSize + pageSize);
  const totalCount = filteredDatas.length;
  const columns = Object.values(columnsMap);

  if (!data.length && pageOffset > 0) {
    return getPaginationTable({ ...options, pageOffset: pageOffset - 1 });
  }

  return { data, pageOffset, pageSize, totalCount, columns };
}

export function findLastIndex(array = [], matcher = () => {}) {
  let lastIndex = -1;
  for (let i = array.length - 1; i >= 0; i -= 1) {
    if (matcher(array[i])) {
      lastIndex = i;
      break;
    }
  }
  return lastIndex;
}

export function isJsonParseError(error) {
  const { message = '', stack = '' } = error;
  // e.message => firefox
  return message.includes('JSON.parse') ||
    // e.stack => chrome
    stack.includes('JSON.parse') ||
    // e.message(JSON Parse) => safari
    message.includes('JSON Parse') ||
    stack.includes('JSON Parse');
}

/* eslint-disable max-len, no-param-reassign */
export function getDurationTime(intl, timestamp, length = 2, lessOneSecond = true) {
  if (!Number.isNaN(+timestamp)) {
    timestamp = +timestamp;

    if (timestamp < 1000 && lessOneSecond) {
      return intl.formatMessage(strings.LESS_THAN_ONE_SECOND);
    }

    const result = [];
    const durations = [
      { value: dayjs.duration(timestamp).years(), scale: intl.formatMessage(strings.SCALE_YEAR) },
      { value: dayjs.duration(timestamp).months(), scale: intl.formatMessage(strings.SCALE_MONTH) },
      { value: dayjs.duration(timestamp).days(), scale: intl.formatMessage(strings.SCALE_DAY) },
      { value: dayjs.duration(timestamp).hours(), scale: intl.formatMessage(strings.SCALE_HOUR) },
      { value: dayjs.duration(timestamp).minutes(), scale: intl.formatMessage(strings.SCALE_MINUTE) },
      { value: dayjs.duration(timestamp).seconds(), scale: intl.formatMessage(strings.SCALE_SECOND) },
    ];

    durations.some((duration, index) => {
      if (duration.value || (durations.length - 1 === index && result.length < length)) {
        result.push(duration);
      }
      if (result.length >= length) return true;
      return false;
    });

    return result
      // 过滤掉第一个后面都为0的时间单位
      .filter((item, idx) => item.value || idx === 0)
      .map(item => `${item.value} ${item.scale}`)
      .join(' ');
  }
  return timestamp;
}

export function normalizeSentences(content = '') {
  const sentences = content.split('.');
  return sentences
    .map(sentence => {
      const [firstWord, ...restWords] = sentence.trim();
      return firstWord
        ? [firstWord.toUpperCase(), ...restWords].join('')
        : '';
    })
    .join('. ');
}

// 自适应图表时间中位轴算法
export function getFitDayAxis(startDate = dayjs(), endDate = dayjs(), maxTickLength = 60) {
  if (maxTickLength < 2) {
    throw new Error('function getFitDayAxis params `maxTickLength` should be greater than 1.');
  }

  const isStartOfDay = startDate.isSame(startDate.startOf('day'));
  const isEndOfDay = endDate.isSame(endDate.endOf('day').add(1, 'millisecond'));

  const durationStart = isStartOfDay ? startDate : startDate.endOf('day').add(1, 'millisecond');
  const durationEnd = isEndOfDay ? endDate : endDate.startOf('day');

  let totalAxis = [];
  let finalAxis = [];

  let cursor = durationStart;

  while (durationEnd.valueOf() >= cursor.valueOf()) {
    totalAxis = [...totalAxis, cursor.valueOf()];
    cursor = cursor.add(1, 'd');
  }

  const shouldScale = totalAxis.length > maxTickLength - 2;

  if (shouldScale) {
    finalAxis = totalAxis
      .filter((axis, idx) => (idx + 1) % Math.ceil(totalAxis.length / maxTickLength) === 0)
      .slice(0, maxTickLength - 2);
  } else {
    finalAxis = totalAxis;
    if (isStartOfDay) {
      finalAxis = finalAxis.slice(1, finalAxis.length);
    }
    if (isEndOfDay) {
      finalAxis = finalAxis.slice(0, finalAxis.length - 1);
    }
  }

  return [startDate.valueOf(), ...finalAxis, endDate.valueOf()];
}

export function getFitAxis(startTime = Date.now(), endTime = Date.now(), maxTickLength = 60) {
  const startDate = dayjs(startTime);
  const endDate = dayjs(endTime);
  return getFitDayAxis(startDate, endDate, maxTickLength);
}

export function formatSeconds(intl, value, precision = 2) {
  const scale = intl.formatMessage(strings.SCALE_SECOND);

  let num = dayjs.duration(value).asSeconds();
  if (!Number.isInteger(num)) {
    num = num.toFixed(precision);
  }

  return `${num}${scale}`;
}

export function getHumanizeJoinString(intl, items = [], between = strings.BETWEEN_AND) {
  let humanizeString = '';

  if (items.length === 1) {
    [humanizeString] = items;
  }

  if (items.length > 1) {
    const [lastItem, ...restItemsArray] = items.reverse();
    const restItems = restItemsArray.reverse().join(intl.formatMessage(strings.BETWEEN_SPLIT));
    humanizeString = intl.formatMessage(between, { restItems, lastItem });
  }

  return humanizeString;
}
