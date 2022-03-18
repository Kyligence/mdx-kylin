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
import { AutoComplete } from 'kyligence-ui-react';

import './index.less';
import { InjectIntl } from '../../store';
import { strings } from '../../constants';

export default
@InjectIntl()
class SuggestionFilterInput extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    options: PropTypes.array.isRequired,
    triggerOnFocus: PropTypes.bool,
    className: PropTypes.string,
    onSelect: PropTypes.func,
    placeholder: PropTypes.string,
  };

  static defaultProps = {
    triggerOnFocus: false,
    className: '',
    placeholder: '',
    onSelect: () => {},
  };

  state = {
    filterInput: '',
  };

  handleFilterInput = value => {
    this.setState({ filterInput: value });
  }

  handleFilterOptions = (queryString, callback) => {
    const { options } = this.props;
    callback(options);
  }

  handleSelectOption = option => {
    const { onSelect } = this.props;
    const { filterInput } = this.state;
    onSelect(option.value, filterInput);
    this.setState({ filterInput: '' });
  }

  renderFilterOptions = option => {
    const { intl } = this.props;
    const { filterInput } = this.state;
    return (
      <div className="search-option">
        <div className="search-label">
          <span>{intl.formatMessage(strings.SEARCH_BY)}</span>
          <span>{option.item.label}</span>
          <span>{intl.formatMessage(strings.SEMICOLON)}</span>
        </div>
        <div className="search-value">{filterInput}</div>
      </div>
    );
  }

  render() {
    const { triggerOnFocus, className, placeholder } = this.props;
    const { filterInput } = this.state;
    return (
      <AutoComplete
        className={classnames('suggestion-filter-input', className)}
        prefixIcon="icon-superset-search"
        value={filterInput}
        triggerOnFocus={triggerOnFocus}
        placeholder={placeholder}
        fetchSuggestions={this.handleFilterOptions}
        customItem={this.renderFilterOptions}
        onChange={this.handleFilterInput}
        onSelect={this.handleSelectOption}
      />
    );
  }
}
