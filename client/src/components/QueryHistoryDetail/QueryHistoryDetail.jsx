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
import React, { Fragment, PureComponent } from 'react';
import ReactDOM from 'react-dom';
import PropTypes from 'prop-types';
import { Checkbox, Loading, Table, Tabs, Button, Tag } from 'kyligence-ui-react';
import { createSelector } from 'reselect';
import ClipboardJS from 'clipboard';

import './index.less';
import { configs, strings } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import Pagination from '../Pagination/Pagination';
import CodeEditor from '../CodeEditor/CodeEditor';
import { detailLabelMap, detailValueMap } from './handler';

const { queryTypes, pageCount } = configs;
const QUERY_CONTENT = 'QUERY_CONTENT';
const QUERY_INFO = 'QUERY_INFO';

export default
@Connect()
@InjectIntl()
class QueryHistoryDetail extends PureComponent {
  static propTypes = {
    boundQueryHistoryActions: PropTypes.object.isRequired,
    mdxQuery: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
  };

  copyButton = React.createRef();
  queryList = React.createRef();
  copyTrigger = null;

  state = {
    sqlList: [],
    selectedRow: null,
    pageOffset: 0,
    pageSize: pageCount.sqlHistory,
    totalCount: 0,
    isLoading: false,
    filter: {
      isOnlyFailure: false,
    },
  };

  getListData = createSelector(
    state => state.mdxQuery,
    state => state.pageOffset,
    state => state.sqlList,
    state => state.isOnlyFailure,
    (mdxQuery, pageOffset, sqlList, isOnlyFailure) => {
      const isFirstPage = pageOffset === 0;
      if (isFirstPage && !isOnlyFailure) {
        const mdxQueryInList = { ...mdxQuery, id: `MDX-${mdxQuery.id}`, rowType: queryTypes.MDX };
        return [mdxQueryInList, ...sqlList];
      }
      return sqlList;
    },
  );

  constructor(props) {
    super(props);

    const { listData: [firstMdxRow] } = this;
    this.state.selectedRow = firstMdxRow;
  }

  async componentDidMount() {
    await this.fetchData();

    const { selectedRow } = this.state;
    this.queryList.current.setCurrentRow(selectedRow);
  }

  get listData() {
    const { mdxQuery } = this.props;
    const { pageOffset, sqlList, filter: { isOnlyFailure } } = this.state;
    return this.getListData({ mdxQuery, pageOffset, sqlList, isOnlyFailure });
  }

  fetchData = async () => {
    const { boundQueryHistoryActions, mdxQuery } = this.props;
    const { pageOffset, pageSize, filter } = this.state;
    const pagination = { pageOffset, pageSize };

    this.setState({ isLoading: true });

    const newFilter = { ...filter };
    delete newFilter.isOnlyFailure;

    const onlyFailure = filter.isOnlyFailure ? 0 : undefined;
    const response = await boundQueryHistoryActions.getQuerySqlByQueryId({
      ...pagination, ...newFilter, onlyFailure, queryId: mdxQuery.queryId,
    });
    const sqlList = response.list.map(item => ({ ...item, rowType: queryTypes.SQL }));
    this.setState({ isLoading: false, sqlList, totalCount: response.total });

    return response;
  }

  addCopyEventListener = copyButton => {
    if (copyButton) {
      const copyButtonEl = ReactDOM.findDOMNode(copyButton);
      this.copyTrigger = new ClipboardJS(copyButtonEl, { action: 'copy', text: this.handleCopyQueryText });
    }
  }

  handleCopyQueryText = () => {
    const { selectedRow } = this.state;
    return selectedRow.content || selectedRow.sqlText;
  }

  handlePagination = async (pageOffset, pageSize) => {
    this.setState({ pageOffset, pageSize }, this.fetchData);
  }

  handleFilter = (key, value) => {
    const { filter: oldFilter } = this.state;
    const filter = { ...oldFilter, [key]: value };
    this.setState({ filter, pageOffset: 0 }, this.fetchData);
  }

  handleShowFailureQuery = value => {
    this.handleFilter('isOnlyFailure', value);
  }

  handleSelectRow = row => {
    this.setState({ selectedRow: row });
  }

  renderColumns = () => {
    const { intl } = this.props;
    return [{
      prop: 'rowType',
      bodyClassName: 'table-body-query-item',
      render(row) {
        return (
          <div className="query-list-item">
            <span className="query-type">
              {row.rowType === queryTypes.MDX && 'MDX'}
              {row.rowType === queryTypes.SQL && 'SQL'}
            </span>
            {row.status === false && row.rowType === queryTypes.SQL && <Tag className="failure-status" type="danger">{intl.formatMessage(strings.FAILURE)}</Tag>}
            {row.content || row.sqlText}
          </div>
        );
      },
    }];
  }

  render() {
    const { listData, handleSelectRow } = this;
    const { intl } = this.props;
    const { filter, selectedRow, isLoading, totalCount, pageOffset, pageSize } = this.state;
    const { renderColumns } = this;
    const isFirstMdxPage = listData.some(row => row.rowType === queryTypes.MDX);
    const queryTitle = selectedRow.rowType === queryTypes.MDX
      ? intl.formatMessage(strings.MDX_QUERY)
      : intl.formatMessage(strings.SQL_QUERY);

    return (
      <div className={isFirstMdxPage ? 'query-history-detail has-mdx clearfix' : 'query-history-detail clearfix'}>
        <div className="query-list">
          <div className="query-list-header clearfix">
            <span>{intl.formatMessage(strings.QUERY_CONTENT)}</span>
            <Checkbox
              className="mdx-it-only-failure-query filter-failure-query"
              checked={filter.isOnlyFailure}
              onChange={this.handleShowFailureQuery}
            >
              {intl.formatMessage(strings.SHOW_FAILED_SQL_QUERY_ONLY)}
            </Checkbox>
          </div>
          <Loading loading={isLoading}>
            <Table
              border
              highlightCurrentRow
              rowKey="id"
              ref={this.queryList}
              showHeader={false}
              columns={renderColumns()}
              data={listData}
              emptyText={intl.formatMessage(strings.NO_DATA)}
              onCurrentChange={handleSelectRow}
            />
            <Pagination
              small
              layout="prev, pager, next"
              style={{ marginTop: '20px' }}
              totalCount={totalCount}
              pageSize={pageSize}
              pageOffset={pageOffset}
              onPagination={this.handlePagination}
            />
          </Loading>
        </div>
        <div className="query-content">
          <Tabs activeName={QUERY_CONTENT} className="query-content-tab">
            <Tabs.Pane label={queryTitle} name={QUERY_CONTENT}>
              <Fragment key={selectedRow.id}>
                <CodeEditor
                  readOnly
                  wrapEnabled
                  className="mdx-it-query-history-content"
                  mode={selectedRow.rowType.toLowerCase()}
                  width="100%"
                  minLines={15}
                  maxLines={15}
                  value={selectedRow.content || selectedRow.sqlText}
                />
                <Button className="copy-button" ref={this.addCopyEventListener}>
                  {intl.formatMessage(strings.COPY)}
                </Button>
              </Fragment>
            </Tabs.Pane>
            <Tabs.Pane label={intl.formatMessage(strings.DETAIL)} name={QUERY_INFO}>
              <table className="query-detail" key={selectedRow.id}>
                {selectedRow.rowType === queryTypes.MDX && (
                  <tbody>
                    {['queryId', 'executeTime', 'isCached', 'isOtherQueryEngine', 'networkSize', 'isTimeout', 'multiDimDatasetTime', 'transferTime', 'datasetName', 'isGateway'].map(field => (
                      <tr className="query-detail-item" key={field}>
                        <td className="key">
                          {intl.formatMessage(detailLabelMap[field])}
                          {intl.formatMessage(strings.SEMICOLON)}
                        </td>
                        <td className="value">{detailValueMap[field](selectedRow[field], intl)}</td>
                      </tr>
                    ))}
                  </tbody>
                )}
                {selectedRow.rowType === queryTypes.SQL && (
                  <tbody>
                    {['id', 'keQueryId', 'sqlCacheUsed', 'sqlExecutionTime', 'status'].map(field => (
                      <tr className="query-detail-item" key={field}>
                        <td className="key">
                          {intl.formatMessage(detailLabelMap[field])}
                          {intl.formatMessage(strings.SEMICOLON)}
                        </td>
                        <td className="value">{detailValueMap[field](selectedRow[field], intl)}</td>
                      </tr>
                    ))}
                  </tbody>
                )}
              </table>
            </Tabs.Pane>
          </Tabs>
        </div>
      </div>
    );
  }
}
