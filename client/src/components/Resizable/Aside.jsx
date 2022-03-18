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

const EMPTY_FUNC = () => {};

export default class Aside extends PureComponent {
  static propTypes = {
    children: PropTypes.node,
    parent: PropTypes.object,
    className: PropTypes.string,
    minWidth: PropTypes.number,
    maxWidth: PropTypes.number,
    onResize: PropTypes.func,
  };

  static defaultProps = {
    className: '',
    minWidth: 0,
    maxWidth: 0,
    parent: null,
    children: null,
    onResize: EMPTY_FUNC,
  };

  $el = React.createRef();
  $handler = React.createRef();
  isDrag = false;
  startX = 0;
  startAsideWidth = 0;

  state = {
    width: 0,
  };

  constructor(props) {
    super(props);
    this.handleResize = this.handleResize.bind(this);
    this.handleMouseUp = this.handleMouseUp.bind(this);
    this.handleMouseDown = this.handleMouseDown.bind(this);
  }

  componentDidMount() {
    window.addEventListener('mousedown', this.handleMouseDown);
    window.addEventListener('mouseup', this.handleMouseUp);
    window.addEventListener('mousemove', this.handleResize);
  }

  componentWillUnmount() {
    window.removeEventListener('mousedown', this.handleMouseDown);
    window.removeEventListener('mouseup', this.handleMouseUp);
    window.removeEventListener('mousemove', this.handleResize);
  }

  get style() {
    const { width } = this.state;
    return {
      width: width ? `${width}px` : null,
    };
  }

  handleMouseDown(event) {
    const { $el, $handler } = this;
    const { target } = event;

    if (target === $handler.current) {
      this.isDrag = true;
      this.startX = event.pageX;
      this.startAsideWidth = $el.current.getBoundingClientRect().width;

      const { parent } = this.props;
      const { $content } = parent;
      if ($content.current) {
        this.startContentWidth = $content.current.$el.current.getBoundingClientRect().width;
      }
    }
  }

  handleMouseUp() {
    if (this.isDrag) {
      this.isDrag = false;
      this.startX = 0;
      this.startAsideWidth = 0;
      this.startContentWidth = 0;
    }
  }

  handleResize(event) {
    if (this.isDrag) {
      const { parent, minWidth, maxWidth: inputMaxWidth, onResize } = this.props;
      const { $content, $el } = parent;
      const containerWidth = $el.current.getBoundingClientRect().width;
      const maxWidth = (inputMaxWidth < containerWidth && inputMaxWidth)
        ? inputMaxWidth
        : containerWidth;
      const offsetX = event.pageX - this.startX;

      if (this.startAsideWidth + offsetX < maxWidth && this.startAsideWidth + offsetX > minWidth) {
        const width = this.startAsideWidth + offsetX;
        this.setState({ width });
        onResize(width);
        if ($content.current) {
          $content.current.setState({ width: this.startContentWidth - offsetX });
        }
      }
    }
  }

  render() {
    const { $el, $handler } = this;
    const { children, className } = this.props;
    const classname = classnames('resize-aside', className);
    return (
      <div ref={$el} className={classname} style={this.style}>
        {children}
        <div ref={$handler} className="resize-handler">||</div>
      </div>
    );
  }
}
