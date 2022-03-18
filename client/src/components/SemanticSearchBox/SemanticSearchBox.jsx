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
