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
import classnames from 'classnames';
import { Popover, Checkbox } from 'kyligence-ui-react';

import './index.less';
import { InjectIntl } from '../../store';
import { strings } from '../../constants';
import { dataHelper } from '../../utils';

const { isEmpty } = dataHelper;

export default
@InjectIntl()
class TableFilterBox extends PureComponent {
  static propTypes = {
    single: PropTypes.bool,
    headerText: PropTypes.string,
    intl: PropTypes.object.isRequired,
    onFilter: PropTypes.func,
    filters: PropTypes.array.isRequired,
    value: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.number,
      PropTypes.bool,
      PropTypes.array,
    ]),
  };

  static defaultProps = {
    onFilter: () => {},
    single: false,
    headerText: '',
    value: undefined,
  }

  get filterIconClass() {
    const { value, single } = this.props;
    const isSelected = single ? !isEmpty(value) : !!value.length;
    return classnames(['icon-superset-filter', isSelected && 'selected']);
  }

  renderSingleCheckbox = () => {
    const { filters, value, onFilter } = this.props;
    return (
      <div className="el-checkbox-group">
        {filters.map(filter => (
          <Checkbox
            key={filter.value}
            label={filter.value}
            checked={filter.value === value}
            onChange={isChecked => onFilter(isChecked ? filter.value : undefined)}
          >
            {filter.label}
          </Checkbox>
        ))}
      </div>
    );
  }

  renderMultipleCheckbox = () => {
    const { filters, value, onFilter } = this.props;
    return (
      <Checkbox.Group value={value} onChange={val => onFilter(val)}>
        {filters.map(filter => (
          <Checkbox key={filter.value} label={filter.value}>{filter.label}</Checkbox>
        ))}
      </Checkbox.Group>
    );
  }

  renderContent = () => {
    const { filters, single, intl } = this.props;

    if (filters.length) {
      return single ? this.renderSingleCheckbox() : this.renderMultipleCheckbox();
    }
    return (
      <span className="empty-text">
        {intl.formatMessage(strings.NO_DATA)}
      </span>
    );
  }

  render() {
    const { headerText } = this.props;

    return (
      <div className="table-filter-box">
        {headerText}
        <Popover
          placement="bottom"
          trigger="hover"
          content={this.renderContent()}
          popperProps={{
            positionFixed: true,
            modifiers: {
              flip: { enabled: false },
              hide: { enabled: false },
              preventOverflow: { enabled: false },
            },
          }}
        >
          <i className={this.filterIconClass} />
        </Popover>
      </div>
    );
  }
}
