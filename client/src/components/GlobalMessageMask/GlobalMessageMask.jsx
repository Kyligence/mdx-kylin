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
import View from '../View/View';
import logoImg from '../../assets/img/loading.gif';

export default
@Connect({
  mapState: {
    isShowGlobalMask: state => state.global.isShowGlobalMask,
    globalMaskMessage: state => state.global.globalMaskMessage,
    globalMaskData: state => state.global.globalMaskData,
  },
})
@InjectIntl()
class GlobalMessageMask extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    isShowGlobalMask: PropTypes.bool.isRequired,
    globalMaskMessage: PropTypes.object,
    globalMaskData: PropTypes.object,
  };

  static defaultProps = {
    globalMaskMessage: null,
    globalMaskData: null,
  };

  render() {
    const { isShowGlobalMask, globalMaskMessage, globalMaskData, intl } = this.props;

    return (
      <Fragment>
        <View isShow={isShowGlobalMask} enter={false} duration={500} classNames="fade-out">
          <div className="global-message-mask">
            <div className="content">
              <img className="logo" alt="logo" src={logoImg} />
              <span className="text">
                {globalMaskMessage && intl.formatMessage(globalMaskMessage, globalMaskData)}
              </span>
            </div>
          </div>
        </View>
      </Fragment>
    );
  }
}
