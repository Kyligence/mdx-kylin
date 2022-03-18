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
