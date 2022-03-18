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
import { Dialog, Button, Form, Input, Radio } from 'kyligence-ui-react';

import './index.less';
import { strings } from '../../constants';
import { getDefaultState, formTypes } from './handler';
import { Connect, InjectIntl } from '../../store';
import { dataHelper } from '../../utils';

const { normalizeSentences } = dataHelper;

export default
@Connect({
  namespace: 'modal/SaveNotExistedModal',
  defaultState: getDefaultState(),
  mapState: {
    isShow: state => state.isShow,
    callback: state => state.callback,
    form: state => state.form,
    rules: state => state.rules,
    entityId: state => state.entityId,
    handleSave: state => state.handleSave,
    isSubmiting: state => state.isSubmiting,
  },
}, {
  mapState: {
    dataset: state => state.workspace.dataset,
  },
})
@InjectIntl()
class SaveNotExistedModal extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    isShow: PropTypes.bool.isRequired,
    isSubmiting: PropTypes.bool.isRequired,
    callback: PropTypes.func.isRequired,
    handleSave: PropTypes.func.isRequired,
    form: PropTypes.object.isRequired,
    rules: PropTypes.object.isRequired,
    entityId: PropTypes.string.isRequired,
  };

  $form = React.createRef();

  componentDidMount() {
    const { boundModalActions } = this.props;
    boundModalActions.registerModal('SaveNotExistedModal', getDefaultState());
  }

  toggleSubmiting = isSubmiting => {
    const { boundModalActions } = this.props;
    boundModalActions.setModalData('SaveNotExistedModal', { isSubmiting });
  }

  handleInput = (key, value) => {
    const { boundModalActions } = this.props;
    boundModalActions.setModalForm('SaveNotExistedModal', { [key]: value });
  }

  handleHideModal = () => {
    const { boundModalActions } = this.props;
    boundModalActions.hideModal('SaveNotExistedModal');
  }

  handleCancel = () => {
    const { callback } = this.props;
    callback({ isSubmit: false });
    this.handleHideModal();
  }

  handleAbort = () => {
    const { callback, form } = this.props;
    callback({ isSubmit: false, ...form });
    this.handleHideModal();
  }

  handleSaveNew = async () => {
    const { callback, form, handleSave } = this.props;
    try {
      this.toggleSubmiting(true);
      await handleSave(form);

      callback({ isSubmit: true, ...form });
      this.handleHideModal();
    } catch {} finally {
      this.toggleSubmiting(false);
    }
  }

  handleSubmit = async () => {
    const { form } = this.props;

    switch (form.formType) {
      case formTypes.SAVE_AS: await this.handleSaveNew(); break;
      case formTypes.GIVE_UP_EDIT: this.handleAbort(); break;
      default: break;
    }
  }

  render() {
    const { isShow, isSubmiting, intl, entityId, form, rules } = this.props;
    const { $form } = this;
    const entity = dataHelper.translate(intl, entityId);
    return (
      <Dialog
        className="save-not-existed-modal"
        visible={isShow}
        closeOnClickModal={false}
        title={intl.formatMessage(strings.NOTIFICATION)}
        onCancel={this.handleCancel}
      >
        <Dialog.Body>
          <div className="save-not-existed-tip">
            <div>
              <i className="icon-superset-error_01" />
              {normalizeSentences(intl.formatMessage(strings.ENTITY_NOT_EXISTED_TIP, { entity }))}
            </div>
            <Form ref={$form} model={form} rules={rules}>
              <Form.Item prop="name">
                <Radio
                  value={formTypes.SAVE_AS}
                  checked={form.formType === formTypes.SAVE_AS}
                  onChange={value => this.handleInput('formType', value)}
                >
                  {intl.formatMessage(strings.SAVE_AS)}
                </Radio>
                <Input
                  value={form.name}
                  disabled={form.formType !== formTypes.SAVE_AS}
                  onChange={value => this.handleInput('name', value)}
                />
              </Form.Item>
              <Form.Item prop="formType">
                <Radio
                  value={formTypes.GIVE_UP_EDIT}
                  checked={form.formType === formTypes.GIVE_UP_EDIT}
                  onChange={value => this.handleInput('formType', value)}
                >
                  {intl.formatMessage(strings.GIVE_UP_EDIT)}
                </Radio>
                <div className="save-not-existed-give-up-tip">
                  {intl.formatMessage(strings.GIVE_UP_EDIT_TIP, { entity })}
                </div>
              </Form.Item>
            </Form>
          </div>
        </Dialog.Body>
        <Dialog.Footer className="dialog-footer">
          <Button onClick={this.handleCancel} disabled={isSubmiting}>
            {intl.formatMessage(strings.CANCEL)}
          </Button>
          <Button type="primary" onClick={this.handleSubmit} loading={isSubmiting}>
            {(() => {
              switch (form.formType) {
                case formTypes.GIVE_UP_EDIT:
                  return intl.formatMessage(strings.GIVE_UP_EDIT);
                case formTypes.SAVE_AS:
                  return intl.formatMessage(strings.SAVE_AS);
                default:
                  return intl.formatMessage(strings.OK);
              }
            })()}
          </Button>
        </Dialog.Footer>
      </Dialog>
    );
  }
}
