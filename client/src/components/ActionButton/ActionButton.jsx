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
import React, { PureComponent, Fragment } from 'react';
import { injectIntl as InjectIntl } from 'react-intl';
import classnames from 'classnames';
import PropTypes from 'prop-types';
import { Dropdown, Tooltip } from 'kyligence-ui-react';
import { strings } from '../../constants';

export default
@InjectIntl
class ActionButton extends PureComponent {
  static keepIconCount = 2;
  static maxIconCount = 3;

  static propTypes = {
    intl: PropTypes.object.isRequired,
    actions: PropTypes.array.isRequired,
    data: PropTypes.object.isRequired,
  };

  get displayActions() {
    const { actions } = this.props;
    return actions
      .filter(actionItem => this.getIsShow(actionItem.isShow))
      .map((actionItem, index) => ({ ...actionItem, command: String(index) }));
  }

  get isShowDropdown() {
    return this.displayActions.length > ActionButton.maxIconCount;
  }

  get buttonItems() {
    const { data } = this.props;
    const buttonItems = this.isShowDropdown
      ? this.displayActions.slice(0, ActionButton.keepIconCount)
      : this.displayActions.slice(0, ActionButton.maxIconCount);

    return buttonItems.map(action => {
      const visiable = action.visibleHandler ? action.visibleHandler(data) : true;
      return action.isShow && visiable && (
        <Tooltip
          key={action.label}
          effect="dark"
          placement="top"
          content={action.label}
          className="action-item"
          positionFixed
        >
          <i
            aria-label="button"
            role="button"
            tabIndex="0"
            className={classnames('action-icon', action.iconClass, this.getIsDisabled(data.isDisabled) && 'disabled')}
            onClick={() => action.handler && action.handler(data)}
            onKeyUp={() => action.handler && action.handler(data)}
          />
        </Tooltip>
      );
    });
  }

  get dropdownItems() {
    const { data, intl } = this.props;
    const dropdownItems = this.displayActions.slice(ActionButton.keepIconCount);
    return this.isShowDropdown && (
      <Dropdown
        className="action-item"
        trigger="click"
        onCommand={command => this.handleCommand(command)}
        menu={(
          <Dropdown.Menu positionFixed>
            {dropdownItems.map(action => {
              const visiable = action.visibleHandler ? action.visibleHandler(data) : true;
              return action.isShow && visiable && (
                <Dropdown.Item
                  key={action.label}
                  command={action.command}
                  className={action.className}
                  disabled={this.getIsDisabled(action.isDisabled)}
                >
                  {action.label}
                </Dropdown.Item>
              );
            })}
          </Dropdown.Menu>
        )}
      >
        <Tooltip
          key="more"
          effect="dark"
          placement="top"
          content={intl.formatMessage(strings.MORE)}
          className="action-item"
          positionFixed
        >
          <i className="action-icon icon-superset-table_others" />
        </Tooltip>
      </Dropdown>
    );
  }

  getIsShow(isShow) {
    const { data } = this.props;
    if (typeof isShow === 'boolean') {
      return isShow;
    }
    if (isShow) {
      return isShow(data);
    }
    return true;
  }

  getIsDisabled(isDisabled) {
    const { data } = this.props;
    if (typeof isDisabled === 'boolean') {
      return isDisabled;
    }
    if (isDisabled) {
      return isDisabled(data);
    }
    return false;
  }

  handleCommand(command) {
    const { data } = this.props;
    const currentAction = this.displayActions.find(action => action.command === command);
    if (currentAction.handler) {
      currentAction.handler(data);
    }
  }

  render() {
    return (
      <Fragment>
        {this.buttonItems}
        {this.dropdownItems}
      </Fragment>
    );
  }
}
