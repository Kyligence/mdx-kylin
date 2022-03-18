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
import { Form, Input, Button, Alert, MessageBox, Select, Switch, Tooltip, Loading } from 'kyligence-ui-react';

import './index.less';
import Block from '../Block/Block';
import { Connect, InjectIntl } from '../../store';
import { strings, configs } from '../../constants';
import { messageHelper, dataHelper, datasetHelper } from '../../utils';
import { validator, calcMeasureFormats, getAllMeasureOptions, isErrorMeasure, CALCULATED_MEASURE } from './handler';
import CodeEditor from '../CodeEditor/CodeEditor';
import FolderInput from '../FolderInput/FolderInput';
import FormatInput from '../FormatInput/FormatInput';
import FormatPreview from '../FormatPreview/FormatPreview';

const { nodeTypes, formatTypes } = configs;

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
class CalculateMeasureBlock extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    boundGlobalActions: PropTypes.object.isRequired,
    boundModalActions: PropTypes.object.isRequired,
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
      format: '',
      formatType: '',
      desc: '',
      folder: CALCULATED_MEASURE,
      subfolder: '',
      expression: '',
      isVisible: true,
      invisible: [],
      visible: [],
      nonEmptyBehavior: [],
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

    const isEditCMeasure = !oldIsEditMode && newIsEditMode && data.name;

    if (isEditCMeasure) {
      this.$form.current.validate();
    }
  }

  get isCreating() {
    const { data } = this.props;
    const { isEditMode } = this.state;
    return !data.name && isEditMode;
  }

  get rules() {
    const isDulp = this.isCMeasureDuplicate;

    return {
      name: [{ required: true, validator: validator.name(this.props, isDulp), trigger: 'blur' }],
      format: [{ required: true, validator: validator.format(this.props), trigger: 'blur' }],
      folder: [{ required: true, validator: validator.folder(this.props), trigger: 'blur' }],
      subfolder: [{ validator: validator.subfolder(this.props), trigger: 'blur' }],
      nonEmptyBehavior: [{ validator: validator.nonEmptyBehavior(this), trigger: 'change' }],
      expression: [{ required: true, validator: validator.expression(this), trigger: 'blur' }],
      // desc: [{ validator: validator.desc(this.props), trigger: 'blur' }],
    };
  }

  get isCMeasureDuplicate() {
    const { form } = this.state;
    const { dataset, data } = this.props;
    const { name } = form;
    const oldAlias = (data.alias || data.name) && (
      data.alias ? data.alias.toUpperCase() : data.name.toUpperCase()
    );

    return [
      ...dataset.calculateMeasures,
      ...dataset.models.reduce((allMeasures, model) => [
        ...allMeasures, ...model.measures,
      ], []),
    ]
      // 如果是有alias则是measure，如果没有alias就是CM，检查是否重名
      .map(item => (item.alias ? item.alias.toUpperCase() : item.name.toUpperCase()))
      .filter(item => item !== oldAlias)
      .includes(name.toUpperCase().trim());
  }

  get measureGroupOptions() {
    const { dataset } = this.props;
    const modelOptions = dataset.models.map(model => ({ label: model.name, value: model.name }));
    return [
      { lable: CALCULATED_MEASURE, value: CALCULATED_MEASURE },
      ...modelOptions,
    ];
  }

  get allMeasureOptions() {
    const { dataset, data: { nonEmptyBehaviorErrors } } = this.props;
    const { form } = this.state;
    return getAllMeasureOptions({ dataset, form, nonEmptyBehaviorErrors });
  }

  get mdxAutoCompletions() {
    const { dataset } = this.props;
    const { form } = this.state;
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
      ...calcMeasures.filter(measure => measure.value !== `[Measures].[${form.name}]`),
      ...namedSets,
    ];
  }

  get selectedBehaviorValues() {
    const { form } = this.state;
    // 映射 [{ name, alias, model }] 成 [alias]字符串数组
    return form.nonEmptyBehavior.map(behavior => behavior.alias);
  }

  get folderOptions() {
    const { dataset } = this.props;
    const { form } = this.state;
    const { folder: measureGroup } = form;
    return datasetHelper.getMeasureFolderOptions({ dataset, measureGroup });
  }

  get customFormats() {
    const { dataset } = this.props;
    return datasetHelper.getCustomFormats({ dataset });
  }

  handleInputBehavior = selectedBehaviorValues => {
    const { allMeasureOptions } = this;
    // 映射behavior value字符串，变成 { name, alias, model }结构存进store
    const formValue = selectedBehaviorValues
      .map(value => {
        const { data: selectedMeasure } = allMeasureOptions.find(option => option.value === value);
        return {
          name: selectedMeasure.name,
          alias: selectedMeasure.alias,
          model: selectedMeasure.model,
        };
      });

    this.handleInput('nonEmptyBehavior', formValue);
  };

  handleExpressionWizard = async () => {
    const { form } = this.state;
    const { boundModalActions, intl } = this.props;
    const title = intl.formatMessage(strings.CALCULATED_MEASURE);
    await boundModalActions.setModalData('ExpressionWizardModal', { category: 'calculateMeasure', title, originValue: form.expression });

    const { isSubmit, expression } = await boundModalActions.showModal('ExpressionWizardModal');
    if (isSubmit) {
      this.handleInput('expression', expression);
      this.$form.current.validateField('expression');
    }
  }

  isErrorMeasure = behavior => {
    const { data: { nonEmptyBehaviorErrors } } = this.props;
    return isErrorMeasure(behavior, nonEmptyBehaviorErrors);
  };

  /* eslint-disable max-len */
  asyncFormData = (nextProps = {}) => {
    const { data: oldData } = this.props;
    const { data: newData } = nextProps;
    const data = newData || oldData;

    if (oldData !== newData) {
      const { name = '', format = 'regular', formatType = formatTypes.NO_FORMAT, folder = CALCULATED_MEASURE, subfolder = '', expression = '', desc = '', invisible = [], isVisible = true, visible = [], translation = {}, nonEmptyBehavior = [] } = data;
      const isEditMode = !name;
      const form = { name, format, formatType, folder, subfolder, expression, desc, invisible, isVisible, visible, translation, nonEmptyBehavior };
      this.setState({ form });
      this.handleToggleEdit(isEditMode);
    }
  }

  /* eslint-disable class-methods-use-this */
  handleGetFormatOption = (query, cb) => {
    const options = calcMeasureFormats.map(value => ({ value }));
    cb(options);
  }

  showDeleteAlert = () => {
    const { intl } = this.props;
    const { form } = this.state;
    const params = { cMeasureName: form.name };
    const messageContent = intl.formatMessage(strings.CONFIRM_DELETE_CMEASURE, params);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  showVisibleAlert = () => {
    const { intl } = this.props;
    const { form } = this.state;
    const itemType = intl.formatMessage(strings.CALCULATED_MEASURE).toLowerCase();
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

    const params = { cMeasureName: form.name };
    const message = intl.formatMessage(strings.SUCCESS_DELETE_CMEASURE, params);
    messageHelper.notifySuccess(message);
  }

  handleClickDelete = async () => {
    const { boundDatasetActions, onDelete } = this.props;
    const { form } = this.state;

    await this.showDeleteAlert();
    await boundDatasetActions.deleteCalculatedMeasure(form);
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

    const params = { cMeasureName: newForm.name };
    const message = intl.formatMessage(strings.SUCCESS_SUBMIT_CMEASURE, params);
    messageHelper.notifySuccess(message);
  }

  handleSubmit = () => {
    const { boundDatasetActions, data: oldData, onSubmit } = this.props;
    const { form, isValidating } = this.state;

    if (!isValidating) {
      this.setState({ isSubmiting: true });
      this.$form.current.validate(async valid => {
        try {
          if (valid) {
            const newForm = { ...form, error: '' };
            const data = this.isCreating
              ? boundDatasetActions.setCalculatedMeasure(null, newForm)
              : boundDatasetActions.setCalculatedMeasure(oldData.name, newForm);

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
      ? intl.formatMessage(strings.ADD_CALCULATED_MEASURE)
      : intl.formatMessage(strings.EDIT_CALCULATED_MEASURE);

    return (
      <div className="clearfix">
        <div className="pull-left">
          {isEditMode
            ? editTitle
            : intl.formatMessage(strings.SHOW_CALCULATED_MEASURE)}
        </div>
        <div className="pull-right">
          {isEditMode ? (
            <Fragment key={isEditMode}>
              <Button type="text" size="small" icon="icon-superset-close" onClick={this.handleCancelEdit}>
                {intl.formatMessage(strings.CANCEL)}
              </Button>
              <Button type="text" size="small" icon="icon-superset-table_save" className="mdx-it-cMeasure-submit primary" loading={isSubmiting} onClick={this.handleSubmit}>
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

  renderExpressionLabel = () => {
    const { intl } = this.props;
    return (
      <Fragment>
        <span>{intl.formatMessage(strings.EXPRESSION)}</span>
        <Button plain className="expression-wizard" type="primary" size="small" onClick={this.handleExpressionWizard}>
          {intl.formatMessage(strings.USE_TEMPLATE)}
        </Button>
      </Fragment>
    );
  }

  render() {
    const {
      $form,
      mdxAutoCompletions,
      measureGroupOptions,
      allMeasureOptions,
      selectedBehaviorValues,
    } = this;
    const { isEditMode, form, isValidating } = this.state;
    const { data, intl, className } = this.props;

    return (
      <Block
        className={classNames(className, 'calculate-measure-block')}
        header={this.renderHeader()}
      >
        {isEditMode ? (
          <Form ref={$form} model={form} labelPosition="top" rules={this.rules}>
            <Form.Item label={intl.formatMessage(strings.CALCULATED_MEASURE_NAME)} prop="name">
              <Input className="mdx-it-cMeasure-name-input" value={form.name} onChange={value => this.handleInput('name', value)} />
            </Form.Item>
            <FormatInput
              className="mdx-it-cMeasure-format-input"
              format={form.format}
              formatType={form.formatType}
              customFormats={this.customFormats}
              onChange={this.handleInput}
            />
            <Form.Item
              prop="folder"
              label={intl.formatMessage(strings.MEASURE_GROUP)}
              description={intl.formatMessage(strings.MEASURE_GROUP_DESC)}
            >
              <Select value={form.folder} onChange={value => this.handleInput('folder', value)} popperProps={configs.disablePopperAutoFlip}>
                {measureGroupOptions.map(option => (
                  <Select.Option key={option.value} label={option.label} value={option.value} />
                ))}
              </Select>
            </Form.Item>
            <FolderInput
              prop="subfolder"
              name={form.name}
              nodeType={nodeTypes.CALCULATE_MEASURE}
              value={form.subfolder}
              options={this.folderOptions}
              onChange={this.handleInput}
            />
            <Form.Item labelClass="expression-field" label={this.renderExpressionLabel()} prop="expression">
              <Loading className="mdx-it-cMeasure-expression-validate-loading" loading={isValidating}>
                <CodeEditor
                  className="mdx-it-cMeasure-expression-input"
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
              <div className="mdx-tip-content">{intl.formatMessage(strings.MDX_EXPRESSION_TIP)}</div>
            </div>
            <Form.Item className="notity-block" label={intl.formatMessage(strings.NON_EMPTY_BEHAVIOR)} prop="nonEmptyBehavior">
              <Alert
                type="info"
                icon="icon-superset-infor"
                closable={false}
                title={intl.formatMessage(strings.NON_EMPTY_BEHAVIOR_TIP)}
              />
              <Select multiple value={selectedBehaviorValues} onChange={value => this.handleInputBehavior(value)} popperProps={configs.disablePopperAutoFlip}>
                {allMeasureOptions.map(option => (
                  <Select.Option key={option.value} label={option.label} value={option.value} disabled={option.disabled} />
                ))}
              </Select>
            </Form.Item>
            <Form.Item label={intl.formatMessage(strings.VISIBLE)} prop="isVisible" className="form-switch">
              <Switch value={form.isVisible} onText="ON" offText="OFF" onChange={value => this.handleChangeVisible(value)} />
            </Form.Item>
            <div className="mdx-tip">
              <i className="icon-superset-alert" />
              <div className="mdx-tip-content">{intl.formatMessage(strings.VISIBILITY_TIP)}</div>
            </div>
            {/* <Form.Item label={intl.formatMessage(strings.DESCRIPTION)} prop="desc">
              <Input value={form.desc} onChange={value => this.handleInput('desc', value)} />
            </Form.Item> */}
          </Form>
        ) : (
          <table className="plain-table">
            <tbody>
              <tr>
                <td className="font-medium" style={{ minWidth: '190px' }}>{intl.formatMessage(strings.CALCULATED_MEASURE_NAME)}</td>
                <td className="mdx-it-cMeasure-display-name" style={{ whiteSpace: 'pre-wrap' }}>{form.name}</td>
              </tr>
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.FORMAT)}</td>
                <td className="mdx-it-cMeasure-display-format">
                  <FormatPreview isReadOnly format={form.format} />
                </td>
              </tr>
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.MEASURE_GROUP)}</td>
                <td>{form.folder}</td>
              </tr>
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.FOLDER)}</td>
                <td className="mdx-it-cMeasure-display-folder" style={{ whiteSpace: 'pre-wrap' }}>
                  {form.subfolder ? form.subfolder : intl.formatMessage(strings.NONE)}
                </td>
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
                <td className="mdx-it-cMeasure-display-expression" style={{ whiteSpace: 'pre-wrap' }}>{form.expression}</td>
              </tr>
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.NON_EMPTY_BEHAVIOR)}</td>
                <td style={{ padding: 0 }}>
                  {form.nonEmptyBehavior.map(behavior => (
                    <div className="inline-row" key={behavior.alias}>
                      {this.isErrorMeasure(behavior) ? (
                        <Tooltip placement="top" content={intl.formatMessage(strings.NON_EMPTY_BEHAVIOR_DELETED, behavior)}>
                          <i className="error-icon icon-superset-alert" />
                        </Tooltip>
                      ) : null}
                      [Measures].[{behavior.alias}]
                    </div>
                  ))}
                </td>
              </tr>
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.VISIBLE)}</td>
                <td>
                  {form.isVisible
                    ? intl.formatMessage(strings.YES)
                    : intl.formatMessage(strings.NO)}
                </td>
              </tr>
              {/* <tr>
                <td className="font-medium">{intl.formatMessage(strings.DESCRIPTION)}</td>
                <td>{form.desc}</td>
              </tr> */}
            </tbody>
          </table>
        )}
      </Block>
    );
  }
}
