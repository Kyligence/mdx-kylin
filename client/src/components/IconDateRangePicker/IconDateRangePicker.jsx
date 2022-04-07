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
import React, { PureComponent, Fragment } from 'react';
import classnames from 'classnames';
import PropTypes from 'prop-types';
import { DateRangePicker, Tooltip } from 'kyligence-ui-react';
import dayjs from 'dayjs';

import './index.less';
import { Connect, InjectIntl } from '../../store';
import { strings } from '../../constants';
import { dataHelper } from '../../utils';

const EMPTY_FUNC = () => {};

export default
@Connect()
@InjectIntl()
class IconDateRangePicker extends PureComponent {
  static propTypes = {
    className: PropTypes.string,
    isShowValue: PropTypes.bool,
    onChange: PropTypes.func,
    value: PropTypes.array,
    intl: PropTypes.object.isRequired,
    shortcuts: PropTypes.array,
  };

  static defaultProps = {
    className: '',
    isShowValue: true,
    value: null,
    onChange: EMPTY_FUNC,
    shortcuts: [],
  };

  static shortcutTypes = {
    LAST_24_HOURS: 'LAST_24_HOURS',
    LAST_7_DAYS: 'LAST_7_DAYS',
    LAST_30_DAYS: 'LAST_30_DAYS',
  };

  pickerRef = React.createRef();

  state = {
    value: this.props.value,
  };

  componentDidUpdate(prevProps) {
    const { value: oldValue } = prevProps;
    const { value: newValue } = this.props;

    if (oldValue !== newValue) {
      setTimeout(() => this.setState({ value: newValue }));
    }
  }

  get shortcuts() {
    const { intl, shortcuts = [] } = this.props;
    return [
      {
        key: 'LAST_24_HOURS',
        text: intl.formatMessage(strings.LAST_24_HOURS),
        onClick: () => this.handleChangeDate([
          dayjs().subtract(1, 'day').toDate(),
          dayjs().toDate(),
        ], true),
      },
      {
        key: 'LAST_7_DAYS',
        text: intl.formatMessage(strings.LAST_7_DAYS),
        onClick: () => this.handleChangeDate([
          dayjs().subtract(7, 'day').toDate(),
          dayjs().toDate(),
        ], true),
      },
      {
        key: 'LAST_30_DAYS',
        text: intl.formatMessage(strings.LAST_30_DAYS),
        onClick: () => this.handleChangeDate([
          dayjs().subtract(30, 'day').toDate(),
          dayjs().toDate(),
        ], true),
      },
    ].filter(shortcut => shortcuts.includes(shortcut.key));
  }

  handleChangeDate = (value, isTogglePicker = false) => {
    const { onChange } = this.props;

    this.setState({ value });
    onChange(value);

    if (isTogglePicker) {
      this.pickerRef.current.togglePickerVisible();
    }
  };

  handleToggleDatePicker = event => {
    // 消除table上的onClick事件
    event.stopPropagation();
  };

  renderDateRangeText = () => {
    const { intl } = this.props;
    const { value } = this.state;

    if (value && value.length === 2) {
      const to = intl.formatMessage(strings.TO);
      const startText = dataHelper.getDateString(value[0].getTime());
      const endText = dataHelper.getDateString(value[1].getTime());

      return (
        <Fragment>
          <span className="start-time">
            <Tooltip appendToBody placement="top" content={startText}>{startText}</Tooltip>
          </span>
          <span className="separator">{to}</span>
          <span className="end-time">
            <Tooltip appendToBody placement="top" content={endText}>{endText}</Tooltip>
          </span>
        </Fragment>
      );
    }

    return null;
  };

  render() {
    const { isShowValue, className } = this.props;
    const { value } = this.state;
    const { shortcuts } = this;

    return (
      <div className={classnames('icon-date-range-picker', className)} onClick={this.handleToggleDatePicker}>
        {isShowValue ? (
          <span className="date-range-value">
            {this.renderDateRangeText()}
          </span>
        ) : null}
        <div className="picker-icon">
          <i className="icon-superset-type_timestamp" />
          <DateRangePicker
            ref={this.pickerRef}
            isShowTime
            value={value}
            onChange={this.handleChangeDate}
            shortcuts={shortcuts.length ? shortcuts : null}
          />
        </div>
      </div>
    );
  }
}
