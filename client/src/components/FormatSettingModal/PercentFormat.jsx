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
import { Form, InputNumber } from 'kyligence-ui-react';

import { InjectIntl } from '../../store';
import { strings } from '../../constants';
import FormatPreview from '../FormatPreview/FormatPreview';

const DUMMY_FUNC = () => {};

export default
@InjectIntl()
class PercentFormat extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    format: PropTypes.string.isRequired,
    decimalCount: PropTypes.number.isRequired,
    onChange: PropTypes.func,
  };

  static defaultProps = {
    onChange: DUMMY_FUNC,
  };

  handleChangeDecimal = value => {
    const { onChange } = this.props;
    onChange('decimalCount', value);
  }

  handleChangeNegative = value => {
    const { onChange } = this.props;
    onChange('negativeType', value);
  }

  render() {
    const { intl, format, decimalCount } = this.props;
    const { formatMessage } = intl;

    return (
      <>
        <div className="format-description">
          {formatMessage(strings.PERCENTAGE_FORMAT_DESC)}
        </div>
        <Form.Item label={formatMessage(strings.SAMPLE)}>
          <FormatPreview format={format} />
        </Form.Item>
        <Form.Item label={formatMessage(strings.DECIMAL_PLACES)} prop="decimalCount">
          <InputNumber
            max={30}
            theme="arrow"
            defaultValue={decimalCount}
            value={decimalCount}
            onChange={this.handleChangeDecimal}
          />
        </Form.Item>
      </>
    );
  }
}
