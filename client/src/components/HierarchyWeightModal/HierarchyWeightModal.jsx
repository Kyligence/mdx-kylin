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
import React, { PureComponent } from 'react';
import PropTypes from 'prop-types';
import { Dialog, Button, Form, Input, Select } from 'kyligence-ui-react';

import './index.less';
import { strings, configs } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { getDefaultState, getColumnAliasDict, validator, getErrorColumns } from './handler';

const { numericalTypes } = configs;
const EMPTY_ARRAY = [];

export default
@Connect({
  namespace: 'modal/HierarchyWeightModal',
  defaultState: getDefaultState(),
  mapState: {
    isShow: state => state.isShow,
    form: state => state.form,
    modelName: state => state.modelName,
    tableName: state => state.tableName,
    callback: state => state.callback,
    columns: state => state.columns,
    errors: state => state.errors,
  },
}, {
  mapState: {
    dataset: state => state.workspace.dataset,
  },
})
@InjectIntl()
class HierarchyWeightModal extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    isShow: PropTypes.bool.isRequired,
    columns: PropTypes.array.isRequired,
    form: PropTypes.object.isRequired,
    modelName: PropTypes.string.isRequired,
    tableName: PropTypes.string.isRequired,
    errors: PropTypes.array.isRequired,
    callback: PropTypes.func.isRequired,
    dataset: PropTypes.object.isRequired,
  };

  $form = React.createRef();

  state = {
    isSubmiting: false,
  };

  /* eslint-disable camelcase */
  UNSAFE_componentWillMount() {
    const { boundModalActions } = this.props;
    boundModalActions.registerModal('HierarchyWeightModal', getDefaultState());
  }

  componentDidUpdate(prevProps) {
    const { isShow: oldIsShow } = prevProps;
    const { isShow: newIsShow } = this.props;

    if (!oldIsShow && newIsShow) {
      setTimeout(() => {
        this.$form.current.validate();
      });
    }
  }

  get columnAliasDict() {
    const { columns } = this.props;
    return getColumnAliasDict({ columns });
  }

  get weightColOptions() {
    const { errorColumns } = this;
    const { columns, form: { dimCols } } = this.props;
    return columns.filter(column => {
      const isNumerical = numericalTypes.some(numericalType => numericalType.test(column.dataType));
      // weight columns should not be hierarchy levels
      const isNotDimCols = !dimCols.includes(column.value);
      return isNumerical && isNotDimCols;
    }).map(option => ({
      ...option,
      disabled: errorColumns.includes(option.value),
    }));
  }

  get errorColumns() {
    const { errors = EMPTY_ARRAY } = this.props;
    return getErrorColumns({ errors });
  }

  get rules() {
    const { form, modelName, tableName } = this.props;
    const { errorColumns } = this;
    const rules = {};

    form.weightCols.forEach((weightCol, idx) => {
      rules[`weightCols:${idx}`] = [{
        validator: validator.weightCols({ ...this.props, errorColumns, modelName, tableName }),
        trigger: 'change',
      }];
    });

    return rules;
  }

  toggleSubmiting = isSubmiting => {
    this.setState({ isSubmiting });
  };

  handleInput = (key, value) => {
    const { boundModalActions } = this.props;
    boundModalActions.setModalForm('HierarchyWeightModal', { [key]: value });
  };

  handleChangeWeightCol = (index, weightCol) => {
    const { form: { weightCols: oldWeightCols } } = this.props;
    const weightCols = [
      ...oldWeightCols.slice(0, index),
      weightCol || null,
      ...oldWeightCols.slice(index + 1),
    ];
    this.handleInput('weightCols', weightCols);
  };

  handleHideModal = () => {
    const { boundModalActions } = this.props;
    boundModalActions.hideModal('HierarchyWeightModal');
  };

  handleCancel = () => {
    const { callback } = this.props;
    this.handleHideModal();
    callback({ isSubmit: false });
  };

  handleSubmit = () => {
    const { callback, form } = this.props;
    const { dimCols, weightCols } = form;

    this.toggleSubmiting(true);

    this.$form.current.validate(valid => {
      if (valid) {
        this.handleHideModal();
        callback({ isSubmit: true, dimCols, weightCols });
      }
      this.toggleSubmiting(false);
    });
  };

  render() {
    const { isShow, intl: { formatMessage }, dataset, form } = this.props;
    const { isSubmiting } = this.state;
    const { isLoading } = dataset;
    const { $form, weightColOptions, columnAliasDict } = this;

    return (
      <Dialog
        className="hierarchy-weight-modal"
        visible={isShow}
        title={formatMessage(strings.SET_WEIGHT)}
        onCancel={this.handleCancel}
      >
        <Dialog.Body>
          <div className="mdx-tip">
            <i className="icon-superset-alert" />
            <div className="mdx-tip-content">{formatMessage(strings.HIERARCHY_WEIGHT_TIP)}</div>
          </div>
          <Form ref={$form} model={form} rules={this.rules}>
            <table className="hierarchy-weight-list">
              {form.dimCols.map((dimCol, index) => (
                // 此处的key无法用uuid，列表渲染元素不会发生插入删除等位移操作，所以只能用Index
                <tr className="hierarchy-weight-item" key={/* eslint-disable */index/* eslint-enable */}>
                  <td>
                    <Form.Item
                      label={index ? undefined : formatMessage(strings.DIMENSION)}
                    >
                      <Input disabled key={dimCol} value={columnAliasDict[dimCol]} />
                    </Form.Item>
                  </td>
                  <td>
                    <Form.Item
                      label={index ? undefined : formatMessage(strings.WEIGHT_COLUMN)}
                      prop={`weightCols:${index}`}
                    >
                      <Select
                        clearable
                        filterable
                        value={form.weightCols[index]}
                        placeholder={formatMessage(strings.PLEASE_SEARCH_AND_SELECT_ONE)}
                        onChange={value => this.handleChangeWeightCol(index, value)}
                      >
                        {weightColOptions.map(weightColOption => (
                          <Select.Option
                            key={weightColOption.value}
                            label={weightColOption.label}
                            value={weightColOption.value}
                            disabled={weightColOption.disabled}
                          />
                        ))}
                      </Select>
                    </Form.Item>
                  </td>
                </tr>
              ))}
            </table>
          </Form>
        </Dialog.Body>
        <Dialog.Footer className="dialog-footer">
          <Button onClick={this.handleCancel} disabled={isLoading || isSubmiting}>
            {formatMessage(strings.CANCEL)}
          </Button>
          <Button type="primary" onClick={this.handleSubmit} disabled={isLoading} loading={isSubmiting}>
            {formatMessage(strings.OK)}
          </Button>
        </Dialog.Footer>
      </Dialog>
    );
  }
}
