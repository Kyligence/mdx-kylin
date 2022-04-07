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
