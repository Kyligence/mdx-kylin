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
