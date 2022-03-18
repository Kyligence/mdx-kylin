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
import { Input } from 'kyligence-ui-react';

import './index.less';
import { Connect, InjectIntl } from '../../store';
import { configs, SVGIcon } from '../../constants';

const { formatValue } = configs;

export default
@Connect()
@InjectIntl()
class FormatPreview extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    format: PropTypes.string.isRequired,
    isReadOnly: PropTypes.bool,
  };

  static defaultProps = {
    isReadOnly: false,
  };

  mounted = false;

  state = {
    formattedValue: '',
    isLoading: false,
  };

  componentDidMount() {
    this.onFormatChange();

    this.mounted = true;
  }

  componentDidUpdate(prevProps) {
    this.onFormatChange(prevProps);
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  onFormatChange = async (prevProps = {}) => {
    const { format: prevFormat } = prevProps;
    const { format, boundDatasetActions } = this.props;

    if (prevFormat !== format) {
      try {
        this.setState({ formattedValue: '', isLoading: true });

        const formattedValue = await boundDatasetActions.previewFormatSample(format, formatValue);

        if (this.mounted) {
          this.setState({ formattedValue, isLoading: false });
        }
      } catch {} finally {
        if (this.mounted) {
          this.setState({ isLoading: false });
        }
      }
    }
  }

  render() {
    const { isReadOnly } = this.props;
    const { formattedValue, isLoading } = this.state;

    return !isReadOnly ? (
      <Input
        disabled
        className="format-preview"
        prefixIcon={isLoading ? <SVGIcon.Loading className="loading" /> : undefined}
        value={formattedValue}
      />
    ) : (
      <div className="format-preview">
        {isLoading ? <SVGIcon.Loading className="loading" /> : formattedValue}
      </div>
    );
  }
}
