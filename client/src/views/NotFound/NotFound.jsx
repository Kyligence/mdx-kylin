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
import { Button } from 'kyligence-ui-react';

import './index.less';
import { strings } from '../../constants';
import { dataHelper } from '../../utils';
import { Connect, InjectIntl } from '../../store';

export default
@Connect()
@InjectIntl()
class NotFound extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
  };

  static getSearch(name) {
    return new window.URLSearchParams(window.location.search).get(name);
  }

  state = {
    step: +NotFound.getSearch('step') || 1000,
    duration: +NotFound.getSearch('duration') || 0,
    fallbackUrl: decodeURIComponent(NotFound.getSearch('fallbackUrl') || ''),
    messageId: encodeURIComponent(NotFound.getSearch('messageId') || ''),
    icon: encodeURIComponent(NotFound.getSearch('icon') || ''),
    pageId: decodeURIComponent(NotFound.getSearch('pageId') || ''),
    entityId: decodeURIComponent(NotFound.getSearch('entityId') || ''),
  };

  timer = null;

  componentDidMount() {
    this.setTimer();
  }

  componentWillUnmount() {
    this.unsetTimer();
  }

  setTimer = () => {
    const { step } = this.state;

    this.timer = setInterval(() => {
      const { duration } = this.state;
      const newDuration = duration - step;

      if (newDuration >= 0) {
        // 当时间大于等于0，则倒计时
        this.setState({ duration: newDuration });
      } else if (newDuration < 0) {
        // 当时间小于0，则跳转
        this.handleRedirect();
      }
    }, step);
  }

  unsetTimer = () => {
    clearInterval(this.timer);
  }

  handleRedirect = () => {
    const { history } = this.props;
    const { fallbackUrl } = this.state;
    history.push(fallbackUrl);
  }

  renderRedirect = () => {
    const { intl } = this.props;
    const { duration, pageId } = this.state;

    const pageText = dataHelper.translate(intl, pageId).toLocaleLowerCase();
    const time = dataHelper.getDurationTime(intl, duration, 1, false);

    const page = <Button type="text" key={pageText} onClick={this.handleRedirect}>{pageText}</Button>;
    const durationText = intl.formatMessage(strings.REDIRECT_AFTER_TIMER, { page, time });

    return <span>{durationText}</span>;
  }

  renderMessage = () => {
    const { intl } = this.props;
    const { messageId, entityId } = this.state;
    const entity = dataHelper.translate(intl, entityId);
    const message = dataHelper.translate(intl, messageId, { entity });

    return <span>{message}</span>;
  }

  render() {
    const { messageId, icon, pageId } = this.state;

    return (
      <div className="not-found">
        <div className="not-found-body">
          <div className="not-found-icon">
            <i className={icon} />
          </div>
          <div className="not-found-text">
            {messageId ? this.renderMessage() : null}
            {pageId ? this.renderRedirect() : null}
          </div>
        </div>
      </div>
    );
  }
}
