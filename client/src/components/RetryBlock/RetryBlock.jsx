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
import * as PropTypes from 'prop-types';

import './index.less';
import { InjectIntl } from '../../store';
import { strings } from '../../constants';

const EMPTY_FUNC = () => {};

export default
@InjectIntl()
class RetryBlock extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    onRetry: PropTypes.func,
  };

  static defaultProps = {
    onRetry: EMPTY_FUNC,
  };

  render() {
    const { intl, onRetry } = this.props;
    return (
      <div className="retry-block" onClick={onRetry}>
        <div className="retry">
          <div className="retry-icon">
            <i className="icon-superset-table_refresh" />
          </div>
          <div className="retry-text">
            {intl.formatMessage(strings.UNKONW_ERROR_TO_RETRY_1)}
          </div>
          <div className="retry-text">
            {intl.formatMessage(strings.UNKONW_ERROR_TO_RETRY_2)}
          </div>
        </div>
      </div>
    );
  }
}
