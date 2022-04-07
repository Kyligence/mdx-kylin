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
