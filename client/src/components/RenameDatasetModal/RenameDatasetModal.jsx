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
import { Dialog, Button, Form, Input } from 'kyligence-ui-react';

import './index.less';
import { strings, configs } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { getDefaultState, validator } from './handler';

const { datasetCreateTypes } = configs;

export default
@Connect({
  namespace: 'modal/RenameDatasetModal',
  defaultState: getDefaultState(),
  mapState: {
    isShow: state => state.isShow,
    form: state => state.form,
    callback: state => state.callback,
    datasetId: state => state.datasetId,
  },
}, {
  mapState: {
    dataset: state => state.workspace.dataset,
  },
})
@InjectIntl()
class RenameDatasetModal extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    boundDatasetActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    isShow: PropTypes.bool.isRequired,
    form: PropTypes.object.isRequired,
    callback: PropTypes.func.isRequired,
    datasetId: PropTypes.number,
    dataset: PropTypes.object.isRequired,
  };

  static defaultProps = {
    datasetId: null,
  };

  $form = React.createRef();

  state = {
    isSubmiting: false,
  };

  constructor(props) {
    super(props);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleCancel = this.handleCancel.bind(this);
    this.handleHideModal = this.handleHideModal.bind(this);
  }

  /* eslint-disable camelcase */
  UNSAFE_componentWillMount() {
    const { boundModalActions } = this.props;
    boundModalActions.registerModal('RenameDatasetModal', getDefaultState());
  }

  /* eslint-disable camelcase */
  async UNSAFE_componentWillReceiveProps(nextProps) {
    const { isShow: oldIsShow } = this.props;
    const { isShow: newIsShow, datasetId, boundDatasetActions } = nextProps;

    if (!oldIsShow && newIsShow && datasetId) {
      const dataset = await boundDatasetActions.setDatasetStore(datasetId);
      await this.handleInput('datasetName', dataset.datasetName);

      const { form } = this.props;
      await boundDatasetActions.setDatasetBasicInfo(form);
    }

    if (oldIsShow && !newIsShow) {
      boundDatasetActions.clearAll();
    }
  }

  get rules() {
    return {
      datasetName: [{ required: true, validator: validator.datasetName(this.props), trigger: 'blur' }],
    };
  }

  toggleSubmiting(isSubmiting) {
    this.setState({ isSubmiting });
  }

  handleInput(key, value) {
    const { boundModalActions } = this.props;
    boundModalActions.setModalForm('RenameDatasetModal', { [key]: value });
  }

  handleHideModal() {
    const { boundModalActions } = this.props;
    boundModalActions.hideModal('RenameDatasetModal');
  }

  handleCancel() {
    const { callback } = this.props;
    this.handleHideModal();
    callback({ isSubmit: false });
  }

  handleSubmit() {
    const { boundDatasetActions, callback, form, datasetId } = this.props;
    const { datasetName } = form;

    this.toggleSubmiting(true);

    this.$form.current.validate(async valid => {
      try {
        if (valid) {
          const createType = datasetCreateTypes.RENAME;

          await boundDatasetActions.setDatasetBasicInfo(form);
          await boundDatasetActions.updateDatasetJson(datasetId, { createType });
          this.handleHideModal();
          callback({ isSubmit: true, datasetName });
        }
      } catch (e) {}

      this.toggleSubmiting(false);
    });
  }

  render() {
    const { isShow, intl, dataset, form } = this.props;
    const { isSubmiting } = this.state;
    const { isLoading } = dataset;
    const { $form } = this;

    return (
      <Dialog
        className="rename-dataset-modal"
        closeOnPressEscape={false}
        closeOnClickModal={false}
        visible={isShow}
        title={intl.formatMessage(strings.DATASET_RENAME)}
        onCancel={this.handleCancel}
      >
        <Dialog.Body>
          <Form ref={$form} model={form} rules={this.rules}>
            <Form.Item prop="datasetName" label={intl.formatMessage(strings.NEW_NAME)}>
              <Input
                placeholder={intl.formatMessage(strings.VALID_NAME_WITH_CHINESE)}
                value={form.datasetName}
                onChange={val => this.handleInput('datasetName', val)}
              />
            </Form.Item>
          </Form>
        </Dialog.Body>
        <Dialog.Footer className="dialog-footer">
          <Button onClick={this.handleCancel} disabled={isLoading || isSubmiting}>
            {intl.formatMessage(strings.CANCEL)}
          </Button>
          <Button type="primary" onClick={this.handleSubmit} disabled={isLoading} loading={isSubmiting}>
            {intl.formatMessage(strings.OK)}
          </Button>
        </Dialog.Footer>
      </Dialog>
    );
  }
}
