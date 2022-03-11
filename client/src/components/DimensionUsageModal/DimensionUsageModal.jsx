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
import { Dialog, Button, Form, Select } from 'kyligence-ui-react';

import './index.less';
import { strings, configs } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { validator, getDefaultState } from './handler';
import BlockButton from '../BlockButton/BlockButton';

export default
@Connect({
  namespace: 'modal/DimensionUsageModal',
  defaultState: getDefaultState(),
  mapState: {
    isShow: state => state.isShow,
    model: state => state.model,
    form: state => state.form,
    callback: state => state.callback,
  },
}, {
  mapState: {
    dataset: state => state.workspace.dataset,
  },
})
@InjectIntl()
class DimensionUsageModal extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    boundDatasetActions: PropTypes.object.isRequired,
    isShow: PropTypes.bool.isRequired,
    model: PropTypes.string.isRequired,
    intl: PropTypes.object.isRequired,
    form: PropTypes.object.isRequired,
    callback: PropTypes.func.isRequired,
    dataset: PropTypes.object.isRequired,
  };

  $form = React.createRef();

  state = {
    isSubmitLoading: false,
  };

  constructor(props) {
    super(props);
    this.handleCancel = this.handleCancel.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleHideModal = this.handleHideModal.bind(this);
  }

  /* eslint-disable camelcase */
  UNSAFE_componentWillMount() {
    const { boundModalActions } = this.props;
    boundModalActions.registerModal('DimensionUsageModal', getDefaultState());
  }

  /* eslint-disable camelcase */
  UNSAFE_componentWillReceiveProps(nextProps) {
    const { isShow: newIsShow } = nextProps;
    const { isShow: oldIsShow } = this.props;

    if (newIsShow && !oldIsShow) {
      setTimeout(() => {
        this.$form.current.validate();
      }, 100);
    }
  }

  componentWillUnmount() {
    const { boundModalActions } = this.props;
    boundModalActions.destroyModal('DimensionUsageModal');
  }

  get rules() {
    return {
      relationFactKey: [{ required: true, validator: validator.relationFactKey(this.props, this.factKeyOptions), trigger: 'change' }],
      relationBridgeTableName: [{ required: true, validator: validator.relationBridgeTableName(this.props, this.dimensionTableOptions), trigger: 'change' }],
    };
  }

  get dimensionTableOptions() {
    const { dataset, model } = this.props;
    const currentModel = dataset.models.find(m => m.name === model) || {};
    const modelRelation = dataset.dimTableModelRelations.find(m => m.modelName === model) || {};
    const tableRelations = modelRelation.tableRelations || [];
    const { factTableAlias = '' } = currentModel || {};
    const factTableName = factTableAlias.split('.')[1];

    const dimensionTableOptions = tableRelations.filter(table => (
      table.tableName !== factTableName
      // Issue: https://github.com/Kyligence/MDX/issues/286
      // && table.tableName !== form.tableName
    ))
      .map(table => ({ label: table.tableName, value: table.tableName }));

    // // cube变更后，dataset json中就没有原来的FK了。
    // // 需要从error中补足
    // if (form.bridgeTableError) {
    //   const tableName = form.bridgeTableError.name.split('.')[1];
    //   dimensionTableOptions.push({
    //     label: columnName,
    //     value: `${tableName}.${columnName}`,
    //   });
    // }

    return dimensionTableOptions;
  }

  get factKeyOptions() {
    const { dataset, model, form } = this.props;
    const currentModel = dataset.models.find(m => m.name === model) || {};
    const { dimensionTables = [] } = currentModel;
    const { factTableAlias = '' } = currentModel || {};
    const factTableName = factTableAlias.split('.')[1];
    const factTable = dimensionTables.find(table => table.name === factTableName);
    const factKeyOptions = factTable
      ? factTable.dimCols
        .filter(column => column)
        .map(column => ({
          label: column.name,
          value: `${factTable.name}.${column.name}`,
        }))
      : [];
    // cube变更后，dataset json中就没有原来的FK了。
    // 需要从error中补足
    if (form.factKeyError) {
      const tableName = form.factKeyError.name.split('.')[1];
      const columnName = form.factKeyError.name.split('.')[2];
      factKeyOptions.push({
        label: columnName,
        value: `${tableName}.${columnName}`,
      });
    }
    return factKeyOptions;
  }

  get description() {
    const { form, intl } = this.props;
    const { relationType } = form || {};
    switch (relationType) {
      case configs.tableRelationTypes.JOINT: {
        return intl.formatMessage(strings.JOINT_TIP);
      }
      case configs.tableRelationTypes.NOT_JOINT: {
        return intl.formatMessage(strings.NOT_JOINT_TIP);
      }
      case configs.tableRelationTypes.MANY_TO_MANY: {
        return intl.formatMessage(strings.MANY_TO_MANY_TIP);
      }
      default: return null;
    }
  }

  toggleSubmitLoading(isSubmitLoading) {
    this.setState({ isSubmitLoading });
  }

  handleHideModal() {
    const { boundModalActions } = this.props;
    boundModalActions.hideModal('DimensionUsageModal');
  }

  handleInput(key, value) {
    const { boundModalActions } = this.props;
    if (key === 'relationType' && [0, 1].includes(value)) {
      const emptyData = { relationFactKey: null, relationBridgeTableName: null };
      boundModalActions.setModalForm('DimensionUsageModal', emptyData);
    }
    boundModalActions.setModalForm('DimensionUsageModal', { [key]: value });
  }

  handleCancel() {
    const { callback } = this.props;
    this.handleHideModal();
    callback({ isSubmit: false });
  }

  handleSubmit() {
    const { boundDatasetActions, callback, form, model } = this.props;

    this.$form.current.validate(async valid => {
      if (valid) {
        this.toggleSubmitLoading(true);
        try {
          boundDatasetActions.setDimensionUsage(model, form);
          this.handleHideModal();
          callback({ isSubmit: true });
        } catch (e) {
          this.toggleSubmitLoading(false);
        }
        this.toggleSubmitLoading(false);
      }
    });
  }

  render() {
    const { isShow, form, intl } = this.props;
    const { isSubmitLoading } = this.state;
    const { dimensionTableOptions, factKeyOptions, description, $form } = this;
    return (
      <Dialog
        className="dimension-usage-modal"
        visible={isShow}
        closeOnPressEscape={false}
        closeOnClickModal={false}
        title={intl.formatMessage(strings.DEFINE_DIMENSION_USAGE)}
        onCancel={this.handleHideModal}
      >
        <Dialog.Body>
          <Form key={isShow} ref={$form} model={form} rules={this.rules} labelPosition="top">
            <Form.Item className="text-center" prop="relationType">
              <BlockButton
                icon="icon-superset-link_2"
                currentValue={form.relationType}
                value={configs.tableRelationTypes.JOINT}
                onInput={value => this.handleInput('relationType', value)}
              >
                {intl.formatMessage(strings.JOINT)}
              </BlockButton>
              <BlockButton
                icon="icon-superset-manytomany"
                currentValue={form.relationType}
                value={configs.tableRelationTypes.MANY_TO_MANY}
                onInput={value => this.handleInput('relationType', value)}
              >
                {intl.formatMessage(strings.MANY_TO_MANY)}
              </BlockButton>
            </Form.Item>
            <div className="relation-tip">
              <i className="icon-superset-alert" />
              <span className="tip-text">{description}</span>
            </div>
            {form.relationType === configs.tableRelationTypes.MANY_TO_MANY && (
              <div className="many-to-many-form">
                <Form.Item
                  prop="relationFactKey"
                  label={intl.formatMessage(strings.DEDUPLICATE_BY_THIS_COLUMN)}
                  description={intl.formatMessage(strings.DEDUPLICATE_BY_THIS_COLUMN_DESC)}
                >
                  <Select
                    value={form.relationFactKey || []}
                    onChange={value => this.handleInput('relationFactKey', value)}
                  >
                    {factKeyOptions.map(option => (
                      <Select.Option key={option.value} label={option.label} value={option.value} />
                    ))}
                  </Select>
                </Form.Item>
                <Form.Item
                  prop="relationBridgeTableName"
                  label={intl.formatMessage(strings.INTERMEDIATE_TABLE)}
                  description={intl.formatMessage(strings.INTERMEDIATE_TABLE_DESC)}
                >
                  <Select
                    value={form.relationBridgeTableName || []}
                    onChange={value => this.handleInput('relationBridgeTableName', value)}
                  >
                    {dimensionTableOptions.map(option => (
                      <Select.Option key={option.value} label={option.label} value={option.value} />
                    ))}
                  </Select>
                </Form.Item>
              </div>
            )}
          </Form>
        </Dialog.Body>
        <Dialog.Footer className="dialog-footer">
          <Button onClick={this.handleCancel} disabled={isSubmitLoading}>
            {intl.formatMessage(strings.CANCEL)}
          </Button>
          <Button onClick={this.handleSubmit} type="primary" loading={isSubmitLoading}>
            {intl.formatMessage(strings.OK)}
          </Button>
        </Dialog.Footer>
      </Dialog>
    );
  }
}
