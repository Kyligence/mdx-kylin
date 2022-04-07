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
import { Form, Input, Button, Tooltip } from 'kyligence-ui-react';

import { Connect, InjectIntl } from '../../store';
import { strings } from '../../constants';
import { validator } from './handler';

export default
@Connect()
@InjectIntl()
class LoginForm extends PureComponent {
  static propTypes = {
    boundSystemActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
  };

  $form = React.createRef();

  state = {
    isLoading: false,
    form: {
      username: '',
      password: '',
    },
  };

  constructor(props) {
    super(props);
    this.handleLogin = this.handleLogin.bind(this);
  }

  get rules() {
    const { intl } = this.props;
    return {
      username: [{ validator: validator.username(intl), trigger: 'blur' }],
      password: [{ validator: validator.password(intl), trigger: 'blur' }],
    };
  }

  toggleLoading(isLoading) {
    this.setState({ isLoading });
  }

  handleInput(key, value) {
    const { form: oldForm } = this.state;
    const form = { ...oldForm, [key]: value };
    this.setState({ form });
  }

  async handleLogin(event) {
    event.preventDefault();

    const { boundSystemActions } = this.props;
    const { form } = this.state;
    const { $form } = this;

    this.toggleLoading(true);

    $form.current.validate(async isValid => {
      if (isValid) {
        try {
          await boundSystemActions.login(form);
        } catch (e) {}
      }
      this.toggleLoading(false);
    });
  }

  render() {
    const { intl } = this.props;
    const { form, isLoading } = this.state;
    const { $form } = this;

    return (
      <Form className="login-form" ref={$form} model={form} rules={this.rules} onSubmit={this.handleLogin}>
        <Form.Item prop="username">
          <Input
            className="input-username mdx-it-username-input"
            type="text"
            value={form.username}
            placeholder={intl.formatMessage(strings.USERNAME)}
            onChange={value => this.handleInput('username', value)}
          />
        </Form.Item>
        <Form.Item prop="password">
          <Input
            className="input-password mdx-it-password-input"
            type="password"
            value={form.password}
            placeholder={intl.formatMessage(strings.PASSWORD)}
            onChange={value => this.handleInput('password', value)}
          />
        </Form.Item>
        <div className="login-tips clearfix">
          <Tooltip className="pull-right" popperClass="forget-password-tip" placement="top" content={intl.formatMessage(strings.FORGET_PASSWORD_TIP)}>
            <Button className="forget-password" type="text">
              {intl.formatMessage(strings.FORGET_PASSWORD)}
            </Button>
          </Tooltip>
        </div>
        <Button className="login-button mdx-it-login-button" type="primary" nativeType="submit" loading={isLoading} onClick={this.handleLogin}>
          {intl.formatMessage(strings.SIGNIN)}
        </Button>
      </Form>
    );
  }
}
