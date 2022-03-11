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
import ReactDOM from 'react-dom';
import { createSelector } from 'reselect';

import { strings } from '../../constants';
import { dataHelper } from '../../utils';

/* eslint-disable react/jsx-filename-extension, react/no-danger */
function renderReversedTooltip(seriesList = []) {
  const tooltip = document.createElement('div');
  const seriesNameStyle = { fontSize: '12px', color: '#6e7079', marginLeft: '2px' };
  const seriesValueStyle = { float: 'right', marginLeft: '20px', fontSize: '14px', color: '#464646', fontWeight: 900 };
  // 对序列进行倒序
  const reversedSeries = [...seriesList].reverse();
  // 渲染排序后的tooltip
  ReactDOM.render((
    <Fragment>
      {dayjs(+seriesList[0].axisValue).format('YYYY-MM-DD')}
      {reversedSeries.map(series => (
        <div key={series.seriesName}>
          <span dangerouslySetInnerHTML={{ __html: series.marker }} />
          <span style={seriesNameStyle}>{series.seriesName}</span>
          <span style={seriesValueStyle}>{series.value}</span>
        </div>
      ))}
    </Fragment>
  ), tooltip);

  return tooltip;
}

const colorMap = {
  '0-3000': '#1C95E7',
  '3000-10000': '#53B5F7',
  '10000-30000': '#96D3FC',
  '30000-60000': '#BFE5FF',
  '60000-null': '#E8F5FF',
};

function getChartOption({ axis, data, intl }) {
  return {
    tooltip: {
      trigger: 'axis',
      appendToBody: true,
      formatter: renderReversedTooltip,
      axisPointer: {
        type: 'shadow',
      },
    },
    legend: {
      show: true,
      left: 0,
    },
    grid: {
      top: '40px',
      left: '6px',
      right: '6px',
      bottom: '2px',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: axis.slice(0, axis.length - 1),
      axisLabel: {
        formatter: value => dayjs(+value).format('MM-DD'),
      },
      axisTick: {
        alignWithLabel: true,
      },
    },
    yAxis: {
      type: 'value',
    },
    series: data.map(queryCost => {
      const minText = dataHelper.getDurationTime(intl, queryCost.min, 1, false);
      const maxText = dataHelper.getDurationTime(intl, queryCost.max, 1, false);
      let seriesName = `${minText}~${maxText}`;

      if (queryCost.max === undefined) {
        const unit = intl.formatMessage(strings.SCALE_SECOND);
        seriesName = intl.formatMessage(strings.BIGGER_THAN_N_UNIT, { n: 60, unit });
      }

      if (queryCost.max === 60000) {
        seriesName = `${minText}~${60}${intl.formatMessage(strings.SCALE_SECOND)}`;
      }

      return {
        name: seriesName,
        type: 'bar',
        stack: 'total',
        label: {
          show: true,
        },
        emphasis: {
          focus: 'series',
        },
        itemStyle: {
          color: colorMap[`${queryCost.min}-${queryCost.max}`],
        },
        labelLayout: {
          hideOverlap: true,
        },
        data: queryCost.data.map(value => ({
          value,
          label: {
            show: !!value,
          },
        })),
      };
    }),
  };
}

export const getQueryCostChart = createSelector(
  state => state.intl,
  state => state.statistics,
  (intl, statistics) => getChartOption({ ...statistics, intl }),
);

export const getAxis = createSelector(
  state => state.startTime,
  state => state.endTime,
  state => state.maxTickCount,
  (startTime, endTime, maxTickCount) => dataHelper.getFitAxis(startTime, endTime, maxTickCount),
);
