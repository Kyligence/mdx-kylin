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
import React from 'react';
import bytes from 'bytes';
import PropTypes from 'prop-types';
import { createSelector } from 'reselect';
import { Button, Form, Input } from 'kyligence-ui-react';

import { Connect, InjectIntl } from '../../store';
import { strings, configs } from '../../constants';

const excelMimeTypes = configs.excelMimeTypes.join(',');

const getFileLimit = createSelector(
  state => state.limitNumber,
  limitNumber => bytes(limitNumber),
);

export default
@Connect()
@InjectIntl()
class RepairSchema extends React.PureComponent {
  static propTypes = {
    boundToolkitActions: PropTypes.object.isRequired,
    configurations: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
  };

  state = {
    fileName: '',
    file: null,
    warning: '',
    isValidating: false,
    isUploading: false,
    progress: 0,
  };

  $form = React.createRef();

  componentDidUpdate(prevProps, prevState) {
    setTimeout(async () => {
      const { file: prevFile } = prevState;
      const { file: currFile, isValidating } = this.state;
      const { intl } = this.props;

      if (currFile !== prevFile && !isValidating) {
        try {
          this.setState({ isValidating: true, warning: '' });
          if (await this.checkSchemaSourceInvalid()) {
            const warning = intl.formatMessage(strings.NON_KYLIN_SOURCE);
            this.setState({ warning });
          }
        } catch (e) {
          console.error(e); // eslint-disable-line no-console
        } finally {
          this.setState({ isValidating: false });
        }
      }
    });
  }

  get rules() {
    return {
      file: [{ required: true, validator: this.validateFile, trigger: 'change' }],
    };
  }

  get fileLimit() {
    const { configurations: { 'repair-excel-limit': limitNumber } } = this.props;
    return getFileLimit({ limitNumber });
  }

  checkSchemaSourceInvalid = () => new Promise((resolve, reject) => {
    this.$form.current.validate(async valid => {
      if (valid) {
        try {
          const { boundToolkitActions } = this.props;
          const { file } = this.state;
          const { connections, pivot_tables: pivotTables } =
            await boundToolkitActions.checkSchemaSource({ file });

          resolve(pivotTables.others > 0 || connections.others > 0);
        } catch (e) {
          reject(e);
        }
      } else {
        resolve(false);
      }
    });
  });

  validateFile = async (rule, value, callback) => {
    const { intl: { formatMessage } } = this.props;
    const { configurations: { 'repair-excel-limit': limitNumber } } = this.props;
    const { fileLimit } = this;

    if (!value) {
      const message = formatMessage(strings.FILE_IS_EMPTY);
      callback(new Error(message));
    } else if (value.size > limitNumber) {
      const message = formatMessage(strings.EXCEED_FILE_LIMITED, { fileLimit });
      callback(new Error(message));
    } else {
      callback();
    }
  };

  handleUploading = event => {
    const { loaded, total } = event;
    this.setState({ progress: (loaded / total) * 100 });
  };

  handleDownloading = () => {
    // https://github.com/axios/axios/issues/1591#issuecomment-430254264
    this.setState({ progress: 0 });
  };

  handleUploadFile = async () => {
    const { boundToolkitActions } = this.props;
    const { file } = this.state;

    this.setState({ isUploading: true });

    this.$form.current.validate(async valid => {
      try {
        if (valid) {
          this.setState({ isUploading: true });
          await boundToolkitActions.repairSchema(
            { file },
            this.handleUploading,
            this.handleDownloading,
          );
        }
      } finally {
        this.setState({ isUploading: false });
      }
    });
  };

  handleInputFile = event => {
    const [file] = event.target.files;
    this.setState({ fileName: file.name, file });
  };

  render() {
    const { intl } = this.props;
    const { fileLimit } = this;
    const { fileName, isUploading, progress, file, warning, isValidating } = this.state;
    const { formatMessage } = intl;

    return (
      <Form ref={this.$form} className="repair-schema" labelPosition="top" model={this.state} rules={this.rules}>
        <div className="el-form-item">
          <div className="el-form-item__label">{formatMessage(strings.EXCEL_REPAIR_TOOL_DESC_1)}</div>
          <div>{formatMessage(strings.EXCEL_REPAIR_TOOL_DESC_2)}</div>
          <div>{formatMessage(strings.EXCEL_REPAIR_TOOL_DESC_3)}</div>
          <div>{formatMessage(strings.EXCEL_REPAIR_TOOL_DESC_4, { fileLimit })}</div>
          <div>{formatMessage(strings.EXCEL_REPAIR_TOOL_DESC_5)}</div>
          <div>{formatMessage(strings.EXCEL_REPAIR_TOOL_DESC_6)}</div>
        </div>
        <Form.Item label={formatMessage(strings.EXCEL_REPAIR_TOOL)} prop="file">
          <div className="file-upload">
            <Input disabled={isUploading || isValidating} className="file-name" placeholder={formatMessage(strings.CLICK_UPLOAD_EXCEL, { fileLimit })} value={fileName} />
            <input disabled={isUploading || isValidating} className="upload-zone" type="file" accept={excelMimeTypes} onChange={this.handleInputFile} value="" />
          </div>
        </Form.Item>
        {warning && (
          <div className="warning-error">{warning}</div>
        )}
        <div className="actions">
          <span className="file-upload" style={{ marginRight: '10px' }}>
            <Button type="primary" disabled={isUploading} loading={isValidating}>
              {isValidating
                ? formatMessage(strings.SCANNING_FILE)
                : formatMessage(strings.SELECT)}
            </Button>
            <input disabled={isUploading || isValidating} className="upload-zone" type="file" accept={excelMimeTypes} onChange={this.handleInputFile} value="" />
          </span>
          <Button type="primary" disabled={!file || isValidating} loading={isUploading} onClick={this.handleUploadFile}>
            {formatMessage(strings.REPAIR_DOWNLOAD)}
          </Button>
          {(!!progress) && (
            <div className="repair-status">
              {formatMessage(strings.DONT_REFRESH_PAGE)}
              {formatMessage(strings.UPLOAD_FILE_STATUS, { progress: progress.toFixed(0) })}
            </div>
          )}
        </div>
      </Form>
    );
  }
}
