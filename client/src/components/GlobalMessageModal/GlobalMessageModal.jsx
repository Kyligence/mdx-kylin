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
import { Button, Dialog, Input } from 'kyligence-ui-react';

import './index.less';
import { Connect, InjectIntl } from '../../store';
import { strings } from '../../constants';
import { getDefaultState } from './handler';

export default
@Connect({
  namespace: 'modal/GlobalMessageModal',
  defaultState: getDefaultState(),
  mapState: {
    isShow: state => state.isShow,
    callback: state => state.callback,
    message: state => state.message,
    detail: state => state.detail,
    data: state => state.data,
  },
})
@InjectIntl()
class GlobalMessageModal extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    isShow: PropTypes.bool.isRequired,
    message: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.object,
    ]).isRequired,
    detail: PropTypes.string.isRequired,
    data: PropTypes.object.isRequired,
    callback: PropTypes.func.isRequired,
  };

  state = {
    isShowDetail: false,
  };

  constructor(props) {
    super(props);
    this.handleCancel = this.handleCancel.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleHideModal = this.handleHideModal.bind(this);
    this.toggleShowDetail = this.toggleShowDetail.bind(this);
  }

  componentDidMount() {
    const { boundModalActions } = this.props;
    boundModalActions.registerModal('GlobalMessageModal', getDefaultState());
  }

  toggleShowDetail() {
    const { isShowDetail } = this.state;
    this.setState({ isShowDetail: !isShowDetail });
  }

  handleHideModal() {
    const { boundModalActions } = this.props;
    this.setState({ isShowDetail: false });
    boundModalActions.hideModal('GlobalMessageModal');
  }

  handleCancel() {
    const { callback } = this.props;
    this.handleHideModal();
    callback({ isSubmit: false });
  }

  handleSubmit() {
    const { callback } = this.props;
    this.handleHideModal();
    callback({ isSubmit: true });
  }

  render() {
    const { isShow, message, detail, intl, data } = this.props;
    const { isShowDetail } = this.state;

    const isStringMessage = typeof message === 'string';
    const formatedMessage = !isStringMessage
      ? intl.formatMessage(message, data)
      : message;

    return (
      <Dialog
        className="global-message-modal mdx-it-global-message-modal"
        visible={isShow}
        closeOnClickModal={false}
        title={intl.formatMessage(strings.NOTIFICATION)}
        onCancel={this.handleCancel}
      >
        <Dialog.Body>
          <div className="dialog-icon">
            <i className="error icon-superset-error_01" />
          </div>
          <div className="dialog-message">
            <span>{formatedMessage}</span>
          </div>
        </Dialog.Body>
        <Dialog.Footer className="dialog-footer">
          <Button className="mdx-it-modal-confirm" type="primary" onClick={this.handleSubmit}>{intl.formatMessage(strings.OK)}</Button>
          {detail && (
            <Button onClick={this.toggleShowDetail}>
              {intl.formatMessage(strings.DETAIL)}
            </Button>
          )}
        </Dialog.Footer>
        {isShowDetail && (
          <div className="dialog-detail">
            <Input size="small" type="textarea" readOnly rows={10} value={detail} />
            {/* <Button size="small" className="copy">{strings.COPY}</Button> */}
          </div>
        )}
      </Dialog>
    );
  }
}
