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
import { Dialog, Button, Form, Input } from 'kyligence-ui-react';

import './index.less';
import { strings, configs } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { getDefaultState, validator } from './handler';

const { datasetCreateTypes } = configs;

export default
@Connect({
  namespace: 'modal/CloneDatasetModal',
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
    defaultDatasetAccess: state => state.system.configurations['insight.dataset.allow-access-by-default'],
  },
})
@InjectIntl()
class CloneDatasetModal extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    boundDatasetActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    defaultDatasetAccess: PropTypes.bool.isRequired,
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
    boundModalActions.registerModal('CloneDatasetModal', getDefaultState());
  }

  /* eslint-disable camelcase */
  async UNSAFE_componentWillReceiveProps(nextProps) {
    const { isShow: oldIsShow } = this.props;
    const { isShow: newIsShow, datasetId, boundDatasetActions, defaultDatasetAccess } = nextProps;

    if (!oldIsShow && newIsShow && datasetId) {
      const dataset = await boundDatasetActions.setDatasetStore(datasetId);
      boundDatasetActions.setDatasetBasicInfo({ access: defaultDatasetAccess });
      await this.handleInput('datasetName', `${dataset.datasetName}_clone`);

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
    boundModalActions.setModalForm('CloneDatasetModal', { [key]: value });
  }

  handleHideModal() {
    const { boundModalActions } = this.props;
    boundModalActions.hideModal('CloneDatasetModal');
  }

  handleCancel() {
    const { callback } = this.props;
    this.handleHideModal();
    callback({ isSubmit: false });
  }

  handleSubmit() {
    const { boundDatasetActions, callback, form } = this.props;
    const { datasetName } = form;

    this.toggleSubmiting(true);

    this.$form.current.validate(async valid => {
      try {
        if (valid) {
          await boundDatasetActions.setDatasetBasicInfo(form);
          await boundDatasetActions.saveDatasetJson({ createType: datasetCreateTypes.CLONE });
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
        className="clone-dataset-modal"
        closeOnPressEscape={false}
        closeOnClickModal={false}
        visible={isShow}
        title={intl.formatMessage(strings.DATASET_CLONE)}
        onCancel={this.handleCancel}
      >
        <Dialog.Body>
          <Form ref={$form} model={form} rules={this.rules}>
            <Form.Item prop="datasetName" label={intl.formatMessage(strings.NEW_NAME)}>
              <Input
                placeholder={intl.formatMessage(strings.INPUT_DATASET_NAME)}
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
