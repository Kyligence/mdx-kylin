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
import { Input, MessageBox, Loading } from 'kyligence-ui-react';

import './index.less';
import { Connect, InjectIntl } from '../../store';
import { messageHelper } from '../../utils';
import { strings } from '../../constants';
import DragModelSource from './DragModelSource';
import DragModelLayer from './DragModelLayer';
import DropModelContainer from './DropModelContainer';
import WithDragDropContext from '../../components/WithDragDropContext/WithDragDropContext';

export default
@Connect({
  mapState: {
    dataset: state => state.workspace.dataset,
  },
})
@InjectIntl()
class DatasetRelation extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    match: PropTypes.object.isRequired,
    dataset: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
  };

  state = {
    modelFilter: '',
    isLoadingModels: true,
  };

  constructor(props) {
    super(props);
    this.handleDrop = this.handleDrop.bind(this);
    this.deleteModel = this.deleteModel.bind(this);
  }

  async componentDidMount() {
    const { boundDatasetActions } = this.props;
    await boundDatasetActions.getProjectModels();
    this.setState({ isLoadingModels: false });
  }

  get datasetId() {
    const { match } = this.props;
    return match.params.datasetId;
  }

  showDeleteAlert() {
    const { intl } = this.props;
    const messageContent = intl.formatMessage(strings.CONFIRM_DELETE_MODEL);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  async deleteModel(modelName) {
    const { dataset, boundDatasetActions, intl } = this.props;
    const newModels = dataset.models.filter(model => model.name !== modelName);
    const relationships = [...dataset.modelRelations];
    const newRelationships = relationships
      .filter(({ modelLeft, modelRight }) => modelLeft !== modelName && modelRight !== modelName);

    await this.showDeleteAlert();

    boundDatasetActions.updateModels(newModels);
    boundDatasetActions.updateRelationship(newRelationships);

    const message = intl.formatMessage(strings.REMOVE_MODEL_SUCCESS, { modelName });
    messageHelper.notifySuccess(message);
  }

  handleDrop(model) {
    const { boundDatasetActions } = this.props;
    boundDatasetActions.addRelationModel(model);
  }

  handleSearch(modelFilter) {
    this.setState({ modelFilter });
  }

  render() {
    const { dataset, boundDatasetActions, intl } = this.props;
    const { modelFilter, isLoadingModels } = this.state;
    const { datasetId } = this;
    const models = dataset.models.map(m => m.name);
    const hasErrorModels = dataset.models.some(m => m.error);
    const errorModelTip = hasErrorModels ? intl.formatMessage(strings.PLEASE_REPAIRE_MODELS) : '';
    const modelList = dataset.modelsAndTablesList.filter(m => {
      if (modelFilter) {
        const isIncludedInFilter = m.modelName.toLowerCase().includes(modelFilter.toLowerCase());
        return models.indexOf(m.modelName) === -1 && isIncludedInFilter;
      }
      return models.indexOf(m.modelName) === -1;
    });
    const isEditMode = !!datasetId;
    return (
      <WithDragDropContext>
        <div className="dataset-relation">
          <div className="dataset-model-list">
            <div className="model-filter-wrapper">
              <Input
                prefixIcon="icon-superset-search"
                value={modelFilter}
                placeholder={intl.formatMessage(strings.SEARCH)}
                onChange={val => this.handleSearch(val)}
              />
            </div>
            <div className="model-list-wrapper">
              <Loading loading={isLoadingModels}>
                <DragModelLayer />
                {modelList.length > 0 && modelList.map(m => (
                  <DragModelSource
                    isDraggable={!hasErrorModels}
                    key={m.modelName}
                    name={m.modelName}
                    modify={m.lastModified}
                    isEditMode={isEditMode}
                    message={errorModelTip}
                  />
                ))}
                {modelList.length === 0 && (
                  <span className="tip">
                    {intl.formatMessage(strings.NO_MODELS)}
                  </span>
                )}
              </Loading>
            </div>
          </div>
          <div className="dataset-maker">
            <DropModelContainer
              isEditMode={isEditMode}
              content={dataset.models}
              onDrop={this.handleDrop}
              deleteModel={this.deleteModel}
              boundDatasetActions={boundDatasetActions}
            />
          </div>
        </div>
      </WithDragDropContext>
    );
  }
}
