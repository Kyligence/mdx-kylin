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
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { Menu, Button, Tooltip, MessageBox } from 'kyligence-ui-react';

import './index.less';
import { Connect, InjectIntl } from '../../store';
import { datasetListUrl, datasetRoleListUrl } from './handler';
import { strings, SVGIcon } from '../../constants';
import ProjectSelector from '../ProjectSelector/ProjectSelector';
import {
  LoginLogo,
  CollapseMenuButton,
  LanguageSwitcher,
  ProfileButton,
} from './partition';

export default
@Connect({
  mapReselect: {
    isAdminMode: reselect => reselect.system.isAdminMode,
    isLoginMode: reselect => reselect.system.isLoginMode,
    canManageSystem: reselect => reselect.system.canManageSystem,
  },
  mapState: {
    locale: state => state.system.language.locale,
    currentUser: state => state.system.currentUser,
    isMenuCollapsed: state => state.global.isMenuCollapsed,
    isNoPageHeader: state => state.global.isNoPageHeader,
    isDatasetEdit: state => state.global.isDatasetEdit,
  },
})
@InjectIntl()
class PageHeader extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    boundGlobalActions: PropTypes.object.isRequired,
    boundSystemActions: PropTypes.object.isRequired,
    boundUpgradeActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    locale: PropTypes.string.isRequired,
    currentUser: PropTypes.object.isRequired,
    isMenuCollapsed: PropTypes.bool.isRequired,
    isAdminMode: PropTypes.bool.isRequired,
    isLoginMode: PropTypes.bool.isRequired,
    isNoPageHeader: PropTypes.bool.isRequired,
    isDatasetEdit: PropTypes.bool.isRequired,
    canManageSystem: PropTypes.bool.isRequired,
    history: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);
    this.handleLogout = this.handleLogout.bind(this);
    this.handleUpgrade = this.handleUpgrade.bind(this);
    this.handleClickAbout = this.handleClickAbout.bind(this);
    this.handleCollapseMenu = this.handleCollapseMenu.bind(this);
    this.handleChangeLanguage = this.handleChangeLanguage.bind(this);
    this.handleClickManagement = this.handleClickManagement.bind(this);
  }

  get collapseIcon() {
    const { isMenuCollapsed } = this.props;

    return !isMenuCollapsed
      ? 'icon-superset-grid_01'
      : 'icon-superset-grid_02';
  }

  get docsUrl() {
    const { locale } = this.props;
    switch (locale) {
      case 'en': return 'https://docs.kyligence.io/books/mdx/v1.3/en/index.html';
      case 'zh': return 'https://docs.kyligence.io/books/mdx/v1.3/zh-cn/index.html';
      default: return 'https://docs.kyligence.io/books/mdx/v1.3/en/index.html';
    }
  }

  get upgradeTooltip() {
    const { intl } = this.props;
    return intl.formatMessage(strings.DATASET_UPGRADE_1_1_4);
  }

  showLostConfirm = async () => {
    const { boundGlobalActions, intl } = this.props;
    const messageContent = intl.formatMessage(strings.SWITCH_LOST_EDIT);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    await MessageBox.confirm(messageContent, messageTitle, messageOptions);
    boundGlobalActions.toggleFlag('isDatasetEdit', false);
    boundGlobalActions.toggleFlag('isSemanticEdit', false);
  }

  toggleToAdminMode() {
    const { boundGlobalActions, history } = this.props;
    boundGlobalActions.toggleFlag('isShowEnterAdmin', true);

    setTimeout(() => {
      history.push(datasetRoleListUrl);
      boundGlobalActions.toggleFlag('isShowEnterAdmin', false);
    }, 2000);
  }

  toggleToCommonMode() {
    const { boundGlobalActions, history } = this.props;

    boundGlobalActions.toggleFlag('isShowLeaveAdmin', true);

    setTimeout(() => {
      history.push(datasetListUrl);
      boundGlobalActions.toggleFlag('isShowLeaveAdmin', false);
    }, 2000);
  }

  handleCollapseMenu() {
    const { boundGlobalActions, isMenuCollapsed } = this.props;
    boundGlobalActions.setMenuCollapsed(!isMenuCollapsed);
  }

  handleClickAbout() {
    const { boundModalActions } = this.props;
    boundModalActions.showModal('AboutModal');
  }

  async handleLogout() {
    const { boundSystemActions, isDatasetEdit } = this.props;

    try {
      if (isDatasetEdit) {
        await this.showLostConfirm();
      }
      await boundSystemActions.logout();
    } catch {}
  }

  async handleChangeLanguage(locale) {
    const { boundSystemActions } = this.props;
    await boundSystemActions.getLanguage(locale);
  }

  async handleClickManagement() {
    const { isAdminMode, isDatasetEdit } = this.props;

    try {
      if (isDatasetEdit) {
        await this.showLostConfirm();
      }
      if (isAdminMode) {
        this.toggleToCommonMode();
      } else {
        this.toggleToAdminMode();
      }
    } catch {}
  }

  handleUpgrade() {
    const { boundUpgradeActions } = this.props;
    boundUpgradeActions.upgradeAllDatasets();
  }

  render() {
    const {
      isLoginMode, currentUser, locale, isAdminMode, intl, history, isNoPageHeader,
    } = this.props;
    const { canManageSystem } = this.props;
    const { upgradeTooltip } = this;
    const menuTheme = isLoginMode ? 'dark' : 'light';
    const adminButtonClass = isAdminMode ? 'is-selected' : null;

    return !isNoPageHeader ? (
      <Menu className="page-header" mode="horizontal" theme={menuTheme}>
        <div className="page-header-left">
          {isLoginMode ? (
            <LoginLogo />
          ) : (
            <Fragment>
              <CollapseMenuButton
                className="header-item"
                onClick={this.handleCollapseMenu}
                icon={this.collapseIcon}
              />
              {!isAdminMode && <ProjectSelector className="header-item" history={history} />}
            </Fragment>
          )}
        </div>
        <div className="page-header-right">
          {!isLoginMode && canManageSystem && (
            <div style={{ display: 'inline-block' }}>
              {isAdminMode && (
                <Tooltip
                  className="upgrade"
                  popperClass="upgrade-tips"
                  positionFixed
                  content={upgradeTooltip}
                  style={{ verticalAlign: 'middle' }}
                >
                  <Button
                    square
                    className="header-button"
                    type="secondary"
                    icon="icon-superset-table_refresh"
                    onClick={this.handleUpgrade}
                  />
                </Tooltip>
              )}
              <Tooltip
                positionFixed
                style={{ verticalAlign: 'middle' }}
                content={isAdminMode
                  ? intl.formatMessage(strings.EXIT_MANAGEMENT)
                  : intl.formatMessage(strings.ENTER_MANAGEMENT)}
              >
                <Button size="small" type="secondary" className={classnames('mdx-it-management-button header-button', adminButtonClass)} onClick={this.handleClickManagement}>
                  <SVGIcon.SystemConfig />
                </Button>
              </Tooltip>
            </div>
          )}
          <LanguageSwitcher onClick={this.handleChangeLanguage} language={locale} />
          {!isLoginMode
            ? <ProfileButton currentUser={currentUser} onLogout={this.handleLogout} intl={intl} />
            : null}
        </div>
      </Menu>
    ) : null;
  }
}
