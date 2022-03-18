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
import { Dialog, Button, Form } from 'kyligence-ui-react';

import './index.less';
import { formatHelper } from '../../utils';
import { strings, configs } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { getDefaultState, validator, getFormatBySettings } from './handler';
import SwitchButton from '../SwitchButton/SwitchButton';
import NoFormat from './NoFormat';
import NumberFormat from './NumberFormat';
import CurrencyFormat from './CurrencyFormat';
import PercentFormat from './PercentFormat';
import CustomFormat from './CustomFormat';

const { formatTypes, formatTranslations } = configs;
const { parseFormatToSettings } = formatHelper;

export default
@Connect({
  namespace: 'modal/FormatSettingModal',
  defaultState: getDefaultState(),
  mapState: {
    isShow: state => state.isShow,
    form: state => state.form,
    format: state => state.format,
    formatType: state => state.formatType,
    customFormats: state => state.customFormats,
    callback: state => state.callback,
  },
})
@InjectIntl()
class FormatSettingModal extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    isShow: PropTypes.bool.isRequired,
    format: PropTypes.string.isRequired,
    formatType: PropTypes.string.isRequired,
    customFormats: PropTypes.arrayOf(PropTypes.string).isRequired,
    form: PropTypes.object.isRequired,
    callback: PropTypes.func.isRequired,
  };

  $form = React.createRef();

  state = {
    isSubmiting: false,
  };

  /* eslint-disable camelcase */
  UNSAFE_componentWillMount() {
    const { boundModalActions } = this.props;
    boundModalActions.registerModal('FormatSettingModal', getDefaultState());
  }

  componentDidUpdate(prevProps) {
    this.onPropFormatAndTypeChange(prevProps);
  }

  get formatOptions() {
    const { intl } = this.props;
    const { formatMessage } = intl;

    return Object
      .values(formatTypes)
      .map(formatType => ({
        label: formatMessage(formatTranslations[formatType]),
        value: formatType,
      }));
  }

  get rules() {
    return {
      customFormat: [{ required: true, validator: validator.customFormat(this.props), trigger: 'blur' }],
    };
  }

  get format() {
    const { form } = this.props;
    return getFormatBySettings({ form });
  }

  onPropFormatAndTypeChange = prevProps => {
    const { format: prevFormat, formatType: prevFormatType } = prevProps;
    const { format, formatType, boundModalActions } = this.props;

    if (format !== prevFormat || formatType !== prevFormatType) {
      const settings = parseFormatToSettings(format, formatType);

      // 判断当初始为自定义格式时，将度量中的自定义格式赋值到输入框中，反之则用默认值
      if (formatType === formatTypes.CUSTOM) {
        settings.customFormat = format;
      }

      boundModalActions.setModalForm('FormatSettingModal', { ...settings, formatType });
    }
  }

  toggleSubmiting = isSubmiting => {
    this.setState({ isSubmiting });
  }

  handleChangeFormatType = formatType => {
    const { boundModalActions, form } = this.props;
    const { format } = this;

    if (formatType === formatTypes.CUSTOM) {
      form.customFormat = format;
    }

    boundModalActions.setModalForm('FormatSettingModal', { ...form, formatType });
  }

  handleInput = (key, value) => {
    const { boundModalActions } = this.props;
    boundModalActions.setModalForm('FormatSettingModal', { [key]: value });
  }

  handleHideModal = () => {
    const { boundModalActions } = this.props;
    boundModalActions.hideModal('FormatSettingModal');
  }

  handleCancel = () => {
    const { callback } = this.props;
    this.handleHideModal();
    callback({ isSubmit: false });
  }

  handleSubmit = () => {
    const { callback, form } = this.props;
    const { formatType } = form;
    const { format } = this;

    this.toggleSubmiting(true);

    this.$form.current.validate(async valid => {
      try {
        if (valid) {
          this.handleHideModal();
          callback({ isSubmit: true, formatType, format });
        }
      } catch (e) {}

      this.toggleSubmiting(false);
    });
  }

  renderForm = () => {
    const { form, customFormats } = this.props;

    switch (form.formatType) {
      case formatTypes.NUMBER:
        return <NumberFormat {...form} format={this.format} onChange={this.handleInput} />;
      case formatTypes.CURRENCY:
        return <CurrencyFormat {...form} format={this.format} onChange={this.handleInput} />;
      case formatTypes.PERCENT:
        return <PercentFormat {...form} format={this.format} onChange={this.handleInput} />;
      case formatTypes.CUSTOM:
        return (
          <CustomFormat
            {...form}
            format={this.format}
            onChange={this.handleInput}
            options={customFormats}
          />
        );
      case formatTypes.NO_FORMAT:
      default:
        return <NoFormat {...form} format={this.format} />;
    }
  }

  render() {
    const { isShow, intl, form } = this.props;
    const { isSubmiting } = this.state;
    const { formatMessage } = intl;
    const { $form } = this;

    return (
      <Dialog
        className="format-setting-modal"
        closeOnPressEscape={false}
        closeOnClickModal={false}
        visible={isShow}
        title={formatMessage(strings.FORMAT_SETTING)}
        onCancel={this.handleCancel}
      >
        <Dialog.Body>
          <SwitchButton
            className="switch-format-type"
            value={form.formatType}
            options={this.formatOptions}
            onChange={this.handleChangeFormatType}
          />
          <Form labelPosition="top" ref={$form} model={form} rules={this.rules}>
            {this.renderForm()}
          </Form>
        </Dialog.Body>
        <Dialog.Footer className="dialog-footer">
          <Button onClick={this.handleCancel} disabled={isSubmiting}>
            {formatMessage(strings.CANCEL)}
          </Button>
          <Button type="primary" onClick={this.handleSubmit} loading={isSubmiting}>
            {formatMessage(strings.OK)}
          </Button>
        </Dialog.Footer>
      </Dialog>
    );
  }
}
