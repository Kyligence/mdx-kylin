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
