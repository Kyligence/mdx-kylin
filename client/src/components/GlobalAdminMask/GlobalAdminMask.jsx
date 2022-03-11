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
import { Connect, InjectIntl } from '../../store';
import { strings } from '../../constants';
import View from '../View/View';
import logoImg from '../../assets/img/loading.gif';

export default
@Connect({
  mapState: {
    isShowEnterAdmin: state => state.global.isShowEnterAdmin,
    isShowLeaveAdmin: state => state.global.isShowLeaveAdmin,
  },
})
@InjectIntl()
class GlobalAdminMask extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    isShowEnterAdmin: PropTypes.bool.isRequired,
    isShowLeaveAdmin: PropTypes.bool.isRequired,
  };

  render() {
    const { isShowEnterAdmin, isShowLeaveAdmin, intl } = this.props;

    return (
      <Fragment>
        <View isShow={isShowEnterAdmin} enter={false} duration={500} classNames="fade-out">
          <div className="global-admin-mask mdx-it-global-admin-mask">
            <div className="content">
              <img className="logo" alt="logo" src={logoImg} />
              <span className="text">
                {intl.formatMessage(strings.ENTER_ADMIN_MODE)}
              </span>
            </div>
          </div>
        </View>
        <View isShow={isShowLeaveAdmin} enter={false} duration={500} classNames="fade-out">
          <div className="global-admin-mask">
            <div className="content">
              <img className="logo" alt="logo" src={logoImg} />
              <span className="text">
                {intl.formatMessage(strings.LEAVE_ADMIN_MODE)}
              </span>
            </div>
          </div>
        </View>
      </Fragment>
    );
  }
}
