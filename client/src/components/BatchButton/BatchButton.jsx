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
