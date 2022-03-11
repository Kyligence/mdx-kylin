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
