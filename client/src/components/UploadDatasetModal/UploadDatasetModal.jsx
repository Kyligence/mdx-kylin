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
import { strings } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { getDefaultState, validator } from './handler';

export default
@Connect({
  namespace: 'modal/UploadDatasetModal',
  defaultState: getDefaultState(),
  mapState: {
    isShow: state => state.isShow,
    form: state => state.form,
    callback: state => state.callback,
  },
}, {
  mapState: {
    currentProject: state => state.system.currentProject,
  },
})
@InjectIntl()
class UploadDatasetModal extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    boundModalActions: PropTypes.object.isRequired,
    currentProject: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    isShow: PropTypes.bool.isRequired,
    form: PropTypes.object.isRequired,
    callback: PropTypes.func.isRequired,
  };

  $form = React.createRef();

  state = {
    isSubmiting: false,
  };

  constructor(props) {
    super(props);

    const { boundModalActions } = props;
    boundModalActions.registerModal('UploadDatasetModal', getDefaultState());
  }

  get rules() {
    return {
      file: [{ required: true, validator: validator.file(this.props), trigger: 'change' }],
    };
  }

  toggleSubmiting = isSubmiting => {
    this.setState({ isSubmiting });
  }

  handleInputFile = event => {
    const { boundModalActions } = this.props;
    const [file] = event.target.files;

    boundModalActions.setModalForm('UploadDatasetModal', { fileName: file.name, file });
  }

  handleCancel = () => {
    const { boundModalActions, callback } = this.props;

    callback({ isSubmit: false });
    boundModalActions.hideModal('UploadDatasetModal');
  }

  handleSubmit = () => {
    const { boundModalActions, boundDatasetActions, callback, form, currentProject } = this.props;
    const { name: projectName } = currentProject;

    this.toggleSubmiting(true);

    this.$form.current.validate(async valid => {
      try {
        if (valid) {
          const { differences, token, __headers } =
            await boundDatasetActions.uploadDatasetPackage(form.file, projectName);
          await boundModalActions.hideModal('UploadDatasetModal', false);

          // 如果返回节点信息，则按节点转发
          const [host, port] = __headers ? __headers()['mdx-execute-node'].split(':') : [];

          await boundModalActions.setModalData('DifferDatasetsModal', { differences, token, host, port });
          const { isSubmit } = await boundModalActions.showModal('DifferDatasetsModal');

          if (!isSubmit) {
            boundModalActions.showModal('UploadDatasetModal', null, false);
          } else {
            callback({ isSubmit: true });
            boundModalActions.hideModal('UploadDatasetModal');
          }
        }
      } catch (e) {}

      this.toggleSubmiting(false);
    });
  }

  render() {
    const { isShow, intl, form } = this.props;
    const { isSubmiting } = this.state;
    const { $form } = this;
    const { formatMessage } = intl;
    const { fileName } = form;

    return (
      <Dialog
        className="upload-dataset-modal"
        closeOnPressEscape={false}
        closeOnClickModal={false}
        visible={isShow}
        title={formatMessage(strings.IMPORT_DATASET)}
        onCancel={this.handleCancel}
      >
        <Dialog.Body>
          <div className="warning-tip">
            <i className="icon-superset-warning_16" />
            <span className="warning-text">{formatMessage(strings.EXPORT_DATASET_DESC)}</span>
          </div>
          <Form ref={$form} labelPosition="top" model={form} rules={this.rules}>
            <Form.Item label={formatMessage(strings.SELECT_UPLOAD_FILE)} prop="file">
              <div className="file-upload">
                <Input disabled={isSubmiting} className="file-name" value={fileName} placeholder={formatMessage(strings.PLEASE_SELECT)} />
                <input disabled={isSubmiting} className="upload-zone" type="file" accept="application/json,application/zip" onChange={this.handleInputFile} value="" />
              </div>
            </Form.Item>
          </Form>
        </Dialog.Body>
        <Dialog.Footer className="dialog-footer">
          <Button onClick={this.handleCancel} disabled={isSubmiting}>
            {formatMessage(strings.CANCEL)}
          </Button>
          <Button type="primary" onClick={this.handleSubmit} loading={isSubmiting}>
            {formatMessage(strings.PARSE)}
          </Button>
        </Dialog.Footer>
      </Dialog>
    );
  }
}
