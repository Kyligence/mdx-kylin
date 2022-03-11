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
