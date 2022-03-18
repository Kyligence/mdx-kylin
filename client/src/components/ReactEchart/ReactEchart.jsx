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
import * as echarts from 'echarts';
import ResizeObserver from 'resize-observer-polyfill';
import throttle from 'lodash/throttle';

import './index.less';
import { domHelper } from '../../utils';

export default class ReactEchart extends PureComponent {
  static propTypes = {
    option: PropTypes.object.isRequired,
  };

  chartRef = React.createRef();
  echart = null;

  componentDidMount() {
    const { option } = this.props;
    const { current: $chart } = this.chartRef;

    this.echart = echarts.init($chart);
    this.echart.setOption(option);

    this.handleResizeThrottle = throttle(this.handleResize, 500);
    this.handleMouseMoveThrottle = throttle(this.handleMouseMove, 500);

    new ResizeObserver(this.handleResizeThrottle).observe($chart);
    window.addEventListener('mousemove', this.handleMouseMoveThrottle);
  }

  componentDidUpdate() {
    const { option } = this.props;
    this.echart.clear();
    this.echart.setOption(option);
  }

  componentWillUnmount() {
    window.removeEventListener('mousemove', this.handleMouseMoveDebounce);
  }

  handleResize = ([entry]) => {
    const { width, height } = entry.contentRect;
    this.echart.resize({ width, height });
  };

  handleMouseMove = event => {
    const parentNodes = domHelper.findParents(event.target);
    if (!parentNodes.includes(this.chartRef.current)) {
      this.echart.dispatchAction({ type: 'hideTip' });
    }
  };

  render() {
    return <div className="echarts" ref={this.chartRef} />;
  }
}
