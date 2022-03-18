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
