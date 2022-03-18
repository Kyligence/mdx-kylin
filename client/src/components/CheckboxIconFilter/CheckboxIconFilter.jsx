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
