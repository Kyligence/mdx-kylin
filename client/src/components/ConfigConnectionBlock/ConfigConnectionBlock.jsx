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
import { Loading, Button } from 'kyligence-ui-react';
import classnames from 'classnames';

import './index.less';
import { strings } from '../../constants';
import { dataHelper, messageHelper } from '../../utils';
import { Connect, InjectIntl } from '../../store';
import Block from '../Block/Block';

export default
@Connect({
  mapState: {
    pollings: state => state.global.pollings,
    configurations: state => state.system.configurations,
    isAutoPopConnectionUser: state => state.global.isAutoPopConnectionUser,
  },
})
@InjectIntl()
class ConfigConnectionBlock extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    boundSystemActions: PropTypes.object.isRequired,
    boundGlobalActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    configurations: PropTypes.object.isRequired,
    isAutoPopConnectionUser: PropTypes.bool.isRequired,
    pollings: PropTypes.object.isRequired,
  };

  state = {
    isLoading: false,
    isRefreshing: false,
  };

  constructor(props) {
    super(props);

    this.handleRestartSyncTask = this.handleRestartSyncTask.bind(this);
    this.handleEditConnectionUser = this.handleEditConnectionUser.bind(this);
    this.renderTaskStatus = this.renderTaskStatus.bind(this);
    this.renderTaskSyncTime = this.renderTaskSyncTime.bind(this);
    this.renderConnectionUser = this.renderConnectionUser.bind(this);
  }

  async componentDidMount() {
    const { boundGlobalActions, isAutoPopConnectionUser } = this.props;

    if (isAutoPopConnectionUser) {
      await this.handleEditConnectionUser();
      boundGlobalActions.toggleFlag('isAutoPopConnectionUser', false);
    }
  }

  get taskStatus() {
    const { isRefreshing } = this.state;
    const { configurations } = this.props;
    const status = configurations['insight.kylin.status'];

    return (isRefreshing || !status) ? 'starting' : status;
  }

  get taskStatusIcon() {
    switch (this.taskStatus) {
      case 'starting': return classnames(['icon-info', 'icon-superset-loading']);
      case 'active': return classnames(['icon-success', 'icon-superset-good_health']);
      case 'inactive': return classnames(['icon-error', 'icon-superset-error_01']);
      default: return null;
    }
  }

  get tableData() {
    const { intl, configurations } = this.props;
    return {
      [intl.formatMessage(strings.KYLIN_IP_C)]: configurations['insight.kylin.host'],
      [intl.formatMessage(strings.KYLIN_PORT_C)]: configurations['insight.kylin.port'],
      [intl.formatMessage(strings.SYNC_TASK_STATUS_C)]: this.renderTaskStatus(),
      [intl.formatMessage(strings.LAST_SYNC_TIME_C)]: this.renderTaskSyncTime(),
      [intl.formatMessage(strings.CONNECTION_USER_C)]: this.renderConnectionUser(),
    };
  }

  showEditConnectionUserSuccess() {
    const { intl } = this.props;
    const message = intl.formatMessage(strings.SUCCESS_EDIT_CONNECTION_USER);
    messageHelper.notifySuccess(message);
  }

  showSyncTaskRestartSuccess() {
    const { intl } = this.props;
    const message = intl.formatMessage(strings.SUCCESS_RESTART_SYNC_TASK);
    messageHelper.notifySuccess(message);
  }

  async handleRestartSyncTask() {
    const { boundSystemActions, pollings } = this.props;
    this.setState({ isRefreshing: true, isLoading: true });

    try {
      await boundSystemActions.restartSyncTask();
      this.showSyncTaskRestartSuccess();
    } catch {}

    this.setState({ isLoading: false });
    await pollings.configuration.start();
    this.setState({ isRefreshing: false });
  }

  async handleEditConnectionUser() {
    const { boundModalActions, pollings } = this.props;
    const { isSubmit } = await boundModalActions.showModal('ConnectionUserModal');

    if (isSubmit) {
      this.showEditConnectionUserSuccess();
      this.setState({ isRefreshing: true });
      await pollings.configuration.start();
      this.setState({ isRefreshing: false });
    }
  }

  renderTaskStatus() {
    const { intl } = this.props;
    const { taskStatus, taskStatusIcon } = this;
    return (
      <Fragment>
        <i className={taskStatusIcon} />
        <span>{intl.formatMessage(strings[taskStatus.toUpperCase()])}</span>
      </Fragment>
    );
  }

  renderTaskSyncTime() {
    const { intl, configurations } = this.props;
    const timestamp = configurations['insight.kylin.last_updated'];
    return (
      <Fragment>
        <span>
          {!Number.isNaN(+timestamp) && +timestamp
            ? dataHelper.getDateString(timestamp * 1000)
            : intl.formatMessage(strings.NONE)}
        </span>
        <Button
          className="action"
          type="text"
          icon="icon-superset-table_resure"
          onClick={this.handleRestartSyncTask}
        >
          {intl.formatMessage(strings.RESTART_SYNC_TASK)}
        </Button>
      </Fragment>
    );
  }

  renderConnectionUser() {
    const { intl, configurations } = this.props;
    return (
      <Fragment>
        <span>
          {configurations['insight.kylin.username'] || intl.formatMessage(strings.NONE)}
        </span>
        <Button
          className="action"
          type="text"
          icon="icon-superset-switch"
          onClick={this.handleEditConnectionUser}
        >
          {intl.formatMessage(strings.EDIT_USER_INFORMATION)}
        </Button>
      </Fragment>
    );
  }

  render() {
    const { tableData } = this;
    const { intl } = this.props;
    const { isLoading } = this.state;
    return (
      <Block className="configuration-connection-block" header={intl.formatMessage(strings.KYLIN_NODE_CONFIGURATION)}>
        <Loading loading={isLoading}>
          <table className="configurations">
            <tbody>
              {Object.entries(tableData).map(([key, value]) => (
                <tr className="configuration" key={key}>
                  <td className="key">{key}</td>
                  <td className="value">{value}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Loading>
      </Block>
    );
  }
}
