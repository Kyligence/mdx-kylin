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
import debounce from 'lodash/debounce';
import { Button } from 'kyligence-ui-react';

import './index.less';

// Rewrite from: https://github.com/zoltantothcom/react-clamp-lines
export default class ClampLines extends PureComponent {
  static propTypes = {
    text: PropTypes.string.isRequired,
    lines: PropTypes.number,
    ellipsis: PropTypes.string,
    moreText: PropTypes.string,
    lessText: PropTypes.string,
    className: PropTypes.string,
    delay: PropTypes.number,
    stopPropagation: PropTypes.bool,
    tailSpace: PropTypes.number,
  };

  static defaultProps = {
    lines: 3,
    ellipsis: '...',
    moreText: 'Read more',
    lessText: 'Read less',
    delay: 300,
    className: '',
    stopPropagation: false,
    tailSpace: 12,
  };

  state = {
    expanded: false,
    noClamp: false,
    text: '',
  };

  $text = React.createRef();
  original = '';
  watch = true;
  lineHeight = 0;
  start = 0;
  middle = 0;
  end = 0;
  // If window is undefined it means the code is executed server-side
  ssr = typeof window === 'undefined';

  constructor(props) {
    super(props);

    this.original = props.text;
    this.state.text = props.text.substring(0, 20);

    if (!this.ssr) {
      this.handleDebounceResize = debounce(this.handleResize, props.delay);
    }
  }

  componentDidMount() {
    const { text } = this.props;
    if (text && !this.ssr) {
      this.lineHeight = this.$text.current.clientHeight + 1;
      this.clampLines();

      if (this.watch) {
        window.addEventListener('resize', this.handleDebounceResize);
      }
    }
  }

  componentDidUpdate(prevProps) {
    const { text: oldText } = prevProps;
    const { text: newText } = this.props;

    if (oldText !== newText) {
      this.original = newText;
      this.clampLines();
    }
  }

  componentWillUnmount() {
    if (!this.ssr) {
      window.removeEventListener('resize', this.handleDebounceResize);
    }
  }

  get ellipsis() {
    const { noClamp } = this.state;
    const { ellipsis } = this.props;
    return this.watch && !noClamp ? ellipsis : '';
  }

  handleResize = () => {
    const { expanded } = this.state;
    if (this.watch) {
      this.setState({ noClamp: false });
      this.clampLines();
      this.setState({ expanded: !expanded });
    }
  }

  clampLines = () => {
    if (this.$text.current) {
      const { lines, tailSpace } = this.props;
      this.setState({ text: '' });

      const maxHeight = this.lineHeight * lines + 1;

      this.start = 0;
      this.middle = 0;
      this.end = this.original.length;

      while (this.start <= this.end) {
        this.middle = Math.floor((this.start + this.end) / 2);
        this.$text.current.innerText = this.original.slice(0, this.middle);
        if (this.middle === this.original.length) {
          this.setState({
            text: this.original,
            noClamp: true,
          });
          return;
        }

        this.moveMarkers(maxHeight);
      }

      this.$text.current.innerText =
        this.original.slice(0, this.middle - 5 - tailSpace) + this.ellipsis;
      this.setState({
        text: this.original.slice(0, this.middle - 5 - tailSpace) + this.ellipsis,
      });
    }
  };

  moveMarkers = maxHeight => {
    if (this.$text.current.clientHeight <= maxHeight) {
      this.start = this.middle + 1;
    } else {
      this.end = this.middle - 1;
    }
  };

  handleToggleExpand = event => {
    const { stopPropagation } = this.props;
    const { expanded } = this.state;

    if (event) {
      event.preventDefault();
      if (stopPropagation) {
        event.stopPropagation();
      }
    }

    this.watch = !this.watch;
    if (this.watch) {
      this.clampLines();
    } else {
      this.setState({ text: this.original });
    }

    this.setState({ expanded: !expanded });
  };

  renderButton = () => {
    const { expanded, noClamp } = this.state;
    const { moreText, lessText } = this.props;

    return !noClamp && moreText && lessText ? (
      <Button type="text" className="clamp-lines-more" onClick={this.handleToggleExpand}>
        {this.watch ? moreText : lessText}
        <i className={classnames({ 'icon-superset-more_01': true, expanded })} />
      </Button>
    ) : null;
  }

  render() {
    const { text: propText, className } = this.props;
    const { text: stateText } = this.state;

    return propText ? (
      <div className={classnames('clamp-lines', className)}>
        <div className="clamp-lines-text" ref={this.$text}>
          {stateText}
        </div>
        {this.renderButton()}
      </div>
    ) : null;
  }
}
