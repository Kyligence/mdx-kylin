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
/* eslint-disable */
import React, { PureComponent } from 'react';
import PropTypes from 'prop-types';
import { Dialog, Button, MessageBox } from 'kyligence-ui-react';

import './index.less';
import DoubleSelector from '../DoubleSelector/DoubleSelector';
import { Connect, InjectIntl } from '../../store';
import { strings } from '../../constants';
import { messageHelper, storeHelper } from '../../utils';
import { getDefaultState } from './handler';

export default
@Connect({
  namespace: 'modal/AddDatasetRelationshipModal',
  defaultState: getDefaultState(),
  mapState: {
    isShow: state => state.isShow,
    callback: state => state.callback,
    form: state => state.form,
    disabled: state => state.disabled,
  },
}, {
  mapState: {
    modelsAndTablesList: state => state.workspace.dataset.modelsAndTablesList,
    modelRelations: state => state.workspace.dataset.modelRelations,
    models: state => state.workspace.dataset.models,
  },
})
@InjectIntl()
class AddDatasetRelationshipModal extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    boundDatasetActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    isShow: PropTypes.bool.isRequired,
    disabled: PropTypes.bool.isRequired,
    form: PropTypes.object.isRequired,
    callback: PropTypes.func.isRequired,
  };

  $modelRelationsSelect = React.createRef();

  state = {
    relationship: {},
    leftTables: [],
    rightTables: [],
    isSubmitLoading: false,
  };

  constructor(props) {
    super(props);
    this.state.relationship = props.form;
    this.handleCancel = this.handleCancel.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleHideModal = this.handleHideModal.bind(this);
    this.getTablesFromModel = this.getTablesFromModel.bind(this);
    this.isValidRelation = this.isValidRelation.bind(this);
    this.onSelect = this.onSelect.bind(this);
  }

  async componentWillMount() {
    const { boundModalActions } = this.props;
    await boundModalActions.registerModal('AddDatasetRelationshipModal', getDefaultState());
  }

  /* eslint-disable camelcase */
  UNSAFE_componentWillReceiveProps(nextProps) {
    const { form: oldform } = this.props;
    const { form: newform } = nextProps;
    if (newform !== oldform && newform && newform.name) {
      this.setState({ relationship: newform });
      this.setTableOptions(newform);
    }
  }

  componentWillUnmount() {
    const { boundModalActions } = this.props;
    boundModalActions.destroyModal('AddDatasetRelationshipModal');
  }

  onSelect(key, value) {
    const newRelationship = { ...this.currentRelationship, [key]: value };
    let newTablesOption = [];
    if (key === 'modelLeft') {
      newRelationship.relation = [{ left: '', right: '' }];
      newRelationship.name = newRelationship.modelLeft + '&&' + newRelationship.modelRight;
      const leftModel = this.props.modelsAndTablesList
        .filter(model => model.modelName === newRelationship.modelLeft);
      if (leftModel) {
        newTablesOption = leftModel[0].tables;
      }
      this.setState({
        relationship: newRelationship,
        leftTables: newTablesOption,
      });
    } else if (key === 'modelRight') {
      newRelationship.relation = [{ left: '', right: '' }];
      newRelationship.name = newRelationship.modelLeft + '&&' + newRelationship.modelRight;
      const rightModel = this.props.modelsAndTablesList
        .filter(model => model.modelName === newRelationship.modelRight);
      if (rightModel) {
        newTablesOption = rightModel[0].tables;
      }
      this.setState({
        relationship: newRelationship,
        rightTables: newTablesOption,
      });
    } else if (key === 'relation') {
      this.setState({
        relationship: newRelationship,
      });
    }
    this.setTableOptions(newRelationship);
  }

  setTableOptions(relationship) {
    let leftTablesOption = [];
    let rightTableOption = [];
    const leftModel = this.props.modelsAndTablesList
    .filter(model => model.modelName === relationship.modelLeft);
    const rightModel = this.props.modelsAndTablesList
    .filter(model => model.modelName === relationship.modelRight);
    if (leftModel) {
      leftTablesOption = leftModel[0].tables.filter(tableName => !relationship.relation.some(relation => relation.left === tableName));
    }
    if (rightModel) {
      rightTableOption = rightModel[0].tables.filter(tableName => !relationship.relation.some(relation => relation.right === tableName));
    }
    this.setState({
      leftTables: leftTablesOption,
      rightTables: rightTableOption,
    });
  }

  get currentRelationship() {
    if (this.state.relationship && this.state.relationship.name) {
      return this.state.relationship;
    }
    return {};
  }

  get isEditMode() {
    return this.props.form && !!this.props.form.name;
  }

  getTablesFromModel(name) {
    if (name) {
      const model =  this.props.modelsAndTablesList.filter(m => m.modelName === name);
      return model.tables;
    }
    return [];
  }

  handleHideModal() {
    const { boundModalActions } = this.props;
    this.setState({
      relationship: {},
    });
    boundModalActions.hideModal('AddDatasetRelationshipModal');
  }

  handleCancel() {
    const { callback } = this.props;
    this.handleHideModal();
    callback({ isSubmit: false, relation: this.currentRelationship });
  }

  isValidRelation(relationship) {
    // models need to be distinct
    if (relationship.modelLeft === relationship.modelRight) {
      return false;
    }
    // new relation should have relation tables
    const tableRelations = relationship.relation.filter(relation => relation.left !== '' && relation.right !== '');
    if (tableRelations.length <= 0 && this.isDuplicatedRelationship(relationship)) {
      return false;
    }
    return true;
  }

  isValidRelationTables(relationTables) {
    // filter empty tables
    const tableRelations = relationTables.filter(table => table.left !== '' && table.right !== '');
    return tableRelations.length !== 0;
  }

  isNewModel(name) {
    const models =  this.props.models
      .filter(model => model.name === name);
    return models.length === 0;
  }

  isDuplicatedRelationship(relationship) {
    const relationships =  this.props.modelRelations
      .filter((rel) => {
        return (rel.modelLeft === relationship.modelLeft
          && rel.modelRight === relationship.modelRight
          || rel.modelLeft === relationship.modelRight
          && rel.modelRight === relationship.modelLeft
        )
      });
    return relationships.length === 0;
  }

  addNewRelation() {
    const { boundDatasetActions, intl } = this.props;
    const relation = this.currentRelationship.relation.filter(r => r.left && r.right);
    const currentRelationship = { ...this.currentRelationship, relation };
    const relatedModel = currentRelationship.modelRight;
    if (!this.isDuplicatedRelationship(currentRelationship)) {
      messageHelper.notifyFailed(intl.formatMessage(strings.DUPLICATE_RELATIONSHIP));
      this.toggleSubmitLoading(false);
      return false;
    }
    // if (this.isNewModel(relatedModel) && this.props.models.length >= 3) {
    //   messageHelper.notifyFailed(strings.OVER_MAX_MODELS);
    //   this.toggleSubmitLoading(false);
    //   return false;
    // }
    boundDatasetActions.addRelationship(this.currentRelationship);
    if (this.isNewModel(relatedModel)) {
      const relatedModelObj = this.props.modelsAndTablesList
        .filter(model => model.modelName === relatedModel);
      boundDatasetActions.addRelationModel({
        name: relatedModelObj[0].modelName,
        modify: relatedModelObj[0].lastModified,
      });
    }
    this.handleHideModal();
    return true;
  }

  updateCurrentRelation() {
    const { boundDatasetActions, modelRelations } = this.props;
    const { currentRelationship } = this;
    const isRelationValid = currentRelationship.relation && this.isValidRelationTables(currentRelationship.relation);

    if (isRelationValid) {
      const updatedRelationIdx = modelRelations.findIndex(r => r.name === currentRelationship.name);
      const relation = currentRelationship.relation.filter(r => r.left && r.right);
      const newRelation = { ...currentRelationship, relation };
      const newState = updatedRelationIdx === -1
        ? [...modelRelations, newRelation]
        : storeHelper.set(modelRelations, [updatedRelationIdx], newRelation);
      boundDatasetActions.updateRelationship(newState);
    }

    this.handleHideModal();
  }

  showConfirmAlert() {
    const { intl } = this.props;
    const messageContent = intl.formatMessage(strings.CONFIRM_UPDATE_RELATION);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  async handleSubmit() {
    const { callback, disabled, intl } = this.props;
    this.toggleSubmitLoading(true);

    const valid = await this.$modelRelationsSelect.current.validate();

    try {
      if (!this.isValidRelation(this.currentRelationship) || !valid) {
        messageHelper.notifyFailed(intl.formatMessage(strings.NOT_VALID_RELATION));
      } else if (!this.isEditMode) {
        this.addNewRelation();
        callback({ isSubmit: true, relation: this.currentRelationship });
      } else {
        if (disabled) {
          await this.showConfirmAlert();
        }

        callback({ isSubmit: true, relation: this.currentRelationship });
        this.updateCurrentRelation();
      }
    } finally {
      this.toggleSubmitLoading(false);
    }
  }

  toggleSubmitLoading(isSubmitLoading) {
    this.setState({ isSubmitLoading });
  }


  render() {
    const { isShow, disabled, intl } = this.props;
    const { isSubmitLoading } = this.state;
    const leftModels = this.props.models.map(model => model.name);
    const rightModels = this.props.modelsAndTablesList.map(model => model.modelName);
    const ModelSelectProps = {
      valueKey: 'models',
      leftPlaceholder: intl.formatMessage(strings.SELECT_FROM_MODEL),
      rightPlaceholder: intl.formatMessage(strings.SELECT_LOOKUP_MODEL),
      leftTitle: intl.formatMessage(strings.CURRENT_MODEL),
      rightTitle: intl.formatMessage(strings.RELATED_MODEL),
      onSelect: this.onSelect,
      canEditColumns: false,
      values: [{ left: this.currentRelationship.modelLeft || '',
        right: this.currentRelationship.modelRight || '',
      }],
      leftOptions: leftModels,
      rightOptions: rightModels,
      disabledForm: this.isEditMode,
      disabled,
      linkIcon: 'icon-superset-link',
    };
    const TableSelectProps = {
      valueKey: 'tables',
      leftPlaceholder: intl.formatMessage(strings.SELECT_TABLE),
      rightPlaceholder: intl.formatMessage(strings.SELECT_TABLE),
      leftTitle: intl.formatMessage(strings.TABLE),
      rightTitle: intl.formatMessage(strings.TABLE),
      onSelect: this.onSelect,
      canEditColumns: true,
      values: this.currentRelationship.relation || [],
      leftOptions: this.state.leftTables,
      rightOptions: this.state.rightTables,
      disabledForm: false,
      disabled: false,
      linkIcon: 'icon-superset-equal',
    };
    return (
      <Dialog
        className="create-relationship-modal"
        visible={isShow}
        title={intl.formatMessage(strings.ADD_RELATED_MODEL)}
        onCancel={this.handleCancel}
      >
        <Dialog.Body>
          <div className="model-select-wrapper">
            <DoubleSelector {...ModelSelectProps} />
          </div>
          <div className="table-select-wrapper">
            <DoubleSelector ref={this.$modelRelationsSelect} {...TableSelectProps} />
          </div>
        </Dialog.Body>
        <Dialog.Footer className="dialog-footer">
          <Button disabled={isSubmitLoading} onClick={this.handleCancel}>
            {intl.formatMessage(strings.CANCEL)}
          </Button>
          <Button loading={isSubmitLoading} onClick={this.handleSubmit} type="primary">
            {intl.formatMessage(strings.OK)}
          </Button>
        </Dialog.Footer>
      </Dialog>
    );
  }
}
