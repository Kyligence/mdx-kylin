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

import './index.less';

export default class BlockButton extends PureComponent {
  static propTypes = {
    currentValue: PropTypes.any,
    value: PropTypes.any,
    icon: PropTypes.string,
    onInput: PropTypes.func,
    children: PropTypes.node,
    disabled: PropTypes.bool,
  };

  static defaultProps = {
    icon: '',
    currentValue: null,
    value: null,
    onInput: () => {},
    children: [],
    disabled: false,
  };

  constructor(props) {
    super(props);
    this.handleInput = this.handleInput.bind(this);
  }

  get warpperClass() {
    const { currentValue, value, disabled } = this.props;
    return classnames('block-button-warpper', { active: currentValue === value, disabled });
  }

  get iconClass() {
    const { icon } = this.props;
    return classnames(['icon', icon]);
  }

  handleInput() {
    const { onInput, value, currentValue, disabled } = this.props;
    if (onInput && currentValue !== value && !disabled) {
      onInput(value);
    }
  }

  render() {
    const { children } = this.props;
    return (
      <div className={this.warpperClass}>
        <div className="block-button" onClick={this.handleInput}>
          <i className={this.iconClass} />
        </div>
        <div className="block-button-text">{children}</div>
      </div>
    );
  }
}
