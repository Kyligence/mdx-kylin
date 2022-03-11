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

import './index.less';
import { Connect } from '../../store';
import BreadCrumb from '../../components/BreadCrumb/BreadCrumb';
import PageHeader from '../../components/PageHeader/PageHeader';
import PageMenu from '../../components/PageMenu/PageMenu';
import PageFooter from '../../components/PageFooter/PageFooter';

export default
@Connect({
  mapState: {
    isNoBreadCrumb: state => state.global.isNoBreadCrumb,
    isNoPageHeader: state => state.global.isNoPageHeader,
  },
  mapReselect: {
    isLoginMode: reselect => reselect.system.isLoginMode,
  },
})
class Layout extends PureComponent {
  static propTypes = {
    children: PropTypes.node.isRequired,
    isLoginMode: PropTypes.bool.isRequired,
    history: PropTypes.object.isRequired,
  };

  render() {
    const { children, isLoginMode, history } = this.props;

    return (
      <div className="layout">
        {isLoginMode ? (
          <Fragment>
            <PageHeader history={history} isLoginMode={isLoginMode} />
            {children}
            <PageFooter />
          </Fragment>
        ) : (
          <Fragment>
            <div className="layout-left" style={{ maxWidth: '184px' }}>
              <PageMenu history={history} />
            </div>
            <div className="layout-right" style={this.layoutRightStyle}>
              <PageHeader history={history} isLoginMode={isLoginMode} />
              <BreadCrumb history={history} />
              <div className="layout-content">
                {children}
              </div>
            </div>
          </Fragment>
        )}
      </div>
    );
  }
}
