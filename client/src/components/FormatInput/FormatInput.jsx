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
