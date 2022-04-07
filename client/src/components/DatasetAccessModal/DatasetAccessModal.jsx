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
import { Dialog, Button, Form, Select, Input, Table, Loading } from 'kyligence-ui-react';

import './index.less';
import { Connect, InjectIntl } from '../../store';
import { strings } from '../../constants';
import { messageHelper, dataHelper, datasetHelper } from '../../utils';
import { validator, getDefaultState, getRestrictTypes, getRestrictTypeOptions, getDialogConfigs, getFullVisibilityItemsWithAccess, getFilteredRestrictOptions } from './handler';
import Resizable from '../Resizable/Resizable';
import DimensionBar from '../DimensionBar/DimensionBar';
import MeasureBar from '../MeasureBar/MeasureBar';
import Pagination from '../Pagination/Pagination';
import EmptyEntityBlock from '../EmptyEntityBlock/EmptyEntityBlock';

export default
@Connect({
  namespace: 'modal/DatasetAccessModal',
  defaultState: getDefaultState(),
  mapState: {
    isShow: state => state.isShow,
    isEdit: state => state.isEdit,
    isLoading: state => state.isLoading,
    nodeType: state => state.nodeType,
    pageOffset: state => state.pageOffset,
    pageSize: state => state.pageSize,
    selectedItem: state => state.selectedItem,
    accessMapping: state => state.accessMapping,
    filterAccess: state => state.filterAccess,
    filterRestrict: state => state.filterRestrict,
    callback: state => state.callback,
    usedRestricts: state => state.usedRestricts,
    form: state => state.form,
  },
  options: {
    forwardRef: true,
  },
}, {
  mapState: {
    dataset: state => state.workspace.dataset,
    userList: state => state.data.userList,
    roleList: state => state.data.datasetRoleList,
    userGroupList: state => state.data.userGroupList,
  },
})
@InjectIntl()
class DatasetAccessModal extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    boundDatasetActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    isShow: PropTypes.bool.isRequired,
    isEdit: PropTypes.bool.isRequired,
    isLoading: PropTypes.bool.isRequired,
    nodeType: PropTypes.oneOf(['column', 'measure', 'calculateMeasure', 'namedSet']).isRequired,
    pageOffset: PropTypes.number.isRequired,
    pageSize: PropTypes.number.isRequired,
    selectedItem: PropTypes.object,
    filterAccess: PropTypes.string.isRequired,
    filterRestrict: PropTypes.string.isRequired,
    callback: PropTypes.func.isRequired,
    userList: PropTypes.object.isRequired,
    roleList: PropTypes.object.isRequired,
    userGroupList: PropTypes.object.isRequired,
    dataset: PropTypes.object.isRequired,
    accessMapping: PropTypes.object.isRequired,
    usedRestricts: PropTypes.arrayOf(PropTypes.shape({
      name: PropTypes.string,
      type: PropTypes.oneOf(['role', 'user', 'group']),
    })).isRequired,
    form: PropTypes.shape({
      name: PropTypes.string,
      type: PropTypes.oneOf(['role', 'user', 'group']),
      items: PropTypes.array,
    }).isRequired,
  };

  static defaultProps = {
    selectedItem: null,
  };

  $form = React.createRef();
  $dimensionBar = React.createRef();
  $measureBar = React.createRef();
  $visibilityTable = React.createRef();

  /* eslint-disable camelcase */
  UNSAFE_componentWillMount() {
    const { boundModalActions } = this.props;
    boundModalActions.registerModal('DatasetAccessModal', getDefaultState());
  }

  /* eslint-disable camelcase, max-len */
  UNSAFE_componentWillReceiveProps(nextProps) {
    const { isShow: newIsShow } = nextProps;
    const { isShow: oldIsShow } = this.props;

    if (!oldIsShow && newIsShow) {
      setTimeout(async () => {
        await this.refreshAccess();
        this.handleShowDefaultView(nextProps);
      });
    }
  }
  /* eslint-enable */

  componentWillUnmount() {
    const { boundModalActions } = this.props;
    boundModalActions.destroyModal('DatasetAccessModal');
  }

  get isVisibleMode() {
    const { dataset } = this.props;
    // false为白名单模式, true为黑名单模式
    return !dataset.access;
  }

  get rules() {
    return {
      name: [{ required: true, validator: validator.name(this.props), trigger: 'change' }],
    };
  }

  /* eslint-disable max-len */
  get tableData() {
    const { filterAccess, pageOffset, pageSize, selectedItem, nodeType } = this.props;
    const currentConfigs = getDialogConfigs(this.props)[nodeType];
    const filters = currentConfigs.getFilters(filterAccess);
    const datas = selectedItem ? selectedItem.children.filter(data => data.nodeType === nodeType) : [];
    return dataHelper.getPaginationTable({ datas, pageOffset, pageSize, filters });
  }
  /* eslint-enable */

  get restrictTypeOptions() {
    return getRestrictTypeOptions(this.props);
  }

  get restrictOptions() {
    const { userList, roleList, userGroupList, usedRestricts, form, filterRestrict } = this.props;
    const { type: restrictType } = form;
    return getFilteredRestrictOptions({
      restrictType, usedRestricts, userList, roleList, userGroupList, filterRestrict,
    });
  }

  get nodeTypeText() {
    const { nodeType, intl } = this.props;

    switch (nodeType) {
      case 'namedSet':
        return intl.formatMessage(strings.NAMEDSET);
      case 'column':
        return intl.formatMessage(strings.DIMENSION);
      case 'measure':
        return intl.formatMessage(strings.MEASURE);
      case 'calculateMeasure':
        return intl.formatMessage(strings.CALCULATED_MEASURE);
      default:
        return '';
    }
  }

  get datasetKeyMap() {
    const { dataset } = this.props;
    return datasetHelper.getDatasetKeyMap({ dataset });
  }

  getKeAccessMapping = async () => {
    const { boundModalActions, boundDatasetActions, form } = this.props;

    if (['user', 'group'].includes(form.type) && form.name) {
      // 如果是KE用户或用户组，则获取accessMapping
      try {
        this.toggleLoading(true);
        const accessMapping = await boundDatasetActions.getKeAccessMapping(form.name, form.type);
        boundModalActions.setModalData('DatasetAccessModal', { accessMapping });
      } finally {
        this.toggleLoading(false);
      }
    } else {
      // 如果是MDX数据集角色，则清空accessMapping
      boundModalActions.setModalData('DatasetAccessModal', { accessMapping: {} });
    }
  };

  setInitData = nextProps => {
    const { isEdit, nodeType, dataset, accessMapping } = nextProps;

    if (!isEdit && this.isVisibleMode) {
      const items = getFullVisibilityItemsWithAccess(dataset, nodeType, accessMapping);
      this.handleInput('items', items);
    }
  }

  getIsVisibleNode = node => {
    const { nodeType } = this.props;

    switch (nodeType) {
      case 'namedSet':
        return ['model', 'table', 'namedSetRoot'].includes(node.data.nodeType);
      case 'column':
        return ['model', 'table'].includes(node.data.nodeType);
      case 'measure':
        return ['measureGroup'].includes(node.data.nodeType);
      case 'calculateMeasure':
        return ['calculateMeasureRoot', 'measureGroup'].includes(node.data.nodeType);
      default:
        return true;
    }
  };

  getIsNodeHasEntity = parent => {
    const { nodeType } = this.props;
    const { children = [] } = parent;

    let hasEntity = false;

    for (const child of children) {
      if (child.nodeType === nodeType) {
        hasEntity = true;
      } else if (hasEntity !== true) {
        hasEntity = this.getIsNodeHasEntity(child);
      }
    }
    return hasEntity;
  };

  toggleLoading = isLoading => {
    const { boundModalActions } = this.props;
    boundModalActions.setModalData('DatasetAccessModal', { isLoading });
  };

  // 此处 setInitData 使用this.props，因为调用过 getKeAccessMapping之后，
  // nextProps会变为oldProps，this.props会变成newProps
  refreshAccess = async () => {
    await this.getKeAccessMapping();
    this.setInitData(this.props);
  };

  shouldNodeEditable = currentNodeType => {
    const { nodeType } = this.props;

    switch (nodeType) {
      case 'namedSet':
        return ['namedSetRoot'].includes(currentNodeType);
      case 'column':
        return ['table', 'namedSetRoot'].includes(currentNodeType);
      case 'measure':
        return ['measureGroup'].includes(currentNodeType);
      case 'calculateMeasure':
        return ['calculateMeasureRoot', 'measureGroup'].includes(currentNodeType);
      default:
        return false;
    }
  }

  shouldTreeNodeRender = node => {
    const { getIsVisibleNode, getIsNodeHasEntity } = this;
    return getIsVisibleNode(node) && getIsNodeHasEntity(node.data);
  }

  showHasNoAccessMessage = () => {
    const { intl } = this.props;
    const { nodeTypeText, isVisibleMode } = this;
    const params = { nodeType: nodeTypeText.toLowerCase() };
    const message = isVisibleMode
      ? intl.formatMessage(strings.PLEASE_SELECT_ANY_VISIBLE_ACCESS, params)
      : intl.formatMessage(strings.PLEASE_SELECT_ANY_INVISIBLE_ACCESS, params);
    messageHelper.notifyWarning(message);
  }

  handleFilterAccess = value => {
    const { boundModalActions } = this.props;
    boundModalActions.setModalData('DatasetAccessModal', { filterAccess: value });
  }

  handleFilterRestrict = (value = '') => {
    const { boundModalActions } = this.props;
    boundModalActions.setModalData('DatasetAccessModal', { filterRestrict: value });
  }

  handleShowDefaultView = nextProps => {
    const { nodeType } = nextProps;

    setTimeout(() => {
      const { $dimensionBar, $measureBar } = this;
      if ($dimensionBar.current) {
        if (nodeType === 'column') {
          $dimensionBar.current.selectDefaultTable();
        } else if (nodeType === 'namedSet') {
          $dimensionBar.current.selectDefaultNamedSet();
        }
      } else if ($measureBar.current) {
        const defaultSelectedTypes = nodeType === 'measure'
          ? ['measureGroup']
          : ['calculateMeasureRoot', 'measureGroup'];
        $measureBar.current.selectDefaultMeasureGroup(defaultSelectedTypes);
      }
    }, 200);
  }

  handlePagination = (pageOffset, pageSize) => {
    const { boundModalActions } = this.props;
    boundModalActions.setModalData('DatasetAccessModal', { pageOffset, pageSize });
  }

  handleHideModal = () => {
    const { boundModalActions } = this.props;
    boundModalActions.hideModal('DatasetAccessModal');
  }

  handleInput = (key, value) => {
    const { boundModalActions } = this.props;
    return boundModalActions.setModalForm('DatasetAccessModal', { [key]: value });
  }

  handleInputRestrictType = value => {
    this.handleInput('type', value);
    this.handleInput('name', '');
    setTimeout(() => this.refreshAccess());
  }

  handleInputRestrict = value => {
    const { form: { type } } = this.props;
    this.handleInput('name', value);
    this.handleInput('key', `${type}-${value}`);
    setTimeout(() => this.refreshAccess());
  }

  handleSelectItems = selectItemKeys => {
    const { datasetKeyMap } = this;
    const selection = selectItemKeys.map(key => datasetKeyMap[key]);
    this.handleInput('items', selection);
  }

  handleCancel = () => {
    const { callback } = this.props;
    this.handleHideModal();
    callback({ isSubmit: false });
  }

  handleShowVisibility = async data => {
    const { boundModalActions } = this.props;

    if (this.shouldNodeEditable(data.nodeType)) {
      await boundModalActions.setModalData('DatasetAccessModal', { selectedItem: data });
    }
  }

  handleSubmit = () => {
    const { callback, form, intl } = this.props;

    this.$form.current.validate(async formValid => {
      const hasRestricts = !!form.items.length;
      const isValid = formValid && hasRestricts;

      if (!hasRestricts) {
        this.showHasNoAccessMessage();
      }

      if (isValid) {
        const params = { username: form.name };
        const message = strings.SAVE_VISIBILITY_RESTRICT_SUCCESSFULLY;
        const successMsg = intl.formatMessage(message, params);
        messageHelper.notifySuccess(successMsg);
        this.handleHideModal();
        callback({ isSubmit: true, form });
      }
    });
  }

  renderAccessTooltip = row => {
    const { intl, accessMapping, form } = this.props;
    const isAccessInKE = datasetHelper.getIsAccessInKE(row.key, accessMapping);
    const restrictType = getRestrictTypes(this.props)[form.type];

    const tooltip = [];

    if (!row.isVisible) {
      // 当MDX可见性被关闭
      const nodeType = getDialogConfigs(this.props)[row.nodeType].entityName.toLowerCase();
      tooltip.push(intl.formatMessage(strings.DISABLED_COLUMN_VISIBILITY, { nodeType }));
      // 当KE的列级权限被禁用
    }

    if (!isAccessInKE) {
      const nodeType = getDialogConfigs(this.props)[row.nodeType].entityName.toLowerCase();
      const type = !form.type
        ? intl.formatMessage(strings.USER_OR_GROUP_OR_ROLE).toLowerCase()
        : restrictType.toLowerCase();

      const effectedByColumns = accessMapping[row.key].effectedBy
        .filter(effectedBy => effectedBy.type === 'column')
        .map(effectedBy => `[${effectedBy.name}]`);

      const effectedByTables = accessMapping[row.key].effectedBy
        .filter(effectedBy => effectedBy.type === 'table')
        .map(effectedBy => `[${effectedBy.name}]`);

      if (effectedByColumns.length) {
        if (tooltip.length) {
          tooltip.push(<br />);
          tooltip.push(<br />);
        }
        tooltip.push(intl.formatMessage(strings.DISABLED_COLUMN_IN_KE, {
          type, effectedBy: effectedByColumns.join(', '), nodeType,
        }));
      }

      if (effectedByTables.length) {
        if (tooltip.length) {
          tooltip.push(<br />);
          tooltip.push(<br />);
        }
        tooltip.push(intl.formatMessage(strings.DISABLED_TABLE_IN_KE, {
          type, effectedBy: effectedByTables.join(', '), nodeType,
        }));
      }
    }

    return tooltip.length ? tooltip : null;
  };

  renderEmpty = () => {
    const { intl } = this.props;
    const { nodeTypeText: entity } = this;
    return (
      <EmptyEntityBlock
        icon="icon-superset-empty_box"
        content={intl.formatMessage(strings.EMPTY_DATASET_ENTITY, { entity })}
      />
    );
  };

  render() {
    /* eslint-disable max-len */
    const { isShow, nodeType, form, selectedItem, filterAccess, isEdit, intl, isLoading } = this.props;
    const { $form, $dimensionBar, $measureBar, $visibilityTable, restrictTypeOptions, restrictOptions } = this;
    /* eslint-enable */
    const currentConfigs = getDialogConfigs(this.props)[nodeType] || {};
    const restrictTypes = getRestrictTypes(this.props);

    return (
      <Dialog
        size="large"
        className={`visibility-restrict-modal ${nodeType}`}
        closeOnClickModal={false}
        visible={isShow}
        title={currentConfigs.dialogTitle}
        onCancel={this.handleHideModal}
      >
        <Dialog.Body>
          <Form ref={$form} labelPosition="top" model={form} rules={this.rules}>
            <div className="select-restrict">
              <Form.Item className="select-restrict-type" label={intl.formatMessage(strings.USER_OR_GROUP_OR_ROLE)} prop="type">
                <Select
                  disabled={isEdit}
                  value={form.type}
                  onChange={this.handleInputRestrictType}
                >
                  {restrictTypeOptions.map(option => (
                    <Select.Option key={option.value} label={option.label} value={option.value} />
                  ))}
                </Select>
              </Form.Item>
              <Form.Item className="select-restrict-name" label={restrictTypes[form.type]} prop="name">
                <Select
                  isLazy
                  filterable
                  disabled={isEdit}
                  value={form.name}
                  filterMethod={this.handleFilterRestrict}
                  onChange={this.handleInputRestrict}
                >
                  {restrictOptions.map(option => (
                    <Select.Option key={option.value} label={option.label} value={option.value} />
                  ))}
                </Select>
              </Form.Item>
            </div>
            <div className="el-form-item is-required" style={{ margin: 0 }}>
              <span className="el-form-item__label" style={{ padding: '0 0 15px 0' }}>{currentConfigs.entityName}</span>
            </div>
            <Resizable.Layout>
              <Resizable.Aside className="visibility-list" minWidth={225} maxWidth={450}>
                {['column', 'namedSet'].includes(nodeType) && (
                  <DimensionBar
                    ref={$dimensionBar}
                    isOnlyTree
                    hideArrowWhenNoLeaves
                    shouldNodeRender={this.shouldTreeNodeRender}
                    onClick={this.handleShowVisibility}
                    renderEmpty={this.renderEmpty}
                  />
                )}
                {['measure', 'calculateMeasure'].includes(nodeType) && (
                  <MeasureBar
                    ref={$measureBar}
                    isOnlyTree
                    hideArrowWhenNoLeaves
                    shouldNodeRender={this.shouldTreeNodeRender}
                    onClick={this.handleShowVisibility}
                    renderEmpty={this.renderEmpty}
                  />
                )}
              </Resizable.Aside>
              <Resizable.Content className="visibility-content">
                {(() => {
                  const { tableData } = this;
                  return (
                    <Fragment>
                      <header className="visibility-content-header clearfix">
                        <h1 className="content font-medium pull-left">
                          {currentConfigs.renderContentHeader(selectedItem, tableData.data)}
                        </h1>
                        <Input
                          className="table-filter pull-right"
                          value={filterAccess}
                          onChange={this.handleFilterAccess}
                          placeholder={currentConfigs.filterPlaceholder}
                          prefixIcon="icon-superset-search"
                        />
                      </header>
                      <Loading loading={isLoading}>
                        <Table
                          rowKey="key"
                          reserveSelection
                          showOverflowTooltip
                          ref={$visibilityTable}
                          currentRowKey={form.items.map(item => item.key)}
                          columns={currentConfigs.columns}
                          data={tableData.data}
                          onSelectChange={selection => this.handleSelectItems(selection)}
                          emptyText={intl.formatMessage(strings.NO_DATA)}
                          rowTooltip={{
                            columnAt: (row, column) => column.type === 'selection',
                            placement: 'top',
                            popperClass: 'dataset-access-tooptip',
                            content: this.renderAccessTooltip,
                          }}
                        />
                        <Pagination
                          small
                          layout="prev, pager, next"
                          style={{ marginTop: '20px' }}
                          totalCount={tableData.totalCount}
                          pageSize={tableData.pageSize}
                          pageOffset={tableData.pageOffset}
                          onPagination={this.handlePagination}
                        />
                      </Loading>
                    </Fragment>
                  );
                })()}
              </Resizable.Content>
            </Resizable.Layout>
          </Form>
        </Dialog.Body>
        <Dialog.Footer className="dialog-footer">
          <Button onClick={this.handleCancel}>{intl.formatMessage(strings.CANCEL)}</Button>
          <Button type="primary" onClick={this.handleSubmit}>{intl.formatMessage(strings.OK)}</Button>
        </Dialog.Footer>
      </Dialog>
    );
  }
}
