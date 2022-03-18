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
