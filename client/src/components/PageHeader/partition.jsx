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
import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { Dropdown, Button } from 'kyligence-ui-react';

import { strings, SVGIcon } from '../../constants';
import logoImageUrl from '../../assets/img/logo.png';

export function LoginLogo() {
  return (
    <div className="logo">
      <img src={logoImageUrl} alt="logo" />
      <span> MDX</span>
    </div>
  );
}

export function CollapseMenuButton(props) {
  const { onClick, icon, className } = props;
  const buttonClass = classnames('side-menu-toggle', className);
  return (
    <div role="button" tabIndex="0" className={buttonClass} onClick={onClick} onKeyUp={onClick}>
      <i className={icon} />
    </div>
  );
}

export function LanguageSwitcher(props) {
  const { onClick } = props;
  return (
    <Dropdown
      className="language-button mdx-it-language-button"
      menu={(
        <Dropdown.Menu>
          <Dropdown.Item className="mdx-it-language-en" onClick={() => onClick('en')}>
            En
          </Dropdown.Item>
          <Dropdown.Item className="mdx-it-language-zh" onClick={() => onClick('zh')}>
            中
          </Dropdown.Item>
        </Dropdown.Menu>
      )}
    >
      <Button size="small" type="secondary" className="header-button">
        <SVGIcon.Language />
      </Button>
    </Dropdown>
  );
}

export function ProfileButton(props) {
  const { intl, currentUser, onLogout } = props;

  return (
    <Dropdown
      className="profile-button mdx-it-profile-button"
      menu={(
        <Dropdown.Menu>
          <Dropdown.Item className="mdx-it-logout" onClick={onLogout}>{intl.formatMessage(strings.LOGOUT)}</Dropdown.Item>
        </Dropdown.Menu>
      )}
    >
      <span className="el-dropdown-link">
        <span className="username mdx-it-logged-in-username">{currentUser.username}</span>
        <i className="icon-superset-more more" />
      </span>
    </Dropdown>
  );
}

export function HelpButton(props) {
  const { docsUrl, onClickAbout, intl } = props;

  return (
    <Dropdown
      className="mdx-it-help-button"
      menu={(
        <Dropdown.Menu>
          <Dropdown.Item className="mdx-it-dropdown-manual">
            <a className="link" target="_blank" rel="noopener noreferrer nofollow" href={docsUrl}>
              {intl.formatMessage(strings.KYLIN_INSIGHT_MANUAL)}
            </a>
          </Dropdown.Item>
          <Dropdown.Item className="mdx-it-dropdown-about" onClick={onClickAbout}>
            {intl.formatMessage(strings.ABOUT_KYLIN_INSIGHT)}
          </Dropdown.Item>
        </Dropdown.Menu>
      )}
    >
      <Button size="small" type="secondary" className="header-button">
        <SVGIcon.Help />
      </Button>
    </Dropdown>
  );
}

CollapseMenuButton.propTypes = {
  onClick: PropTypes.func.isRequired,
  icon: PropTypes.string.isRequired,
  className: PropTypes.string,
};

CollapseMenuButton.defaultProps = {
  className: '',
};

LanguageSwitcher.propTypes = {
  onClick: PropTypes.func.isRequired,
};

ProfileButton.propTypes = {
  intl: PropTypes.object.isRequired,
  onLogout: PropTypes.func.isRequired,
  currentUser: PropTypes.object.isRequired,
};

HelpButton.propTypes = {
  onClickAbout: PropTypes.func.isRequired,
  docsUrl: PropTypes.string.isRequired,
  intl: PropTypes.object.isRequired,
};
