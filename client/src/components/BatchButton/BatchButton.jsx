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
import { Button, Dropdown } from 'kyligence-ui-react';
import { t } from '@superset-ui/translation';

import './index.less';
import { strings } from '../../constants';

export default class BatchButton extends PureComponent {
  static propTypes = {
    actions: PropTypes.array,
    onClick: PropTypes.func,
  };

  static defaultProps = {
    actions: [],
  };

  constructor(props) {
    super(props);
    this.state = {};

    this.renderMenuItem = this.renderMenuItem.bind(this);
  }

  renderMenuItem() {
    return (
      <Dropdown.Menu>
        {this.props.actions.map(action => (
          <Dropdown.Item command={action} key={action}>{t(action)}</Dropdown.Item>
        ))}
      </Dropdown.Menu>
    );
  }

  render() {
    const { onClick, actions } = this.props;

    return !!actions.length && (
      <Dropdown className="batch-button" onCommand={onClick} menu={this.renderMenuItem()} trigger="click">
        <Button>
          {strings.ACTIONS}
          <i className="icon-superset-more more" />
        </Button>
      </Dropdown>
    );
  }
}
