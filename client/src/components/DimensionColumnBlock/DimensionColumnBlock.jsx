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
import { Form, Input, Button, Select, Switch, MessageBox, Tag, Tooltip, Alert, Loading } from 'kyligence-ui-react';

import './index.less';
import Block from '../Block/Block';
import { Connect, InjectIntl } from '../../store';
import { strings, configs } from '../../constants';
import { validator, dimColumnStrings } from './handler';
import { messageHelper, datasetHelper } from '../../utils';
import CodeEditor from '../CodeEditor/CodeEditor';
import FolderInput from '../FolderInput/FolderInput';

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
class DimensionColumnBlock extends PureComponent {
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
    isValidating: false,
    form: {
      name: '',
      alias: '',
      desc: '',
      type: '',
      dataType: '',
      isVisible: true,
      invisible: [],
      visible: [],
      nameColumn: null,
      valueColumn: null,
      properties: [],
      subfolder: '',
      defaultMember: '',
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
    const isDulp = this.isColumnAliasDuplicate;
    return {
      alias: [{ required: true, validator: validator.alias(this.props, isDulp), trigger: 'blur' }],
      nameColumn: [{ validator: validator.nameColumn(this), trigger: 'change' }],
      valueColumn: [{ validator: validator.valueColumn(this), trigger: 'change' }],
      properties: [{ validator: validator.properties(this), trigger: 'change' }],
      subfolder: [{ validator: validator.subfolder(this.props), trigger: 'blur' }],
      defaultMember: [{ validator: validator.defaultMember(this), trigger: 'blur' }],
      // desc: [{ validator: validator.desc(this.props), trigger: 'blur' }],
    };
  }

  // 重名规则：当前table下是否重名
  get isColumnAliasDuplicate() {
    const { form } = this.state;
    const { data, dataset } = this.props;
    const { model, table, alias } = data;
    const currentModel = dataset.models.find(item => item.name === model);
    const currentTable = currentModel.dimensionTables.find(item => item.name === table);
    const currentColumns = currentTable.dimCols;
    return currentColumns
      .map(item => item.alias.toUpperCase())
      .filter(item => item !== alias.toUpperCase())
      .includes(form.alias.toUpperCase());
  }

  get dimColumnOptions() {
    const { intl: { formatMessage } } = this.props;
    return Object.entries(configs.dimColumnTypes)
      .map(([labelKey, value]) => {
        const label = formatMessage(strings[labelKey]);
        return { label, value };
      });
  }

  get currentModel() {
    const { dataset, data } = this.props;
    return dataset.models.find(model => (
      model.name === data.model
    ));
  }

  get currentTable() {
    const { data } = this.props;
    return this.currentModel.dimensionTables.find(table => (
      table.name === data.table
    ));
  }

  get nameColumnOptions() {
    const { intl: { formatMessage }, data } = this.props;
    const { nameColumnError } = data;
    const errorOption = nameColumnError && nameColumnError.obj.split('.')[2];

    return [
      { label: formatMessage(strings.NONE), value: null },
      ...this.currentTable.dimCols
        .map(column => ({
          label: column.name,
          value: column.name,
          disabled: errorOption === column.name,
        })),
    ];
  }

  get valueColumnOptions() {
    const { intl: { formatMessage }, data } = this.props;
    const { valueColumnError } = data;
    const errorOption = valueColumnError && valueColumnError.obj.split('.')[2];

    return [
      { label: formatMessage(strings.NONE), value: null },
      ...this.currentTable.dimCols
        .map(column => ({
          label: column.name,
          value: column.name,
          disabled: errorOption === column.name,
        })),
    ];
  }

  get propertyOptions() {
    const { data } = this.props;

    return [
      ...this.currentTable.dimCols
        .map(column => ({
          name: column.alias,
          columnName: column.name,
          columnAlias: column.alias,
          disabled: this.isInvalidProperty(column.name),
        }))
        .filter(({ columnName }) => columnName !== data.name),
    ];
  }

  get nameColumnMsg() {
    const { intl: { formatMessage }, data } = this.props;
    const { datasetKeyMap } = this;
    const { form } = this.state;
    const { nameColumnError } = data;

    let message = null;
    if (nameColumnError) {
      const [modelName, tableName, columnName] = nameColumnError.obj.split('.');
      if (columnName === form.nameColumn) {
        const tableAlias = datasetKeyMap[`${modelName}-${tableName}`].alias;
        const params = { modelName, tableAlias, columnName };
        message = formatMessage(strings.NAME_COLUMN_DIM_COL_DELETED, params);
      }
    }
    return message;
  }

  get valueColumnMsg() {
    const { intl: { formatMessage }, data } = this.props;
    const { form } = this.state;
    const { datasetKeyMap } = this;
    const { valueColumnError } = data;

    let message = null;
    if (valueColumnError) {
      const [modelName, tableName, columnName] = valueColumnError.obj.split('.');
      if (columnName === form.valueColumn) {
        const tableAlias = datasetKeyMap[`${modelName}-${tableName}`].alias;
        const params = { modelName, tableAlias, columnName };
        message = formatMessage(strings.VALUE_COLUMN_DIM_COL_DELETED, params);
      }
    }
    return message;
  }

  get defaultMemberMsg() {
    const { intl: { formatMessage }, data } = this.props;
    return data.defaultMemberError && formatMessage(strings.INVALID_DEFAULT_MEMBER_EXPRESSION);
  }

  /* eslint-disable react/jsx-one-expression-per-line */
  get invalidPropertiesMsg() {
    const { intl: { formatMessage }, data } = this.props;
    const { datasetKeyMap } = this;
    const { propertyErrors } = data;
    const modelName = data.model;
    const tableName = data.table;
    const tableAlias = datasetKeyMap[`${modelName}-${tableName}`].alias;

    return propertyErrors && propertyErrors.length
      ? (
        <Fragment>
          <div>{formatMessage(strings.PROPERTY_TOTAL_COLUMN_DELETED)}</div>
          {propertyErrors.map(propertyError => {
            const columnName = propertyError.obj.split('.')[2];
            const columnKey = `${modelName}-${tableName}-c-${columnName}`;
            const columnAlias = datasetKeyMap[columnKey].alias;
            return <div key={columnKey}>[{modelName}].[{tableAlias}].[{columnAlias}]</div>;
          })}
        </Fragment>
      )
      : null;
  }

  get hasInvalidProperty() {
    const { data } = this.props;
    return data.properties.some(property => this.isInvalidProperty(property.columnName));
  }

  get defaultMemberCompletions() {
    const { dataset, data: column } = this.props;
    const { currentTable: table } = this;
    let columnCompletions = [];

    if (table) {
      const { columns } = datasetHelper.getMdxDatasetWords(dataset);
      columnCompletions = columns.filter(completion => (
        completion.meta === 'dimension' && completion.value.indexOf(`[${table.alias}].[${column.alias}]`) === 0
      ));
    }

    return columnCompletions;
  }

  get datasetKeyMap() {
    const { dataset } = this.props;
    return datasetHelper.getDatasetKeyMap({ dataset });
  }

  get folderOptions() {
    const { dataset, data } = this.props;
    const { model: modelName, table: tableName } = data;
    return datasetHelper.getDimensionFolderOptions({ dataset, modelName, tableName });
  }

  getInvalidPropertyMsg = propertyId => {
    const { intl: { formatMessage }, data } = this.props;
    const { datasetKeyMap } = this;
    const modelName = data.model;
    const tableName = data.table;
    const tableAlias = datasetKeyMap[`${modelName}-${tableName}`].alias;
    const columnAlias = datasetKeyMap[`${modelName}-${tableName}-c-${propertyId}`].alias;
    const params = { modelName, tableAlias, columnName: columnAlias };
    return formatMessage(strings.PROPERTY_COLUMN_DIM_COL_DELETED, params);
  }

  isInvalidProperty = propertyId => {
    const { data } = this.props;
    const { propertyErrors } = data;
    return propertyErrors
      ? propertyErrors.some(error => error.obj.split('.')[2] === propertyId)
      : false;
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

  showVisibleAlert = () => {
    const { intl: { formatMessage } } = this.props;
    const { form } = this.state;
    const itemType = formatMessage(strings.DIMENSION).toLowerCase();
    const itemName = form.alias;
    const params = { itemType, itemName };
    const messageContent = formatMessage(strings.CONFIRM_INVISIBLE_DATASET, params);
    const messageTitle = formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  handleInput = (key, value) => {
    const { form: oldForm } = this.state;
    const form = { ...oldForm, [key]: value };
    this.setState({ form });
  }

  handleInputPropery = (columnNames = []) => {
    const { datasetKeyMap } = this;
    const { form: oldForm } = this.state;

    const properties = columnNames.map(name => {
      const model = this.currentModel.name;
      const table = this.currentTable.name;
      const datasetKey = `${model}-${table}-c-${name}`;
      const { alias } = datasetKeyMap[datasetKey];
      // name是properyName，目前等于columnAlias
      // columnName是前端唯一标识维度列的id
      // columnAlias是后端需要使用的alias
      return { name: alias, columnName: name, columnAlias: alias };
    });
    const form = { ...oldForm, properties };
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
    setTimeout(() => {
      this.$form.current.validate();
    });
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

    const params = { columnName: newForm.alias };
    const message = formatMessage(strings.SUCCESS_SUBMIT_DIMENSION, params);
    messageHelper.notifySuccess(message);
  }

  handleSubmit = () => {
    const { boundDatasetActions, data: oldData, onSubmit } = this.props;
    const { form } = this.state;

    this.$form.current.validate(async valid => {
      if (valid) {
        const data = boundDatasetActions.setDimColumn(oldData.model, oldData.table, form);

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
          {isEditMode
            ? formatMessage(strings.EDIT_DIMENSION)
            : formatMessage(strings.SHOW_DIMENSION)}
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

  renderDefaultMemberLabel = () => {
    const { intl: { formatMessage } } = this.props;
    return (
      <div className="default-member-label">
        <span>{formatMessage(strings.DEFAULT_MEMBER)}</span>
        <Tooltip appendToBody placement="right" popperClass="el-form-item__description_popper" content={formatMessage(strings.DEFAULT_MEMBER_DESC)}>
          <i className="icon-superset-what" />
        </Tooltip>
        <span className="beta">Beta</span>
      </div>
    );
  }

  render() {
    const {
      dimColumnOptions,
      nameColumnOptions,
      valueColumnOptions,
      propertyOptions,
      defaultMemberCompletions,
      isInvalidProperty,
      hasInvalidProperty,
      invalidPropertiesMsg,
      nameColumnMsg,
      valueColumnMsg,
      defaultMemberMsg,
      $form,
    } = this;

    const { intl: { locale, formatMessage }, className } = this.props;
    const { isEditMode, isValidating, form } = this.state;

    return (
      <Block
        className={classNames(className, 'dimension-column-block')}
        header={this.renderHeader()}
      >
        {isEditMode ? (
          <Form ref={$form} model={form} labelPosition="top" rules={this.rules}>
            <Form.Item label={formatMessage(strings.KEY_COLUMN)} prop="name">
              <Input disabled value={form.name} onChange={value => this.handleInput('name', value)} />
            </Form.Item>
            <Form.Item label={formatMessage(strings.DIMENSION_NAME)} prop="alias">
              <Input value={form.alias} onChange={value => this.handleInput('alias', value)} />
            </Form.Item>
            <Form.Item label={formatMessage(strings.DATA_TYPE)} prop="dataType">
              <Input disabled value={form.dataType} onChange={value => this.handleInput('dataType', value)} />
            </Form.Item>
            <Form.Item
              prop="type"
              label={formatMessage(strings.TYPE)}
              description={formatMessage(strings.TYPE_DESC_FOR_COLUMN)}
            >
              <Select key={locale} value={form.type} onChange={value => this.handleInput('type', value)} popperProps={configs.disablePopperAutoFlip}>
                {dimColumnOptions.map(option => (
                  <Select.Option key={option.value} label={option.label} value={option.value} />
                ))}
              </Select>
            </Form.Item>
            <Form.Item
              prop="nameColumn"
              className="notity-block"
              label={formatMessage(strings.NAME_COLUMN)}
              description={formatMessage(strings.NAME_COLUMN_DESC)}
            >
              <Alert
                type="info"
                icon="icon-superset-infor"
                closable={false}
                title={formatMessage(strings.NAME_COLUMN_TIP)}
              />
              <Select key={locale} filterable value={form.nameColumn || null} onChange={value => this.handleInput('nameColumn', value)} popperProps={configs.disablePopperAutoFlip}>
                {nameColumnOptions.map(option => (
                  <Select.Option
                    key={option.value}
                    label={option.label}
                    value={option.value}
                    disabled={option.disabled}
                  />
                ))}
              </Select>
            </Form.Item>
            <Form.Item
              prop="valueColumn"
              className="notity-block"
              label={formatMessage(strings.VALUE_COLUMN)}
              description={formatMessage(strings.VALUE_COLUMN_DESC)}
            >
              <Alert
                type="info"
                icon="icon-superset-infor"
                closable={false}
                title={formatMessage(strings.VALUE_COLUMN_TIP)}
              />
              <Select key={locale} filterable value={form.valueColumn || null} onChange={value => this.handleInput('valueColumn', value)} popperProps={configs.disablePopperAutoFlip}>
                {valueColumnOptions.map(option => (
                  <Select.Option
                    key={option.value}
                    label={option.label}
                    value={option.value}
                    disabled={option.disabled}
                  />
                ))}
              </Select>
            </Form.Item>
            <Form.Item
              prop="properties"
              className="notity-block"
              label={formatMessage(strings.PROPERTIES)}
              description={formatMessage(strings.PROPERTIES_DESC)}
            >
              <Alert
                type="info"
                icon="icon-superset-infor"
                closable={false}
                title={formatMessage(strings.PROPERTIES_TIP)}
              />
              <Select
                multiple
                filterable
                value={form.properties.map(property => property.columnName)}
                onChange={value => this.handleInputPropery(value)}
                popperProps={configs.disablePopperAutoFlip}
              >
                {propertyOptions.map(option => (
                  <Select.Option
                    key={option.columnName}
                    label={option.columnAlias}
                    value={option.columnName}
                    disabled={option.disabled}
                  />
                ))}
              </Select>
            </Form.Item>
            <FolderInput
              prop="subfolder"
              name={form.alias}
              nodeType={nodeTypes.DIM_COLUMN}
              value={form.subfolder}
              options={this.folderOptions}
              onChange={this.handleInput}
            />
            <Form.Item label={formatMessage(strings.VISIBLE)} prop="isVisible" className="form-switch">
              <Switch value={form.isVisible} onText="ON" offText="OFF" onChange={value => this.handleChangeVisible(value)} />
            </Form.Item>
            <div className="mdx-tip">
              <i className="icon-superset-alert" />
              <div className="mdx-tip-content">{formatMessage(strings.VISIBILITY_TIP)}</div>
            </div>
            <Form.Item
              prop="defaultMember"
              label={this.renderDefaultMemberLabel()}
            >
              <Loading className="mdx-it-default-member-expression-validate-loading" loading={isValidating}>
                <CodeEditor
                  className="mdx-it-dimension-default-member-input"
                  mode="mdx"
                  height="200px"
                  width="100%"
                  placeholder={formatMessage(strings.PLEASE_ENTER)}
                  minLines={8}
                  maxLines={30}
                  value={form.defaultMember}
                  completions={defaultMemberCompletions}
                  onChange={value => this.handleInput('defaultMember', value)}
                />
              </Loading>
            </Form.Item>
            <div className="mdx-tip">
              <i className="icon-superset-alert" />
              <div className="mdx-tip-content">
                {formatMessage(strings.MDX_DEFAULT_MEMBER_TIP)}
              </div>
            </div>
            {/* MDX中暂不开放 */}
            {/* <Form.Item label={formatMessage(strings.DESCRIPTION)} prop="desc">
              <Input value={form.desc} onChange={value => this.handleInput('desc', value)} />
            </Form.Item> */}
          </Form>
        ) : (
          <table className="plain-table">
            <tbody>
              <tr>
                <td className="font-medium">{formatMessage(strings.KEY_COLUMN)}</td>
                <td style={{ whiteSpace: 'pre-wrap' }}>{form.name}</td>
              </tr>
              <tr>
                <td className="font-medium">{formatMessage(strings.DIMENSION_NAME)}</td>
                <td>{form.alias}</td>
              </tr>
              <tr>
                <td className="font-medium">{formatMessage(strings.DATA_TYPE)}</td>
                <td>{form.dataType}</td>
              </tr>
              <tr>
                <td className="font-medium">{formatMessage(strings.TYPE)}</td>
                <td>{formatMessage(strings[dimColumnStrings[form.type]])}</td>
              </tr>
              <tr>
                <td className="font-medium">
                  {formatMessage(strings.NAME_COLUMN)}
                  {nameColumnMsg && (
                    <Tooltip positionFixed placement="top" content={nameColumnMsg}>
                      <i className="error-icon icon-superset-alert" />
                    </Tooltip>
                  )}
                </td>
                <td>{form.nameColumn ? form.nameColumn : formatMessage(strings.NONE)}</td>
              </tr>
              <tr>
                <td className="font-medium">
                  {formatMessage(strings.VALUE_COLUMN)}
                  {valueColumnMsg && (
                    <Tooltip positionFixed placement="top" content={valueColumnMsg}>
                      <i className="error-icon icon-superset-alert" />
                    </Tooltip>
                  )}
                </td>
                <td>{form.valueColumn ? form.valueColumn : formatMessage(strings.NONE)}</td>
              </tr>
              <tr>
                <td className="font-medium">
                  {formatMessage(strings.PROPERTIES)}
                  {hasInvalidProperty && (
                    <Tooltip positionFixed placement="top" content={invalidPropertiesMsg}>
                      <i className="error-icon icon-superset-alert" />
                    </Tooltip>
                  )}
                </td>
                {!!form.properties.length && (
                  <td className="tag-list">
                    {form.properties.map(property => {
                      const isValidProperty = !isInvalidProperty(property.columnName);
                      const invalidPropertyMsg = this.getInvalidPropertyMsg(property.columnName);
                      return isValidProperty ? (
                        <Tag type={isValidProperty ? 'primary' : 'danger'} key={property.columnName}>
                          {property.columnAlias}
                        </Tag>
                      ) : (
                        <Tooltip positionFixed placement="top" content={invalidPropertyMsg} key={property.columnName}>
                          <Tag type={isValidProperty ? 'primary' : 'danger'}>
                            {property.columnAlias}
                          </Tag>
                        </Tooltip>
                      );
                    })}
                  </td>
                )}
                {!form.properties.length && (
                  <td>{formatMessage(strings.NONE)}</td>
                )}
              </tr>
              <tr>
                <td className="font-medium">{formatMessage(strings.FOLDER)}</td>
                <td style={{ whiteSpace: 'pre-wrap' }}>
                  {form.subfolder ? form.subfolder : formatMessage(strings.NONE)}
                </td>
              </tr>
              <tr>
                <td className="font-medium">{formatMessage(strings.VISIBLE)}</td>
                <td>
                  {form.isVisible
                    ? formatMessage(strings.YES)
                    : formatMessage(strings.NO)}
                </td>
              </tr>
              <tr>
                <td className="font-medium">
                  {formatMessage(strings.DEFAULT_MEMBER)}
                  {defaultMemberMsg && (
                    <Tooltip positionFixed placement="top" content={defaultMemberMsg}>
                      <i className="error-icon icon-superset-alert" />
                    </Tooltip>
                  )}
                </td>
                <td>
                  {form.defaultMember ? form.defaultMember : formatMessage(strings.NONE)}
                </td>
              </tr>
              {/* MDX中暂不开放 */}
              {/* <tr>
                <td className="font-medium">{formatMessage(strings.DESCRIPTION)}</td>
                <td>{form.desc}</td>
              </tr> */}
            </tbody>
          </table>
        )}
      </Block>
    );
  }
}
