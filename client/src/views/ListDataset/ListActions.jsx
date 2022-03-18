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
import { Button } from 'kyligence-ui-react';
import { withRouter as WithRouter } from 'react-router';
import classNames from 'classnames';

import { Connect, InjectIntl } from '../../store';
import { strings } from '../../constants';
import { createUrl } from './handler';

export default
@Connect({
  mapState: {
    currentProject: state => state.system.currentProject,
  },
})
@InjectIntl()
@WithRouter
class ListActions extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    boundSystemActions: PropTypes.object.isRequired,
    boundModalActions: PropTypes.object.isRequired,
    className: PropTypes.string.isRequired,
    intl: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
    currentProject: PropTypes.object.isRequired,
    onRefreshList: PropTypes.func.isRequired,
  };

  state = {
    isDefaultAccessing: false,
  };

  refreshConfiguration = async () => {
    const { boundSystemActions } = this.props;

    try {
      this.setState({ isDefaultAccessing: true });
      await boundSystemActions.getConfigurations();
    } catch {} finally {
      this.setState({ isDefaultAccessing: false });
    }
  };

  handleCreate = async () => {
    const { history, boundDatasetActions } = this.props;

    await this.refreshConfiguration();
    await boundDatasetActions.clearAll();
    history.push(createUrl);
  }

  handleImport = async () => {
    const { boundModalActions, onRefreshList } = this.props;
    const { isSubmit } = await boundModalActions.showModal('UploadDatasetModal');

    if (isSubmit) {
      onRefreshList();
    }
  }

  handleExport = async () => {
    const { boundModalActions } = this.props;
    await boundModalActions.showModal('ExportDatasetModal');
  }

  render() {
    const { intl, currentProject, className } = this.props;
    const { isDefaultAccessing } = this.state;
    const { formatMessage } = intl;
    return (
      <div className={classNames('list-actions', className)}>
        <Button plain type="primary" icon="icon-superset-add_2" disabled={!currentProject.name} onClick={this.handleCreate} loading={isDefaultAccessing}>
          {formatMessage(strings.CREATE_DATASET)}
        </Button>
        <Button plain type="primary" icon="icon-superset-import" disabled={!currentProject.name} onClick={this.handleImport}>
          {formatMessage(strings.IMPORT_DATASET)}
        </Button>
        <Button plain type="primary" icon="icon-superset-export" onClick={this.handleExport}>
          {formatMessage(strings.EXPORT_DATASET)}
        </Button>
      </div>
    );
  }
}
