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
