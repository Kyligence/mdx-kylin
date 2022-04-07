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
import { Tooltip, Select, Input } from 'kyligence-ui-react';

import './index.less';
import { strings } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { getNodeIcon } from './handler';

export default
@Connect()
@InjectIntl()
class SemanticSearchBox extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    nodeTypeOptions: PropTypes.array,
    filter: PropTypes.shape({
      nodeType: PropTypes.string.isRequired,
      name: PropTypes.string.isRequired,
    }).isRequired,
    onChange: PropTypes.func,
  };

  static defaultProps = {
    nodeTypeOptions: ['table', 'column', 'hierarchy', 'namedSet', 'measure', 'calculateMeasure'],
    onChange: () => {},
  };

  get filterOptions() {
    const { intl, nodeTypeOptions } = this.props;
    return [
      {
        label: intl.formatMessage(strings.DIMENSION_TABLES),
        value: 'table',
        icon: getNodeIcon({ nodeType: 'table' }),
      },
      {
        label: intl.formatMessage(strings.DIMENSION),
        value: 'column',
        icon: getNodeIcon({ nodeType: 'column' }),
      },
      {
        label: intl.formatMessage(strings.HIERARCHY),
        value: 'hierarchy',
        icon: getNodeIcon({ nodeType: 'hierarchy' }),
      },
      {
        label: intl.formatMessage(strings.NAMEDSET),
        value: 'namedSet',
        icon: getNodeIcon({ nodeType: 'namedSet' }),
      },
      {
        label: intl.formatMessage(strings.MEASURE),
        value: 'measure',
        icon: getNodeIcon({ nodeType: 'measure' }),
      },
      {
        label: intl.formatMessage(strings.CALCULATED_MEASURE),
        value: 'calculateMeasure',
        icon: getNodeIcon({ nodeType: 'calculateMeasure' }),
      },
    ].filter(option => nodeTypeOptions.includes(option.value));
  }

  handleChange(key, value) {
    const { filter, onChange } = this.props;
    onChange({ ...filter, [key]: value });
  }

  render() {
    const { intl, filter } = this.props;
    const { filterOptions } = this;
    const filterIcon = getNodeIcon({ nodeType: filter.nodeType });

    return (
      <div className="semantic-search-box clearfix">
        <Select
          positionFixed
          className="icon-mode filter-type"
          prefixIcon={filterIcon}
          value={filter.nodeType}
          onChange={value => this.handleChange('nodeType', value)}
        >
          {filterOptions.map(option => (
            <Select.Option key={option.value} value={option.value}>
              <Tooltip
                positionFixed
                className="option-item"
                popperClass="option-item-tooltip"
                placement="top"
                content={option.label}
              >
                <i className={option.icon} />
              </Tooltip>
            </Select.Option>
          ))}
        </Select>
        <Input
          className="filter-name"
          value={filter.name}
          onChange={value => this.handleChange('name', value)}
          placeholder={intl.formatMessage(strings.FILTER)}
        />
      </div>
    );
  }
}
