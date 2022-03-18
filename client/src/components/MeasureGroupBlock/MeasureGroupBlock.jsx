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
import { Form, Input, Button } from 'kyligence-ui-react';

import './index.less';
import Block from '../Block/Block';
import { Connect, InjectIntl } from '../../store';
import { strings } from '../../constants';
import { messageHelper } from '../../utils';
import { validator } from './handler';

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
class MeasureGroupBlock extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    boundGlobalActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    dataset: PropTypes.object.isRequired,
    data: PropTypes.object.isRequired,
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
    const isDulp = this.isModelAliasDuplicate;
    return {
      alias: [{ required: true, validator: validator.alias(this.props, isDulp), trigger: 'blur' }],
    };
  }

  get isModelAliasDuplicate() {
    const { form } = this.state;
    const { data, dataset } = this.props;
    const { alias } = data;
    return dataset.models
      .map(item => item.alias.toUpperCase())
      .filter(item => item !== alias.toUpperCase())
      .includes(form.alias.toUpperCase());
  }

  asyncFormData = (nextProps = {}) => {
    const { data: oldData } = this.props;
    const { data: newData } = nextProps;
    const data = newData || oldData;

    if (oldData !== newData) {
      this.setState({ form: data });
      this.handleToggleEdit(false);
    }
  }

  handleInput = (key, value) => {
    const { form: oldForm } = this.state;
    const form = { ...oldForm, [key]: value };
    this.setState({ form });
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
    const { intl } = this.props;

    const params = { measureGroupName: newForm.alias };
    const message = intl.formatMessage(strings.SUCCESS_SUBMIT_MODEL, params);
    messageHelper.notifySuccess(message);
  }

  handleSubmit = () => {
    const { boundDatasetActions, data: oldData } = this.props;
    const { form } = this.state;

    this.$form.current.validate(async valid => {
      if (valid) {
        const data = boundDatasetActions.setMeasureGroup(oldData.name, form);
        this.showSubmitSuccess(data);
        this.handleToggleEdit(false);
      }
    });
  }

  renderHeader = () => {
    const { intl } = this.props;
    const { isEditMode } = this.state;

    return (
      <div className="clearfix">
        <div className="pull-left">
          {isEditMode
            ? intl.formatMessage(strings.EDIT_MEASURE_GROUP)
            : intl.formatMessage(strings.SHOW_MEASURE_GROUP)}
        </div>
        <div className="pull-right">
          {isEditMode ? (
            <Fragment key={isEditMode}>
              <Button type="text" size="small" icon="icon-superset-close" onClick={this.handleCancelEdit}>
                {intl.formatMessage(strings.CANCEL)}
              </Button>
              <Button type="text" size="small" icon="icon-superset-table_save" onClick={this.handleSubmit}>
                {intl.formatMessage(strings.SAVE)}
              </Button>
            </Fragment>
          ) : (
            <Fragment key={isEditMode}>
              <Button type="text" size="small" icon="icon-superset-table_edit" onClick={this.handleClickEdit}>
                {intl.formatMessage(strings.EDIT)}
              </Button>
            </Fragment>
          )}
        </div>
      </div>
    );
  }

  render() {
    const { intl, className } = this.props;
    const { isEditMode, form } = this.state;
    const { $form } = this;

    return (
      <Block
        className={classNames(className, 'measure-group-block')}
        header={this.renderHeader()}
      >
        {isEditMode ? (
          <Form ref={$form} model={form} labelPosition="top" rules={this.rules}>
            <Form.Item label={intl.formatMessage(strings.MODEL_NAME)} prop="name">
              <Input disabled value={form.name} onChange={value => this.handleInput('name', value)} />
            </Form.Item>
            <Form.Item
              prop="alias"
              label={intl.formatMessage(strings.MEASURE_GROUP)}
              description={intl.formatMessage(strings.MEASURE_GROUP_DESC)}
            >
              <Input value={form.alias} onChange={value => this.handleInput('alias', value)} />
            </Form.Item>
          </Form>
        ) : (
          <table className="plain-table">
            <tbody>
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.MODEL_NAME)}</td>
                <td style={{ whiteSpace: 'pre-wrap' }}>{form.name}</td>
              </tr>
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.MEASURE_GROUP)}</td>
                <td style={{ whiteSpace: 'pre-wrap' }}>{form.alias}</td>
              </tr>
            </tbody>
          </table>
        )}
      </Block>
    );
  }
}
