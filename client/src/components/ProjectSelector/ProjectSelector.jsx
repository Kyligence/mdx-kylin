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
