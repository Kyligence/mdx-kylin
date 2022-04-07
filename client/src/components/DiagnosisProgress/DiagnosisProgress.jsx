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
import { Progress, Button, Input } from 'kyligence-ui-react';
import classnames from 'classnames';

import './index.less';
import { strings } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { dataHelper } from '../../utils';
import { getPendingIdx, getRunningIdx, getSuccessIdx, getErrorIdx, getDownloadUrl } from './handler';

export default
@Connect()
@InjectIntl()
class DiagnosisProgress extends PureComponent {
  static propTypes = {
    boundDiagnosisActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    cluster: PropTypes.object.isRequired,
    startAt: PropTypes.number.isRequired,
    endAt: PropTypes.number.isRequired,
    onFinish: PropTypes.func,
    onRetry: PropTypes.func,
  };

  static defaultProps = {
    onFinish: () => {},
    onRetry: () => {},
  };

  state = {
    isShowDetail: false,
    results: [],
    downloadMs: 0,
    retryMs: 0,
  };

  constructor(props) {
    super(props);
    this.handleRetry = this.handleRetry.bind(this);
    this.toggleDetail = this.toggleDetail.bind(this);
    this.handleDownload = this.handleDownload.bind(this);
  }

  async componentDidMount() {
    this.loopAskProgress(true);
  }

  get pendingIdx() {
    return getPendingIdx(this.state);
  }

  get runningIdx() {
    return getRunningIdx(this.state);
  }

  get successIdx() {
    return getSuccessIdx(this.state);
  }

  get errorIdx() {
    return getErrorIdx(this.state);
  }

  get successCount() {
    return this.successIdx + 1;
  }

  get isAllSuccess() {
    const { results } = this.state;
    return results.length && this.successCount === results.length;
  }

  get downloadUrl() {
    return getDownloadUrl(this.state);
  }

  get isProgressStop() {
    return this.errorIdx !== -1 || this.isAllSuccess;
  }

  get progress() {
    const { results } = this.state;
    if (results.length) {
      const duringCount = results.length - 1;
      const currentDuring = [results.length - 1, results.length].includes(this.successCount)
        ? duringCount
        : this.successCount;
      return Math.floor((currentDuring / duringCount) * 100);
    }
    return 0;
  }

  get message() {
    const { intl } = this.props;
    const { results, downloadMs } = this.state;

    let message;
    if (downloadMs) {
      const timer = Math.round(downloadMs / 1000);
      message = dataHelper.translate(intl, strings.PACKAGE_WAITTING, { timer });
    } else if (results.length === 0) {
      message = null;
    } else if (this.errorIdx !== -1) {
      message = dataHelper.translate(intl, strings.PACKAGE_ERROR);
    } else if (this.pendingIdx === 0) {
      message = dataHelper.translate(intl, results[0].phase);
    } else if (this.runningIdx !== -1) {
      message = dataHelper.translate(intl, results[this.runningIdx].phase);
    } else if (this.successCount === results.length) {
      message = dataHelper.translate(intl, results[this.successIdx].phase);
    }
    return message;
  }

  get status() {
    const { results } = this.state;

    let status;
    if (results.length === 0) {
      status = null;
    } else if (this.errorIdx !== -1) {
      status = 'exception';
    } else if (this.successCount === results.length) {
      status = 'success';
    }
    return status;
  }

  getPackageState() {
    const { boundDiagnosisActions, cluster } = this.props;

    return new Promise((resolve, reject) => boundDiagnosisActions.getPackageState(cluster)
      .then(async results => this.setState({ results }, resolve))
      .catch(reject));
  }

  async loopAskProgress(isClearResults) {
    if (isClearResults) this.clearResults();

    await this.getPackageState();

    if (!this.isProgressStop) {
      setTimeout(() => this.loopAskProgress(), 2000);
    } else {
      const { onFinish, cluster } = this.props;
      onFinish(cluster);
    }
  }

  limitDownload(downloadMs, waitMs) {
    this.setState({ downloadMs });

    if (downloadMs > 0) {
      setTimeout(() => this.limitDownload(downloadMs - waitMs, waitMs), waitMs);
    }
  }

  limitRetry(retryMs, waitMs) {
    this.setState({ retryMs });

    if (retryMs > 0) {
      setTimeout(() => this.limitRetry(retryMs - waitMs, waitMs), waitMs);
    }
  }

  toggleDetail() {
    const { isShowDetail } = this.state;
    this.setState({ isShowDetail: !isShowDetail });
  }

  clearResults() {
    this.setState({ results: [] });
  }

  handleDownload() {
    const { boundDiagnosisActions, cluster } = this.props;
    const { downloadUrl } = this;
    const { host, port } = cluster;
    boundDiagnosisActions.downloadPackage({ host, port, fileName: downloadUrl });
    this.limitDownload(10000, 1000);
  }

  async handleRetry() {
    const { boundDiagnosisActions, onRetry, onFinish, cluster, startAt, endAt } = this.props;
    const { host, port } = cluster;

    this.limitRetry(5000, 1000);
    onRetry(cluster);

    try {
      await boundDiagnosisActions.retryPackage({ host, port, startAt, endAt });
      this.loopAskProgress(true);
    } catch (e) {
      onFinish(cluster);
    }
  }

  render() {
    const { progress, status, message, downloadUrl, isAllSuccess, errorIdx } = this;
    const { intl, cluster } = this.props;
    const { downloadMs, retryMs, results, isShowDetail } = this.state;
    const { host, port } = cluster;
    const canDownload = downloadMs === 0 && downloadUrl;
    const runningIdx = this.runningIdx + 1;
    const totalIdx = results.length;
    const isError = errorIdx !== -1;
    const clusterProgressClass = classnames('cluster-progress-info', isError && 'is-error');
    const showDetailClass = classnames('icon-superset-more', isShowDetail && 'is-reverse');

    return (
      <div className="diagnosis-progress clearfix">
        <div className="pull-left">
          <Button
            plain
            square
            size="small"
            type="primary"
            icon="icon-superset-download"
            onClick={this.handleDownload}
            disabled={!canDownload}
          />
        </div>
        <div className="progress-detail">
          <div className="cluster-title">{`${host}:${port}`}</div>
          <div className="cluster-progress">
            <Progress percentage={progress} status={status} />
            {isError && (
              <div className="cluster-actions">
                <Button type="text" disabled={!!retryMs} onClick={this.handleRetry}>{intl.formatMessage(strings.RETRY)}</Button>
              </div>
            )}
          </div>
          {(!isAllSuccess || !!downloadMs) && (
            <div className={clusterProgressClass}>
              {!isError && !downloadMs && (
                <span className="current-step">{runningIdx} / {totalIdx}</span>
              )}
              {message}
              {isError && (
                <span className="show-error" onClick={this.toggleDetail}>
                  {intl.formatMessage(isShowDetail ? strings.HIDE_DETAILS : strings.SHOW_DETAILS)}
                  <i className={showDetailClass} />
                </span>
              )}
            </div>
          )}
          {isShowDetail && isError && (
            <div className="cluster-error">
              <Input
                disabled
                type="textarea"
                autosize={{ minRows: 10 }}
                value={results[errorIdx].detail}
              />
            </div>
          )}
        </div>
      </div>
    );
  }
}
