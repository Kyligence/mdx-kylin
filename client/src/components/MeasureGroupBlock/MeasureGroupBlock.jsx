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
