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
import { Pagination as ElPagination } from 'kyligence-ui-react';

import './index.less';

export default class Pagination extends PureComponent {
  static propTypes = {
    style: PropTypes.object,
    small: PropTypes.bool,
    layout: PropTypes.string,
    pageSizes: PropTypes.array,
    className: PropTypes.string,
    pageOffset: PropTypes.oneOfType([
      PropTypes.number,
      PropTypes.string,
    ]).isRequired,
    pageSize: PropTypes.oneOfType([
      PropTypes.number,
      PropTypes.string,
    ]).isRequired,
    totalCount: PropTypes.number.isRequired,
    onPagination: PropTypes.func.isRequired,
  };

  static defaultProps = {
    layout: 'total,sizes,prev,pager,next,jumper',
    pageSizes: [10, 20, 30, 50, 100],
    style: {},
    small: false,
    className: '',
  };

  handlePagination({ pageOffset, pageSize }) {
    const { onPagination } = this.props;
    if (onPagination) {
      onPagination(+(pageOffset - 1), +pageSize);
    }
  }

  /* eslint-disable max-len */
  render() {
    const { pageOffset, pageSize, pageSizes, totalCount, layout, style, small, className } = this.props;

    return totalCount ? (
      <div className="pagination" style={style}>
        <ElPagination
          className={className}
          small={small}
          layout={layout}
          total={+totalCount}
          pageSizes={pageSizes}
          pageSize={+pageSize}
          currentPage={+(+pageOffset + 1)}
          onSizeChange={e => this.handlePagination({ pageSize: e, pageOffset: 1 })}
          onCurrentChange={e => this.handlePagination({ pageSize, pageOffset: e })}
        />
      </div>
    ) : null;
  }
}
