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
import { Link } from 'react-router-dom';
import { Layout, Loading, Tooltip } from 'kyligence-ui-react';
import dayjs from 'dayjs';
import numeral from 'numeral';

import './index.less';
import { pageUrls, strings } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { dataHelper } from '../../utils';
import Block from '../Block/Block';
import RetryBlock from '../RetryBlock/RetryBlock';

const DEFAULT_DATE_RANGE = {
  startTime: dayjs().subtract(1, 'day').valueOf(),
  endTime: dayjs().valueOf(),
};

export default
@Connect({
  mapState: {
    currentProject: state => state.system.currentProject,
  },
})
@InjectIntl()
class StatisticsBasicBlock extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    boundSystemActions: PropTypes.object.isRequired,
    currentProject: PropTypes.object.isRequired,
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
      queryUsersCount: 0,
      mdxQueryTotalCount: 0,
      mdxQueryFailedCount: 0,
      queryAvgCount: 0,
      queryAvgTime: 0,
    },
  };

  async componentDidMount() {
    await this.fetchData();
  }

  async componentDidUpdate(prevProps) {
    await this.handleWatchDateRange(prevProps, this.props);
  }

  get statistics() {
    const { intl } = this.props;
    const { statistics } = this.state;

    return [
      {
        key: intl.formatMessage(strings.MDX_QUERIES),
        label: intl.formatMessage(strings.MDX_QUERIES),
        icon: 'icon-superset-query',
        value: numeral(statistics.mdxQueryTotalCount).format('0,0'),
      },
      {
        key: intl.formatMessage(strings.AVG_QUERIES_PER_USER),
        label: intl.formatMessage(strings.AVG_QUERIES_PER_USER),
        icon: 'icon-superset-avg_user',
        value: numeral(statistics.queryAvgCount).format('0,0'),
      },
      {
        key: intl.formatMessage(strings.MDX_FAILED_QUERIES),
        label: intl.formatMessage(strings.MDX_FAILED_QUERIES),
        icon: 'icon-superset-query_failed',
        value: numeral(statistics.mdxQueryFailedCount).format('0,0'),
      },
      {
        key: intl.formatMessage(strings.AVG_QUERY_TIME),
        label: intl.formatMessage(strings.AVG_QUERY_TIME),
        icon: 'icon-superset-avg_time',
        value: +statistics.queryAvgTime <= 60000
          ? dataHelper.formatSeconds(intl, +statistics.queryAvgTime)
          : dataHelper.getDurationTime(intl, +statistics.queryAvgTime, 1),
      },
      {
        key: intl.formatMessage(strings.ACTIVE_USERS),
        label: intl.formatMessage(strings.ACTIVE_USERS),
        icon: 'icon-superset-type_group',
        value: numeral(statistics.queryUsersCount).format('0,0'),
      },
    ];
  }

  fetchData = async () => {
    const { boundSystemActions, dateRange, currentProject } = this.props;

    try {
      this.setState({ isLoading: true });

      if (currentProject.name) {
        const statistics = await boundSystemActions.getStatisticBasic(dateRange);
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

  renderHeader = () => {
    const { intl } = this.props;
    return (
      <Fragment>
        <span>{intl.formatMessage(strings.QUERY_STATISTICS)}</span>
        <Tooltip placement="top" content={intl.formatMessage(strings.VIEW_QUERY_HISTORY)}>
          <Link className="link-query-history" to={pageUrls.queryHistory}>
            <i className="icon-superset-circle_right" />
          </Link>
        </Tooltip>
      </Fragment>
    );
  };

  render() {
    const { isLoading, isError } = this.state;
    const { statistics } = this;

    return (
      <Block className="statistics-basic-block" header={this.renderHeader()}>
        <Loading loading={isLoading}>
          {!isError ? (
            <Layout.Row gutter={16}>
              {statistics.map(statistic => (
                <Layout.Col sm="24" md="4" key={statistic.key}>
                  <div className="basic-info-item">
                    <div className="basic-info-icon-wrapper">
                      <div className="basic-info-icon">
                        <i className={statistic.icon} />
                      </div>
                    </div>
                    <div className="basic-info-content">
                      <div className="basic-info-label">{statistic.label}</div>
                      <div className="basic-info-value">{statistic.value}</div>
                    </div>
                  </div>
                </Layout.Col>
              ))}
            </Layout.Row>
          ) : <RetryBlock onRetry={this.fetchData} />}
        </Loading>
      </Block>
    );
  }
}
