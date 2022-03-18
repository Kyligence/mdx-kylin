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
