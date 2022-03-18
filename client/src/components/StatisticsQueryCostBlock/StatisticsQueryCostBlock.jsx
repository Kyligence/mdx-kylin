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
import React, { PureComponent } from 'react';
import PropTypes from 'prop-types';
import { Loading } from 'kyligence-ui-react';
import dayjs from 'dayjs';

import './index.less';
import { strings } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import Block from '../Block/Block';
import RetryBlock from '../RetryBlock/RetryBlock';
import { getQueryCostChart, getAxis } from './handler';
import ReactEchart from '../ReactEchart/ReactEchart';
import EmptyEntityBlock from '../EmptyEntityBlock/EmptyEntityBlock';

const DEFAULT_DATE_RANGE = {
  startTime: dayjs().subtract(1, 'day').valueOf(),
  endTime: dayjs().valueOf(),
};

export default
@Connect({
  mapState: {
    currentProject: state => state.system.currentProject,
    datasetList: state => state.data.datasetList,
  },
})
@InjectIntl()
class StatisticsQueryCostBlock extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    boundSystemActions: PropTypes.object.isRequired,
    currentProject: PropTypes.object.isRequired,
    datasetList: PropTypes.object.isRequired,
    dateRange: PropTypes.shape({
      startTime: PropTypes.number,
      endTime: PropTypes.number,
    }),
  };

  static defaultProps = {
    dateRange: DEFAULT_DATE_RANGE,
  };

  state = {
    isLoading: false,
    isError: false,
    statistics: {
      axis: [],
      data: [],
    },
  };

  async componentDidMount() {
    await this.fetchData();
  }

  async componentDidUpdate(prevProps) {
    await this.handleWatchDateRange(prevProps, this.props);
  }

  get chart() {
    const { intl } = this.props;
    const { statistics } = this.state;

    return getQueryCostChart({ statistics, intl });
  }

  get axis() {
    const { dateRange } = this.props;
    return getAxis({ ...dateRange, maxTickLength: 60 });
  }

  getIsEmptyChart = () => {
    const { statistics } = this.state;
    return !statistics.data.some(queryCostRange => (
      queryCostRange.data.some(queryCost => queryCost)
    ));
  };

  fetchData = async () => {
    const { boundSystemActions, currentProject } = this.props;
    const { axis } = this;

    try {
      this.setState({ isLoading: true });

      if (currentProject.name) {
        const statistics = await boundSystemActions.getStatisticQueryCost({ axis });
        this.setState({ statistics });
      }
      this.setState({ isError: false });
    } catch (e) {
      this.setState({ isError: true });
    } finally {
      this.setState({ isLoading: false });
    }
  };

  handleWatchDateRange = async (prevProps, nextProps) => {
    const { dateRange: oldDateRange } = prevProps;
    const { dateRange: newDateRange } = nextProps;

    if (oldDateRange !== newDateRange) {
      await this.fetchData();
    }
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

  renderChart = chart => {
    const { isError } = this.state;
    const { getIsEmptyChart, renderEmptyData } = this;

    const renderContent = () => (!isError
      ? <ReactEchart option={chart} />
      : <RetryBlock onRetry={this.fetchData} />);

    return !getIsEmptyChart()
      ? renderContent()
      : renderEmptyData();
  };

  render() {
    const { intl, datasetList } = this.props;
    const { isLoading } = this.state;
    const { chart } = this;

    return (
      <Block className="statistics-query-cost-block" header={intl.formatMessage(strings.QUERY_TIME)}>
        <Loading loading={isLoading}>
          {datasetList.data.length
            ? this.renderChart(chart)
            : this.renderEmptyDataset()}
        </Loading>
      </Block>
    );
  }
}
