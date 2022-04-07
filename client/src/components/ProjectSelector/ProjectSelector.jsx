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
import { Select, MessageBox } from 'kyligence-ui-react';

import './index.less';
import { Connect, InjectIntl } from '../../store';
import { strings, configs } from '../../constants';

export default
@Connect({
  mapReselect: {
    currentSite: reselect => reselect.system.currentSite,
    projects: reselect => reselect.system.projects,
  },
  mapState: {
    currentProject: state => state.system.currentProject,
  },
})
@InjectIntl()
class ProjectSelector extends PureComponent {
  static propTypes = {
    boundSystemActions: PropTypes.object.isRequired,
    boundGlobalActions: PropTypes.object.isRequired,
    currentSite: PropTypes.object.isRequired,
    currentProject: PropTypes.object.isRequired,
    projects: PropTypes.array.isRequired,
    className: PropTypes.string.isRequired,
    intl: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
  };

  state = {
    isRefreshSelector: false,
  };

  constructor(props) {
    super(props);
    this.handleChangeProject = this.handleChangeProject.bind(this);
  }

  get isShowSelector() {
    const { currentSite } = this.props;
    const { adminSites } = configs;
    return !adminSites.includes(currentSite.name);
  }

  async showLostConfirm() {
    const { boundGlobalActions, intl } = this.props;
    const messageContent = intl.formatMessage(strings.SWITCH_LOST_EDIT);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    await MessageBox.confirm(messageContent, messageTitle, messageOptions);
    boundGlobalActions.toggleFlag('isDatasetEdit', false);
    boundGlobalActions.toggleFlag('isSemanticEdit', false);
  }

  updateSelector() {
    this.setState({ isRefreshSelector: true }, () => {
      this.setState({ isRefreshSelector: false });
    });
  }

  async handleChangeProject(projectName) {
    const { boundSystemActions, currentSite, projects, history } = this.props;
    const { fallbackUrl } = currentSite;
    const selectedProject = projects.find(item => item.name === projectName);

    if (fallbackUrl) {
      try {
        await this.showLostConfirm();
        await boundSystemActions.setCurrentProject(selectedProject);
        history.push(fallbackUrl);
      } catch (e) {
        this.updateSelector();
      }
    } else {
      await boundSystemActions.setCurrentProject(selectedProject);
    }
  }

  render() {
    const { currentProject, className, projects } = this.props;
    const { isRefreshSelector } = this.state;
    const { isShowSelector } = this;
    const selectClass = classnames('project-selector', className);

    return !isRefreshSelector && isShowSelector ? (
      <Select
        filterable
        showOverflowTooltip
        theme="secondary"
        className={selectClass}
        value={currentProject.name}
        onChange={this.handleChangeProject}
      >
        {projects.map(projectItem => (
          <Select.Option key={projectItem.name} label={projectItem.name} value={projectItem.name} />
        ))}
      </Select>
    ) : null;
  }
}
