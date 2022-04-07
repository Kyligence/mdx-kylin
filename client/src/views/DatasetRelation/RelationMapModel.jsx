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
import { Tooltip } from 'kyligence-ui-react';

import { Connect, InjectIntl } from '../../store';
import { strings } from '../../constants';
import { dataHelper } from '../../utils';

export default
@Connect({
  mapState: {
    dataset: state => state.workspace.dataset,
  },
})
@InjectIntl()
class RelationMapModel extends PureComponent {
  static propTypes = {
    intl: PropTypes.object.isRequired,
    name: PropTypes.string,
    deleteModel: PropTypes.func,
    canDelete: PropTypes.bool,
    isDragging: PropTypes.bool,
    dataset: PropTypes.object.isRequired,
    error: PropTypes.object,
    message: PropTypes.string,
  };

  static defaultProps = {
    name: null,
    deleteModel: () => {},
    canDelete: true,
    error: null,
    isDragging: false,
    message: '',
  }

  constructor(props) {
    super(props);
    this.deleteModelContainer = this.deleteModelContainer.bind(this);
  }

  get modify() {
    const { dataset, name } = this.props;
    const currentModel = dataset.modelsAndTablesList.find(model => model.modelName === name) || {};
    return currentModel.lastModified;
  }

  get errorMessage() {
    const { intl, error, name: modelName } = this.props;
    const values = { modelName, tableName: '', columnName: '' };
    const errorMessages = {
      MODEL_DELETED: intl.formatMessage(strings.MODEL_DELETED, values),
      HIERARCHY_DIM_COL_DELETED: intl.formatMessage(strings.HIERARCHY_DIM_COL_DELETED, values),
    };
    return errorMessages[error && error.type];
  }

  get errorClass() {
    const { error } = this.props;

    const errorClasses = {
      MODEL_DELETED: 'deleted-model',
      HIERARCHY_DIM_COL_DELETED: 'deleted-hierarchy-column',
    };
    return errorClasses[error && error.type];
  }

  deleteModelContainer() {
    const { name, deleteModel } = this.props;
    deleteModel(name);
  }

  renderComponent() {
    const { name, canDelete, isDragging, intl } = this.props;
    const { errorMessage, errorClass } = this;
    const deleteable = canDelete;
    const classname = classnames('map-model-wrapper', isDragging && 'dragging', errorClass);

    return (
      <div className={classname} id={name}>
        <div className="map-warpper-header">
          <span className="title-sm font-medium" title={name}>
            {errorMessage && (
              <Tooltip positionFixed popperClass="deleted-model-tip" effect="dark" placement="top" content={errorMessage}>
                <i className="error-icon icon-superset-alert" />
              </Tooltip>
            )}
            {name}
          </span>
          <div className="model-actions">
            {deleteable && (
              <i
                className="icon-superset-table_delete"
                role="button"
                tabIndex="0"
                onClick={this.deleteModelContainer}
              />
            )}
          </div>
        </div>
        <dir className="map-warpper-description">
          <p className="content-sm font-medium">{intl.formatMessage(strings.LAST_MODIFIED_C)}</p>
          <span className="content-sm">
            {this.modify
              ? dataHelper.getDateString(this.modify)
              : intl.formatMessage(strings.NO_DATA)}
          </span>
        </dir>
      </div>
    );
  }

  render() {
    const { message } = this.props;
    const component = this.renderComponent();

    return message ? (
      <Tooltip
        positionFixed
        popperClass="model-tip"
        effect="dark"
        placement="top"
        content={message}
        popperProps={{
          modifiers: {
            preventOverflow: {
              boundariesElement: 'scrollParent',
            },
          },
        }}
      >
        {component}
      </Tooltip>
    ) : component;
  }
}
