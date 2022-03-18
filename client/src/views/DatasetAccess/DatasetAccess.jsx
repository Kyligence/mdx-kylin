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
import { Table, Input, Button, Tabs, Tag, MessageBox, Layout, Loading, Tooltip } from 'kyligence-ui-react';

import './index.less';
import { dataHelper, datasetHelper, messageHelper, templateUrl } from '../../utils';
import { Connect, InjectIntl } from '../../store';
import { strings, configs } from '../../constants';
import Pagination from '../../components/Pagination/Pagination';
import { restrictTypes, getTabsConfig, getInitState, listUrl } from './handler';
import { getVisibleRestrictListForColumn, getVisibleRestrictListForMeasure, getVisibleRestrictListForCMeasure, getVisibleRestrictListForNamedSet, getAccessListFromVisible } from './computedVisible';
import { getInvisibleRestrictListForColumn, getInvisibleRestrictListForMeasure, getInvisibleRestrictListForCMeasure, getInvisibleRestrictListForNamedSet, getAccessListFromInvisible } from './computedInvisible';

const { nodeTypes } = configs;

export default
@Connect({
  mapState: {
    dataset: state => state.workspace.dataset,
    currentProject: state => state.system.currentProject,
  },
})
@InjectIntl()
class DatasetAccess extends PureComponent {
  static propTypes = {
    boundUserActions: PropTypes.object.isRequired,
    boundUserGroupActions: PropTypes.object.isRequired,
    boundDatasetActions: PropTypes.object.isRequired,
    boundDatasetRoleActions: PropTypes.object.isRequired,
    boundModalActions: PropTypes.object.isRequired,
    match: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    dataset: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
    currentProject: PropTypes.object.isRequired,
  };

  state = {
    ...getInitState(),
    tabType: this.tabsConfig.column.tabType,
  };

  async componentDidMount() {
    this.setState({ isLoading: true });
    await this.initDataset();
    await this.setAdminRestrict();
    await this.setDefaultRestrict();
    this.setState({ isLoading: false });
  }

  async componentDidUpdate(prevProps, prevState) {
    await this.onRestrictChanged(prevState);
  }

  get datasetId() {
    const { match } = this.props;
    return match.params.datasetId;
  }

  get tabsConfig() {
    const { intl, dataset } = this.props;
    return getTabsConfig({ intl, dataset });
  }

  get isVisibleMode() {
    const { dataset } = this.props;
    // false取visible字段, true取invisible字段
    return !dataset.access;
  }

  get selectedRestrict() {
    const { selectedRestrictKey } = this.state;
    const { restrictList } = this;
    return restrictList.find(restrict => restrict.key === selectedRestrictKey);
  }

  get filterRestrict() {
    const { filterRestrict, tabType } = this.state;
    switch (tabType) {
      case nodeTypes.COLUMN: return filterRestrict.column;
      case nodeTypes.MEASURE: return filterRestrict.measure;
      case nodeTypes.CALCULATE_MEASURE: return filterRestrict.calculateMeasure;
      case nodeTypes.NAMEDSET: return filterRestrict.namedSet;
      default: return '';
    }
  }

  get filterAccess() {
    const { filterAccess, tabType } = this.state;
    switch (tabType) {
      case nodeTypes.COLUMN: return filterAccess.column;
      case nodeTypes.MEASURE: return filterAccess.measure;
      case nodeTypes.CALCULATE_MEASURE: return filterAccess.calculateMeasure;
      case nodeTypes.NAMEDSET: return filterAccess.namedSet;
      default: return '';
    }
  }

  get restrictList() {
    const { dataset } = this.props;
    const { tabType } = this.state;
    const { filterRestrict, isVisibleMode } = this;
    switch (tabType) {
      case nodeTypes.COLUMN:
        return isVisibleMode
          ? getVisibleRestrictListForColumn({ dataset, filterRestrict })
          : getInvisibleRestrictListForColumn({ dataset, filterRestrict });
      case nodeTypes.MEASURE:
        return isVisibleMode
          ? getVisibleRestrictListForMeasure({ dataset, filterRestrict })
          : getInvisibleRestrictListForMeasure({ dataset, filterRestrict });
      case nodeTypes.CALCULATE_MEASURE:
        return isVisibleMode
          ? getVisibleRestrictListForCMeasure({ dataset, filterRestrict })
          : getInvisibleRestrictListForCMeasure({ dataset, filterRestrict });
      case nodeTypes.NAMEDSET:
        return isVisibleMode
          ? getVisibleRestrictListForNamedSet({ dataset, filterRestrict })
          : getInvisibleRestrictListForNamedSet({ dataset, filterRestrict });
      default: return null;
    }
  }

  /* eslint-disable max-len */
  get visibleList() {
    const { dataset } = this.props;
    const { tabType, accessMapping } = this.state;
    const { isVisibleMode, filterAccess, selectedRestrict: { items: accessList = [] } = {} } = this;

    return isVisibleMode
      ? getAccessListFromVisible({ dataset, visibleList: accessList, tabType, accessMapping, filterAccess })
      : getAccessListFromInvisible({ dataset, invisibleList: accessList, tabType, accessMapping, filterAccess });
  }
  /* eslint-enable */

  get pagedVisibleList() {
    const { pageOffset, pageSize } = this.state;
    const { visibleList } = this;
    return dataHelper.getPaginationTable({ datas: visibleList, pageOffset, pageSize });
  }

  getKeAccessMapping = async () => {
    const { boundDatasetActions } = this.props;
    const { selectedRestrict: { name: restrictName, type: restrictType } } = this;

    if (['user', 'group'].includes(restrictType)) {
      // 如果是KE用户或用户组，则获取accessMapping
      try {
        this.setState({ isKeAccessLoading: true });
        const accessMapping = await boundDatasetActions
          .getKeAccessMapping(restrictName, restrictType);

        this.setState({ isKeAccessLoading: false, accessMapping });
      } finally {
        this.setState({ isKeAccessLoading: false });
      }
    } else {
      // 如果是MDX数据集角色，则清空accessMapping
      this.setState({ isKeAccessLoading: false, accessMapping: {} });
    }
  };

  setAdminRestrict = () => {
    const { dataset, boundDatasetActions } = this.props;
    const { isVisibleMode, tabsConfig } = this;

    return Promise.all(Object.values(tabsConfig).map(async tabConfig => {
      if (isVisibleMode) {
        const items = datasetHelper.getFullVisibilityItems(dataset, tabConfig.tabType);
        const form = { type: 'role', name: configs.inbuildDatasetRoles[0], items };

        await boundDatasetActions.deleteRestrict('visible', form, items);
        await this.saveRestrict(tabConfig.tabType, form);
      }
    }));
  };

  setDefaultRestrict = () => new Promise(resolve => {
    const { restrictList } = this;
    if (restrictList[0]) {
      const selectedRestrictKey = restrictList[0].key;
      this.setState({ selectedRestrictKey, pageOffset: 0, pageSize: 10 }, resolve);
    }
  });

  onRestrictChanged = async prevState => {
    const { selectedRestrictKey: oldRestrictKey } = prevState;
    const { selectedRestrictKey: newRestrictKey } = this.state;
    if (oldRestrictKey !== newRestrictKey && this.selectedRestrict) {
      await this.getKeAccessMapping();
    }
  };

  handleExit = () => {
    const { history } = this.props;
    history.push(listUrl);
  };

  // 加setTimeout，让组件渲染代码执行完成之后，再跳转页面，
  // 消除react警告。
  handleNotFound = () => {
    setTimeout(() => {
      const { history } = this.props;
      const fallbackUrl = listUrl;
      history.push(templateUrl.getNotFoundUrl({
        fallbackUrl,
        duration: 5000,
        pageId: strings.DATASET_LIST.id,
        icon: 'icon-superset-sad',
        messageId: strings.NOT_FOUND_ENTITY.id,
        entityId: strings.DATASET.id,
      }));
    });
  };

  initDataset = async () => {
    const {
      boundDatasetRoleActions,
      boundDatasetActions,
      boundUserActions,
      boundUserGroupActions,
      currentProject,
    } = this.props;
    const { datasetId } = this;

    await Promise.all([
      boundUserActions.getAllUsers({ project: currentProject.name }),
      boundUserGroupActions.getAllUserGroups({ project: currentProject.name }),
      boundDatasetRoleActions.getAllRoles(),
    ]);

    const isDatasetExisted = await boundDatasetActions.setDatasetStore(datasetId);
    if (!isDatasetExisted) {
      this.handleNotFound();
    }
  };

  refreshDataset = async () => {
    const { boundDatasetActions } = this.props;
    const { datasetId } = this;

    const isDatasetExisted = await boundDatasetActions.setDatasetStore(datasetId);
    if (!isDatasetExisted) {
      throw this.handleNotFound();
    }
  };

  saveRestrict = (nodeType, form) => {
    const { boundDatasetActions } = this.props;
    const { isVisibleMode } = this;
    const { items: accessList, ...restrict } = form;
    const restrictType = isVisibleMode ? 'visible' : 'invisible';

    delete restrict.key;

    const datasetItems = accessList.map(access => {
      const oldRestrict = access[restrictType].filter(restrictItem => (
        restrictItem.name !== restrict.name || restrictItem.type !== restrict.type
      ));
      const restricts = [...oldRestrict, restrict];
      return { ...access, [restrictType]: restricts };
    });

    switch (nodeType) {
      case 'column': return boundDatasetActions.batchSetDimColumns(datasetItems, false);
      case 'measure': return boundDatasetActions.batchSetDimMeasures(datasetItems, false);
      case 'calculateMeasure': return boundDatasetActions.batchSetCMeasures(datasetItems, false);
      case 'namedSet': return boundDatasetActions.batchSetNamedSets(datasetItems, false);
      default: return null;
    }
  }

  getRestrictType = restrict => {
    const { intl } = this.props;
    let restrictType = '';

    switch (restrict.type) {
      case 'user': restrictType = intl.formatMessage(strings.USER); break;
      case 'role': restrictType = intl.formatMessage(strings.ROLE); break;
      default: restrictType = ''; break;
    }

    return restrictType.toLowerCase();
  }

  saveDataset = async saveRestrict => {
    const { boundDatasetActions } = this.props;
    const { datasetId } = this;

    try {
      this.setState({ isLoading: true });

      await this.refreshDataset();
      await saveRestrict();
      await boundDatasetActions.updateDatasetJson(datasetId, 'acl');
    } catch {} finally {
      this.setState({ isLoading: false });
    }
  }

  showDeleteAlert = row => {
    const { intl } = this.props;
    const restrictType = this.getRestrictType(row);
    const params = { restrictType, restrictRecord: row.name };

    const messageContent = intl.formatMessage(strings.CONFIRM_DELETE_ACCESS_LIST, params);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  showDeleteSuccess = row => {
    const { intl } = this.props;
    const restrictType = this.getRestrictType(row);
    const params = { restrictType, restrictRecord: row.name };

    const messageContent = intl.formatMessage(strings.SUCCESS_DELETE_ACCESS_LIST, params);
    return messageHelper.notifySuccess(messageContent);
  }

  deleteRestrict = async row => {
    const { boundDatasetActions } = this.props;
    const { isVisibleMode } = this;
    const deleteItems = row.items;

    if (isVisibleMode) {
      await boundDatasetActions.deleteRestrict('visible', row, deleteItems);
    } else {
      await boundDatasetActions.deleteRestrict('invisible', row, deleteItems);
    }
  }

  handleChangeTab = instance => {
    const tabType = instance.props.name;
    this.setState({ ...getInitState(), tabType }, this.setDefaultRestrict);
  };

  handleAddRestrict = async () => {
    const { boundModalActions } = this.props;
    const { tabType: nodeType } = this.state;
    const { restrictList: usedRestricts } = this;

    boundModalActions.setModalData('DatasetAccessModal', { nodeType, usedRestricts });
    const { isSubmit, form } = await boundModalActions.showModal('DatasetAccessModal');

    if (isSubmit) {
      await this.saveDataset(() => this.saveRestrict(nodeType, form));

      const { key: selectedRestrictKey } = form;
      const pageOffset = 0;
      const pageSize = 10;
      this.setState({ selectedRestrictKey, pageOffset, pageSize });
    }
  };

  handleEditRestrict = async row => {
    const { boundModalActions } = this.props;
    const { tabType: nodeType } = this.state;

    boundModalActions.setModalData('DatasetAccessModal', { nodeType, isEdit: true });
    const { isSubmit, form } = await boundModalActions.showModal('DatasetAccessModal', row);

    if (isSubmit) {
      await this.saveDataset(async () => {
        // 先清空该用户所有可见性
        this.deleteRestrict(row);
        // 再添加该用户应有的可见性
        await this.saveRestrict(nodeType, form);
      });

      const { key: selectedRestrictKey } = form;
      const pageOffset = 0;
      const pageSize = 10;
      this.setState({ selectedRestrictKey, pageOffset, pageSize });
    }
  }

  handleDeleteRestrict = async row => {
    const { boundDatasetActions } = this.props;
    const { isVisibleMode } = this;
    const deleteItems = row.items;

    await this.showDeleteAlert(row);

    await this.saveDataset(async () => {
      if (isVisibleMode) {
        await boundDatasetActions.deleteRestrict('visible', row, deleteItems);
      } else {
        await boundDatasetActions.deleteRestrict('invisible', row, deleteItems);
      }
    });

    this.showDeleteSuccess(row);
    await this.setDefaultRestrict();
  }

  handleFilterRestrict = value => {
    const { tabType, filterRestrict: oldFilterRestrict } = this.state;
    const filterRestrict = { ...oldFilterRestrict, [tabType]: value };

    this.setState({ filterRestrict }, this.setDefaultRestrict);
  };

  handleFilterAccess = value => {
    const { tabType, filterAccess: oldFilterAccess } = this.state;
    const filterAccess = { ...oldFilterAccess, [tabType]: value };

    this.setState({ filterAccess });
  };

  handleSelectRestrict = rowKey => {
    const pageOffset = 0;
    const pageSize = 10;
    this.setState({ selectedRestrictKey: rowKey, pageOffset, pageSize });
  };

  handlePagination = (pageOffset, pageSize) => {
    this.setState({ pageOffset, pageSize });
  }

  renderRestrictTag = row => {
    const { intl } = this.props;

    switch (row.type) {
      case restrictTypes.USER:
        return <Tag type="primary">{intl.formatMessage(strings.USER)}</Tag>;
      case restrictTypes.GROUP:
        return <Tag className="user-group">{intl.formatMessage(strings.USER_GROUP)}</Tag>;
      case restrictTypes.ROLE:
        return <Tag type="success">{intl.formatMessage(strings.ROLE)}</Tag>;
      default:
        return row.type;
    }
  };

  renderRestrictList = () => {
    const { intl, dataset } = this.props;
    const { tabType, selectedRestrictKey } = this.state;
    const { restrictList, filterRestrict } = this;
    const { access: isDefaultVisible } = dataset;

    const isBuildinRole = row => row.type === 'role' && configs.inbuildDatasetRoles.includes(row.name);
    const restrictListHeight = restrictList.length ? 'calc(100% - 56px - 28px - 20px)' : '';

    const tableActions = [
      {
        label: intl.formatMessage(strings.EDIT),
        iconClass: 'icon-superset-table_edit',
        isShow: row => !isBuildinRole(row),
        handler: row => this.handleEditRestrict(row),
      }, {
        label: intl.formatMessage(strings.DELETE),
        iconClass: 'icon-superset-table_delete',
        isShow: row => !isBuildinRole(row),
        handler: row => this.handleDeleteRestrict(row),
      },
    ];

    return (
      <Fragment>
        <Layout.Row className="header-actions">
          <Layout.Col xs="24" sm="24" md="12" lg="12">
            <Button plain className="add-button" type="primary" icon="icon-superset-add_2" onClick={this.handleAddRestrict}>
              {isDefaultVisible
                ? intl.formatMessage(strings.RESTRICT)
                : intl.formatMessage(strings.GRANT)}
            </Button>
          </Layout.Col>
          <Layout.Col xs="24" sm="24" md="12" lg="12">
            <Input
              className="filter-input"
              prefixIcon="icon-superset-search"
              value={filterRestrict}
              placeholder={intl.formatMessage(strings.SEARCH_USER_OR_ROLE)}
              onChange={this.handleFilterRestrict}
            />
          </Layout.Col>
        </Layout.Row>
        <Table
          highlightCurrentRow
          rowKey="key"
          className={tabType}
          style={{ width: '100%' }}
          height={restrictListHeight}
          currentRowKey={selectedRestrictKey}
          columns={[
            { label: intl.formatMessage(strings.USER_OR_GROUP_OR_ROLE), prop: 'name' },
            { label: '', prop: 'type', bodyClassName: 'wrap-cell-tags', width: 80, render: this.renderRestrictTag },
            ...dataHelper.getActionInColumns(this.props, tableActions),
          ]}
          emptyText={intl.formatMessage(strings.NO_DATA)}
          data={restrictList}
          onCurrentChange={rowKey => this.handleSelectRestrict(rowKey)}
        />
      </Fragment>
    );
  };

  renderVisibleAccess = tabConfig => {
    const { intl } = this.props;
    const { tabType, isKeAccessLoading } = this.state;
    const { pagedVisibleList, filterAccess } = this;

    return (
      <Loading style={{ height: '100%' }} loading={isKeAccessLoading}>
        <Layout.Row className="header-actions">
          <Layout.Col xs="24" sm="24" md="12" lg="12">
            <span className="header-text">{tabConfig.headerText}</span>
          </Layout.Col>
          <Layout.Col xs="24" sm="24" md="12" lg="12">
            <Input
              className="filter-input"
              prefixIcon="icon-superset-search"
              value={filterAccess}
              placeholder={tabConfig.placeholder}
              onChange={this.handleFilterAccess}
            />
          </Layout.Col>
        </Layout.Row>
        <Table
          stripe
          showOverflowTooltip
          height={pagedVisibleList.totalCount ? 'calc(100% - 56px - 28px - 20px)' : ''}
          className={tabType}
          rowClassName={tabConfig.rowClassName}
          columns={tabConfig.columns}
          data={pagedVisibleList.data}
          emptyText={intl.formatMessage(strings.NO_DATA)}
        />
        <Pagination
          totalCount={pagedVisibleList.totalCount}
          pageSize={pagedVisibleList.pageSize}
          pageOffset={pagedVisibleList.pageOffset}
          onPagination={this.handlePagination}
        />
      </Loading>
    );
  };

  render() {
    const { dataset, intl } = this.props;
    const { isLoading } = this.state;
    const { tabsConfig } = this;
    const { access: isDefaultVisible, datasetName } = dataset;
    const entity = intl.formatMessage(strings.DATASET);
    const entities = dataHelper.getHumanizeJoinString(intl, [
      intl.formatMessage(strings.DIMENSION).toLowerCase(),
      intl.formatMessage(strings.MEASURE).toLowerCase(),
      intl.formatMessage(strings.CALCULATED_MEASURE).toLowerCase(),
      intl.formatMessage(strings.NAMEDSET).toLowerCase(),
    ]);

    return (
      <Loading className="dataset-access" loading={isLoading}>
        <div className="access-header">
          <Tooltip appendToBody popperClass="dataset-access-return-tooltip" placement="top" content={intl.formatMessage(strings.RETURN)}>
            <i className="icon-superset-return" onClick={this.handleExit} />
          </Tooltip>
          <span className="header-title">
            {intl.formatMessage(strings.ENTITY_ACCESS_TITLE, { entity, name: datasetName })}
          </span>
        </div>
        <div className="access-desc">
          <i className="icon-superset-alert" />
          <span>
            {isDefaultVisible
              ? intl.formatMessage(strings.DATASET_ITEM_IS_VISIBLE_BY_DEFAULT, { entities })
              : intl.formatMessage(strings.DATASET_ITEM_IS_INVISIBLE_BY_DEFAULT, { entities })}
          </span>
        </div>
        <Tabs className="access-body" onTabClick={this.handleChangeTab}>
          {Object.values(tabsConfig).map(tabConfig => (
            <Tabs.Pane
              key={tabConfig.tabType}
              label={tabConfig.tabText}
              name={tabConfig.tabType}
            >
              <Layout.Row gutter="15">
                <Layout.Col span="8">
                  {this.renderRestrictList(tabConfig)}
                </Layout.Col>
                <Layout.Col span="16">
                  {this.renderVisibleAccess(tabConfig)}
                </Layout.Col>
              </Layout.Row>
            </Tabs.Pane>
          ))}
        </Tabs>
      </Loading>
    );
  }
}
