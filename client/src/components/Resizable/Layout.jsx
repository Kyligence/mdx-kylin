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
import React, { PureComponent, Children } from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { getResizableTypes } from './handler';

export default class Layout extends PureComponent {
  static propTypes = {
    children: PropTypes.node.isRequired,
    className: PropTypes.string,
  };

  static defaultProps = {
    className: '',
  };

  $el = React.createRef();
  $aside = React.createRef();
  $content = React.createRef();

  get children() {
    const { children } = this.props;
    return Children.map(children, child => {
      const ref = getResizableTypes(child, this);
      return React.cloneElement(child, { ref, parent: this });
    });
  }

  render() {
    const { $el } = this;
    const { className } = this.props;
    const classname = classnames('resize-layout', 'clearfix', className);

    return (
      <div ref={$el} className={classname}>
        {this.children}
      </div>
    );
  }
}
