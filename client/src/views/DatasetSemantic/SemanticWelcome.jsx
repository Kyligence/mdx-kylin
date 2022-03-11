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

import { strings } from '../../constants';
import Block from '../../components/Block/Block';
import { InjectIntl } from '../../store';

export default
@InjectIntl()
class SemanticWelcome extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
  };

  get semanticNotices() {
    const { intl } = this.props;

    const hierachyButton = this.getCreateButton('icon-superset-add_hierachy');
    const namedSetButton = this.getCreateButton('icon-superset-add_named_set');
    const cMeasureButton = this.getCreateButton('icon-superset-add_calculated_measure');

    return [
      intl.formatMessage(strings.SEMANTIC_WELCOME_1),
      intl.formatMessage(strings.SEMANTIC_WELCOME_2),
      intl.formatMessage(strings.SEMANTIC_WELCOME_3),
      intl.formatMessage(strings.SEMANTIC_WELCOME_4, { button: hierachyButton }),
      intl.formatMessage(strings.SEMANTIC_WELCOME_5, { button: namedSetButton }),
      intl.formatMessage(strings.SEMANTIC_WELCOME_6, { button: cMeasureButton }),
    ];
  }

  /* eslint-disable class-methods-use-this */
  getCreateButton(iconClass) {
    return (
      <i className={classnames('create-button', iconClass)} />
    );
  }

  /* eslint-disable react/no-array-index-key */
  render() {
    const { intl } = this.props;
    const { semanticNotices } = this;
    return (
      <Block
        className="semantic-welcome"
        header={intl.formatMessage(strings.TIPS)}
      >
        <div className="description">
          <i className="description-icon icon-superset-infor" />
          <span className="description-text">
            {intl.formatMessage(strings.SEMANTIC_NOTICE_WARN)}
          </span>
        </div>
        <div className="steps-desc">
          {semanticNotices.map((notice, index) => (
            <div key={index} className="step-desc">{notice}</div>
          ))}
        </div>
      </Block>
    );
  }
}
