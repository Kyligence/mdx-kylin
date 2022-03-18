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
import React, { Fragment } from 'react';
import ReactDOM from 'react-dom';
import { createSelector } from 'reselect';
import dayjs from 'dayjs';

import { dataHelper } from '../../utils';

/* eslint-disable react/jsx-filename-extension, react/no-danger */
function renderSortedTooltip(intl, isFormatY2Time, seriesList = []) {
  const tooltip = document.createElement('div');
  const seriesNameStyle = { fontSize: '12px', color: '#6e7079', marginLeft: '2px' };
  const seriesValueStyle = { float: 'right', marginLeft: '20px', fontSize: '14px', color: '#464646', fontWeight: 900 };
  // 对序列进行排序
  const sortedSeries = [...seriesList]
    .sort((paramA, paramB) => (paramA.value < paramB.value ? 1 : -1));
  // 格式化时间
  const formatTime = value => (
    value <= 60000
      ? dataHelper.formatSeconds(intl, value, 2)
      : dataHelper.getDurationTime(intl, value, 1)
  );

  // 渲染排序后的tooltip
  ReactDOM.render((
    <Fragment>
      {dayjs(+seriesList[0].axisValue).format('YYYY-MM-DD')}
      {sortedSeries.map(series => (
        <div key={series.seriesName}>
          <span dangerouslySetInnerHTML={{ __html: series.marker }} />
          <span style={seriesNameStyle}>{series.seriesName}</span>
          <span style={seriesValueStyle}>
            {isFormatY2Time
              ? formatTime(+series.value)
              : series.value}
          </span>
        </div>
      ))}
    </Fragment>
  ), tooltip);

  return tooltip;
}

function getChartOption({ axis, data, intl, isFormatY2Time }) {
  const hasData = data && data.length;
  return {
    tooltip: {
      trigger: 'axis',
      appendToBody: true,
      formatter: (...args) => renderSortedTooltip(intl, isFormatY2Time, ...args),
    },
    grid: {
      top: '6px',
      left: hasData ? '6px' : '20px',
      right: '20px',
      bottom: '2px',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: axis.slice(0, axis.length - 1),
      axisLabel: {
        formatter: value => dayjs(+value).format('MM-DD'),
      },
    },
    yAxis: {
      type: 'value',
      // 如果Y轴是时间，则语义化时间轴
      axisLine: {
        show: true,
      },
      minInterval: !isFormatY2Time ? 1 : undefined,
      ...isFormatY2Time ? {
        axisLabel: {
          formatter: value => {
            const formatTime = time => (
              time <= 60000
                ? dataHelper.formatSeconds(intl, time, 1)
                : dataHelper.getDurationTime(intl, time, false)
            );
            return formatTime(+value);
          },
        },
      } : {},
    },
    series: data
      .map(dataset => ({
        symbol: 'none',
        name: dataset.datasetName,
        type: 'line',
        data: dataset.data,
      })),
  };
}

export const chartTypes = {
  QUERIES: 'queries',
  FAILED_QUERIES: 'failedQueries',
  AVG_QUERY_TIME: 'avgQueryTime',
};

export const getAxis = createSelector(
  state => state.startTime,
  state => state.endTime,
  state => state.maxTickCount,
  (startTime, endTime, maxTickCount) => dataHelper.getFitAxis(startTime, endTime, maxTickCount),
);

export const getLineChart = createSelector(
  state => state.axis,
  state => state.data,
  state => state.intl,
  state => state.isFormatY2Time,
  (axis, data, intl, isFormatY2Time) => (
    getChartOption({ axis, data, intl, isFormatY2Time })
  ),
);

export const getTopNDatasets = createSelector(
  state => state.datasetList,
  state => state.ranking,
  state => state.topN,
  (datasetList, ranking, topN) => {
    // 排名前五的数据集名称
    const topNDatasets = ranking.slice(0, topN);
    // 排名前五剩下的数据集名称
    const otherDatasets = datasetList.data
      .filter(dataset => !topNDatasets.some(topNDataset => topNDataset.name === dataset.dataset))
      .map(dataset => ({ ...dataset, name: dataset.dataset }));

    return {
      topNDatasets,
      otherDatasets,
    };
  },
);
