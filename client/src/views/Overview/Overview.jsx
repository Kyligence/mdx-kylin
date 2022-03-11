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
import { Layout } from 'kyligence-ui-react';
import PropTypes from 'prop-types';
import dayjs from 'dayjs';

import './index.less';
import { Connect } from '../../store';
import IconDateRangePicker from '../../components/IconDateRangePicker/IconDateRangePicker';
import StatisticsBasicBlock from '../../components/StatisticsBasicBlock/StatisticsBasicBlock';
import StatisticsTrendBlock from '../../components/StatisticsTrendBlock/StatisticsTrendBlock';
import StatisticsQueryCostBlock from '../../components/StatisticsQueryCostBlock/StatisticsQueryCostBlock';
import StatisticsRankingBlock from '../../components/StatisticsRankingBlock/StatisticsRankingBlock';

export default
@Connect({
  mapState: {
    currentProject: state => state.system.currentProject,
  },
})
class Overview extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    currentProject: PropTypes.object.isRequired,
  };

  state = {
    isLoadingRanking: true,
    datasetRanking: [],
    dateRange: {
      startTime: dayjs().subtract(7, 'day').valueOf(),
      endTime: dayjs().valueOf(),
    },
  };

  async componentDidMount() {
    this.fetchAllDatasets();
  }

  fetchAllDatasets = async () => {
    const { boundDatasetActions, currentProject } = this.props;
    const pageOffset = 0;
    const pageSize = 9999999;
    const withLocation = false;

    if (currentProject.name) {
      await boundDatasetActions.getDatasets({ pageOffset, pageSize, withLocation });
    }
  };

  handleBeforeLoadRanking = () => {
    this.setState({ isLoadingRanking: true });
  };

  handleSetDatasetRanking = datasetsRanking => {
    const sortedRanking = datasetsRanking
      .sort((datasetA, datasetB) => (datasetA.data < datasetB.data ? 1 : -1));

    this.setState({
      isLoadingRanking: false,
      datasetRanking: sortedRanking,
    });
  };

  handleChangeDateRange = ([startDate, endDate]) => {
    const { dateRange } = this.state;
    const startTime = startDate.getTime();
    const endTime = endDate.getTime();

    const isRangeChanged =
      dateRange.startTime !== startTime ||
      dateRange.endTime !== endTime;

    if (isRangeChanged) {
      this.setState({ dateRange: { ...dateRange, startTime, endTime } });
    }
  };

  render() {
    const { datasetRanking, isLoadingRanking, dateRange } = this.state;
    const { shortcutTypes } = IconDateRangePicker.WrappedComponent;

    return (
      <div className="overview">
        <Layout.Row>
          <Layout.Col span={24}>
            <IconDateRangePicker
              value={[
                dayjs(dateRange.startTime).toDate(),
                dayjs(dateRange.endTime).toDate(),
              ]}
              shortcuts={[
                shortcutTypes.LAST_24_HOURS,
                shortcutTypes.LAST_7_DAYS,
                shortcutTypes.LAST_30_DAYS,
              ]}
              onChange={this.handleChangeDateRange}
            />
          </Layout.Col>
        </Layout.Row>
        <Layout.Row>
          <StatisticsBasicBlock dateRange={dateRange} />
        </Layout.Row>
        <Layout.Row gutter={20}>
          <Layout.Col md={12} sm={24}>
            <Layout.Row gutter={10}>
              <Layout.Col span={24}>
                <StatisticsTrendBlock
                  dateRange={dateRange}
                  isLoadingRanking={isLoadingRanking}
                  datasetRanking={datasetRanking}
                />
              </Layout.Col>
              <Layout.Col span={24}>
                <StatisticsQueryCostBlock dateRange={dateRange} />
              </Layout.Col>
            </Layout.Row>
          </Layout.Col>
          <Layout.Col md={12} sm={24}>
            <StatisticsRankingBlock
              dateRange={dateRange}
              onBeforeLoadData={this.handleBeforeLoadRanking}
              onDataLoaded={this.handleSetDatasetRanking}
            />
          </Layout.Col>
        </Layout.Row>
      </div>
    );
  }
}
