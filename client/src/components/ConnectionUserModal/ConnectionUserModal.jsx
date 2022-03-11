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
import { Dialog, Button, Form, Input, Alert } from 'kyligence-ui-react';

import './index.less';
import { strings, stringsMap, business } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { dataHelper } from '../../utils';
import { getDefaultState, validator, formatPostForm } from './handler';

export default
@Connect({
  namespace: 'modal/ConnectionUserModal',
  defaultState: getDefaultState(),
  mapState: {
    isShow: state => state.isShow,
    callback: state => state.callback,
    form: state => state.form,
    errorMessage: state => state.errorMessage,
    errorData: state => state.errorData,
  },
}, {
  mapState: {
    configurations: state => state.system.configurations,
  },
})
@InjectIntl()
class ConnectionUserModal extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    boundSystemActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    isShow: PropTypes.bool.isRequired,
    callback: PropTypes.func.isRequired,
    form: PropTypes.object.isRequired,
    configurations: PropTypes.object.isRequired,
    errorMessage: PropTypes.oneOfType([
      PropTypes.object,
      PropTypes.string,
    ]).isRequired,
    errorData: PropTypes.object.isRequired,
  };

  $form = React.createRef();

  state = {
    isSubmiting: false,
  };

  constructor(props) {
    super(props);
    this.handleCancel = this.handleCancel.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleHideModal = this.handleHideModal.bind(this);
  }

  /* eslint-disable camelcase */
  UNSAFE_componentWillMount() {
    const { boundModalActions } = this.props;
    boundModalActions.registerModal('ConnectionUserModal', getDefaultState());
  }

  get rules() {
    return {
      username: [{ required: true, validator: validator.username(this.props), trigger: 'blur' }],
      password: [{ required: true, validator: validator.password(this.props), trigger: 'blur' }],
    };
  }

  get isNotInitConnection() {
    const { configurations } = this.props;
    return !!configurations['insight.kylin.username'];
  }

  setResponseMessage(response = {}) {
    const { boundModalActions } = this.props;
    const messageCode = response.data.errorMesg.match(business.responseRegx)[0].replace(/\[|\]/g, '');

    let errorMessage = response.data.errorMesg;
    let errorData = {};

    if (stringsMap[messageCode]) {
      const error = stringsMap[messageCode](errorMessage);
      errorMessage = error.message;
      errorData = error.data;
    } else if (strings[messageCode]) {
      errorMessage = strings[messageCode];
    } else if (
      errorMessage.includes('[MDX-00000001]') &&
      errorMessage.includes('Connection refused') &&
      errorMessage.includes('KYLIN')
    ) {
      errorMessage = strings.ERROR_KE_CONNECT;
    } else {
      [errorMessage] = errorMessage.split('\n');
    }

    boundModalActions.setModalData('ConnectionUserModal', { errorMessage, errorData });
  }

  toggleSubmiting(isSubmiting) {
    this.setState({ isSubmiting });
  }

  handleHideModal(isSubmit = false) {
    const { boundModalActions, callback } = this.props;
    boundModalActions.hideModal('ConnectionUserModal');

    if (typeof isSubmit === 'boolean') {
      callback({ isSubmit });
    }
  }

  handleInput(key, value) {
    const { boundModalActions } = this.props;
    boundModalActions.setModalForm('ConnectionUserModal', { [key]: value });
  }

  handleCancel() {
    this.handleHideModal(false);
  }

  handleSubmit() {
    const { boundSystemActions, form } = this.props;

    this.toggleSubmiting(true);

    this.$form.current.validate(async valid => {
      try {
        if (valid) {
          await boundSystemActions.updateConfigurations(formatPostForm(form));
          this.handleHideModal(true);
        }
      } catch (error) {
        this.setResponseMessage(error.response);
      }
      this.toggleSubmiting(false);
    });
  }

  render() {
    const { isShow, intl, form, errorMessage, errorData } = this.props;
    const { isSubmiting } = this.state;
    const { $form, rules, isNotInitConnection } = this;
    const modalTitle = isNotInitConnection
      ? intl.formatMessage(strings.EDIT_CONNECTION_USER)
      : intl.formatMessage(strings.INIT_CONNECTION_USER);

    return (
      <Dialog
        className="connection-user-modal"
        closeOnClickModal={false}
        visible={isShow}
        title={modalTitle}
        onCancel={this.handleHideModal}
        showClose={isNotInitConnection}
        closeOnPressEscape={false}
      >
        <Dialog.Body>
          <Alert
            showIcon
            type="warning"
            icon="icon-superset-infor"
            closable={false}
            title={dataHelper.translate(intl, errorMessage, errorData)}
          />
          <Form ref={$form} model={form} labelPosition="top" rules={rules}>
            <Form.Item label={intl.formatMessage(strings.USERNAME)} prop="username">
              <Input
                className="input-username"
                type="text"
                value={form.username}
                disabled={isSubmiting}
                placeholder={intl.formatMessage(strings.USERNAME)}
                onChange={value => this.handleInput('username', value)}
              />
            </Form.Item>
            <Form.Item label={intl.formatMessage(strings.PASSWORD)} prop="password">
              <Input
                className="input-password"
                type="password"
                value={form.password}
                disabled={isSubmiting}
                placeholder={intl.formatMessage(strings.PASSWORD)}
                onChange={value => this.handleInput('password', value)}
              />
            </Form.Item>
          </Form>
        </Dialog.Body>
        <Dialog.Footer className="dialog-footer">
          {isNotInitConnection && (
            <Button onClick={this.handleCancel} disabled={isSubmiting}>
              {intl.formatMessage(strings.CANCEL)}
            </Button>
          )}
          <Button type="primary" onClick={this.handleSubmit} loading={isSubmiting}>
            {intl.formatMessage(strings.OK)}
          </Button>
        </Dialog.Footer>
      </Dialog>
    );
  }
}
