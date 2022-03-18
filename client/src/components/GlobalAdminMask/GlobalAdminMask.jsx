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
