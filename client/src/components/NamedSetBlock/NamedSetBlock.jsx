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
import { Form, Input, Cascader, Button, MessageBox, Switch, Tooltip, Loading } from 'kyligence-ui-react';

import './index.less';
import Block from '../Block/Block';
import { Connect, InjectIntl } from '../../store';
import { strings } from '../../constants';
import { messageHelper, dataHelper, datasetHelper } from '../../utils';
import { validator } from './handler';
import CodeEditor from '../CodeEditor/CodeEditor';

const NAMED_SET = 'Named Set';

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
class NamedSetBlock extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    boundGlobalActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    data: PropTypes.object.isRequired,
    dataset: PropTypes.object.isRequired,
    onDelete: PropTypes.func.isRequired,
    onCancelCreate: PropTypes.func.isRequired,
    onSubmit: PropTypes.func.isRequired,
    className: PropTypes.string,
  };

  static defaultProps = {
    className: '',
  };

  $form = React.createRef();

  state = {
    isEditMode: false,
    isValidating: false,
    isSubmiting: false,
    form: {
      name: '',
      location: NAMED_SET,
      expression: '',
      isVisible: true,
      invisible: [],
      visible: [],
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

  componentDidUpdate(prevProps, prevState) {
    const { data } = this.props;
    const { isEditMode: oldIsEditMode } = prevState;
    const { isEditMode: newIsEditMode } = this.state;

    const isEditNamedSet = !oldIsEditMode && newIsEditMode && data.name;

    if (isEditNamedSet) {
      this.$form.current.validate();
    }
  }

  get isCreating() {
    const { data } = this.props;
    const { isEditMode } = this.state;
    return !data.name && isEditMode;
  }

  get rules() {
    return {
      name: [{ required: true, validator: validator.name(this.props, this.isNamedSetDuplicate), trigger: 'blur' }],
      expression: [{ required: true, validator: validator.expression(this), trigger: 'blur' }],
    };
  }

  get isNamedSetDuplicate() {
    const { form } = this.state;
    const { dataset, data } = this.props;
    const { name } = form;
    const oldAlias = data.name && data.name.toUpperCase();

    return dataset.namedSets
      .map(item => item.name.toUpperCase())
      .filter(item => item !== oldAlias)
      .includes(name.toUpperCase().trim());
  }

  get namedSetOptions() {
    const { dataset } = this.props;

    return [
      { label: NAMED_SET, value: NAMED_SET },
      ...dataset.models.map(model => ({
        label: model.name,
        value: model.name,
        children: model.dimensionTables ? model.dimensionTables.map(table => ({
          label: table.alias,
          value: table.alias,
        })) : [],
      })),
    ];
  }

  get mdxAutoCompletions() {
    const { dataset } = this.props;
    const {
      models,
      tables,
      columns,
      measures,
      hierarchys,
      calcMeasures,
      namedSets,
    } = datasetHelper.getMdxDatasetWords(dataset);
    return [
      ...models,
      ...tables,
      ...columns,
      ...measures,
      ...hierarchys,
      ...calcMeasures,
      ...namedSets,
    ];
  }

  /* eslint-disable max-len */
  asyncFormData = (nextProps = {}) => {
    const { data: oldData } = this.props;
    const { data: newData } = nextProps;
    const data = newData || oldData;

    if (oldData !== newData) {
      const { name = '', location = NAMED_SET, expression = '', isVisible = true, invisible = [], visible = [], translation = {} } = data;
      const isEditMode = !name;
      const form = { name, location, expression, isVisible, invisible, visible, translation };
      this.setState({ form });
      this.handleToggleEdit(isEditMode);
    }
  }

  showDeleteAlert = () => {
    const { intl } = this.props;
    const { form } = this.state;
    const params = { namedSetName: form.name };
    const messageContent = intl.formatMessage(strings.CONFIRM_DELETE_NAMEDSET, params);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  showVisibleAlert = () => {
    const { intl } = this.props;
    const { form } = this.state;
    const itemType = intl.formatMessage(strings.NAMEDSET).toLowerCase();
    const itemName = form.name;
    const params = { itemType, itemName };
    const messageContent = intl.formatMessage(strings.CONFIRM_INVISIBLE_DATASET, params);
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

  handleChangeVisible = async value => {
    if (value === false) {
      try {
        // 当关闭可见性时，弹框提示
        await this.showVisibleAlert();
        this.handleInput('isVisible', false);
      } catch (e) {
        // 如果取消关闭，则Switch回到ON
        this.handleInput('isVisible', true);
      }
    } else {
      this.handleInput('isVisible', value);
    }
  }

  handleClickEdit = () => {
    this.handleToggleEdit(true);
  }

  showDeleteSuccess = form => {
    const { intl } = this.props;

    const params = { namedSetName: form.name };
    const message = intl.formatMessage(strings.SUCCESS_DELETE_NAMEDSET, params);
    messageHelper.notifySuccess(message);
  }

  handleClickDelete = async () => {
    const { boundDatasetActions, onDelete } = this.props;
    const { form } = this.state;

    await this.showDeleteAlert();
    await boundDatasetActions.deleteNamedSet(form);
    this.showDeleteSuccess(form);

    if (onDelete) {
      onDelete();
    }
  }

  handleCancelEdit = () => {
    const { onCancelCreate } = this.props;
    if (this.isCreating && onCancelCreate) {
      onCancelCreate();
    }

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

    const params = { namedSetName: newForm.name };
    const message = intl.formatMessage(strings.SUCCESS_SUBMIT_NAMEDSET, params);
    messageHelper.notifySuccess(message);
  }

  handleSubmit = () => {
    const { boundDatasetActions, onSubmit, data: oldData } = this.props;
    const { form, isValidating } = this.state;

    if (!isValidating) {
      this.setState({ isSubmiting: true });
      this.$form.current.validate(async valid => {
        try {
          if (valid) {
            const newForm = { ...form, error: '' };
            const data = this.isCreating
              ? boundDatasetActions.setNamedSet(null, newForm)
              : boundDatasetActions.setNamedSet(oldData.name, newForm);

            this.showSubmitSuccess(data);
            this.handleToggleEdit(false);

            if (onSubmit) {
              setTimeout(() => onSubmit(data));
            }
          }
        } finally {
          this.setState({ isSubmiting: false });
        }
      });
    }
  }

  renderHeader = () => {
    const { intl } = this.props;
    const { isEditMode, isSubmiting } = this.state;
    const { isCreating } = this;

    const editTitle = isCreating
      ? intl.formatMessage(strings.ADD_NAMEDSET)
      : intl.formatMessage(strings.EDIT_NAMEDSET);

    return (
      <div className="clearfix">
        <div className="pull-left">
          {isEditMode
            ? editTitle
            : intl.formatMessage(strings.SHOW_NAMEDSET)}
        </div>
        <div className="pull-right">
          {isEditMode ? (
            <Fragment key={isEditMode}>
              <Button type="text" size="small" icon="icon-superset-close" onClick={this.handleCancelEdit}>
                {intl.formatMessage(strings.CANCEL)}
              </Button>
              <Button type="text" size="small" icon="icon-superset-table_save" className="mdx-it-namedset-submit" loading={isSubmiting} onClick={this.handleSubmit}>
                {intl.formatMessage(strings.SAVE)}
              </Button>
            </Fragment>
          ) : (
            <Fragment key={isEditMode}>
              <Button type="text" size="small" icon="icon-superset-table_delete" onClick={this.handleClickDelete}>
                {intl.formatMessage(strings.DELETE)}
              </Button>
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
    const { mdxAutoCompletions, namedSetOptions, $form } = this;
    const { isEditMode, form, isValidating } = this.state;
    const { data, intl, className } = this.props;

    const formLocation = form.location === NAMED_SET ? [NAMED_SET] : form.location.split('.');

    return (
      <Block
        className={classNames(className, 'named-set-block')}
        header={this.renderHeader()}
      >
        {isEditMode ? (
          <Form ref={$form} model={form} labelPosition="top" rules={this.rules}>
            <Form.Item label={intl.formatMessage(strings.NAME)} prop="name">
              <Input className="mdx-it-namedset-name-input" value={form.name} onChange={value => this.handleInput('name', value)} />
            </Form.Item>
            <Form.Item
              prop="expression"
              label={intl.formatMessage(strings.EXPRESSION)}
              description={intl.formatMessage(strings.EXPRESSION_DESC)}
            >
              <Loading className="mdx-it-namedset-expression-validate-loading" loading={isValidating}>
                <CodeEditor
                  className="mdx-it-namedset-expression-input"
                  mode="mdx"
                  height="200px"
                  width="100%"
                  placeholder={intl.formatMessage(strings.PLEASE_ENTER)}
                  minLines={8}
                  maxLines={30}
                  value={form.expression}
                  completions={mdxAutoCompletions}
                  onChange={value => this.handleInput('expression', value)}
                />
              </Loading>
            </Form.Item>
            <div className="mdx-tip">
              <i className="icon-superset-alert" />
              <div className="mdx-tip-content">{intl.formatMessage(strings.NAMEDSET_EXPRESSION_TIP)}</div>
            </div>
            <Form.Item style={{ marginTop: '15px' }} label={intl.formatMessage(strings.LOCATION)}>
              <Loading loading={isValidating}>
                <Cascader
                  filterable
                  disabled
                  value={formLocation}
                  placeholder={intl.formatMessage(strings.SEARCH_MODEL_TABLE)}
                  options={namedSetOptions}
                  onChange={value => this.handleInput('location', value.join('.'))}
                />
              </Loading>
            </Form.Item>
            <Form.Item label={intl.formatMessage(strings.VISIBLE)} prop="isVisible" className="form-switch">
              <Switch value={form.isVisible} onText="ON" offText="OFF" onChange={value => this.handleChangeVisible(value)} />
            </Form.Item>
            <div className="mdx-tip">
              <i className="icon-superset-alert" />
              <div className="mdx-tip-content">{intl.formatMessage(strings.VISIBILITY_TIP)}</div>
            </div>
          </Form>
        ) : (
          <table className="plain-table">
            <tbody>
              <tr>
                <td className="font-medium" style={{ minWidth: '190px' }}>{intl.formatMessage(strings.NAME)}</td>
                <td className="mdx-it-namedset-display-name" style={{ whiteSpace: 'pre-wrap' }}>{form.name}</td>
              </tr>
              <tr>
                <td className="font-medium">
                  {data.error && (
                    <Tooltip placement="top" content={dataHelper.renderStringWithBr(data.error)}>
                      <i className="error-icon icon-superset-alert" />
                    </Tooltip>
                  )}
                  {intl.formatMessage(strings.EXPRESSION)}
                </td>
                <td style={{ whiteSpace: 'pre-wrap' }}>{form.expression}</td>
              </tr>
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.LOCATION)}</td>
                <td>{form.location.replace('.', ' / ')}</td>
              </tr>
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.VISIBLE)}</td>
                <td>
                  {form.isVisible
                    ? intl.formatMessage(strings.YES)
                    : intl.formatMessage(strings.NO)}
                </td>
              </tr>
            </tbody>
          </table>
        )}
      </Block>
    );
  }
}
