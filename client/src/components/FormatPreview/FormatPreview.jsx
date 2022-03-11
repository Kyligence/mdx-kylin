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
