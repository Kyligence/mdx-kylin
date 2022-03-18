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
import debounce from 'lodash/debounce';
import { Dialog, Button, Form, Table, Tree, Tabs, CollapseTransition, Input, MessageBox, Message } from 'kyligence-ui-react';
import classNames from 'classnames';
import ResizeObserver from 'resize-observer-polyfill';

import './index.less';
import { ReactComponent as EmptySVG } from './empty_state.svg';
import { strings, configs, storagePath } from '../../constants';
import { browserHelper } from '../../utils';
import { Connect, InjectIntl } from '../../store';
import { getDefaultState, validator, getColumns, getSelectedDifference, getSelectedType, getSelectedDiffer, getSelectedBrife, ACTION_TYPE, DIFFER_TYPE } from './handler';

const { nodeTypes, nodeIconMaps } = configs;
const { setStorage } = browserHelper;
const { DIFFER_DATASETS_MODAL_IS_SHOW_RULES } = storagePath;

export default
@Connect({
  namespace: 'modal/DifferDatasetsModal',
  defaultState: getDefaultState(),
  mapState: {
    isShow: state => state.isShow,
    form: state => state.form,
    token: state => state.token,
    host: state => state.host,
    port: state => state.port,
    differences: state => state.differences,
    isShowRules: state => state.isShowRules,
    isShowDiffer: state => state.isShowDiffer,
    callback: state => state.callback,
  },
}, {
  mapState: {
    currentProject: state => state.system.currentProject,
  },
})
@InjectIntl()
class DifferDatasetsModal extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    boundModalActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    // eslint-disable-next-line react/no-unused-prop-types
    currentProject: PropTypes.object.isRequired,
    isShow: PropTypes.bool.isRequired,
    differences: PropTypes.array.isRequired,
    token: PropTypes.string.isRequired,
    host: PropTypes.string.isRequired,
    port: PropTypes.string.isRequired,
    form: PropTypes.object.isRequired,
    isShowRules: PropTypes.bool.isRequired,
    isShowDiffer: PropTypes.bool.isRequired,
    callback: PropTypes.func.isRequired,
  };

  $form = React.createRef();
  $differ = React.createRef();
  isDifferObserved = false;

  state = {
    isSubmiting: false,
    tableHeight: 0,
  };

  constructor(props) {
    super(props);

    const { boundModalActions } = props;
    boundModalActions.registerModal('DifferDatasetsModal', getDefaultState());

    this.debounceResizeDiffer = debounce(this.handleResizeDiffer, 200);
    this.differObserver = new ResizeObserver(this.debounceResizeDiffer);
  }

  componentDidUpdate(prevProps) {
    this.onDifferencesChange(prevProps);

    setTimeout(this.observeDiffer);
  }

  get rules() {
    const { form } = this.props;
    const rules = {};

    form.datasets.forEach((dataset, idx) => {
      rules[`datasets:${idx}`] = [{
        validator: validator.datasetName(this.props, idx),
        trigger: 'blur',
      }];
    });

    return rules;
  }

  get columns() {
    const { intl } = this.props;
    const { handleChangeType, handleChangeACL, handleChangeName } = this;
    return getColumns({ intl, handleChangeType, handleChangeACL, handleChangeName });
  }

  get selectedDifference() {
    const { differences, form } = this.props;
    const { selectedId } = form;
    return getSelectedDifference({ differences, selectedId });
  }

  get selectedType() {
    const { form } = this.props;
    const { selectedId, datasets } = form;
    return getSelectedType({ selectedId, datasets });
  }

  get selectedBrife() {
    const { differences, form } = this.props;
    const { selectedId, datasets } = form;
    return getSelectedBrife({ differences, selectedId, datasets });
  }

  get selectedDiffer() {
    const { differences, form } = this.props;
    const { selectedId, datasets } = form;
    return getSelectedDiffer({ differences, selectedId, datasets });
  }

  onDifferencesChange = prevProps => {
    const { differences: prevDifferences } = prevProps;
    const { differences: currDifferences, boundModalActions } = this.props;

    if (prevDifferences !== currDifferences) {
      const datasets = [];

      for (const difference of currDifferences) {
        const dataset = {
          id: difference.id,
          name: difference.dataset,
          type: ACTION_TYPE.ADD_NEW,
          acl: false,
          canCreate: true,
          canOverride: difference.existed,
        };

        if (difference.existed) {
          dataset.type = ACTION_TYPE.OVERRIDE;
        }
        datasets.push(dataset);
      }

      boundModalActions.setModalForm('DifferDatasetsModal', { datasets, selectedId: datasets[0]?.id });
    }
  }

  toggleSubmiting = isSubmiting => {
    this.setState({ isSubmiting });
  }

  toggleMoreRules = () => {
    const { boundModalActions, isShowRules } = this.props;
    boundModalActions.setModalData('DifferDatasetsModal', { isShowRules: !isShowRules });
  }

  toggleShowDiffer = () => {
    const { boundModalActions, isShowDiffer } = this.props;
    boundModalActions.setModalData('DifferDatasetsModal', { isShowDiffer: !isShowDiffer });
  }

  observeDiffer = () => {
    const { differObserver, $differ, isDifferObserved } = this;
    if (!isDifferObserved && $differ.current) {
      differObserver.observe($differ.current);
    }
  }

  unobserveWrapper = () => {
    const { differObserver, $differ, isDifferObserved } = this;
    if (isDifferObserved && $differ.current) {
      differObserver.unobserve($differ.current);
    }
  }

  showOverrideConfirm = () => {
    const { intl, form } = this.props;
    const { datasets } = form;
    const overrideDatasets = datasets.filter(dataset => dataset.type === ACTION_TYPE.OVERRIDE);

    if (overrideDatasets.length) {
      const messageContent = (
        <div className="override-dataset-confirm">
          <div className="description">
            {intl.formatMessage(strings.CONFIRM_OVERRIDE_DATASETS_DESC)}
          </div>
          <Input
            readOnly
            type="textarea"
            className="override-item-list"
            rows={6}
            value={overrideDatasets
              .map(dataset => dataset.name)
              .join('\r\n')}
          />
        </div>
      );

      const messageTitle = intl.formatMessage(strings.CONFIRM_OVERRIDE_DATASETS_TITLE);
      const type = 'warning';
      const confirmButtonText = intl.formatMessage(strings.CONFIRM_OVERRIDE);
      const cancelButtonText = intl.formatMessage(strings.CANCEL);
      const messageOptions = { type, confirmButtonText, cancelButtonText };

      return MessageBox.confirm(messageContent, messageTitle, messageOptions);
    }
    return null;
  }

  showErrorConfirm = datasets => {
    const { intl } = this.props;

    const messageContent = (
      <div className="override-dataset-confirm">
        <div className="description">
          {intl.formatMessage(strings.DELETE_DATASETS_ERROR_DESC)}
        </div>
        <Input
          readOnly
          type="textarea"
          className="override-item-list"
          rows={6}
          value={datasets
            .map(dataset => dataset.name)
            .join('\r\n')}
        />
      </div>
    );

    const messageTitle = intl.formatMessage(strings.DELETE_DATASETS_ERROR_TITLE);
    const type = 'error';
    const confirmButtonText = intl.formatMessage(strings.OK);
    const messageOptions = { type, confirmButtonText };

    return MessageBox.alert(messageContent, messageTitle, messageOptions);
  }

  showResetNameMessage = () => {
    const { intl } = this.props;

    const message = intl.formatMessage(strings.RESET_DATASET_NAME);
    Message.info({ message });
  }

  showSuccessMessage = () => {
    const { intl } = this.props;

    const message = intl.formatMessage(strings.SUCCESS_IMPORT_DATASET);
    Message.success({ message });
  }

  showFailedMessage = () => {
    const { intl } = this.props;

    const message = intl.formatMessage(strings.FAILED_IMPORT_DATASET);
    Message.error({ message });
  }

  handleResizeDiffer = ([entry]) => {
    const { contentRect: { height } } = entry;
    const { tableHeight } = this.state;
    if (tableHeight !== height) {
      this.setState({ tableHeight: height });
    }
  }

  handleSelectDataset = selectedId => {
    const { boundModalActions } = this.props;
    boundModalActions.setModalForm('DifferDatasetsModal', { selectedId });
  }

  handleChangeName = (id, name) => {
    const { boundModalActions, form } = this.props;
    const idx = form.datasets.findIndex(dataset => dataset.id === id);
    const datasets = [
      ...form.datasets.slice(0, idx),
      { ...form.datasets[idx], name },
      ...form.datasets.slice(idx + 1),
    ];
    boundModalActions.setModalForm('DifferDatasetsModal', { datasets });
  }

  handleChangeType = (id, type) => {
    const { boundModalActions, form, differences } = this.props;
    const idx = form.datasets.findIndex(dataset => dataset.id === id);

    const oriName = (differences.find(dataset => dataset.id === id) || {}).dataset;
    const curName = (form.datasets[idx] || {}).name;
    const shouldResetName = [ACTION_TYPE.OVERRIDE, ACTION_TYPE.NOT_IMPORT].includes(type) &&
     oriName !== curName;
    const name = shouldResetName ? oriName : curName;

    const datasets = [
      ...form.datasets.slice(0, idx),
      { ...form.datasets[idx], type, name },
      ...form.datasets.slice(idx + 1),
    ];
    boundModalActions.setModalForm('DifferDatasetsModal', { datasets });

    if (shouldResetName && [ACTION_TYPE.OVERRIDE].includes(type)) {
      this.showResetNameMessage();
    }
  }

  handleChangeACL = (id, acl) => {
    const { boundModalActions, form } = this.props;
    const idx = form.datasets.findIndex(dataset => dataset.id === id);
    const datasets = [
      ...form.datasets.slice(0, idx),
      { ...form.datasets[idx], acl },
      ...form.datasets.slice(idx + 1),
    ];
    boundModalActions.setModalForm('DifferDatasetsModal', { datasets });
  }

  // 初始化表格的默认选项
  handleOpen = () => {
    setStorage(DIFFER_DATASETS_MODAL_IS_SHOW_RULES, false);
  }

  handleClose = () => {
    this.unobserveWrapper();
  }

  handleCancel = () => {
    const { boundModalActions, callback } = this.props;

    callback({ isSubmit: false });
    boundModalActions.hideModal('DifferDatasetsModal');
  }

  handleSubmit = () => {
    const {
      boundModalActions, boundDatasetActions, callback, form, token, host, port, currentProject,
    } = this.props;
    const { name: projectName } = currentProject;

    this.toggleSubmiting(true);

    this.$form.current.validate(async valid => {
      try {
        if (valid) {
          boundModalActions.hideModal('DifferDatasetsModal', false);
          await this.showOverrideConfirm();
          boundModalActions.showModal('DifferDatasetsModal', null, false);

          const datasets = form.datasets.filter(dataset => dataset.type !== ACTION_TYPE.NOT_IMPORT);
          const { success, failed } = await boundDatasetActions
            .importDatasetPackage({ datasets, token, projectName }, host, port);

          if (failed.length) {
            if (success.length) {
              boundModalActions.hideModal('DifferDatasetsModal', false);
              await this.showErrorConfirm(failed);
            } else {
              this.showFailedMessage();
            }
          }

          if (success.length) {
            this.showSuccessMessage();
          }

          callback({ isSubmit: true });
          boundModalActions.hideModal('DifferDatasetsModal');
        }
      } catch {
        boundModalActions.showModal('DifferDatasetsModal', null, false);
      }

      this.toggleSubmiting(false);
    });
  }

  renderContent = (node, data) => {
    const { intl } = this.props;
    const { formatMessage } = intl;
    const iconClass = classNames(nodeIconMaps[data.nodeType], 'entity-icon');
    switch (data.nodeType) {
      case nodeTypes.RELATIONSHIP:
        return (
          <span className={classNames('relations', 'entity-node')}>
            <i className={iconClass} />
            <span className="label">{formatMessage(strings.RELATIONSHIP)}</span>
          </span>
        );
      case nodeTypes.NAMEDSET_ROOT:
        return (
          <span className={classNames('namedset-root', 'entity-node')}>
            <i className={iconClass} />
            <span className="label">{formatMessage(strings.NAMEDSET)}</span>
          </span>
        );
      case nodeTypes.CALCULATE_MEASURE_ROOT:
        return (
          <span className={classNames('calculate-measure-root', 'entity-node')}>
            <i className={iconClass} />
            <span className="label">{formatMessage(strings.CALCULATED_MEASURE)}</span>
          </span>
        );
      case nodeTypes.MODEL_RELATION: {
        const [modelLeft, modelRight] = data.label.split('-');
        return (
          <span className={classNames('model-relation', 'entity-node')}>
            <i className="icon-superset-model entity-icon" />
            <span className="label">
              <span>{modelLeft}</span>
              &nbsp;<i className="icon-superset-link" />&nbsp;
              <span>{modelRight}</span>
              ({data.children.length})
            </span>
          </span>
        );
      }
      case nodeTypes.COMMON_TABLE: {
        const [tableLeft, tableRight] = data.label.split('-');
        return (
          <span className={classNames('common-table', 'entity-node')}>
            <i className={iconClass} />
            <span className="label">
              <span>{tableLeft}</span>
              &nbsp;=&nbsp;
              <span>{tableRight}</span>
            </span>
          </span>
        );
      }
      default:
        return (
          <span className={classNames(data.nodeType, 'entity-node')}>
            <i className={iconClass} />
            <span className="label">{data.label}</span>
          </span>
        );
    }
  }

  render() {
    const { isShow, intl, form, differences, isShowRules, isShowDiffer } = this.props;
    const { isSubmiting, tableHeight } = this.state;
    const { $differ, $form, columns, selectedType, selectedDiffer, selectedBrife } = this;
    const { formatMessage } = intl;

    const hasSemanticChange = selectedDiffer.new_item_count || selectedDiffer.reduce_item_count;
    const hasTranslationChange = selectedDiffer.update_items
      .some(item => item.type === nodeTypes.TRANSLATION && item.detail === 'true');
    const hasDimUsageChange = selectedDiffer.update_items
      .some(item => item.type === nodeTypes.DIM_USAGE && item.detail === 'true');
    const hasDatasetChange = hasSemanticChange || hasTranslationChange || hasDimUsageChange;

    return (
      <Dialog
        className="differ-datasets-modal"
        closeOnPressEscape={false}
        closeOnClickModal={false}
        visible={isShow}
        title={formatMessage(strings.IMPORT_DATASET)}
        onCancel={this.handleCancel}
        onOpen={this.handleOpen}
        onClose={this.handleClose}
      >
        <Dialog.Body className="clearfix">
          <div className="differ-datasets-tip">
            {formatMessage(
              strings.DIFFER_DATASET_DESC,
              {
                number: differences.length,
                viewMore: (
                  <span onClick={this.toggleMoreRules}>
                    <Button type="text">
                      {formatMessage(strings.VIEW_IMPORT_RULES)}
                    </Button>
                    <i className={classNames('icon-superset-arrow_down_16', isShowRules && 'reverse')} />
                  </span>
                ),
              },
            )}
          </div>
          <CollapseTransition isShow={isShowRules}>
            <div className="differ-datasets-helper">
              <ul>
                <li>{formatMessage(strings.IMPORT_RULE_1)}</li>
                <li>{formatMessage(strings.IMPORT_RULE_2)}</li>
              </ul>
            </div>
          </CollapseTransition>
          <Form className="dataset-list" ref={$form} labelPosition="top" model={form} rules={this.rules}>
            <Table
              highlightCurrentRow
              rowClassName={row => row.type}
              height={tableHeight}
              rowKey="id"
              currentRowKey={form.selectedId}
              onCurrentChange={this.handleSelectDataset}
              columns={columns}
              data={form.datasets}
            />
          </Form>
          <div className="dataset-differ" ref={$differ}>
            {selectedType === ACTION_TYPE.ADD_NEW && (
              <>
                <div className="tree-tip clearfix">
                  <i className="icon-superset-infor" />
                  <div className="tip-content">
                    {formatMessage(strings.BRIFE_TREE_TIP, { br: <br /> })}
                  </div>
                </div>
                <Tree
                  defaultExpandAll
                  className="brife-tree"
                  nodeKey="label"
                  data={selectedBrife}
                  renderContent={this.renderContent}
                />
              </>
            )}
            {selectedType === ACTION_TYPE.OVERRIDE && (
              <>
                <div className="tree-tip clearfix">
                  <i className="icon-superset-infor" />
                  <div className="tip-content">
                    {hasDatasetChange
                      ? formatMessage(strings.DIFFER_TREE_TIP, { br: <br /> })
                      : formatMessage(strings.DIFFER_TREE_SAME_TIP, { br: <br /> })}
                  </div>
                </div>
                <div className="differ-content">
                  {hasDatasetChange ? (
                    <>
                      <div className="differ-header clickable" onClick={this.toggleShowDiffer}>
                        <i className={classNames('icon-superset-arrow_down_16', isShowDiffer && 'reverse')} />
                        {formatMessage(strings.SEMANTIC)}
                        <span className="change-status">
                          {hasSemanticChange
                            ? formatMessage(strings.CHANGED)
                            : formatMessage(strings.NO_CHANGE)}
                        </span>
                      </div>
                      <CollapseTransition isShow={isShowDiffer}>
                        <Tabs>
                          <Tabs.Pane label={`${formatMessage(strings.NEW)} (${selectedDiffer.new_item_count})`} name={DIFFER_TYPE.NEW}>
                            <Tree
                              defaultExpandAll
                              className="differ-tree"
                              nodeKey="label"
                              data={selectedDiffer.new_items}
                              renderContent={this.renderContent}
                            />
                          </Tabs.Pane>
                          <Tabs.Pane label={`${formatMessage(strings.DELETE)} (${selectedDiffer.reduce_item_count})`} name={DIFFER_TYPE.DELETE}>
                            <Tree
                              defaultExpandAll
                              className="differ-tree"
                              nodeKey="label"
                              data={selectedDiffer.reduce_items}
                              renderContent={this.renderContent}
                            />
                          </Tabs.Pane>
                        </Tabs>
                      </CollapseTransition>
                      <div className="differ-header">
                        <i className="icon-superset-arrow_down_16" style={{ opacity: 0 }} />
                        {formatMessage(strings.TRANSLATION)}
                        <span className="change-status">
                          {hasTranslationChange
                            ? formatMessage(strings.CHANGED)
                            : formatMessage(strings.NO_CHANGE)}
                        </span>
                      </div>
                      <div className="differ-header">
                        <i className="icon-superset-arrow_down_16" style={{ opacity: 0 }} />
                        {formatMessage(strings.DIMENSION_USAGE)}
                        <span className="change-status">
                          {hasDimUsageChange
                            ? formatMessage(strings.CHANGED)
                            : formatMessage(strings.NO_CHANGE)}
                        </span>
                      </div>
                    </>
                  ) : (
                    <div className="no-data">
                      <div className="content-center">
                        <EmptySVG />
                        <div className="text">
                          {formatMessage(strings.NO_INFORMATION_DISPLAY)}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </>
            )}
            {selectedType === ACTION_TYPE.NOT_IMPORT && (
              <div className="no-data">
                <div className="content-center">
                  <EmptySVG />
                  <div className="text">
                    {formatMessage(strings.NO_INFORMATION_DISPLAY)}
                  </div>
                </div>
              </div>
            )}
          </div>
        </Dialog.Body>
        <Dialog.Footer className="dialog-footer">
          <Button onClick={this.handleCancel} disabled={isSubmiting}>
            {formatMessage(strings.CANCEL)}
          </Button>
          <Button type="primary" onClick={this.handleSubmit} loading={isSubmiting}>
            {formatMessage(strings.IMPORT)}
          </Button>
        </Dialog.Footer>
      </Dialog>
    );
  }
}
