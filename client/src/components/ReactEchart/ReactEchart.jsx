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
