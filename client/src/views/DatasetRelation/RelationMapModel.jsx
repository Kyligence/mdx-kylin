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
