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

export default class Block extends PureComponent {
  static propTypes = {
    header: PropTypes.oneOfType([
      PropTypes.node,
      PropTypes.string,
    ]),
    children: PropTypes.oneOfType([
      PropTypes.node,
      PropTypes.string,
    ]),
    footer: PropTypes.oneOfType([
      PropTypes.node,
      PropTypes.string,
    ]),
    className: PropTypes.string,
  };

  static defaultProps = {
    className: '',
    header: null,
    children: null,
    footer: null,
  };

  render() {
    const { header, children, footer, className } = this.props;
    const blockClass = classnames(['block', className]);
    return (
      <div className={blockClass}>
        {header && (
          <div className="block-header clearfix font-medium">{header}</div>
        )}
        <div className="block-body">{children}</div>
        {footer && (
          <div className="block-footer clearfix">{footer}</div>
        )}
      </div>
    );
  }
}
