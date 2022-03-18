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

import './index.less';
import { Connect, InjectIntl } from '../../store';

export default
@Connect({
  mapState: {
    locale: state => state.system.language.locale,
    isNoPageFooter: state => state.global.isNoPageFooter,
  },
})
@InjectIntl()
class PageFooter extends PureComponent {
  static propTypes = {
    locale: PropTypes.string.isRequired,
    isNoPageFooter: PropTypes.bool.isRequired,
  };

  get linkUrl() {
    const { locale } = this.props;
    switch (locale) {
      case 'en': return 'https://kyligence.io/';
      case 'zh': return 'https://kyligence.io/zh/';
      default: return 'https://kyligence.io/';
    }
  }

  render() {
    const { isNoPageFooter } = this.props;

    return !isNoPageFooter ? (
      <div className="page-footer">
        <span>
          <a href="http://kylin.apache.org" className="link"><i className="fa fa-home" /> Apache Kylin</a> |
          <a href="http://kylin.apache.org/community/" className="link"><i className="fa fa-users" /> Apache Kylin Community</a>
        </span>
      </div>
    ) : null;
  }
}
