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
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { Input, Popover, Checkbox, OverflowTooltip } from 'kyligence-ui-react';

import './index.less';
import { strings } from '../../constants';
import { InjectIntl } from '../../store';
import { getSearchedOptions } from './handler';

const EMPTY_FUNC = () => {};
const EMPTY_OBJECT = {};

export default
@InjectIntl()
class CheckboxIconFilter extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    label: PropTypes.string,
    appendToBody: PropTypes.bool,
    className: PropTypes.string,
    trigger: PropTypes.oneOf(['hover', 'click']),
    placement: PropTypes.oneOf(['top', 'top-start', 'top-end', 'bottom', 'bottom-start', 'bottom-end', 'left', 'left-start', 'left-end', 'right', 'right-start', 'right-end']),
    onChange: PropTypes.func,
    multiple: PropTypes.bool,
    searchPlaceholder: PropTypes.string,
    popperProps: PropTypes.object,
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.number, PropTypes.array]),
    options: PropTypes.arrayOf(PropTypes.shape({
      label: PropTypes.string,
      value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
      options: PropTypes.arrayOf(PropTypes.shape({
        label: PropTypes.string,
        value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
      })),
    })),
  };

  static defaultProps = {
    label: '',
    appendToBody: false,
    multiple: false,
    className: '',
    searchPlaceholder: '',
    placement: 'bottom',
    trigger: 'hover',
    options: [],
    value: undefined,
    onChange: EMPTY_FUNC,
    popperProps: EMPTY_OBJECT,
  };

  state = {
    search: '',
    value: this.props.value,
  };

  constructor(props) {
    super(props);

    if (props.value === undefined) {
      this.state.value = props.multiple ? [] : null;
    }
  }

  componentDidUpdate(prevProps) {
    const { value: oldValue } = prevProps;
    const { value: newValue } = this.props;

    if (oldValue !== newValue) {
      setTimeout(() => this.setState({ value: newValue }));
    }
  }

  get searchedOptions() {
    const { options } = this.props;
    const { search } = this.state;
    return getSearchedOptions({ options, search });
  }

  isOptionChecked = option => {
    const { multiple } = this.props;
    const { value } = this.state;

    return multiple ? value.includes(option.value) : value === option.value;
  };

  handleSearch = search => {
    this.setState({ search });
  };

  handleChange = (nextState, option) => {
    const { multiple, onChange } = this.props;
    const { value: oldValue } = this.state;
    let nextValue = null;

    if (multiple) {
      nextValue = nextState
        ? [...oldValue, option.value]
        : oldValue.filter(item => item !== option.value);
    } else {
      nextValue = option.value;
    }
    this.setState({ value: nextValue }, () => {
      onChange(nextValue);
    });
  };

  renderCatelogue = catelogue => (
    catelogue.options.length ? [
      <div className="catelogue" key={catelogue.label}>{catelogue.label}</div>,
      catelogue.options.map(option => this.renderCheckboxOption(option)),
    ] : null
  );

  renderCheckboxOption = option => (
    <Checkbox
      key={option.value}
      label={option.value}
      disabled={option.disabled}
      checked={this.isOptionChecked(option)}
      onChange={nextState => this.handleChange(nextState, option)}
    >
      <OverflowTooltip className="checkbox-label" content={option.label}>
        <span>{option.label}</span>
      </OverflowTooltip>
    </Checkbox>
  );

  renderNormalOptions = () => {
    const { options } = this.props;
    return options.map(option => (
      option.options
        ? this.renderCatelogue(option)
        : this.renderCheckboxOption(option)
    ));
  };

  renderSearchedOptions = () => {
    const { intl } = this.props;
    const { searchedOptions } = this;
    return searchedOptions.length
      ? searchedOptions.map(option => (
        this.renderCheckboxOption(option)
      ))
      : <div className="no-data">{intl.formatMessage(strings.NO_DATA)}</div>;
  };

  renderOptions = () => {
    const { options } = this.props;
    const { search } = this.state;
    return !search && options.length
      /* 当没有search，正常展示带分类的选项 */
      ? this.renderNormalOptions()
      /* 当有search，则平铺过滤选项 */
      : this.renderSearchedOptions();
  };

  renderDropDown = () => {
    const { searchPlaceholder, options, intl } = this.props;
    const { search } = this.state;
    return (
      <Fragment>
        <Input
          className="search-checkbox"
          prefixIcon="icon-superset-search"
          value={search}
          placeholder={searchPlaceholder}
          onChange={this.handleSearch}
        />
        {options.length ? this.renderOptions() : (
          <div className="no-data">{intl.formatMessage(strings.NO_DATA)}</div>
        )}
      </Fragment>
    );
  };

  render() {
    const { className, trigger, placement, appendToBody, label, popperProps } = this.props;
    return (
      <div className={classnames('checkbox-icon-filter', className)}>
        <span>{label}</span>
        <Popover
          appendToBody={appendToBody}
          placement={placement}
          trigger={trigger}
          content={this.renderDropDown()}
          popperClass="checkbox-icon-filter-popper"
          popperProps={popperProps}
        >
          <i className="icon-superset-filter" />
        </Popover>
      </div>
    );
  }
}
