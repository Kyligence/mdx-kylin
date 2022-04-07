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
import classnames from 'classnames';
import { createSelector } from 'reselect';
import { Tag, Popover, OverflowTooltip } from 'kyligence-ui-react';

import { strings, configs } from '../../constants';
import { dataHelper } from '../../utils';
import TableFilterBox from '../../components/TableFilterBox/TableFilterBox';
import CheckboxIconFilter from '../../components/CheckboxIconFilter/CheckboxIconFilter';
import IconDateRangePicker from '../../components/IconDateRangePicker/IconDateRangePicker';

const { applicationNames } = configs;

export function getDatabaseFieldName(field) {
  switch (field) {
    case 'queryId': return 'query_id';
    case 'username': return 'username';
    case 'startTime': return 'start';
    case 'executeTime': return 'total_execution_time';
    case 'status': return 'success';
    case 'datasetName': return 'dataset_name';
    default: return field;
  }
}

/* eslint-disable react/jsx-filename-extension */
export const getRenderColumns = createSelector(
  state => state.intl,
  state => state.filter,
  state => state.cluster,
  state => state.handleFilter,
  state => state.handleFilterDateRange,
  state => state.renderExpandRow,
  (
    intl,
    filter,
    cluster,
    handleFilter,
    handleFilterDateRange,
    renderExpandRow,
  ) => [
    {
      type: 'expand',
      expandPannel(data) {
        return renderExpandRow(data);
      },
    },
    {
      prop: 'startTime',
      label: intl.formatMessage(strings.START_TIME),
      width: '220px',
      sortable: 'custom',
      labelClassName: 'table-header-start-time',
      bodyClassName: 'table-body-start-time',
      renderHeader: column => {
        const { label, prop } = column;
        const hasFilterStartTime = filter.startTime[0] && filter.startTime[1];
        return (
          <span className="label-start-time">
            {label}
            <IconDateRangePicker
              className={classnames('table-header-filter-time', hasFilterStartTime && 'has-value')}
              isShowValue={false}
              value={hasFilterStartTime ? [
                dayjs(filter.startTime[0]).toDate(),
                dayjs(filter.startTime[1]).toDate(),
              ] : []}
              onChange={value => handleFilterDateRange(prop, value)}
            />
          </span>
        );
      },
      render(row) {
        const { startTime } = row;
        const dateString = dataHelper.getDateString(startTime);
        return (
          <OverflowTooltip content={dateString}>
            <span>{dateString}</span>
          </OverflowTooltip>
        );
      },
    },
    {
      prop: 'executeTime',
      label: intl.formatMessage(strings.EXECUTE_TIME),
      width: '140px',
      sortable: 'custom',
      labelClassName: 'table-header-execute-time',
      bodyClassName: 'table-body-execute-time',
      render(row) {
        const { executeTime } = row;
        return <span>{dataHelper.getDurationTime(intl, executeTime)}</span>;
      },
    },
    {
      prop: 'content',
      label: intl.formatMessage(strings.QUERY_CONTENT),
      labelClassName: 'table-header-content',
      bodyClassName: 'table-body-content',
      render(row) {
        const { content } = row;
        return (
          <Popover
            appendToBody
            popperClass="mdx-query-content"
            placement="bottom"
            trigger="hover"
            popperProps={{
              positionFixed: true,
              modifiers: {
                preventOverflow: { enabled: false },
              },
            }}
            content={(
              <Fragment>
                <div className="query-preview">{content}</div>
                <div className="query-preview-tip">{intl.formatMessage(strings.EXPAND_VIEW_MORE_INFO)}</div>
              </Fragment>
            )}
          >
            <div className="limited-column">
              <span>{content}</span>
            </div>
          </Popover>
        );
      },
    },
    {
      prop: 'datasetName',
      sortable: 'custom',
      label: intl.formatMessage(strings.DATASET_NAME),
      labelClassName: 'table-header-dataset-name',
      bodyClassName: 'table-body-dataset-name',
      render(row) {
        const { datasetName } = row;
        return (
          <OverflowTooltip content={datasetName}>
            <span>{datasetName}</span>
          </OverflowTooltip>
        );
      },
    },
    {
      prop: 'status',
      label: intl.formatMessage(strings.QUERY_STATUS),
      width: '100px',
      labelClassName: 'table-header-status',
      bodyClassName: 'table-body-status',
      filters: [
        { label: intl.formatMessage(strings.SUCCESS), value: true },
        { label: intl.formatMessage(strings.FAILURE), value: false },
      ],
      renderHeader(column) {
        const { label, prop, filters } = column;
        return (
          <TableFilterBox
            single
            value={filter[prop]}
            headerText={label}
            filters={filters}
            onFilter={value => handleFilter(prop, value)}
          />
        );
      },
      render(row) {
        const { status } = row;
        const type = status === true ? 'success' : 'danger';
        const text = status === true ? 'SUCCESS' : 'FAILURE';
        return <Tag type={type}>{intl.formatMessage(strings[text])}</Tag>;
      },
    },
    {
      prop: 'application',
      label: intl.formatMessage(strings.APPLICATION),
      width: '140px',
      labelClassName: 'table-header-application',
      bodyClassName: 'table-body-application',
      filters: Object.entries(applicationNames).map(([value, label]) => ({
        label: dataHelper.translate(intl, label),
        value,
      })),
      renderHeader(column) {
        const { label, prop, filters } = column;
        return (
          <TableFilterBox
            value={filter[prop]}
            headerText={label}
            filters={filters}
            onFilter={value => handleFilter(prop, value)}
          />
        );
      },
      render: row => (applicationNames[row.application]
        ? dataHelper.translate(intl, applicationNames[row.application])
        : row.application),
    },
    {
      prop: 'node',
      label: intl.formatMessage(strings.QUERY_NODE),
      labelClassName: 'table-header-node',
      bodyClassName: 'table-body-node',
      filters: cluster.map(node => ({
        label: node,
        value: node,
      })),
      renderHeader(column) {
        const { label, filters } = column;
        const hasValue = filter.cluster.length;
        return (
          <CheckboxIconFilter
            multiple
            className={classnames('filter-node', hasValue && 'has-value')}
            label={label}
            value={filter.cluster}
            options={filters}
            searchPlaceholder={
              intl.formatMessage(strings.SEARCH_BY) +
              intl.formatMessage(strings.QUERY_NODE).toLowerCase()
            }
            onChange={value => handleFilter('cluster', value)}
            popperProps={{
              positionFixed: true,
              modifiers: {
                flip: { enabled: false },
                hide: { enabled: false },
                preventOverflow: { enabled: false },
              },
            }}
          />
        );
      },
      render(row) {
        const { node } = row;
        return (
          <OverflowTooltip content={node}>
            <span>{node}</span>
          </OverflowTooltip>
        );
      },
    },
    {
      prop: 'username',
      label: intl.formatMessage(strings.USER),
      labelClassName: 'table-header-username',
      bodyClassName: 'table-body-username',
    },
  ],
);

export const filterLabelMap = {
  queryId: strings.QUERY_ID,
  username: strings.USER,
  status: strings.QUERY_STATUS,
  application: strings.APPLICATION,
  node: strings.QUERY_NODE,
  startTime: strings.START_TIME,
};

export const filterValueRender = {
  queryId: value => value,
  username: value => value,
  status: (value, intl) => {
    if (value !== undefined) {
      return value
        ? intl.formatMessage(strings.SUCCESS)
        : intl.formatMessage(strings.FAILURE);
    }
    return undefined;
  },
  application: (value, intl) => dataHelper.translate(intl, applicationNames[value]) || value,
  node: value => value,
  startTime: ([startTimeFrom, startTimeTo], intl) => (
    `${dataHelper.getDateString(startTimeFrom)} ${intl.formatMessage(strings.TO)} ${dataHelper.getDateString(startTimeTo)}`
  ),
};
