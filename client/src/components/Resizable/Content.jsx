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

export default class Content extends PureComponent {
  static propTypes = {
    children: PropTypes.node,
    className: PropTypes.string,
  };

  static defaultProps = {
    className: '',
    children: null,
  };

  $el = React.createRef();

  state = {
    width: 0,
  };

  componentDidMount() {
    setTimeout(() => {
      const { $el } = this;
      const { width } = $el.current.getBoundingClientRect();
      this.setState({ width });
    });
  }

  get style() {
    const { width } = this.state;
    return {
      width: width ? `${width}px` : null,
    };
  }

  render() {
    const { $el } = this;
    const { children, className } = this.props;
    const classname = classnames('resize-content', className);

    return (
      <div ref={$el} className={classname} style={this.style}>
        {children}
      </div>
    );
  }
}
