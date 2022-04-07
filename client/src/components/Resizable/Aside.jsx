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
