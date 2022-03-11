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
import React, { PureComponent, Fragment } from 'react';
import PropTypes from 'prop-types';
import { Loading, Select } from 'kyligence-ui-react';
import dayjs from 'dayjs';

import './index.less';
import { strings } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import Block from '../Block/Block';
import RetryBlock from '../RetryBlock/RetryBlock';
import ReactEchart from '../ReactEchart/ReactEchart';
import EmptyEntityBlock from '../EmptyEntityBlock/EmptyEntityBlock';
import { getRankingChart } from './handler';

const EMPTY_FUNC = () => {};

const DEFAULT_DATE_RANGE = {
  startTime: dayjs().subtract(1, 'day').valueOf(),
  endTime: dayjs().valueOf(),
};

export default
@Connect({
  mapState: {
    locale: state => state.system.language.locale,
    currentProject: state => state.system.currentProject,
    datasetList: state => state.data.datasetList,
  },
})
@InjectIntl()
class StatisticsRankingBlock extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    locale: PropTypes.string.isRequired,
    boundSystemActions: PropTypes.object.isRequired,
    currentProject: PropTypes.object.isRequired,
    datasetList: PropTypes.object.isRequired,
    onDataLoaded: PropTypes.func,
    onBeforeLoadData: PropTypes.func,
    dateRange: PropTypes.shape({
      startTime: PropTypes.number,
      endTime: PropTypes.number,
    }),
  };

  static defaultProps = {
    onDataLoaded: EMPTY_FUNC,
    onBeforeLoadData: EMPTY_FUNC,
    dateRange: DEFAULT_DATE_RANGE,
  };

  state = {
    isLoading: false,
    isError: false,
    rankingTopN: [],
    rankingLastN: [],
    params: {
      direction: 'desc',
      count: 10,
    },
  };

  componentDidMount() {
    this.handleRefreshChart();
  }

  async componentDidUpdate(prevProps) {
    await this.handleWatchDateRange(prevProps, this.props);
  }

  get chart() {
    const { intl } = this.props;
    const { ranking } = this;

    return getRankingChart({ ranking, intl });
  }

  get options() {
    const { intl } = this.props;
    return [
      { label: intl.formatMessage(strings.MOST_USED_DATASETS), value: 'desc' },
      { label: intl.formatMessage(strings.LEAST_USED_DATASETS), value: 'asc' },
    ];
  }

  get ranking() {
    const { params, rankingTopN, rankingLastN } = this.state;
    switch (params.direction) {
      case 'asc': return rankingLastN;
      case 'desc':
      default: return rankingTopN;
    }
  }

  getIsEmptyChart = () => {
    const { ranking } = this;
    return !ranking.length;
  };

  fetchData = async () => {
    const { boundSystemActions, dateRange, currentProject } = this.props;
    const { params } = this.state;

    try {
      this.setState({ isLoading: true });

      if (currentProject.name) {
        const ranking = (await boundSystemActions.getStatisticRanking({ ...dateRange, ...params }))
          .map(rankItem => ({ ...rankItem, name: rankItem.datasetName }));

        if (params.direction === 'desc') {
          this.setState({ rankingTopN: ranking });
        }
        if (params.direction === 'asc') {
          this.setState({ rankingLastN: ranking });
        }
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
      this.handleRefreshChart();
    }
  };

  handleDataLoaded = () => {
    const { onDataLoaded } = this.props;
    const { rankingTopN } = this.state;
    onDataLoaded(rankingTopN);
  };

  handleChangeDirection = async direction => {
    const { params } = this.state;
    this.setState({ params: { ...params, direction } }, () => {
      this.handleRefreshChart();
    });
  };

  handleRefreshChart = async () => {
    const { onBeforeLoadData } = this.props;
    const { params } = this.state;

    try {
      if (params.direction === 'desc') {
        onBeforeLoadData();
      }
      await this.fetchData();
    } finally {
      if (params.direction === 'desc') {
        this.handleDataLoaded();
      }
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
    const { intl, locale } = this.props;
    const { isError, params } = this.state;
    const { options, getIsEmptyChart } = this;

    const renderContent = () => (!isError
      ? <ReactEchart option={chart} />
      : <RetryBlock onRetry={this.fetchData} />);

    return (
      <Fragment>
        <div className="statistics-ranking-header">
          <span className="statistics-ranking-title">{intl.formatMessage(strings.RANK_BY_TIMES_OF_USE)}</span>
          <Select key={locale} size="small" className="statistics-ranking-direction" value={params.direction} onChange={this.handleChangeDirection}>
            {options.map(option => (
              <Select.Option key={option.value} label={option.label} value={option.value} />
            ))}
          </Select>
        </div>
        {!getIsEmptyChart()
          ? renderContent()
          : this.renderEmptyData()}
      </Fragment>
    );
  };

  render() {
    const { intl, datasetList } = this.props;
    const { isLoading } = this.state;
    const { chart } = this;

    return (
      <Block className="statistics-ranking-block" header={intl.formatMessage(strings.RANKING)}>
        <Loading className="statistics-ranking-loading" loading={isLoading}>
          {datasetList.data.length
            ? this.renderChart(chart)
            : this.renderEmptyDataset()}
        </Loading>
      </Block>
    );
  }
}
