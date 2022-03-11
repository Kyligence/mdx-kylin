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
