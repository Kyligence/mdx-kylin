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

import DiagnosisProgress from '../DiagnosisProgress/DiagnosisProgress';

export default class DiagnosisProgressList extends PureComponent {
  static propTypes = {
    clusters: PropTypes.array.isRequired,
    startAt: PropTypes.number.isRequired,
    endAt: PropTypes.number.isRequired,
    onRetry: PropTypes.func,
    onFinish: PropTypes.func,
  };

  static defaultProps = {
    onRetry: () => {},
    onFinish: () => {},
  };

  state = {
    clusters: [],
    finishedClusters: [],
  };

  constructor(props) {
    super(props);
    this.handleRetryPackage = this.handleRetryPackage.bind(this);
    this.handleFinishPackage = this.handleFinishPackage.bind(this);
  }

  componentDidMount() {
    const { clusters } = this.props;
    this.setState({ clusters });
  }

  get isAllFinished() {
    const { finishedClusters, clusters } = this.state;
    return clusters.every(cluster => finishedClusters.some(finishedCluster => (
      cluster.host === finishedCluster.host &&
      cluster.port === finishedCluster.port
    )));
  }

  handleRetryPackage(cluster) {
    const { onRetry } = this.props;
    const { finishedClusters } = this.state;

    this.setState({
      finishedClusters: finishedClusters.filter(
        finishedCluster => (
          finishedCluster.host !== cluster.host &&
          finishedCluster.port !== cluster.port
        ),
      ),
    });
    onRetry(cluster);
  }

  handleFinishPackage(cluster) {
    const { onFinish } = this.props;
    const { finishedClusters } = this.state;
    this.setState({ finishedClusters: [...finishedClusters, cluster] }, () => {
      if (this.isAllFinished) { onFinish(cluster); }
    });
  }

  render() {
    const { startAt, endAt } = this.props;
    const { clusters } = this.state;

    return clusters.map(cluster => (
      <DiagnosisProgress
        key={`${cluster.host}:${cluster.port}`}
        startAt={startAt}
        endAt={endAt}
        cluster={cluster}
        onRetry={this.handleRetryPackage}
        onFinish={this.handleFinishPackage}
      />
    ));
  }
}
