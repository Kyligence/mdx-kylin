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
import { Table } from 'kyligence-ui-react';
import { strings } from '../../constants';
import { dataHelper } from '../../utils';
import { InjectIntl } from '../../store';
import Pagination from '../Pagination/Pagination';

export default
@InjectIntl()
class PaginationTable extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    className: PropTypes.string,
    data: PropTypes.array,
    columns: PropTypes.array,
    filters: PropTypes.array,
  };

  static defaultProps = {
    className: '',
    data: [],
    columns: [],
    filters: [],
  };

  state = {
    pageOffset: 0,
    pageSize: 10,
  };

  constructor(props) {
    super(props);
    this.handlePagination = this.handlePagination.bind(this);
  }

  get tableData() {
    const { data: datas, filters } = this.props;
    const { pageOffset, pageSize } = this.state;
    return dataHelper.getPaginationTable({ datas, pageOffset, pageSize, filters });
  }

  async handlePagination(pageOffset, pageSize) {
    this.setState({ pageOffset, pageSize });
  }

  render() {
    const { pageOffset, pageSize } = this.state;
    const { className, columns, intl } = this.props;

    return (
      <Fragment>
        <Table
          className={className}
          style={{ width: '100%' }}
          emptyText={intl.formatMessage(strings.NO_DATA)}
          columns={columns}
          data={this.tableData.data}
        />
        <Pagination
          totalCount={this.tableData.totalCount}
          pageSize={pageSize}
          pageOffset={pageOffset}
          onPagination={this.handlePagination}
        />
      </Fragment>
    );
  }
}
