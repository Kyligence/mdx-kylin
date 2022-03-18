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
import Menu from 'antd/lib/menu';
import Icon from 'antd/lib/icon';
import Layout from 'antd/lib/layout';
import classnames from 'classnames';
import { MessageBox } from 'kyligence-ui-react';

import { Connect, InjectIntl } from '../../store';
import './index.less';
import { configs, strings } from '../../constants';
import { ReactComponent as FullLogoImageUrl } from '../../assets/img/sidebar-full-logo.svg';
import shortLogoImageUrl from '../../assets/img/sidebar-short-logo.png';

export default
@Connect({
  mapState: {
    global: state => state.global,
    isMenuCollapsed: state => state.global.isMenuCollapsed,
    isNoMenubar: state => state.global.isNoMenubar,
  },
  mapReselect: {
    menus: reselect => reselect.system.menus,
    currentSite: reselect => reselect.system.currentSite,
  },
})
@InjectIntl()
class PageMenu extends PureComponent {
  static propTypes = {
    boundGlobalActions: PropTypes.object.isRequired,
    isMenuCollapsed: PropTypes.bool.isRequired,
    isNoMenubar: PropTypes.bool.isRequired,
    menus: PropTypes.array.isRequired,
    currentSite: PropTypes.object.isRequired,
    global: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);
    this.handleClickMenu = this.handleClickMenu.bind(this);
  }

  async showLostAlert(menuUrl) {
    const { boundGlobalActions, intl, history } = this.props;
    const messageContent = intl.formatMessage(strings.SWITCH_LOST_EDIT);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    await MessageBox.confirm(messageContent, messageTitle, messageOptions);
    boundGlobalActions.disableMenuPreventFlag();
    history.push(menuUrl);
  }

  handleClickMenu(event) {
    const { global, history } = this.props;
    const menuUrl = event.item.props.link;

    const isPreventMenu = configs.preventMenuFlagList.some(flagName => {
      if (global[flagName]) {
        this.showLostAlert(menuUrl);
        return true;
      }
      return false;
    });

    if (!isPreventMenu) {
      history.push(menuUrl);
    }
  }

  renderMenuLogo() {
    const { isMenuCollapsed } = this.props;
    return (
      <div className="logo">
        { !isMenuCollapsed ? (
          <FullLogoImageUrl className="full-logo" />
        ) : (
          <img className="short-logo" src={shortLogoImageUrl} alt="logo" />
        ) }
      </div>
    );
  }

  renderMenuIcon = icon => {
    if (icon && typeof icon === 'string') {
      return <Icon type={`null ${icon}`} />;
    }
    if (icon && typeof icon === 'object') {
      const SVGIcon = icon;
      return <Icon type="null" component={() => <SVGIcon />} />;
    }
    return null;
  }

  renderMenuItem(menu) {
    const { intl } = this.props;
    return (
      <Menu.Item key={menu.menuText.id} link={menu.url}>
        <div className="content">
          {this.renderMenuIcon(menu.icon)}
          <span>{intl.formatMessage(menu.menuText)}</span>
        </div>
      </Menu.Item>
    );
  }

  renderSubMenu(menu, menuIndex) {
    const { intl } = this.props;

    const subMenuTitle = (
      <span>
        {this.renderMenuIcon(menu.icon)}
        <span>{intl.formatMessage(menu.menuText)}</span>
      </span>
    );

    return (
      <Menu.SubMenu key={menuIndex} title={subMenuTitle}>
        {menu.children.map((menuItem, menuItemIndex) => (
          this.renderMenuItem(menuItem, menuItemIndex)
        ))}
      </Menu.SubMenu>
    );
  }

  render() {
    const { isMenuCollapsed, menus = [], currentSite, isNoMenubar } = this.props;
    const pageMenuClass = classnames('page-menu', { collapsed: isMenuCollapsed });
    const activeUrlKey = [
      (currentSite.menuText && currentSite.menuText.id) ||
      (currentSite.belongTo && currentSite.belongTo.id),
    ];

    return !isNoMenubar ? (
      <Layout className={pageMenuClass}>
        <Layout.Sider collapsed={isMenuCollapsed} width="184px" collapsedWidth="64px">
          {this.renderMenuLogo()}
          {menus.length ? (
            <Menu
              mode="inline"
              theme="dark"
              inlineIndent={12}
              onClick={this.handleClickMenu}
              selectedKeys={activeUrlKey}
              defaultOpenKeys={activeUrlKey}
              defaultSelectedKeys={activeUrlKey}
            >
              {menus.map((menu, menuIndex) => {
                if (menu.children.length) {
                  return this.renderSubMenu(menu, menuIndex);
                }
                return this.renderMenuItem(menu, menuIndex);
              })}
            </Menu>
          ) : null}
        </Layout.Sider>
      </Layout>
    ) : null;
  }
}
