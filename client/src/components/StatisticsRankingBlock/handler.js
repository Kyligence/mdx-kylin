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
