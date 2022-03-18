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
import { createSelector } from 'reselect';

function getChartOption({ axis = [], data = [] }) {
  return {
    dataset: [
      {
        dimensions: ['datasetName', 'usage'],
        source: axis.map((dataset, idx) => ([dataset, data[idx]])),
      },
      {
        transform: {
          type: 'sort',
          config: { dimension: 'usage', order: 'asc' },
        },
      },
    ],
    tooltip: {
      trigger: 'axis',
      appendToBody: true,
      axisPointer: {
        type: 'shadow',
      },
    },
    xAxis: {},
    yAxis: {
      type: 'category',
      axisTick: {
        alignWithLabel: true,
      },
    },
    grid: {
      top: '2px',
      left: '6px',
      right: '30px',
      bottom: '2px',
      containLabel: true,
    },
    series: [
      {
        datasetIndex: 1,
        type: 'bar',
        showBackground: true,
        encode: { x: 'usage', y: 'datasetName' },
        backgroundStyle: {
          color: 'rgba(180, 180, 180, 0.2)',
        },
        itemStyle: {
          color: '#0988DE',
        },
      },
    ],
  };
}

export const getRankingChart = createSelector(
  state => state.intl,
  state => state.ranking,
  (intl, ranking) => {
    const axis = ranking.map(dataset => dataset.name);
    const data = ranking.map(dataset => dataset.data);
    return getChartOption({ axis, data, intl });
  },
);
