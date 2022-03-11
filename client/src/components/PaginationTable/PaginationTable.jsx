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
