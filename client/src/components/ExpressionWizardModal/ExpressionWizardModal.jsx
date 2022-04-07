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
import React, { Component, Fragment } from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import debounce from 'lodash/debounce';
import throttle from 'lodash/throttle';
import { Dialog, Button, Select, Form, Tree, Tooltip, Tabs, Input, OverflowTooltip, MessageBox } from 'kyligence-ui-react';

import './index.less';
import { getDefaultState, getIndicatorTree, getColumnOptions, getMeasureOptions, getCMeasureOptions, getHierarchyOptions, getHierarchyLevelOptions, getNamedSetOptions } from './handler';
import { Connect, InjectIntl } from '../../store';
import { expressionHelper, messageHelper } from '../../utils';
import { strings, configs } from '../../constants';
import Resizable from '../Resizable/Resizable';
import CodeEditor from '../CodeEditor/CodeEditor';
import ClampLines from '../ClampLines/ClampLines';

const { nodeTypes, nodeIconMaps } = configs;

export default
@Connect({
  namespace: 'modal/ExpressionWizardModal',
  defaultState: getDefaultState(),
  mapState: {
    isShow: state => state.isShow,
    form: state => state.form,
    filter: state => state.filter,
    category: state => state.category,
    title: state => state.title,
    originValue: state => state.originValue,
    callback: state => state.callback,
  },
}, {
  mapState: {
    locale: state => state.system.language.locale,
    indicatorList: state => state.data.indicatorList,
    dataset: state => state.workspace.dataset,
  },
})
@InjectIntl()
class ExpressionWizardModal extends Component {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    boundIndicatorActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    isShow: PropTypes.bool.isRequired,
    callback: PropTypes.func.isRequired,
    form: PropTypes.object.isRequired,
    filter: PropTypes.object.isRequired,
    dataset: PropTypes.object.isRequired,
    indicatorList: PropTypes.object.isRequired,
    locale: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    originValue: PropTypes.string.isRequired,
    category: PropTypes.oneOf([
      '',
      nodeTypes.CALCULATE_MEASURE,
    ]).isRequired,
  };

  $form = React.createRef();
  $selects = [];
  $description = React.createRef();

  componentDidMount() {
    const { boundModalActions } = this.props;
    boundModalActions.registerModal('ExpressionWizardModal', getDefaultState());

    this.handleFilterIndicatorDebounce = debounce(this.handleFilterIndicator, 500);
    this.handleResizeThrottle = throttle(this.handleResize, 300);
  }

  async componentDidUpdate(prevProps) {
    const { isShow: oldIsShow } = prevProps;
    const { isShow: newIsShow } = this.props;

    if (!oldIsShow && newIsShow) {
      await this.refreshIndicators();
    }
  }

  get expression() {
    const { form: { template, data, params } } = this.props;
    const paramsList = params.map((p, idx) => (data[idx] && data[idx].name) || p.placeholder || `\${${idx}}`);
    return expressionHelper.parseTemplate({ template, paramsList });
  }

  get indicatorTree() {
    const { indicatorList, intl, locale, filter } = this.props;
    const { indicatorName: filterName } = filter;
    return getIndicatorTree({ intl, locale, indicators: indicatorList.data, filterName });
  }

  get columnOptions() {
    const { dataset } = this.props;
    return getColumnOptions({ dataset });
  }

  get hierarchyOptions() {
    const { dataset } = this.props;
    return getHierarchyOptions({ dataset });
  }

  get hierarchyLevelOptions() {
    const { dataset } = this.props;
    return getHierarchyLevelOptions({ dataset });
  }

  get namedSetOptions() {
    const { dataset } = this.props;
    return getNamedSetOptions({ dataset });
  }

  get measureOptions() {
    const { dataset } = this.props;
    return getMeasureOptions({ dataset });
  }

  get cMeasureOptions() {
    const { dataset } = this.props;
    return getCMeasureOptions({ dataset });
  }

  get rules() {
    const { form, intl } = this.props;
    const rules = {};

    form.params.forEach((param, idx) => {
      rules[`data:${idx}`] = [{
        required: true,
        validator: (rule, inputValue, callback) => {
          if (!inputValue) {
            callback(new Error(intl.formatMessage(strings.PLEASE_ENTER)));
          } else {
            callback();
          }
        },
        trigger: 'change',
      }];
    });

    return rules;
  }

  getExpressionOptions = param => {
    const { columnOptions, hierarchyOptions, namedSetOptions, hierarchyLevelOptions } = this;
    const { measureOptions, cMeasureOptions } = this;
    return [
      ...(param.accept.includes(nodeTypes.COLUMN) ? columnOptions : []),
      ...(param.accept.includes(nodeTypes.HIERARCHY) ? hierarchyOptions : []),
      ...(param.accept.includes(nodeTypes.HIERARCHY_LEVEL) ? hierarchyLevelOptions : []),
      ...(param.accept.includes(nodeTypes.NAMEDSET) ? namedSetOptions : []),
      ...(param.accept.includes(nodeTypes.MEASURE) ? measureOptions : []),
      ...(param.accept.includes(nodeTypes.CALCULATE_MEASURE) ? cMeasureOptions : []),
    ];
  };

  refreshIndicators = async () => {
    const { boundIndicatorActions, category } = this.props;
    await boundIndicatorActions.getIndicators({ category });
  };

  showSelectIndicatorAlert = () => {
    const { intl } = this.props;
    messageHelper.notifyWarning(intl.formatMessage(strings.PLEASE_SELECT_TEMPLATE));
  };

  showOverrideConfirm = () => {
    const { intl: { formatMessage } } = this.props;
    const messageContent = formatMessage(strings.CONFIRM_OVERRIDE_EXPRESSION);
    const messageTitle = formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  };

  handleResizeDebounce = () => {};

  handleResize = () => {
    if (this.$description.current) {
      this.$description.current.clampLines();

      for (const $select of this.$selects) {
        /* eslint-disable no-underscore-dangle */
        if ($select.current && $select.current.__wrappedInstance.popperJS) {
          // 修复下拉框在打开的时候，拖拽定位条，位置不准确的问题
          $select.current.__wrappedInstance.popperJS.update();
        }
      }
    }
  };

  handleHideModal = (isSubmit = false, data = {}) => {
    const { boundModalActions, callback } = this.props;
    boundModalActions.hideModal('ExpressionWizardModal');

    if (typeof isSubmit === 'boolean') {
      callback({ isSubmit, ...data });
    }
  };

  handleCancel = () => {
    this.handleHideModal(false);
  };

  handleSelectIndicator = indicator => {
    const { boundModalActions } = this.props;
    const isIndicatorNode = ![
      nodeTypes.DEFAULT_FOLDER,
      nodeTypes.CUSTOM_FOLDER,
      nodeTypes.SUB_FOLDER,
    ].includes(indicator.nodeType);

    if (isIndicatorNode) {
      boundModalActions.setModalForm('ExpressionWizardModal', { ...indicator, data: [] });
    }
  };

  handleFilterIndicatorDebounce = () => {};

  handleFilterIndicator = indicatorName => {
    const { boundModalActions, filter } = this.props;
    boundModalActions.setModalData('ExpressionWizardModal', { filter: { ...filter, indicatorName } });
  };

  handleInput = (idx, value) => {
    const { boundModalActions, form } = this.props;
    const data = [...form.data];
    data[idx] = { name: value, key: value };
    boundModalActions.setModalForm('ExpressionWizardModal', { data });
  };

  handleSubmit = () => {
    const { originValue } = this.props;
    if (this.expression) {
      this.$form.current.validate(async valid => {
        if (valid) {
          const { expression } = this;
          if (originValue) {
            await this.showOverrideConfirm();
          }
          this.handleHideModal(true, { expression });
        }
      });
    } else {
      this.showSelectIndicatorAlert();
    }
  };

  renderIndicatorNode = (node, data) => {
    const indicatorIcon = data.icon || nodeIconMaps[data.nodeType];
    return (
      <span className="overflow-tooltip-warpper">
        <OverflowTooltip content={data.name}>
          <span>
            <i className={classnames('indicator-icon', indicatorIcon)} />
            <span>{data.name}</span>
          </span>
        </OverflowTooltip>
      </span>
    );
  };

  renderLabel = param => (
    <Fragment>
      {param.name}
      {param.description && (
        <Tooltip
          placement="right"
          popperClass="wizard-param-description"
          content={param.description}
        >
          <i className="icon-superset-what" />
        </Tooltip>
      )}
    </Fragment>
  );

  render() {
    const { intl, isShow, form, title, locale } = this.props;
    const { expression, $form, indicatorTree, rules } = this;
    const titleMessage = intl.formatMessage(strings.ENTITY_TEMPLATE, { entity: title });
    return (
      <Dialog className="expression-wizard-modal" closeOnClickModal={false} closeOnPressEscape={false} visible={isShow} title={titleMessage} onCancel={this.handleCancel}>
        <Dialog.Body>
          <Resizable.Layout>
            <Resizable.Aside className="wizard-list" minWidth={200} maxWidth={400} onResize={this.handleResizeThrottle}>
              <div className="search-indicator">
                <Input
                  prefixIcon="icon-superset-search"
                  placeholder={`${intl.formatMessage(strings.SEARCH_BY)}${intl.formatMessage(strings.TEMPLATE).toLowerCase()}`}
                  onChange={this.handleFilterIndicatorDebounce}
                />
              </div>
              <Tree
                isLazy
                highlightCurrent
                defaultExpandAll
                nodeKey="name"
                options={{ label: 'name', children: 'children' }}
                data={indicatorTree}
                renderContent={this.renderIndicatorNode}
                onNodeClicked={this.handleSelectIndicator}
              />
            </Resizable.Aside>
            <Resizable.Content className="wizard-content">
              {form.template ? (
                <Form ref={$form} model={form} rules={rules}>
                  <ClampLines
                    ref={this.$description}
                    className="indicator-description"
                    text={form.description}
                    lines={3}
                    moreText={intl.formatMessage(strings.SHOW_MORE)}
                    lessText={intl.formatMessage(strings.SHOW_LESS)}
                    tailSpace={locale === 'en' ? 12 : 3}
                  />
                  <Tabs type="card" activeName="PARAMETERS">
                    <Tabs.Pane label={intl.formatMessage(strings.PARAMETERS)} name="PARAMETERS">
                      {(() => {
                        // 此处清空ref，并重新生成refs
                        this.$selects = form.params.map(() => React.createRef());
                      })()}
                      {form.params.map((param, idx) => (
                        <Form.Item label={this.renderLabel(param)} key={param.name} prop={`data:${idx}`}>
                          <Select
                            isLazy
                            filterable
                            key={param.name}
                            ref={this.$selects[idx]}
                            value={form.data[idx] && form.data[idx].key}
                            onChange={value => this.handleInput(idx, value)}
                            popperProps={{
                              positionFixed: true,
                            }}
                          >
                            {this.getExpressionOptions(param).map(option => (
                              <Select.Option
                                key={option.value}
                                label={option.label}
                                value={option.value}
                              >
                                <span>{option.value}</span>
                              </Select.Option>
                            ))}
                          </Select>
                        </Form.Item>
                      ))}
                    </Tabs.Pane>
                    <Tabs.Pane label={intl.formatMessage(strings.EXPRESSION)} name="EXPRESSION">
                      <CodeEditor
                        key={expression}
                        readOnly
                        mode="mdx"
                        height="200px"
                        width="100%"
                        minLines={18}
                        maxLines={18}
                        value={expression || ''}
                      />
                    </Tabs.Pane>
                  </Tabs>
                </Form>
              ) : (
                <div className="empty-indicator">
                  <div><i className="icon-superset-insight" /></div>
                  <div>{intl.formatMessage(strings.PLEASE_SELECT_TEMPLATE)}</div>
                </div>
              )}
            </Resizable.Content>
          </Resizable.Layout>
        </Dialog.Body>
        <Dialog.Footer className="dialog-footer">
          <Button onClick={this.handleCancel}>
            {intl.formatMessage(strings.CANCEL)}
          </Button>
          <Button type="primary" onClick={this.handleSubmit}>
            {intl.formatMessage(strings.OK)}
          </Button>
        </Dialog.Footer>
      </Dialog>
    );
  }
}
