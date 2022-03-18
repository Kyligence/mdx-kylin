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
