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
