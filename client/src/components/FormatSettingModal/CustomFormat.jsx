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
import { Form, AutoComplete } from 'kyligence-ui-react';

import { InjectIntl } from '../../store';
import { strings, SVGIcon } from '../../constants';
import FormatPreview from '../FormatPreview/FormatPreview';

const DUMMY_FUNC = () => {};
const DUMMY_ARRAY = [];

export default
@InjectIntl()
class CustomFormat extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    format: PropTypes.string.isRequired,
    customFormat: PropTypes.string.isRequired,
    options: PropTypes.arrayOf(PropTypes.string),
    onChange: PropTypes.func,
  };

  static defaultProps = {
    onChange: DUMMY_FUNC,
    options: DUMMY_ARRAY,
  };

  fetchSuggestions = (value, callback) => {
    const { options } = this.props;
    const internalOptions = [
      { label: '0', value: '0' },
      { label: '0.00', value: '0.00' },
      { label: '#,##0', value: '#,##0' },
      { label: '#,##0.00', value: '#,##0.00' },
      { label: '#,##0.00;-#,##0.00', value: '#,##0.00;-#,##0.00' },
      { label: '#,##0.00;(#,##0.00)', value: '#,##0.00;(#,##0.00)' },
    ];

    const additionalOptions = options
      .filter(option => !internalOptions.some(item => item.value === option))
      .map(option => ({ label: option, value: option }));

    callback([...internalOptions, ...additionalOptions]);
  }

  handleChangeCustomFormat = value => {
    const { onChange } = this.props;
    onChange('customFormat', value);
  }

  handleSelectCustomFormat = ({ value }) => {
    const { onChange } = this.props;
    onChange('customFormat', value);
  }

  render() {
    const { intl, format, customFormat } = this.props;
    const { formatMessage } = intl;

    return (
      <>
        <div className="format-description">
          {formatMessage(strings.CUSTOMIZE_FORMAT_DESC)}
        </div>
        <Form.Item label={formatMessage(strings.SAMPLE)}>
          <FormatPreview format={format} />
        </Form.Item>
        <Form.Item label={formatMessage(strings.TYPE)} prop="customFormat">
          <AutoComplete
            value={customFormat}
            onChange={this.handleChangeCustomFormat}
            onSelect={this.handleSelectCustomFormat}
            fetchSuggestions={this.fetchSuggestions}
          />
        </Form.Item>
        <div className="format-description">
          {formatMessage(strings.CUSTOMIZE_FORMAT_TIP, { link: (
            <a href={formatMessage(strings.LINK_FORMAT_DOC)} target="_blank" rel="noreferrer">
              {formatMessage(strings.FORMAT_DESCRIPTION)}
              <SVGIcon.OpenLink />
            </a>
          ) })}
        </div>
      </>
    );
  }
}
