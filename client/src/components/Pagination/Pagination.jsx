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
