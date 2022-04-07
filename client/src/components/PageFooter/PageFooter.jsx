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
