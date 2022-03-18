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
import { Form, InputNumber, Select } from 'kyligence-ui-react';

import { InjectIntl } from '../../store';
import { configs, strings } from '../../constants';
import { getDecimalFormat, getSampleDecimalFormat } from './handler';
import FormatPreview from '../FormatPreview/FormatPreview';

const { negativeTypes, thousandValue } = configs;
const DUMMY_FUNC = () => {};

export default
@InjectIntl()
class CurrencyFormat extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    format: PropTypes.string.isRequired,
    decimalCount: PropTypes.number.isRequired,
    negativeType: PropTypes.string.isRequired,
    onChange: PropTypes.func,
  };

  static defaultProps = {
    onChange: DUMMY_FUNC,
  };

  get decimal() {
    const { decimalCount } = this.props;
    return getDecimalFormat({ decimalCount });
  }

  get sampleDecimal() {
    const { decimalCount } = this.props;
    return getSampleDecimalFormat({ decimalCount });
  }

  handleChangeDecimal = value => {
    const { onChange } = this.props;
    onChange('decimalCount', value);
  }

  handleChangeNegative = value => {
    const { onChange } = this.props;
    onChange('negativeType', value);
  }

  render() {
    const { intl, format, decimalCount, negativeType } = this.props;
    const { formatMessage } = intl;
    const { sampleDecimal } = this;

    return (
      <>
        <div className="format-description">
          {formatMessage(strings.CURRENCY_FORMAT_DESC)}
        </div>
        <Form.Item label={formatMessage(strings.SAMPLE)}>
          <FormatPreview format={format} />
        </Form.Item>
        <div className="format-description">
          {formatMessage(strings.CURRENCY_FORMAT_TIP)}
        </div>
        <Form.Item label={formatMessage(strings.DECIMAL_PLACES)} prop="decimalCount">
          <InputNumber
            max={30}
            theme="arrow"
            defaultValue={decimalCount}
            value={decimalCount}
            onChange={this.handleChangeDecimal}
          />
        </Form.Item>
        <Form.Item label={formatMessage(strings.MINUS)} prop="negativeType">
          <Select key={`${thousandValue}${sampleDecimal}`} value={negativeType} onChange={this.handleChangeNegative}>
            <Select.Option label={`-${thousandValue}${sampleDecimal}`} value={negativeTypes.NORMAL} />
            <Select.Option label={`(${thousandValue}${sampleDecimal})`} value={negativeTypes.PARENTHESES} />
          </Select>
        </Form.Item>
      </>
    );
  }
}
