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
