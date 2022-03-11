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
import { Dialog } from 'kyligence-ui-react';

import './index.less';
import { strings } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { getDefaultState } from './handler';
import shortLogoImageUrl from '../../assets/img/kylinmdx-logo-for-about-slogan.png';

// 为何react-intl解析出来是字符串？
// const contentParams = {
//   div: (...chunks) => (
//     <div className="about-context state-context" style={{ marginBottom: '30px' }}>
//       {chunks}
//     </div>
//   ),
//   span: (...chunks) => <span>{chunks}</span>,
//   a: (...chunks) => <a href="mailto:info@Kyligence.io">{chunks}</a>,
//   br: (...chunks) => <br />,
// };

export default
@Connect({
  namespace: 'modal/AboutModal',
  defaultState: getDefaultState(),
  mapState: {
    isShow: state => state.isShow,
    callback: state => state.callback,
  },
}, {
  mapState: {
    version: state => state.system.license.version,
    liveDate: state => state.system.license.liveDate,
    licenseType: state => state.system.license.licenseType,
    commitId: state => state.system.license.commitId,
  },
})
@InjectIntl()
class AboutModal extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    isShow: PropTypes.bool.isRequired,
    version: PropTypes.string.isRequired,
    liveDate: PropTypes.string.isRequired,
    licenseType: PropTypes.string.isRequired,
    commitId: PropTypes.string.isRequired,
    callback: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);
    this.handleHideModal = this.handleHideModal.bind(this);
  }

  /* eslint-disable camelcase */
  UNSAFE_componentWillMount() {
    const { boundModalActions } = this.props;
    boundModalActions.registerModal('AboutModal', getDefaultState());
  }

  handleHideModal() {
    const { boundModalActions, callback } = this.props;
    boundModalActions.hideModal('AboutModal');
    callback({ isSubmit: true });
  }

  /* eslint-disable react/no-danger */
  render() {
    const { isShow, commitId, version, licenseType, liveDate, intl } = this.props;
    const titleMessage = intl.formatMessage(strings.ABOUT_KYLIN_INSIGHT);
    const [startDate, endDate] = liveDate.split(',');
    const content = licenseType === 'EVALUATION'
      ? intl.formatMessage(strings.EVALUATION_CONTENT)
      : intl.formatMessage(strings.PURCHASED_CONTENT);

    return (
      <Dialog
        className="about-modal"
        closeOnPressEscape={false}
        closeOnClickModal={false}
        visible={isShow}
        title={titleMessage}
        onCancel={this.handleHideModal}
      >
        <Dialog.Body>
          <div style={{ margin: '10px 0px 30px 0px' }}>
            <img className="about-logo" src={shortLogoImageUrl} alt="logo" />
          </div>
          <div style={{ marginBottom: '30px' }}>
            <div style={{ marginBottom: '5px' }}>
              <span className="font-medium">
                {intl.formatMessage(strings.VERSION_C)}
              </span>
              <span>
                <span>MDX for Kylin</span>
                &nbsp;
                <span>{strings[licenseType] ? intl.formatMessage(strings[licenseType]) : 'N/A'}</span>
                &nbsp;
                <span>{version}</span>
              </span>
            </div>
            <div style={{ marginBottom: '5px' }}>
              <span className="font-medium">
                {intl.formatMessage(strings.VALID_PERIOD_C)}
              </span>
              <span>
                {`${startDate}, ${endDate}`}
              </span>
            </div>
          </div>
          <div style={{ marginBottom: '10px' }}>
            <span className="font-medium">
              {intl.formatMessage(strings.SERVICE_STATEMENT)}
            </span>
          </div>
          <div
            className="about-context state-context"
            style={{ marginBottom: '30px' }}
            dangerouslySetInnerHTML={{ __html: content }}
          />
          {/* {licenseType === 'EVALUATION'
            ? intl.formatMessage(strings.EVALUATION_CONTENT, contentParams)
            : intl.formatMessage(strings.PURCHASED_CONTENT, contentParams)} */}
          <div style={{ marginBottom: '5px' }}>
            <span className="font-medium">
              {intl.formatMessage(strings.SERVICE_END_TIME_C)}
            </span>
            <span>{endDate}</span>
          </div>
          <div className="about-context" style={{ marginBottom: '30px' }}>
            <span className="font-medium">
              {intl.formatMessage(strings.KYLIN_MDX_COMMIT_C)}
            </span>
            <span>{commitId}</span>
          </div>
          <div className="copyright-content">
            <span>
              {intl.formatMessage(strings.COPYRIGHT_CONTENT, { year: new Date().getFullYear() })}
            </span>
          </div>
        </Dialog.Body>
      </Dialog>
    );
  }
}
