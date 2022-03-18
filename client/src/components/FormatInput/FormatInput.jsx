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
import { Button, Form } from 'kyligence-ui-react';

import './index.less';
import { Connect, InjectIntl } from '../../store';
import { strings, configs, SVGIcon } from '../../constants';
import FormatPreview from '../FormatPreview/FormatPreview';

const { formatTranslations } = configs;
const DUMMY_FUNC = () => {};
const DUMMY_ARRAY = [];

export default
@Connect()
@InjectIntl()
class FormatInput extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    format: PropTypes.string.isRequired,
    formatType: PropTypes.string.isRequired,
    onChange: PropTypes.func,
    customFormats: PropTypes.arrayOf(PropTypes.string),
  };

  static defaultProps = {
    onChange: DUMMY_FUNC,
    customFormats: DUMMY_ARRAY,
  };

  handleEditFormat = async () => {
    const {
      boundModalActions,
      format: oriFormat,
      formatType: oriType,
      customFormats,
      onChange,
    } = this.props;

    await boundModalActions.setModalData('FormatSettingModal', { format: oriFormat, formatType: oriType, customFormats });
    const { isSubmit, format, formatType } = await boundModalActions.showModal('FormatSettingModal');

    if (isSubmit) {
      onChange('format', format);
      onChange('formatType', formatType);
    }
  }

  render() {
    const { intl, format, formatType } = this.props;
    const { formatMessage } = intl;

    return (
      <Form.Item
        className="format-input"
        label={formatMessage(strings.FORMAT)}
        description={formatMessage(strings.FORMAT_DESC)}
      >
        <div className="format-input-content">
          <span className="format-type">
            {formatMessage(formatTranslations[formatType])}
          </span>
          <FormatPreview format={format} />
          <Button plain size="small" type="primary" className="edit-format-button" onClick={this.handleEditFormat}>
            <SVGIcon.EditPage />
            <span>{formatMessage(strings.EDIT)}</span>
          </Button>
        </div>
      </Form.Item>
    );
  }
}
