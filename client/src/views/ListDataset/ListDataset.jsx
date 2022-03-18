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
import bytes from 'bytes';
import React, { PureComponent } from 'react';
import PropTypes from 'prop-types';
import debounce from 'lodash/debounce';
import { generatePath } from 'react-router';
import { Table, Loading, MessageBox, Input } from 'kyligence-ui-react';

import './index.less';
import { Connect, InjectIntl } from '../../store';
import { strings, configs } from '../../constants';
import { dataHelper, messageHelper, browserHelper } from '../../utils';
import Pagination from '../../components/Pagination/Pagination';
import ListActions from './ListActions';
import { getRenderColumns, editParamsUrl, accessParamsUrl } from './handler';
import { DatasetService } from '../../services';

export default
@Connect({
  mapState: {
    currentProject: state => state.system.currentProject,
    datasetList: state => state.data.datasetList,
    fileLimit: state => state.system.configurations['insight.dataset.export-file-limit'],
  },
})
@InjectIntl()
class ListDataset extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    boundSystemActions: PropTypes.object.isRequired,
    boundModalActions: PropTypes.object.isRequired,
    currentProject: PropTypes.object.isRequired,
    datasetList: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
    fileLimit: PropTypes.number.isRequired,
  };

  state = {
    pageOffset: browserHelper.getQueryFromLocation('pageOffset') || 0,
    pageSize: browserHelper.getQueryFromLocation('pageSize') || configs.pageCount.datasetList,
    orderBy: browserHelper.getQueryFromLocation('orderBy'),
    direction: browserHelper.getQueryFromLocation('direction'),
    filter: {
      datasetName: '',
    },
    exportMs: null,
  };

  constructor(props) {
    super(props);
    this.handleSort = this.handleSort.bind(this);
    this.handlePagination = this.handlePagination.bind(this);
    this.handleFilterName = this.handleFilterName.bind(this);
    this.fetchDataWithDelay = debounce(this.fetchData, 1000);
  }

  async componentDidMount() {
    this.fetchData();
  }

  get renderColumns() {
    return [
      ...getRenderColumns(this.props),
      ...dataHelper.getActionInColumns(this.props, this.tableActions),
    ];
  }

  get tableActions() {
    const { intl } = this.props;
    const { exportMs } = this.state;
    const exportLimit = exportMs ? ` (${exportMs / 1000}${intl.formatMessage(strings.SECONDS)})` : '';

    return [
      {
        label: intl.formatMessage(strings.EDIT),
        iconClass: 'icon-superset-table_edit',
        isShow: row => row.access.includes('editDataset'),
        handler: row => this.handleClickEdit(row),
      }, {
        label: intl.formatMessage(strings.ACCESS_CONTROL),
        iconClass: 'icon-superset-security',
        isShow: row => row.access.includes('accessDataset'),
        handler: row => this.handleClickAccess(row),
      }, {
        label: intl.formatMessage(strings.CLONE),
        iconClass: 'icon-superset-duplicate',
        isShow: row => row.access.includes('cloneDataset'),
        handler: row => this.handleClickClone(row),
      }, {
        label: intl.formatMessage(strings.RENAME),
        iconClass: 'icon-superset-table_delete',
        isShow: row => row.access.includes('deleteDataset'),
        handler: row => this.handleClickRename(row),
      }, {
        label: `${intl.formatMessage(strings.EXPORT)}${exportLimit}`,
        iconClass: 'icon-superset-export',
        isShow: row => row.access.includes('exportDataset'),
        isDisabled: !!exportMs,
        handler: row => this.handleClickExport(row),
      }, {
        label: intl.formatMessage(strings.DELETE),
        iconClass: 'icon-superset-table_delete',
        className: 'mdx-it-delete',
        isShow: row => row.access.includes('deleteDataset'),
        handler: row => this.handleClickDelete(row),
      },
    ];
  }

  handleClickAccess = async row => {
    const { history } = this.props;
    const accessUrl = generatePath(accessParamsUrl, { datasetId: row.id });
    history.push(accessUrl);
  }

  handleEnterDatasetName = event => {
    const { filter } = this.state;
    const isEnterKey = event.key === 'Enter' || event.keyCode === 13;
    if (isEnterKey && filter.datasetName) {
      this.handleFilterName(filter.datasetName);
    }
  };

  handleClickDelete = async row => {
    const { boundDatasetActions } = this.props;
    const { filter } = this.state;

    // 获取最新数据集默认权限配置
    await this.showDeleteAlert(row);
    await boundDatasetActions.deleteDataset(row.id, { ...filter });
    this.showDeleteSuccess();
  }

  handleClickRename = async row => {
    const { boundModalActions } = this.props;

    boundModalActions.setModalData('RenameDatasetModal', { datasetId: row.id });
    const { isSubmit, datasetName } = await boundModalActions.showModal('RenameDatasetModal');

    if (isSubmit) {
      this.fetchData();
      this.showCloneSuccess(datasetName);
    }
  };

  fetchData = () => {
    const { boundDatasetActions, currentProject } = this.props;
    const { pageOffset, pageSize, orderBy, direction, filter } = this.state;
    const pagination = { pageOffset, pageSize };
    const order = { orderBy, direction };

    if (currentProject.name) {
      return boundDatasetActions.getDatasets({ ...pagination, ...order, ...filter });
    }
    return null;
  }

  showFileLimitConfirm = async size => {
    const { intl, fileLimit } = this.props;
    const { formatMessage } = intl;

    const messageTitle = formatMessage(strings.CONFIRM_EXCEED_SIZE_TITLE);
    const messageContent = formatMessage(strings.CONFIRM_EXCEED_SIZE_CONTENT, {
      size: bytes(size),
      fileLimit: bytes(fileLimit),
    });
    const type = 'warning';
    const cancelButtonText = formatMessage(strings.CONFIRM_EXPORT);
    const confirmButtonText = formatMessage(strings.BACK_TO_SELECT);
    const messageOptions = { type, confirmButtonText, cancelButtonText };

    try {
      await MessageBox.confirm(messageContent, messageTitle, messageOptions);
      // eslint-disable-next-line no-throw-literal
      throw 'confirm';
    } catch (e) {
      if (['confirm', 'close'].includes(e)) {
        throw e;
      }
    }
  }

  showDeleteSuccess() {
    const { intl } = this.props;
    const messageContent = intl.formatMessage(strings.DELETE_DATASET_SUCCESS);
    messageHelper.notifySuccess(messageContent);
  }

  showDeleteAlert(row) {
    const { intl } = this.props;
    let messageContent = intl.formatMessage(strings.CONFIRM_DELETE_DATASETS);

    if (row) {
      const params = { datasetName: row.dataset };
      messageContent = intl.formatMessage(strings.CONFIRM_DELETE_DATASET, params);
    }
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  showCloneSuccess(datasetName) {
    const { intl } = this.props;
    const messageContent = intl.formatMessage(strings.SAVE_DATASET_SUCCESS, { datasetName });
    messageHelper.notifySuccess(messageContent);
  }

  limitExport(exportMs, waitMs) {
    this.setState({ exportMs });

    if (exportMs > 0) {
      setTimeout(() => this.limitExport(exportMs - waitMs, waitMs), waitMs);
    }
  }

  async handleSort({ prop: orderBy, order: direction }) {
    this.setState({ orderBy, direction }, this.fetchData);
  }

  async handlePagination(pageOffset, pageSize) {
    this.setState({ pageOffset, pageSize }, this.fetchData);
  }

  async handleFilterName(datasetName) {
    const { filter: oldFilter } = this.state;
    const filter = { ...oldFilter, datasetName };
    this.setState({ filter, pageOffset: 0 }, () => this.fetchDataWithDelay(1000));
  }

  handleClickEdit(row) {
    const { history } = this.props;
    const editUrl = generatePath(editParamsUrl, { datasetId: row.id });
    history.push(editUrl);
  }

  async handleClickExport(row) {
    const { currentProject: { name: projectName }, fileLimit } = this.props;
    const { size, token } = await DatasetService.generatePackage({
      selectAll: false,
      includes: [row.id],
      projectName,
    });

    // 超出系统配置大小，出现确认弹框
    if (size > fileLimit) {
      await this.showFileLimitConfirm(size);
    }
    // 确认后下载
    await DatasetService.downloadPackage(token, projectName, size);

    this.limitExport(5000, 1000);
  }

  async handleClickClone(row) {
    const { boundModalActions, boundSystemActions } = this.props;

    // 获取最新数据集默认权限配置
    try {
      await boundSystemActions.getConfigurations();
    } catch {}

    boundModalActions.setModalData('CloneDatasetModal', { datasetId: row.id });
    const { isSubmit, datasetName } = await boundModalActions.showModal('CloneDatasetModal');

    if (isSubmit) {
      this.fetchData();
      this.showCloneSuccess(datasetName);
    }
  }

  render() {
    const { datasetList, currentProject, intl } = this.props;
    const { filter } = this.state;
    const { renderColumns } = this;

    const tableData = currentProject.name ? datasetList.data : [];
    const totalCount = currentProject.name ? datasetList.totalCount : 0;

    return (
      <div className="layout-wrapper list-dataset mdx-it-datasets-page">
        <div className="layout-actions clearfix">
          <ListActions className="pull-left" onRefreshList={this.fetchData} />
          <div className="pull-right" onKeyUp={this.handleEnterDatasetName}>
            <Input
              className="mdx-it-search-dataset"
              prefixIcon="icon-superset-search"
              value={filter.datasetName}
              placeholder={intl.formatMessage(strings.SEARCH_DATASET)}
              onChange={this.handleFilterName}
            />
          </div>
        </div>
        <div className="layout-table">
          <Loading className="mdx-it-dataset-search-loading" loading={datasetList.isLoading}>
            <Table
              key={`${datasetList.orderBy}${datasetList.direction}`}
              style={{ width: '100%' }}
              emptyText={intl.formatMessage(strings.NO_DATA)}
              columns={renderColumns}
              data={tableData}
              defaultSort={{ prop: datasetList.orderBy, order: datasetList.direction }}
              onSortChange={this.handleSort}
            />
          </Loading>
          <Pagination
            totalCount={totalCount}
            pageSize={datasetList.pageSize}
            pageOffset={datasetList.pageOffset}
            onPagination={this.handlePagination}
          />
        </div>
      </div>
    );
  }
}
