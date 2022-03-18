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
import classNames from 'classnames';
import { Form, Input, Button, Select, MessageBox } from 'kyligence-ui-react';

import './index.less';
import Block from '../Block/Block';
import { Connect, InjectIntl } from '../../store';
import { strings, configs } from '../../constants';
import { messageHelper } from '../../utils';
import { validator, getHasWeightHierarchy } from './handler';

export default
@Connect({
  mapState: {
    dataset: state => state.workspace.dataset,
  },
  options: {
    forwardRef: true,
  },
})
@InjectIntl({
  forwardRef: true,
})
class DimensionTableBlock extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    boundGlobalActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    dataset: PropTypes.object.isRequired,
    data: PropTypes.object.isRequired,
    onSubmit: PropTypes.func.isRequired,
    className: PropTypes.string,
  };

  static defaultProps = {
    className: '',
  };

  $form = React.createRef();

  state = {
    isEditMode: false,
    form: {
      name: '',
      alias: '',
      type: 'regular',
    },
  };

  /* eslint-disable camelcase */
  UNSAFE_componentWillMount() {
    this.asyncFormData();
  }

  /* eslint-disable camelcase */
  UNSAFE_componentWillReceiveProps(nextProps) {
    this.asyncFormData(nextProps);
  }

  get rules() {
    const isDupl = this.isTableAliasDuplicate;
    return {
      alias: [{ required: true, validator: validator.alias(this.props, isDupl), trigger: 'blur' }],
      type: [{ required: true, validator: validator.type(this.props), trigger: 'blur' }],
    };
  }

  // 重名规则：整个dataset中是否重名
  get isTableAliasDuplicate() {
    const { form } = this.state;
    const { data, dataset } = this.props;
    const { alias } = data;
    const allTables = dataset.models.reduce((allItems, model) => [
      ...allItems, ...model.dimensionTables,
    ], []);
    return allTables
      .map(item => item.alias.toUpperCase())
      .filter(item => item !== alias.toUpperCase())
      .includes(form.alias.toUpperCase());
  }

  get hasWeightHierarchy() {
    const { data } = this.props;
    return getHasWeightHierarchy(data);
  }

  asyncFormData = (nextProps = {}) => {
    const { data: oldData } = this.props;
    const { data: newData } = nextProps;
    const data = newData || oldData;

    if (oldData !== newData) {
      const { name = '', type, alias = '', translation } = data;
      this.setState({ form: { name, type, alias, translation } });
      this.handleToggleEdit(false);
    }
  }

  showConfirmAlert = () => {
    const { intl } = this.props;
    const messageContent = intl.formatMessage(strings.CONFIRM_DIM_TABLE_CHANGE_TYPE_HAS_WEIGHT);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  handleInput = (key, value) => {
    const { form: oldForm } = this.state;
    const form = { ...oldForm, [key]: value };
    this.setState({ form });
  }

  handleChangeType = async value => {
    // https://olapio.atlassian.net/browse/MDX-3286
    const { form } = this.state;
    const { hasWeightHierarchy } = this;

    try {
      if (hasWeightHierarchy && value === configs.dimTableTypes.TIME) {
        await this.showConfirmAlert();
      }
      this.handleInput('type', value);
    } catch {
      this.handleInput('type', value);
      setTimeout(() => {
        this.handleInput('type', form.type);
      });
    }
  }

  handleClickEdit = () => {
    this.handleToggleEdit(true);
  }

  handleCancelEdit = () => {
    this.asyncFormData();
    this.handleToggleEdit(false);
  }

  handleToggleEdit = isEditMode => {
    const { boundGlobalActions } = this.props;
    boundGlobalActions.toggleFlag('isSemanticEdit', isEditMode);

    this.setState({ isEditMode });
  }

  showSubmitSuccess = newForm => {
    const { intl: { formatMessage } } = this.props;

    const values = { tableAlias: newForm.alias };
    const message = formatMessage(strings.SUCCESS_SUBMIT_DIMENSION_TABLE, values);
    messageHelper.notifySuccess(message);
  }

  handleSubmit = () => {
    const { boundDatasetActions, data: oldData, onSubmit } = this.props;
    const { form } = this.state;
    const newForm = { ...form, alias: form.alias.trim() };

    this.$form.current.validate(async valid => {
      if (valid) {
        const data = boundDatasetActions.setDimTable(oldData.model, newForm);
        this.showSubmitSuccess(data);
        this.handleToggleEdit(false);

        if (onSubmit) {
          setTimeout(() => onSubmit(data));
        }
      }
    });
  }

  renderHeader = () => {
    const { intl: { formatMessage } } = this.props;
    const { isEditMode } = this.state;

    return (
      <div className="clearfix">
        <div className="pull-left">
          {formatMessage(strings.SHOW_DIMENSION_TABLE_INFO)}
        </div>
        <div className="pull-right">
          {isEditMode ? (
            <Fragment key={isEditMode}>
              <Button type="text" size="small" icon="icon-superset-close" onClick={this.handleCancelEdit}>
                {formatMessage(strings.CANCEL)}
              </Button>
              <Button type="text" size="small" icon="icon-superset-table_save" onClick={this.handleSubmit}>
                {formatMessage(strings.SAVE)}
              </Button>
            </Fragment>
          ) : (
            <Fragment key={isEditMode}>
              <Button type="text" size="small" icon="icon-superset-table_edit" onClick={this.handleClickEdit}>
                {formatMessage(strings.EDIT)}
              </Button>
            </Fragment>
          )}
        </div>
      </div>
    );
  }

  render() {
    const { intl: { locale, formatMessage }, className } = this.props;
    const { isEditMode, form } = this.state;
    const { $form } = this;

    return (
      <Block
        className={classNames(className, 'dimension-table-block')}
        header={this.renderHeader()}
      >
        {isEditMode ? (
          <Form ref={$form} model={form} labelPosition="top" rules={this.rules}>
            <Form.Item label={formatMessage(strings.TABLE_NAME)} prop="name">
              <Input disabled value={form.name} onChange={value => this.handleInput('name', value)} />
            </Form.Item>
            <Form.Item label={formatMessage(strings.DIMENSION_TABLE_NAME)} prop="alias">
              <Input value={form.alias} onChange={value => this.handleInput('alias', value)} />
            </Form.Item>
            <Form.Item
              prop="type"
              label={formatMessage(strings.TYPE)}
              description={formatMessage(strings.TYPE_DESC_FOR_TABLE)}
            >
              <Select
                key={`${locale}${form.type}`}
                value={form.type}
                popperProps={configs.disablePopperAutoFlip}
                onChange={this.handleChangeType}
              >
                <Select.Option
                  label={formatMessage(strings.REGULAR)}
                  value={configs.dimTableTypes.REGULAR}
                />
                <Select.Option
                  label={formatMessage(strings.TIME)}
                  value={configs.dimTableTypes.TIME}
                />
              </Select>
            </Form.Item>
          </Form>
        ) : (
          <table className="plain-table">
            <tbody>
              <tr>
                <td className="font-medium">{formatMessage(strings.TABLE_NAME)}</td>
                <td style={{ whiteSpace: 'pre-wrap' }}>{form.name}</td>
              </tr>
              <tr>
                <td className="font-medium">{formatMessage(strings.DIMENSION_TABLE_NAME)}</td>
                <td style={{ whiteSpace: 'pre-wrap' }}>{form.alias}</td>
              </tr>
              <tr>
                <td className="font-medium">{formatMessage(strings.TYPE)}</td>
                <td>{formatMessage(strings[form.type.toUpperCase()])}</td>
              </tr>
            </tbody>
          </table>
        )}
      </Block>
    );
  }
}
