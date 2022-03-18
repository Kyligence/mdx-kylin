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
import classnames from 'classnames';
import { TreeTable, Tooltip, Select, Form, Input, MessageBox, Alert, Layout } from 'kyligence-ui-react';

import './index.less';
import SemanticSearchBox from '../../components/SemanticSearchBox/SemanticSearchBox';
import { dataHelper, messageHelper, datasetHelper } from '../../utils';
import { strings, business, configs } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { getTranslationTree, getNodeIcon, translationConfigTypes, calcColSpan } from './handler';

export default
@Connect({
  mapState: {
    language: state => state.system.language.locale,
    dataset: state => state.workspace.dataset,
  },
})
@InjectIntl()
class DatasetTranslation extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    language: PropTypes.string.isRequired,
    dataset: PropTypes.object.isRequired,
  };

  state = {
    isCreateMode: false,
    isFieldVaild: true,
    isMaxLength: false,
    form: {
      translationType: null,
      translation: {
        key: null,
        nodeType: null,
        translationType: null,
        value: null,
      },
    },
    filter: {
      name: '',
      nodeType: 'column',
    },
  };

  $form = React.createRef();
  $table = React.createRef();
  $currentInput = React.createRef();
  isFieldVaild = true;

  constructor(props) {
    super(props);

    this.handleCreateMode = this.handleCreateMode.bind(this);
    this.handleSubmitCreate = this.handleSubmitCreate.bind(this);
    this.handleCancelCreate = this.handleCancelCreate.bind(this);
    this.handleFilterChange = this.handleFilterChange.bind(this);
    this.renderNameHeader = this.renderNameHeader.bind(this);
    this.renderNameColumn = this.renderNameColumn.bind(this);
    this.renderAddTranslation = this.renderAddTranslation.bind(this);
    this.renderTranslationInput = this.renderTranslationInput.bind(this);
    this.renderTranslationHeader = this.renderTranslationHeader.bind(this);
    this.renderActionColumn = this.renderActionColumn.bind(this);
    this.renderDeleteConfirm = this.renderDeleteConfirm.bind(this);
  }

  /* eslint-disable class-methods-use-this */
  get rules() {
    return {
      translationType: [{ required: true, trigger: 'change' }],
    };
  }

  get tableData() {
    const { dataset } = this.props;
    const { filter } = this.state;
    return getTranslationTree({ dataset, filter });
  }

  get columns() {
    const { dataset } = this.props;
    const { isCreateMode } = this.state;
    const { translationTypes } = dataset;

    return [
      {
        prop: 'name',
        render: this.renderNameColumn,
        renderHeader: this.renderNameHeader,
        minWidth: '300px',
      },
      ...translationTypes.map(translationType => ({
        prop: translationType,
        render: this.renderTranslationInput,
        renderHeader: this.renderTranslationHeader,
        minWidth: '200px',
      })),
      ...(isCreateMode ? [{
        renderHeader: this.renderAddTranslation,
        width: '240px',
      }] : []),
      {
        width: '45px',
        renderHeader: this.renderActionColumn,
      },
    ];
  }

  get datasetKeyMap() {
    const { dataset } = this.props;
    return datasetHelper.getDatasetKeyMap({ dataset });
  }

  showDeleteConfirm(translationText) {
    const { intl } = this.props;
    const { renderDeleteConfirm } = this;

    return MessageBox.msgbox({
      title: intl.formatMessage(strings.DELETE_TRANSLATION),
      message: renderDeleteConfirm(translationText),
      customClass: 'delete-translation-confirm',
      showCancelButton: true,
      confirmButtonText: intl.formatMessage(strings.DELETE),
    });
  }

  showFixMessage() {
    const { intl } = this.props;
    const message = intl.formatMessage(strings.WARN_FIX_TRANSLATION);
    messageHelper.notifyWarning(message);
  }

  clearForm() {
    this.setState({
      form: {
        translationType: null,
        translation: {
          key: null,
          nodeType: null,
          translationType: null,
          value: null,
        },
      },
    });
  }

  handleFilterChange(filter) {
    this.setState({ filter });
  }

  handleCreateMode() {
    const { isCreateMode } = this.state;

    if (isCreateMode) return;
    if (!this.isFieldVaild) { this.showFixMessage(); return; }

    const { intl, dataset } = this.props;
    const { translationTypes } = dataset;
    const canCreateTranslation = translationConfigTypes
      .filter(translateType => !translationTypes.includes(translateType))
      .length;

    if (canCreateTranslation) {
      this.setState({ isCreateMode: true });
    } else {
      const message = intl.formatMessage(strings.TRANSLATION_UP_TO_MAX);
      messageHelper.notifyWarning(message);
    }
  }

  handleCancelCreate() {
    this.setState({ isCreateMode: false });
    this.clearForm();
  }

  handleInput(key, value) {
    const { form } = this.state;
    this.setState({ form: { ...form, [key]: value } });
  }

  handleSetAsField(translationType, row) {
    if (!this.isFieldVaild) { this.showFixMessage(); return; }

    const { key, nodeType } = row;
    const value = row.translation[translationType];
    this.handleInput('translation', { key, nodeType, translationType, value });
  }

  handleFieldInput(value) {
    const { form } = this.state;
    const { translation } = form;
    this.handleInput('translation', { ...translation, value });
  }

  handleFieldBlur() {
    if (this.isValidField()) {
      this.handleSubmitField();
    } else {
      setTimeout(() => {
        this.$currentInput.current.refs.input.focus();
      });
    }
  }

  isValidField() {
    const { form } = this.state;
    const { value = '' } = form.translation;
    const regex = business.nameRegExpInDataset.translate;
    const maxLength = configs.datasetMaxLength.translation;
    const isSpecialValue = !regex.test(value);
    const isMaxLength = value.length > maxLength;
    this.isFieldVaild = !isSpecialValue && !isMaxLength;
    this.setState({ isFieldVaild: this.isFieldVaild, isMaxLength });
    return this.isFieldVaild;
  }

  handleSubmitField() {
    const { boundDatasetActions } = this.props;
    const { datasetKeyMap } = this;
    const { form } = this.state;
    const { key, translationType, value = '' } = form.translation;
    const node = datasetKeyMap[key];
    const translation = { ...node.translation, [translationType]: value.trim() };
    const translationNode = { ...node, translation };

    switch (node.nodeType) {
      case 'table': boundDatasetActions.setDimTable(node.model, translationNode, false); break;
      case 'column': boundDatasetActions.setDimColumn(node.model, node.table, translationNode, false); break;
      case 'measure': boundDatasetActions.setDimMeasure(node.model, node.table, translationNode, false); break;
      case 'hierarchy': boundDatasetActions.setHierarchy(node.model, node.table, translationNode.name, translationNode, false); break;
      case 'calculateMeasure': boundDatasetActions.setCalculatedMeasure(translationNode.name, translationNode, false); break;
      case 'namedSet': boundDatasetActions.setNamedSet(translationNode.name, translationNode, false); break;
      default: break;
    }
    this.setState({ form: { ...form, translation: { ...translation, value: value.trim() } } });
  }

  handleSubmitCreate() {
    const { boundDatasetActions } = this.props;
    const { form } = this.state;

    this.$form.current.validate(isValid => {
      if (isValid) {
        boundDatasetActions.setTranslationType(form.translationType);
        this.setState({ isCreateMode: false });
        this.clearForm();
      }
    });
  }

  async handleDeleteTranslation(translationType) {
    if (!this.isFieldVaild) { this.showFixMessage(); return; }

    const { boundDatasetActions, intl } = this.props;
    const translationText = intl.formatMessage(strings[translationType]);
    const result = await this.showDeleteConfirm(translationText);

    if (result === 'confirm') {
      boundDatasetActions.deleteTranslationType(translationType);

      const message = intl.formatMessage(strings.SUCCESS_DELETE_TRANSLATION, { translationText });
      messageHelper.notifySuccess(message);
    }
  }

  renderDeleteConfirm(translationText) {
    const { intl } = this.props;
    return (
      <Fragment>
        <div className="delete-message-title">
          {intl.formatMessage(strings.CONFIRM_DELETE_TRANSLATION, { translationText })}
        </div>
        <Alert
          showIcon
          className="delete-message-description"
          type="error"
          icon="icon-superset-infor"
          closable={false}
          title={intl.formatMessage(strings.CONFIRM_DELETE_TRANSLATION_TIP, { translationText })}
        />
      </Fragment>
    );
  }

  renderNameHeader() {
    const { intl, language } = this.props;
    const { filter } = this.state;

    return (
      <div className="clearfix">
        <div className="header-left">
          {intl.formatMessage(strings.DEFAULT_TRANSLATION)}
        </div>
        <div className={`header-right ${language}`}>
          <SemanticSearchBox
            filter={filter}
            onChange={this.handleFilterChange}
          />
        </div>
      </div>
    );
  }

  renderNameColumn(row) {
    const { intl } = this.props;
    const nodeIcon = getNodeIcon(row);

    return (
      <span className="el-table-cell__text">
        <i className={classnames('node-icon', nodeIcon)} />
        <span className="node-text">{dataHelper.translate(intl, row.name)}</span>
      </span>
    );
  }

  renderAddTranslation() {
    const { intl, dataset, language } = this.props;
    const { form } = this.state;
    const { translationTypes } = dataset;

    return (
      <Form className="clearfix" ref={this.$form} model={form} rules={this.rules}>
        <Form.Item prop="translationType" className="header-left">
          <Select
            positionFixed
            key={language}
            value={form.translationType}
            onChange={value => this.handleInput('translationType', value)}
          >
            {translationConfigTypes
              .filter(translateType => !translationTypes.includes(translateType))
              .map(type => {
                const label = intl.formatMessage(strings[type]);
                return <Select.Option key={type} label={label} value={type} />;
              })}
          </Select>
        </Form.Item>
        <div className="header-right">
          <i className="icon-superset-close language-cancel" onClick={this.handleCancelCreate} />
          <i className="icon-superset-right language-ok" onClick={this.handleSubmitCreate} />
        </div>
      </Form>
    );
  }

  renderTranslationHeader(column) {
    const { intl } = this.props;
    const translateType = column.prop;
    return (
      <div className="clearfix">
        <div className="header-left">
          {intl.formatMessage(strings[translateType])}
        </div>
        <div className="header-right">
          <Tooltip appendToBody placement="top" content={intl.formatMessage(strings.DELETE)}>
            <i
              className="icon-superset-table_delete"
              onClick={() => this.handleDeleteTranslation(translateType)}
            />
          </Tooltip>
        </div>
      </div>
    );
  }

  renderTranslationInput(row, column) {
    if (row.translation) {
      const { intl } = this.props;
      const { $currentInput } = this;
      const { form, isFieldVaild, isMaxLength } = this.state;
      const currentTranslation = column.prop;
      const stateTranslation = form.translation.translationType;
      const storeValue = row.translation[currentTranslation];
      const stateValue = form.translation.value;
      const isCurrentField = form.translation.key === row.key;
      const isCurrentTranslation = currentTranslation === stateTranslation;
      const translationValue = isCurrentField && isCurrentTranslation ? stateValue : storeValue;
      const isCurrentErrorField = !isFieldVaild && isCurrentField && isCurrentTranslation;
      const maxLength = configs.datasetMaxLength.translation;
      const errorMsg = isCurrentErrorField && (
        isMaxLength
          ? intl.formatMessage(strings.TRANSLATION_TOO_LONG, { maxLength })
          : intl.formatMessage(strings.INVALID_TRANSLATION_IN_DATASET)
      );

      return !isCurrentErrorField ? (
        <Input
          size="small"
          value={translationValue}
          onFocus={() => this.handleSetAsField(currentTranslation, row)}
          onChange={value => this.handleFieldInput(value)}
          onBlur={() => this.handleFieldBlur()}
        />
      ) : (
        <Tooltip positionFixed className="error-input el-form-item is-error" placement="top" content={errorMsg}>
          <Input
            size="small"
            ref={$currentInput}
            value={translationValue}
            onFocus={() => this.handleSetAsField(currentTranslation, row)}
            onChange={value => this.handleFieldInput(value)}
            onBlur={() => this.handleFieldBlur()}
          />
        </Tooltip>
      );
    }
    return null;
  }

  renderActionColumn() {
    const { intl } = this.props;
    const { isCreateMode } = this.state;

    return (
      <Tooltip appendToBody placement="top" popperClass="add-translation-tip" content={intl.formatMessage(strings.ADD_TRANSLATION)}>
        <div className="add-translation" disabled={isCreateMode} onClick={this.handleCreateMode}>
          <i className="icon-superset-language_add" />
        </div>
      </Tooltip>
    );
  }

  render() {
    const { columns, tableData, filterRows, $table } = this;
    const { intl, dataset } = this.props;
    const { isCreateMode } = this.state;
    const { translationTypes } = dataset;
    const getColSpan = calcColSpan(translationTypes, isCreateMode);

    return (
      <div className="dataset-translation">
        <Layout.Row>
          <Layout.Col
            xxl={getColSpan('xxl')}
            xl={getColSpan('xl')}
            lg={getColSpan('lg')}
            md={getColSpan('md')}
            sm={getColSpan('sm')}
          >
            <TreeTable
              border
              isExpandAll
              rowKey="key"
              height="100%"
              ref={$table}
              columns={columns}
              data={tableData}
              emptyText={intl.formatMessage(strings.NO_DATA)}
              filterMethod={filterRows}
            />
          </Layout.Col>
        </Layout.Row>
      </div>
    );
  }
}
