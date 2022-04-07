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
import classnames from 'classnames';
import { Form, Input, Button, Select, Cascader, MessageBox, Tooltip } from 'kyligence-ui-react';

import './index.less';
import Block from '../Block/Block';
import { Connect, InjectIntl } from '../../store';
import { strings, configs } from '../../constants';
import { messageHelper, datasetHelper } from '../../utils';
import { validator, getModelTableOptions, getColumnsOptions, getColumnAliasDict, getErrorLevels, getErrorWeights, getErrorColumns } from './handler';

const EMPTY_ARRAY = [];

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
class HierarchyBlock extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    boundGlobalActions: PropTypes.object.isRequired,
    boundModalActions: PropTypes.object.isRequired,
    className: PropTypes.string,
    intl: PropTypes.object.isRequired,
    data: PropTypes.object.isRequired,
    dataset: PropTypes.object.isRequired,
    onDelete: PropTypes.func.isRequired,
    onCancelCreate: PropTypes.func.isRequired,
    onSubmit: PropTypes.func.isRequired,
  };

  static defaultProps = {
    className: '',
  };

  $form = React.createRef();

  state = {
    isEditMode: false,
    form: {
      name: '',
      desc: '',
      tablePath: [],
      dimCols: [],
      weightCols: [],
      translation: [],
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

  get isCreating() {
    const { data } = this.props;
    const { isEditMode } = this.state;
    return !data.name && isEditMode;
  }

  get rules() {
    const { errorLevels } = this;
    return {
      name: [{ required: true, validator: validator.name(this.props, this.isHierarchyDuplicate), trigger: 'blur' }],
      // desc: [{ validator: validator.desc(this.props), trigger: 'blur' }],
      tablePath: [{ required: true, validator: validator.tablePath(this.props), trigger: 'change' }],
      dimCols: [{ required: true, validator: validator.dimCols(this.props, errorLevels, this.columnsOptions), trigger: 'change' }],
    };
  }

  // 重名规则：当前table下是否重名
  get isHierarchyDuplicate() {
    const { form } = this.state;
    const { dataset, data } = this.props;
    const { tablePath, name } = form;
    const [model, table] = tablePath;
    const currentModel = dataset.models.find(item => item.name === model) || {};
    const currentTables = currentModel.dimensionTables || [];
    const currentTable = currentTables.find(item => item.name === table) || {};
    const currentColumns = currentTable.hierarchys || [];
    const oldName = data.name && data.name.toUpperCase();
    return currentColumns
      .map(item => item.name.toUpperCase())
      .filter(item => item !== oldName)
      .includes(name.toUpperCase().trim());
  }

  get modelTableOptions() {
    const { dataset } = this.props;
    return getModelTableOptions(dataset);
  }

  get columnsOptions() {
    const { dataset } = this.props;
    const { form } = this.state;
    return getColumnsOptions(dataset, form.tablePath);
  }

  get columnAliasDict() {
    const { columnsOptions: columns } = this;
    return getColumnAliasDict({ columns });
  }

  get errorLevels() {
    const { data: { errors = EMPTY_ARRAY } } = this.props;
    return getErrorLevels({ errors });
  }

  get errorWeights() {
    const { data: { errors = EMPTY_ARRAY } } = this.props;
    return getErrorWeights({ errors });
  }

  get errorColumns() {
    const { data: { errors = EMPTY_ARRAY } } = this.props;
    return getErrorColumns({ errors });
  }

  get datasetKeyMap() {
    const { dataset } = this.props;
    return datasetHelper.getDatasetKeyMap({ dataset });
  }

  get isTimeTable() {
    // https://olapio.atlassian.net/browse/MDX-3286
    const { form: { tablePath } } = this.state;
    const { datasetKeyMap } = this;
    const [model, table] = tablePath;

    if (model && table) {
      const currentTable = datasetKeyMap[`${model}-${table}`];
      return currentTable.type === configs.dimTableTypes.TIME;
    }
    return false;
  }

  /* eslint-disable max-len */
  asyncFormData = (nextProps = {}) => {
    const { data: oldData } = this.props;
    const { data: newData } = nextProps;
    const data = newData || oldData;

    if (oldData !== newData) {
      const { name = '', model, table, dimCols = [], weightCols = [], desc = '', translation = {} } = data;
      const isEditMode = !name;
      const tablePath = [
        ...model ? [model] : [],
        ...table ? [table] : [],
      ];
      this.setState({ form: { name, desc, tablePath, dimCols, weightCols, translation } });
      this.handleToggleEdit(isEditMode);
    }
  }

  showDeleteAlert = () => {
    const { intl } = this.props;
    const { form } = this.state;
    const params = { hierarchyName: form.name };
    const messageContent = intl.formatMessage(strings.CONFIRM_DELETE_HIERARCHY, params);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  // 这里只会牵涉层级结构的dimCols增减
  refreshWeightCols = (oldForm, newForm) => {
    const { dimCols: oldDimCols, weightCols: oldWeightCols } = oldForm;
    const { dimCols: newDimCols } = newForm;

    const oldWeightColDict = {};
    oldDimCols.forEach((oldDimCol, index) => {
      oldWeightColDict[oldDimCol] = oldWeightCols[index] || null;
    });

    return newDimCols.map(dimCol => oldWeightColDict[dimCol] || null);
  }

  handleInput = (key, value) => {
    const { form: oldForm } = this.state;
    let form = { ...oldForm, [key]: value };
    if (key === 'tablePath') {
      form = { ...form, dimCols: [] };
    }
    if (key === 'dimCols') {
      const weightCols = this.refreshWeightCols(oldForm, form);
      form = { ...form, weightCols };
    }
    this.setState({ form });
  }

  handleClickEdit = () => {
    const { data: { errors = [] } } = this.props;
    this.handleToggleEdit(true);

    setTimeout(() => {
      if (errors.length && this.$form.current) {
        this.$form.current.validate();
      }
    });
  }

  showDeleteSuccess = form => {
    const { intl } = this.props;

    const params = { hierarchyName: form.name };
    const message = intl.formatMessage(strings.SUCCESS_DELETE_HIERARCHY, params);
    messageHelper.notifySuccess(message);
  }

  handleClickDelete = async () => {
    const { boundDatasetActions, onDelete } = this.props;
    const { form } = this.state;
    const [model, table] = form.tablePath;

    await this.showDeleteAlert();
    await boundDatasetActions.deleteHierarchy(model, table, form);
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

    const params = { hierarchyName: newForm.name };
    const message = intl.formatMessage(strings.SUCCESS_SUBMIT_HIERARCHY, params);
    messageHelper.notifySuccess(message);
  }

  handleSetWeight = async () => {
    const { boundModalActions, data } = this.props;
    const { form: { dimCols: oldDimCols, weightCols: oldWeightCols, tablePath } } = this.state;
    const { columnsOptions } = this;
    const [modelName, tableName] = tablePath;

    await boundModalActions.setModalData('HierarchyWeightModal', { modelName, tableName, columns: columnsOptions, errors: data.errors });
    const { isSubmit, weightCols } = await boundModalActions.showModal('HierarchyWeightModal', { dimCols: oldDimCols, weightCols: oldWeightCols });

    if (isSubmit) {
      this.handleInput('weightCols', weightCols);
    }
  }

  handleSubmit = () => {
    const { boundDatasetActions, onSubmit, data: oldData } = this.props;
    const { form } = this.state;
    const [model, table] = form.tablePath;

    this.$form.current.validate(async valid => {
      if (valid) {
        const newForm = { ...form, errors: [] };
        const data = this.isCreating
          ? boundDatasetActions.setHierarchy(model, table, null, newForm)
          : boundDatasetActions.setHierarchy(model, table, oldData.name, newForm);

        if (!this.isCreating) {
          if (oldData.model !== model || oldData.table !== table) {
            boundDatasetActions.deleteHierarchy(oldData.model, oldData.table, oldData);
          }
        }

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
    const { isCreating } = this;

    const editTitle = isCreating
      ? intl.formatMessage(strings.ADD_HIERARCHY)
      : intl.formatMessage(strings.EDIT_HIERARCHY);

    return (
      <div className="clearfix">
        <div className="pull-left">
          {isEditMode
            ? editTitle
            : intl.formatMessage(strings.SHOW_HIERARCHY)}
        </div>
        <div className="pull-right">
          {isEditMode ? (
            <Fragment key={isEditMode}>
              <Button type="default" size="small" icon="icon-superset-close" onClick={this.handleCancelEdit}>
                {intl.formatMessage(strings.CANCEL)}
              </Button>
              <Button plain type="primary" size="small" icon="icon-superset-table_save" onClick={this.handleSubmit}>
                {intl.formatMessage(strings.SAVE)}
              </Button>
            </Fragment>
          ) : (
            <Fragment key={isEditMode}>
              <Button size="small" icon="icon-superset-table_delete" onClick={this.handleClickDelete}>
                {intl.formatMessage(strings.DELETE)}
              </Button>
              <Button size="small" icon="icon-superset-table_edit" onClick={this.handleClickEdit}>
                {intl.formatMessage(strings.EDIT)}
              </Button>
            </Fragment>
          )}
        </div>
      </div>
    );
  }

  renderHierarchyLabel = () => {
    const { intl: { formatMessage } } = this.props;
    const { form } = this.state;
    const { isTimeTable } = this;
    const isCustomRollupDisable = form.dimCols.length < 2;
    return (
      <Fragment>
        <span>{formatMessage(strings.HIERARCHY)}</span>
        {!isTimeTable && (
          <div className="hierarchy-weight">
            <Button type="text" size="small" disabled={isCustomRollupDisable} onClick={this.handleSetWeight}>
              {formatMessage(strings.SET_WEIGHT)}
            </Button>
          </div>
        )}
      </Fragment>
    );
  }

  renderErrorTips = (dimCol, weightCol) => {
    const { intl: { formatMessage } } = this.props;
    const { form: { dimCols, weightCols, tablePath: [modelName, tableName] } } = this.state;
    const { errorColumns } = this;
    const isCurrentLevelError = errorColumns.includes(dimCol) || errorColumns.includes(weightCol);
    const hasErrorColumns = (dimCols.length && dimCols.some(col => errorColumns.includes(col))) ||
      (weightCols.length && weightCols.some(col => errorColumns.includes(col)));

    let lostColumns = '';

    if (isCurrentLevelError) {
      lostColumns = [];

      if (errorColumns.includes(dimCol)) {
        lostColumns.push(`${formatMessage(strings.DIMENSION).toLocaleLowerCase()} [${modelName}].[${tableName}].[${dimCol}]`);
      }
      if (errorColumns.includes(weightCol)) {
        lostColumns.push(`${formatMessage(strings.WEIGHT_COLUMN).toLocaleLowerCase()} [${modelName}].[${tableName}].[${weightCol}]`);
      }
      // Upper case first letter
      const [firstLetter, ...otherLetters] = lostColumns.join(formatMessage(strings.BETWEEN_SPLIT));
      lostColumns = [
        firstLetter.toUpperCase(),
        ...otherLetters,
      ].join('');
    }

    return hasErrorColumns ? (
      <Tooltip placement="top" disabled={!lostColumns} content={formatMessage(strings.COLUMN_LOST_IN_KE, { lostColumns })}>
        <i className={classnames('error-icon', 'icon-superset-alert', !isCurrentLevelError && 'disappear')} />
      </Tooltip>
    ) : null;
  }

  renderLevelIndexAndWeight = levelIndex => (
    <Fragment>
      <div className="index-icon">
        <i>{levelIndex + 1}</i>
      </div>
      {this.renderLevelWeight(levelIndex)}
    </Fragment>
  )

  renderLevelWeight = levelIndex => {
    const { columnAliasDict } = this;
    const { form: { weightCols } } = this.state;
    const isWeightLevel = !!weightCols[levelIndex];
    const hasWeightCol = weightCols.filter(w => w).length;
    const weightIconClass = classnames('icon-superset-weight', !isWeightLevel && 'disappear');

    return hasWeightCol ? (
      <Tooltip
        appendToBody
        placement="top"
        popperClass="weight-column-tooltip"
        content={columnAliasDict[weightCols[levelIndex]]}
        disabled={!isWeightLevel}
        popperProps={configs.disablePopperAutoFlip}
      >
        <i className={weightIconClass} />
      </Tooltip>
    ) : null;
  }

  render() {
    const { intl, className } = this.props;
    const { isEditMode, form } = this.state;
    const { modelTableOptions, columnsOptions, $form, renderHierarchyLabel, errorLevels } = this;

    return (
      <Block
        className={classnames('hierarchy-block', className)}
        header={this.renderHeader()}
      >
        {isEditMode ? (
          <Form ref={$form} model={form} labelPosition="top" rules={this.rules}>
            <Form.Item label={intl.formatMessage(strings.NAME)} prop="name">
              <Input value={form.name} onChange={value => this.handleInput('name', value)} />
            </Form.Item>
            {/* MDX中暂不开放 */}
            {/* <Form.Item label={intl.formatMessage(strings.DESCRIPTION)} prop="desc">
              <Input value={form.desc} onChange={value => this.handleInput('desc', value)} />
            </Form.Item> */}
            <Form.Item label={intl.formatMessage(strings.MODEL_TABLE)} prop="tablePath">
              <Cascader
                clearable
                filterable
                expandTrigger="hover"
                value={form.tablePath}
                placeholder={intl.formatMessage(strings.SEARCH_MODEL_TABLE)}
                options={modelTableOptions}
                onChange={value => this.handleInput('tablePath', value)}
                popperProps={configs.disablePopperAutoFlip}
              />
            </Form.Item>
            <Form.Item labelClass="dim-cols-field" label={renderHierarchyLabel()} prop="dimCols">
              <Select
                multiple
                filterable
                value={form.dimCols}
                onChange={value => this.handleInput('dimCols', value)}
                popperProps={configs.disablePopperAutoFlip}
              >
                {columnsOptions.map(column => (
                  <Select.Option
                    key={column.value}
                    label={column.label}
                    value={column.value}
                    disabled={
                      (errorLevels.includes(column.value) && !form.dimCols.includes(column.value)) ||
                      // hierarchy levels should not has weight columns
                      form.weightCols.includes(column.value)
                    }
                  />
                ))}
              </Select>
            </Form.Item>
          </Form>
        ) : (
          <table className="plain-table">
            <tbody>
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.NAME)}</td>
                <td className="mdx-it-hierarchy-display-name" style={{ whiteSpace: 'pre-wrap' }}>{form.name}</td>
              </tr>
              {/* MDX中暂不开放 */}
              {/* <tr>
                <td className="font-medium">{intl.formatMessage(strings.DESCRIPTION)}</td>
                <td>{form.desc}</td>
              </tr> */}
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.MODEL_TABLE)}</td>
                {(() => {
                  const [model, table] = form.tablePath;
                  const tableAlias = modelTableOptions
                    .find(m => m.value === model).children
                    .find(t => t.value === table).label;

                  return (
                    <td>
                      <span>{model}</span>
                      <span>/</span>
                      <span>{tableAlias}</span>
                    </td>
                  );
                })()}
              </tr>
              <tr>
                <td className="font-medium">{intl.formatMessage(strings.HIERARCHY)}</td>
                <td style={{ padding: 0 }}>
                  {form.dimCols.map((dimCol, dimColIndex) => {
                    const weightCol = form.weightCols[dimColIndex];
                    return (
                      <div className="inline-row" key={dimCol}>
                        {this.renderErrorTips(dimCol, weightCol)}
                        {this.renderLevelIndexAndWeight(dimColIndex)}
                        <span>
                          {(() => {
                            const column = columnsOptions.find(option => option.value === dimCol);
                            return column ? column.label : dimCol;
                          })()}
                        </span>
                      </div>
                    );
                  })}
                </td>
              </tr>
            </tbody>
          </table>
        )}
      </Block>
    );
  }
}
