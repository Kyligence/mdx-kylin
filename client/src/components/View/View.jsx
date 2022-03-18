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
import { CSSTransition } from 'react-transition-group';

export default class View extends PureComponent {
  static propTypes = {
    isShow: PropTypes.bool,
    enter: PropTypes.bool,
    leave: PropTypes.bool,
    duration: PropTypes.number,
    classNames: PropTypes.string,
    children: PropTypes.oneOfType([
      PropTypes.node,
      PropTypes.string,
    ]),
  };

  static defaultProps = {
    isShow: true,
    duration: 0,
    enter: true,
    leave: true,
    classNames: '',
    children: null,
  };

  render() {
    const { children, isShow, duration, classNames, enter, leave } = this.props;

    return (
      <CSSTransition
        unmountOnExit
        in={isShow}
        timeout={duration}
        classNames={classNames}
        enter={enter}
        exit={leave}
      >
        {children}
      </CSSTransition>
    );
  }
}
