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
import { Form, Input, Button, Switch, MessageBox } from 'kyligence-ui-react';

import './index.less';
import Block from '../Block/Block';
import FolderInput from '../FolderInput/FolderInput';
import FormatInput from '../FormatInput/FormatInput';
import FormatPreview from '../FormatPreview/FormatPreview';
import { Connect, InjectIntl } from '../../store';
import { strings, configs } from '../../constants';
import { messageHelper, datasetHelper } from '../../utils';
import { validator, measureFormats } from './handler';

const { nodeTypes } = configs;

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
class DimensionMeasureBlock extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    boundGlobalActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    data: PropTypes.object.isRequired,
    dataset: PropTypes.object.isRequired,
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
      desc: '',
      format: '',
      formatType: '',
      subfolder: '',
      expression: '',
      expressionParams: '',
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

  get rules() {
    const isDulp = this.isMeasureAliasDuplicate;
    return {
      alias: [{ required: true, validator: validator.alias(this.props, isDulp), trigger: 'blur' }],
      format: [{ required: true, validator: validator.format(this.props), trigger: 'blur' }],
      subfolder: [{ validator: validator.subfolder(this.props), trigger: 'blur' }],
      // desc: [{ validator: validator.desc(this.props), trigger: 'blur' }],
    };
  }

  get isMeasureAliasDuplicate() {
    const { form } = this.state;
    const { data, dataset } = this.props;
    const { alias } = data;
    const allMeasures = dataset.models.reduce((measures, model) => [
      ...measures, ...model.measures,
    ], []);
    return [
      ...allMeasures,
      ...dataset.calculateMeasures,
    ]
      // 如果是有alias则是measure，如果没有alias就是CM，检查是否重名
      .map(item => (item.alias ? item.alias.toUpperCase() : item.name.toUpperCase()))
      .filter(item => item !== alias.toUpperCase())
      .includes(form.alias.toUpperCase().trim());
  }

  get folderOptions() {
    const { dataset, data } = this.props;
    const { model: measureGroup } = data;
    return datasetHelper.getMeasureFolderOptions({ dataset, measureGroup });
  }

  get customFormats() {
    const { dataset } = this.props;
    return datasetHelper.getCustomFormats({ dataset });
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

  /* eslint-disable class-methods-use-this */
  handleGetFormatOption = (query, cb) => {
    const options = measureFormats.map(value => ({ value }));
    cb(options);
  }

  showVisibleAlert = () => {
    const { intl } = this.props;
    const { form } = this.state;
    const itemType = intl.formatMessage(strings.MEASURE).toLowerCase();
    const itemName = form.alias;
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

    const params = { measureName: newForm.alias };
    const message = intl.formatMessage(strings.SUCCESS_SUBMIT_MEASURE, params);
    messageHelper.notifySuccess(message);
  }

  handleSubmit = () => {
    const { boundDatasetActions, data: oldData, onSubmit } = this.props;
    const { form } = this.state;

    this.$form.current.validate(async valid => {
      if (valid) {
        const data = boundDatasetActions.setDimMeasure(oldData.model, oldData.table, form);

        this.showSubmitSuccess(data);
        this.handleToggleEdit(false);

        if (onSubmit) {
          setTimeout(() => onSubmit(data));
        }
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
            ? intl.formatMessage(strings.EDIT_MEASURE)
            : intl.formatMessage(strings.SHOW_MEASURE)}
        </div>
        <div className="pull-right">
          {isEditMode ? (
            <Fragment key={isEditMode}>
              <Button type="text" size="small" icon="icon-superset-close" onClick={this.handleCancelEdit}>
                {intl.formatMessage(strings.CANCEL)}
              </Button>
              <Button type="text" size="small" icon="icon-superset-table_save" className="mdx-it-measure-submit" onClick={this.handleSubmit}>
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
        className={classNames(className, 'dimension-measure-block')}
        header={this.renderHeader()}
      >
        {isEditMode ? (
          <Form ref={$form} model={form} labelPosition="top" rules={this.rules}>
            <Form.Item label={intl.formatMessage(strings.MEASURE_NAME)} prop="alias">
              <Input className="mdx-it-measure-alias-input" value={form.alias} onChange={value => this.handleInput('alias', value)} />
            </Form.Item>
            <FormatInput
              className="mdx-it-measure-format-input"
              format={form.format}
              formatType={form.formatType}
              customFormats={this.customFormats}
              onChange={this.handleInput}
            />
            <FolderInput
              prop="subfolder"
              name={form.alias}
              nodeType={nodeTypes.MEASURE}
              value={form.subfolder}
              options={this.folderOptions}
              onChange={this.handleInput}
            />
            <Form.Item label={intl.formatMessage(strings.METRIC_TYPE)} prop="expression">
              <Input disabled value={form.expression} onChange={value => this.handleInput('expression', value)} />
            </Form.Item>
            <Form.Item label={intl.formatMessage(strings.EXPRESSION)} prop="expressionParams">
              <Input disabled value={`${form.expression}(${form.expressionParams})`} onChange={value => this.handleInput('expressionParams', value)} />
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
                <td className="font-medium">{intl.formatMessage(strings.MEASURE_NAME)}</td>
                <td className="mdx-it-measure-display-name" style={{ whiteSpace: 'pre-wrap' }}>{form.alias}</td>
              </tr>
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.FORMAT)}</td>
                <td className="mdx-it-measure-display-format">
                  <FormatPreview isReadOnly format={form.format} />
                </td>
              </tr>
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.FOLDER)}</td>
                <td className="mdx-it-measure-display-folder" style={{ whiteSpace: 'pre-wrap' }}>
                  {form.subfolder ? form.subfolder : intl.formatMessage(strings.NONE)}
                </td>
              </tr>
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.METRIC_TYPE)}</td>
                <td>{form.expression}</td>
              </tr>
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.EXPRESSION)}</td>
                <td>{`${form.expression}(${form.expressionParams})`}</td>
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
