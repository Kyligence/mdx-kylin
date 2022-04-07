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
/* eslint-disable react/no-string-refs */
import React, { PureComponent, Fragment } from 'react';
import { Form, Select, Button } from 'kyligence-ui-react';
import PropTypes from 'prop-types';
import { injectIntl } from 'react-intl';

import './index.less';
import { validator, getInitValues } from './handler';

class DoubleSelector extends PureComponent {
  static propTypes = {
    leftOptions: PropTypes.array.isRequired,
    rightOptions: PropTypes.array.isRequired,
    leftPlaceholder: PropTypes.string.isRequired,
    rightPlaceholder: PropTypes.string.isRequired,
    leftTitle: PropTypes.string.isRequired,
    rightTitle: PropTypes.string.isRequired,
    valueKey: PropTypes.string.isRequired,
    onSelect: PropTypes.func.isRequired,
    values: PropTypes.array.isRequired,
    canEditColumns: PropTypes.bool.isRequired,
    disabledForm: PropTypes.bool.isRequired,
    disabled: PropTypes.bool.isRequired,
    linkIcon: PropTypes.string.isRequired,
  };

  state = {
    canEditColumns: false,
    values: getInitValues(),
  };

  constructor(props) {
    super(props);
    const { values, canEditColumns } = props;
    const hasInputValues = values && values.length > 0;

    this.state.canEditColumns = canEditColumns;
    if (hasInputValues) {
      this.state.values = values;
    }

    this.onChange = this.handleChange.bind(this);
    this.addNewSibling = this.addNewSibling.bind(this);
    this.reduceSibling = this.reduceSibling.bind(this);
  }

  componentDidMount() {
    this.validateErrorMessages();
  }

  /* eslint-disable camelcase */
  UNSAFE_componentWillReceiveProps(nextProps) {
    const { values: newValues } = nextProps;
    const { values: oldValues } = this.state;
    if (newValues !== oldValues) {
      this.setState({ values: newValues });
      this.validateErrorMessages();
    }
  }

  get rules() {
    return {
      left: [{ validator: validator.left(this.props), trigger: 'change' }],
      right: [{ validator: validator.right(this.props), trigger: 'change' }],
    };
  }

  get isDisableDelete() {
    const { values } = this.state;

    return values.length <= 1 && !values[0].left && !values[0].right;
  }

  validateErrorMessages() {
    const { values } = this.state;

    values.forEach((value, index) => {
      if (value.leftError) {
        this.refs[`$form${index}`].validateField('left', () => {}, { value });
      }
      if (value.rightError) {
        this.refs[`$form${index}`].validateField('right', () => {}, { value });
      }
    });
  }

  validate() {
    const { values } = this.state;

    return new Promise(resolve => {
      const formEls = Object.values(this.refs);
      let allFormValid = true;

      formEls.forEach((formEl, index) => {
        formEl.validate(valid => {
          allFormValid = allFormValid && valid;

          if (index + 1 >= formEls.length) {
            resolve(allFormValid);
          }
        }, { value: values[index] });
      });
    });
  }

  addNewSibling() {
    const { values } = this.state;
    const { onSelect } = this.props;
    const newValues = [...values, ...getInitValues()];

    this.setState({ values: newValues });
    onSelect('relation', newValues);
  }

  reduceSibling(deleteItem) {
    const { values } = this.state;
    const { onSelect } = this.props;

    const hasItemsDelete = values.length > 1;
    const isDeleteLastItem = values.length === 1;

    if (hasItemsDelete && !this.isDisableDelete) {
      const newValues = values.filter(value => deleteItem !== value);

      this.setState({ values: newValues });
      onSelect('relation', newValues);
    } else if (isDeleteLastItem && !this.isDisableDelete) {
      const newValues = getInitValues();

      this.setState({ values: getInitValues() });
      onSelect('relation', newValues);
    }
  }

  handleChange(key, inputValue, index) {
    const { values } = this.state;
    const { valueKey, onSelect } = this.props;

    values[index][key] = inputValue;
    this.setState({ values: [...values] });

    if (valueKey === 'models') {
      const modelKey = key === 'left' ? 'modelLeft' : 'modelRight';
      onSelect(modelKey, inputValue);
    } else {
      onSelect('relation', [...values]);
    }
  }

  /* eslint-disable react/no-array-index-key */
  render() {
    const {
      disabledForm,
      disabled,
      rightTitle,
      leftTitle,
      leftOptions,
      rightOptions,
      leftPlaceholder,
      rightPlaceholder,
      linkIcon,
      canEditColumns,
    } = this.props;
    const { values } = this.state;

    return (
      <Fragment>
        {values.map((value, index) => {
          const hasRightLabel = index === 0 ? !!rightTitle : false;
          const hasLeftLabel = index === 0 ? !!leftTitle : false;

          return (
            <div key={index} className="double-selector-wrapper">
              <Form inline ref={`$form${index}`} labelPosition="top" labelWidth="100" className={hasLeftLabel && 'has-label'} model={value} rules={this.rules}>
                <Form.Item label={index === 0 && leftTitle} prop="left">
                  <Select
                    placeholder={leftPlaceholder}
                    value={value.left}
                    disabled={disabledForm || disabled}
                    onChange={val => this.handleChange('left', val, index)}
                  >
                    {value.left && (
                      <Select.Option key={value.left} label={value.left} value={value.left} />
                    )}
                    {leftOptions.map(opt => (
                      <Select.Option key={opt} label={opt} value={opt} />
                    ))}
                  </Select>
                </Form.Item>

                {linkIcon && (
                  <span className="link-icon-wrapper"><i className={linkIcon} /></span>
                )}

                <Form.Item label={index === 0 && rightTitle} className={hasRightLabel && 'has-label'} prop="right">
                  <Select
                    placeholder={rightPlaceholder}
                    value={value.right}
                    disabled={disabledForm || disabled}
                    onChange={val => this.handleChange('right', val, index)}
                    className={canEditColumns && 'hasActionBar'}
                  >
                    {value.right && (
                      <Select.Option key={value.right} label={value.right} value={value.right} />
                    )}
                    {rightOptions.map(opt => (
                      <Select.Option key={opt} label={opt} value={opt} />
                    ))}
                  </Select>
                  {canEditColumns && (
                    <div className="actions-bar">
                      <Button circle size="small" icon="icon-superset-add_2" onClick={this.addNewSibling} disabled={disabled} />
                      <Button circle size="small" icon="icon-superset-minus" onClick={() => this.reduceSibling(value)} disabled={disabled || this.isDisableDelete} />
                    </div>
                  )}
                </Form.Item>
              </Form>
            </div>
          );
        })}
      </Fragment>
    );
  }
}

export default injectIntl(DoubleSelector, { forwardRef: true });
