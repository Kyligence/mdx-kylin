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
