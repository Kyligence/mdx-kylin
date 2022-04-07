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
