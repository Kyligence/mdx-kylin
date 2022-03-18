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
import { Loading, Table, Tag } from 'kyligence-ui-react';
import debounce from 'lodash/debounce';

import './index.less';
import { configs, strings } from '../../constants';
import { browserHelper, dataHelper } from '../../utils';
import { Connect, InjectIntl } from '../../store';
import Pagination from '../../components/Pagination/Pagination';
import QueryHistoryDetail from '../../components/QueryHistoryDetail/QueryHistoryDetail';
import SuggestionFilterInput from '../../components/SuggestionFilterInput/SuggestionFilterInput';
import { getRenderColumns, filterLabelMap, filterValueRender, getDatabaseFieldName } from './handler';

const { isEmpty } = dataHelper;
const { requestTypes } = configs;

export default
@Connect({
  mapState: {
    currentProject: state => state.system.currentProject,
    queryHistory: state => state.data.queryHistory,
    language: state => state.system.language.locale,
  },
})
@InjectIntl()
class QueryHistory extends PureComponent {
  static propTypes = {
    boundQueryHistoryActions: PropTypes.object.isRequired,
    currentProject: PropTypes.object.isRequired,
    queryHistory: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    language: PropTypes.string.isRequired,
  };

  request = null;

  state = {
    pageOffset: browserHelper.getQueryFromLocation('pageOffset') || 0,
    pageSize: browserHelper.getQueryFromLocation('pageSize') || configs.pageCount.datasetList,
    orderBy: browserHelper.getQueryFromLocation('orderBy'),
    direction: browserHelper.getQueryFromLocation('direction'),
    cluster: [],
    filter: {
      queryId: undefined,
      username: undefined,
      status: undefined,
      startTime: [],
      application: [],
      cluster: [],
    },
  };

  constructor(props) {
    super(props);
    this.fetchDataWithDelay = debounce(this.fetchData, 1000);
  }

  componentDidMount() {
    Promise.all([
      this.fetchCluster(),
      this.fetchData(),
    ]);
  }

  get renderColumns() {
    const { renderExpandRow, handleFilter, handleFilterDateRange } = this;
    const { intl } = this.props;
    const { filter, cluster } = this.state;
    return getRenderColumns({
      intl, filter, cluster, handleFilter, renderExpandRow, handleFilterDateRange,
    });
  }

  get filterTags() {
    const { intl } = this.props;
    const { filter } = this.state;
    const filterTags = [];

    for (const [key, filterValue] of Object.entries(filter)) {
      let label = null;
      let renderFunc = () => null;

      switch (key) {
        case 'cluster':
          label = intl.formatMessage(filterLabelMap.node);
          renderFunc = value => filterValueRender.node(value, intl);
          break;
        default:
          label = intl.formatMessage(filterLabelMap[key]);
          renderFunc = value => filterValueRender[key](value, intl);
          break;
      }

      if (key === 'startTime') {
        if (filterValue[0] && filterValue[1]) {
          filterTags.push({ key, label, value: filterValue, render: renderFunc(filterValue) });
        }
      } else if (filterValue instanceof Array) {
        for (const valueItem of filterValue) {
          filterTags.push({ key, label, value: valueItem, render: renderFunc(valueItem) });
        }
      } else {
        filterTags.push({ key, label, value: filterValue, render: renderFunc(filterValue) });
      }
    }

    return filterTags.filter(tag => !isEmpty(tag.value));
  }

  setQueryHistoryRowClass = () => 'query-history-row';

  setRequesting = request => { this.request = request; };

  fetchData = async () => {
    const { boundQueryHistoryActions, currentProject } = this.props;
    const { pageOffset, pageSize, orderBy, direction, filter: oldFilter } = this.state;
    const pagination = { pageOffset, pageSize };
    const order = { orderBy: getDatabaseFieldName(orderBy), direction };

    const filter = {};

    for (const [key, value] of Object.entries(oldFilter)) {
      if (key === 'startTime') {
        [filter.startTimeFrom, filter.startTimeTo] = oldFilter.startTime;
      } else if (getDatabaseFieldName(key)) {
        filter[getDatabaseFieldName(key)] = value;
      }
    }

    if (currentProject.name) {
      if (this.request) {
        this.request.cancel(requestTypes.CANCEL);
      }

      try {
        await boundQueryHistoryActions.getQueryHistory(
          { ...pagination, ...order },
          { ...filter },
          this.setRequesting,
        );
        this.setRequesting(null);
      } catch {
        this.setRequesting(null);
      }
    }
    return null;
  }

  fetchCluster = async () => {
    const { boundQueryHistoryActions } = this.props;
    const cluster = await boundQueryHistoryActions.getQueryHistoryCluster();
    this.setState({ cluster });
  };

  handleSort = async ({ prop: orderBy, order: direction }) => {
    this.setState({ orderBy, direction }, this.fetchData);
  }

  handlePagination = async (pageOffset, pageSize) => {
    this.setState({ pageOffset, pageSize }, this.fetchData);
  }

  handleFilter = (key, value) => {
    const { filter: oldFilter } = this.state;
    const filter = { ...oldFilter, [key]: value };
    this.setState({ filter, pageOffset: 0 }, () => this.fetchDataWithDelay());
  }

  handleFilterDateRange = (key, [start, end]) => {
    const { filter: oldFilter } = this.state;
    const value = start && end ? [start.getTime(), end.getTime()] : [];
    const filter = { ...oldFilter, [key]: value };
    this.setState({ filter, pageOffset: 0 }, () => this.fetchDataWithDelay());
  }

  handleAddFilter = (key, value) => {
    this.handleFilter(key, value);
  }

  handleRemoveFilter = (key, value) => {
    const { filter: oldFilter } = this.state;
    if (['queryId', 'username', 'status'].includes(key)) {
      this.handleFilter(key, undefined);
    } else if (['startTime'].includes(key)) {
      this.handleFilter(key, []);
    } else {
      this.handleFilter(key, oldFilter[key].filter(item => item !== value));
    }
  }

  renderExpandRow = row => <QueryHistoryDetail mdxQuery={row} />

  render() {
    const { intl, queryHistory, currentProject, language } = this.props;
    const { renderColumns, filterTags } = this;

    const tableData = currentProject.name ? queryHistory.data : [];
    const totalCount = currentProject.name ? queryHistory.totalCount : 0;

    return (
      <div className="query-history">
        <div className="layout-actions clearfix">
          <div className="pull-right">
            <SuggestionFilterInput
              key={language}
              className="mdx-it-search-query-history search-query-history"
              placeholder={intl.formatMessage(strings.SEARCH_QUERY_ID_OR_USERNAME)}
              onSelect={this.handleAddFilter}
              options={[
                { label: intl.formatMessage(strings.QUERY_ID), value: 'queryId' },
                { label: intl.formatMessage(strings.USER), value: 'username' },
              ]}
            />
          </div>
        </div>
        {filterTags.length ? (
          <div className="layout-filters">
            {filterTags.map(filterTag => (
              <Tag
                closable
                type="primary"
                key={`${filterTag.key}-${filterTag.value}`}
                onClose={() => this.handleRemoveFilter(filterTag.key, filterTag.value)}
              >
                <span>{filterTag.label}{intl.formatMessage(strings.SEMICOLON)}</span>
                <span>{filterTag.render}</span>
              </Tag>
            ))}
          </div>
        ) : null}
        <div className="layout-table">
          <Loading className="mdx-it-query-history-loading" loading={queryHistory.isLoading}>
            <Table
              key={`${queryHistory.orderBy}${queryHistory.direction}`}
              style={{ width: '100%' }}
              emptyText={intl.formatMessage(strings.NO_DATA)}
              columns={renderColumns}
              data={tableData}
              rowClassName={this.setQueryHistoryRowClass}
              defaultSort={{ prop: queryHistory.orderBy, order: queryHistory.direction }}
              onSortChange={this.handleSort}
            />
          </Loading>
          <Pagination
            className="query-history-pagination"
            totalCount={totalCount}
            pageSize={queryHistory.pageSize}
            pageOffset={queryHistory.pageOffset}
            onPagination={this.handlePagination}
          />
        </div>
      </div>
    );
  }
}
