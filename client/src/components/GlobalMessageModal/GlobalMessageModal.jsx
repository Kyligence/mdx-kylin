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
