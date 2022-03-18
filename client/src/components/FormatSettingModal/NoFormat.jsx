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
import { Form } from 'kyligence-ui-react';

import { InjectIntl } from '../../store';
import { strings } from '../../constants';
import FormatPreview from '../FormatPreview/FormatPreview';

export default
@InjectIntl()
class NoFormat extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    format: PropTypes.string.isRequired,
  };

  render() {
    const { intl, format } = this.props;
    const { formatMessage } = intl;

    return (
      <>
        <div className="format-description">
          {formatMessage(strings.NO_FORMATTING)}
        </div>
        <Form.Item label={intl.formatMessage(strings.SAMPLE)}>
          <FormatPreview format={format} />
        </Form.Item>
      </>
    );
  }
}
