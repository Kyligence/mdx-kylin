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
import React, { PureComponent, Fragment } from 'react';
import PropTypes from 'prop-types';
import { Loading, Tabs } from 'kyligence-ui-react';
import dayjs from 'dayjs';
import numeral from 'numeral';

import './index.less';
import { strings, configs } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import Block from '../Block/Block';
import RetryBlock from '../RetryBlock/RetryBlock';
import ReactEchart from '../ReactEchart/ReactEchart';
import CheckboxIconFilter from '../CheckboxIconFilter/CheckboxIconFilter';
import EmptyEntityBlock from '../EmptyEntityBlock/EmptyEntityBlock';
import {
  getLineChart,
  chartTypes,
  getTopNDatasets,
  getAxis,
} from './handler';

const EMPTY_ARRAY = [];

const DEFAULT_DATE_RANGE = {
  startTime: dayjs().subtract(1, 'day').valueOf(),
  endTime: dayjs().valueOf(),
};

const { requestTypes } = configs;

export default
@Connect({
  mapState: {
    currentProject: state => state.system.currentProject,
    datasetList: state => state.data.datasetList,
  },
})
@InjectIntl()
class StatisticsTrendBlock extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    boundSystemActions: PropTypes.object.isRequired,
    currentProject: PropTypes.object.isRequired,
    datasetList: PropTypes.object.isRequired,
    dateRange: PropTypes.shape({
      startTime: PropTypes.number,
      endTime: PropTypes.number,
    }),
    datasetRanking: PropTypes.arrayOf(PropTypes.shape({
      id: PropTypes.number,
      name: PropTypes.string,
      data: PropTypes.number,
    })),
    isLoadingRanking: PropTypes.bool,
  };

  static defaultProps = {
    datasetRanking: EMPTY_ARRAY,
    isLoadingRanking: false,
    dateRange: DEFAULT_DATE_RANGE,
  };

  request = null;

  state = {
    isLoading: false,
    isError: false,
    selectTab: chartTypes.QUERIES,
    filter: {
      datasetNames: [],
    },
    statistics: {
      // data: [{ name: 'dataset_name', data: [100, 50] }]
      datasetUsage: { data: [], value: 0 },
      queryFailed: { data: [], value: 0 },
      queryAvgTime: { data: [], value: 0 },
    },
  };

  async componentDidUpdate(prevProps) {
    // 此处无需调用 handleWatchDateRange，因为当dataRange变化后，datasetRanking必定发生变化
    await this.handleWatchDatasetRanking(prevProps, this.props);
  }

  get charts() {
    const { intl } = this.props;
    const { statistics } = this.state;
    const { axis } = this;
    const semicolon = intl.formatMessage(strings.SEMICOLON);

    return [
      {
        key: chartTypes.QUERIES,
        label: intl.formatMessage(strings.QUERIES_COUNT),
        chart: getLineChart({ ...statistics.datasetUsage, axis, intl }),
        subLabel: intl.formatMessage(strings.TOTAL_QUERIES) + semicolon,
        value: numeral(statistics.datasetUsage.value).format('0,0'),
        data: statistics.datasetUsage.data,
      },
      {
        key: chartTypes.FAILED_QUERIES,
        label: intl.formatMessage(strings.FAILED_QUERIES),
        chart: getLineChart({ ...statistics.queryFailed, axis, intl }),
        subLabel: intl.formatMessage(strings.TOTAL_FAILED_QUERIES) + semicolon,
        value: numeral(statistics.queryFailed.value).format('0,0'),
        data: statistics.queryFailed.data,
      },
      {
        key: chartTypes.AVG_QUERY_TIME,
        label: intl.formatMessage(strings.AVG_QUERY_TIME),
        chart: getLineChart({ ...statistics.queryAvgTime, axis, intl, isFormatY2Time: true }),
        // subLabel: intl.formatMessage(strings.TOTAL_AVG_QUERY_TIME) + semicolon,
        value: statistics.queryAvgTime.value,
        data: statistics.queryAvgTime.data,
      },
    ];
  }

  get axis() {
    const { dateRange } = this.props;
    return getAxis({ ...dateRange, maxTickLength: 60 });
  }

  get topNDatasets() {
    const { datasetRanking: ranking, datasetList } = this.props;
    return getTopNDatasets({ datasetList, ranking, topN: 5 });
  }

  get datasetOptions() {
    const { intl } = this.props;
    const { topNDatasets, otherDatasets } = this.topNDatasets;

    const topNOptions = topNDatasets.map(({ name }) => ({ label: name, value: name }));
    const otherOptions = otherDatasets.map(({ name }) => ({ label: name, value: name }));

    return [
      { label: intl.formatMessage(strings.POPULAR_DATASETS), options: topNOptions },
      { label: intl.formatMessage(strings.DATASET), options: otherOptions },
    ];
  }

  setRequesting = request => { this.request = request; };

  getIsEmptyChart = chart => !chart.data.length;

  handleWatchDatasetRanking = async (prevProps, nextProps) => {
    const { datasetRanking: oldRanking } = prevProps;
    const { datasetRanking: newRanking } = nextProps;

    if (oldRanking !== newRanking) {
      const { topNDatasets } = this.topNDatasets;
      await this.handleFilterDatasets(topNDatasets.map(dataset => dataset.name));
    }
  };

  fetchData = async () => {
    const { boundSystemActions, currentProject } = this.props;
    const { filter } = this.state;
    const { axis } = this;

    if (this.request) {
      this.request.cancel(requestTypes.CANCEL);
    }

    try {
      this.setState({ isLoading: true });

      if (currentProject.name) {
        /* eslint-disable-next-line max-len */
        const statistics = await boundSystemActions.getStatisticTrend({ axis, ...filter }, this.setRequesting);
        this.setState({ statistics });
      }

      this.setState({ isLoading: false, isError: false });
      this.setRequesting(null);
    } catch (e) {
      if (e.message !== requestTypes.CANCEL) {
        this.setState({ isError: true });
        this.setState({ isLoading: false });
        this.setRequesting(null);
      }
    }
  };

  handleChangeTab = tabItem => {
    this.setState({ selectTab: tabItem.props.name });
  };

  handleFilterDatasets = async datasetNames => {
    const { filter } = this.state;
    this.setState({ filter: { ...filter, datasetNames } }, () => this.fetchData());
  };

  renderChart = chart => {
    const { intl } = this.props;
    const { selectTab, filter } = this.state;
    const { datasetOptions, getIsEmptyChart } = this;
    return (
      <Fragment>
        <CheckboxIconFilter
          multiple
          appendToBody
          className="filter-datasets"
          value={filter.datasetNames}
          options={datasetOptions}
          searchPlaceholder={intl.formatMessage(strings.SEARCH_DATASET)}
          onChange={this.handleFilterDatasets}
        />
        {chart.subLabel ? (
          <div className="trend-sub-info">
            <span className="trend-sub-label">{chart.subLabel}</span>
            <span className="trend-sub-value">{chart.value}</span>
          </div>
        ) : null}
        {!getIsEmptyChart(chart) && selectTab === chart.key ? (
          <ReactEchart option={chart.chart} />
        ) : null}
        {getIsEmptyChart(chart)
          ? this.renderEmptyData()
          : null}
      </Fragment>
    );
  };

  renderEmptyDataset = () => {
    const { intl } = this.props;
    return (
      <EmptyEntityBlock
        icon="icon-superset-empty_box"
        content={intl.formatMessage(strings.EMPTY_DATASETS)}
      />
    );
  };

  renderEmptyData = () => {
    const { intl } = this.props;
    return (
      <EmptyEntityBlock content={intl.formatMessage(strings.NO_DATA)} />
    );
  };

  render() {
    const { isLoading, isError } = this.state;
    const { intl, isLoadingRanking, datasetList } = this.props;
    const { charts } = this;

    return (
      <Block className="statistics-trend-block" header={intl.formatMessage(strings.QUERY_TREND)}>
        <Loading loading={isLoading || isLoadingRanking}>
          {!isError ? (
            <Fragment>
              <Tabs activeName={chartTypes.QUERIES} onTabClick={this.handleChangeTab}>
                {charts.map(chart => (
                  <Tabs.Pane label={chart.label} name={chart.key} key={chart.key}>
                    {datasetList.data.length
                      ? this.renderChart(chart)
                      : this.renderEmptyDataset()}
                  </Tabs.Pane>
                ))}
              </Tabs>
            </Fragment>
          ) : <RetryBlock onRetry={this.fetchData} />}
        </Loading>
      </Block>
    );
  }
}
