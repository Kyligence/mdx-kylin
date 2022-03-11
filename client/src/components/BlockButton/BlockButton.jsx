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
