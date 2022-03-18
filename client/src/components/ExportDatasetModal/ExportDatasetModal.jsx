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
import PropTypes from 'prop-types';
import debounce from 'lodash/debounce';
import { Waypoint } from 'react-waypoint';
import React, { PureComponent } from 'react';
import { Dialog, Button, Message, Checkbox, Loading, MessageBox, Tag, Input } from 'kyligence-ui-react';

import './index.less';
import { strings } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { getDefaultState } from './handler';
import { DatasetService } from '../../services';

export default
@Connect({
  namespace: 'modal/ExportDatasetModal',
  defaultState: getDefaultState(),
  mapState: {
    isShow: state => state.isShow,
    callback: state => state.callback,
    list: state => state.list,
    form: state => state.form,
    pageOffset: state => state.pageOffset,
    pageSize: state => state.pageSize,
    total: state => state.total,
    isLoading: state => state.isLoading,
    progress: state => state.progress,
    filter: state => state.filter,
  },
}, {
  mapState: {
    currentProject: state => state.system.currentProject,
    fileLimit: state => state.system.configurations['insight.dataset.export-file-limit'],
  },
})
@InjectIntl()
class ExportDatasetModal extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    currentProject: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    isShow: PropTypes.bool.isRequired,
    isLoading: PropTypes.bool.isRequired,
    callback: PropTypes.func.isRequired,
    list: PropTypes.array.isRequired,
    form: PropTypes.object.isRequired,
    pageOffset: PropTypes.number.isRequired,
    pageSize: PropTypes.number.isRequired,
    total: PropTypes.number.isRequired,
    fileLimit: PropTypes.number.isRequired,
    progress: PropTypes.number.isRequired,
  };

  state = {
    isSubmiting: false,
    isDownloading: false,
    waypoint: true,
  };

  constructor(props) {
    super(props);

    const { boundModalActions } = props;
    boundModalActions.registerModal('ExportDatasetModal', getDefaultState());

    this.debounceFilterDataset = debounce(this.filterDataset, 1000);
  }

  componentDidUpdate(prevProps) {
    const { isShow: prevIsShow } = prevProps;
    const { isShow: currIsShow } = this.props;

    if (!prevIsShow && currIsShow) {
      this.refreshLoadMore(prevProps);
    }
  }

  filterDataset = async () => {
    const {
      boundModalActions, pageSize, currentProject,
      isLoading, form,
    } = this.props;
    const { name: project } = currentProject;

    try {
      if (!isLoading) {
        boundModalActions.setModalData('ExportDatasetModal', { isLoading: true });

        const datasetName = form.searchName;
        const params = { pageOffset: 0, pageSize, project, datasetName, orderBy: 'status', direction: 'desc' };
        const { list, total } = await DatasetService.fetchDatasets(params);
        boundModalActions.setModalData('ExportDatasetModal', { list, total, pageOffset: 1 });

        this.refreshLoadMore();
      }
    } finally {
      boundModalActions.setModalData('ExportDatasetModal', { isLoading: false });
    }
  }

  showSelectAtLeastOne = () => {
    const { intl } = this.props;

    const message = intl.formatMessage(strings.PLEASE_SELECT_ITEM);
    Message.warning({ message });
  }

  showBrokenItemsConfirm = async brokenItems => {
    const { boundModalActions, intl } = this.props;
    const { formatMessage } = intl;

    boundModalActions.hideModal('ExportDatasetModal', false);

    const messageTitle = formatMessage(strings.EXPORT_ERROR_DATASET_TITLE);
    const messageContent = (
      <div className="broken-dataset-confirm">
        <div className="description">
          {formatMessage(strings.EXPORT_ERROR_DATASET_CONTENT)}
        </div>
        <Input
          readOnly
          type="textarea"
          className="broken-item-list"
          rows={6}
          value={brokenItems
            .map(brokenItem => brokenItem.name)
            .join('\r\n')}
        />
      </div>
    );
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
      } else {
        boundModalActions.showModal('ExportDatasetModal', null, false);
      }
    }
  }

  showFileLimitConfirm = async size => {
    const { boundModalActions, intl, fileLimit } = this.props;
    boundModalActions.hideModal('ExportDatasetModal', false);

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
      } else {
        boundModalActions.showModal('ExportDatasetModal', null, false);
      }
    }
  }

  isDatasetSelected = id => {
    const { form: { selectAll, includes, excludes } } = this.props;
    return !selectAll
      ? includes.includes(id)
      : !excludes.includes(id);
  }

  refreshLoadMore = () => {
    this.setState({ waypoint: false }, () => {
      this.setState({ waypoint: true });
    });
  }

  handleFilterName = async searchName => {
    const { boundModalActions } = this.props;
    boundModalActions.setModalForm('ExportDatasetModal', { searchName, list: [] });

    await this.debounceFilterDataset();
    boundModalActions.setModalForm('ExportDatasetModal', { includes: [], excludes: [] });
  }

  handleSelectDataset = (id, isSelected) => {
    const { boundModalActions, form } = this.props;
    const { isSubmiting } = this.state;

    if (!isSubmiting) {
      if (!form.selectAll) {
        const includes = !isSelected
          ? [...form.includes, id]
          : form.includes.filter(includeId => includeId !== id);
        boundModalActions.setModalForm('ExportDatasetModal', { includes });
      } else {
        const excludes = isSelected
          ? [...form.excludes, id]
          : form.excludes.filter(includeId => includeId !== id);
        boundModalActions.setModalForm('ExportDatasetModal', { excludes });
      }
    }
  }

  handleToggleSelectAll = selectAll => {
    const { boundModalActions } = this.props;
    const { isSubmiting } = this.state;

    if (!isSubmiting) {
      boundModalActions.setModalForm(
        'ExportDatasetModal',
        { selectAll, includes: [], excludes: [] },
      );
    }
  }

  handleLoadMore = async () => {
    const {
      boundModalActions, pageOffset, pageSize, currentProject,
      isLoading, list: oldList, total: oldTotal, form,
    } = this.props;
    const { name: project } = currentProject;

    try {
      if (!isLoading && oldList.length !== oldTotal) {
        boundModalActions.setModalData('ExportDatasetModal', { isLoading: true });

        const datasetName = form.searchName;
        const params = { pageOffset, pageSize, project, datasetName, orderBy: 'status,dataset', direction: 'desc,desc' };
        const response = await DatasetService.fetchDatasets(params);
        const list = [...oldList, ...response.list];
        const { total } = response;
        boundModalActions.setModalData('ExportDatasetModal', { list, total, pageOffset: pageOffset + 1 });

        this.refreshLoadMore();
      }
    } finally {
      boundModalActions.setModalData('ExportDatasetModal', { isLoading: false });
    }
  }

  handleCheckBroken = async () => {
    const { form, currentProject } = this.props;
    const { selectAll, includes, excludes, searchName } = form;
    const { name: projectName } = currentProject;

    const { broken } = await DatasetService.validateBroken(
      !selectAll
        ? { selectAll, includes, projectName }
        : { selectAll, excludes, projectName, searchName },
    );

    if (broken.length) {
      await this.showBrokenItemsConfirm(broken);
    }
  }

  handleDownload = async () => {
    const { fileLimit, form, currentProject } = this.props;
    const { selectAll, includes, excludes, searchName } = form;
    const { name: projectName } = currentProject;

    const { size, token, __headers } = await DatasetService.generatePackage(
      !selectAll
        ? { selectAll, includes, projectName }
        : { selectAll, excludes, projectName, searchName },
    );
    // 如果返回节点信息，则按节点转发
    const [host, port] = __headers ? __headers()['mdx-execute-node'].split(':') : [];

    // 超出系统配置大小，出现确认弹框
    if (size > fileLimit) {
      await this.showFileLimitConfirm(size);
    }
    // 确认后下载
    this.setState({ isDownloading: true });
    await DatasetService.downloadPackage(
      token, projectName, size, host, port, this.handleDownloadStatus,
    );
    this.setState({ isDownloading: false });
  }

  handleDownloadStatus = (event, progress) => {
    const { boundModalActions } = this.props;
    boundModalActions.setModalData('ExportDatasetModal', { progress: Math.round(progress * 100) });
  }

  handleCancel = () => {
    const { boundModalActions, callback } = this.props;

    callback({ isSubmit: false });
    boundModalActions.hideModal('ExportDatasetModal');
  }

  handleSubmit = async () => {
    const { boundModalActions, callback, form, list } = this.props;
    const { selectAll, includes, excludes } = form;

    try {
      this.setState({ isSubmiting: true });

      if (!selectAll) { // 非全选时
        if (includes.length) { // 有选中的数据集，则下载
          await this.handleCheckBroken();
          await this.handleDownload();

          callback({ isSubmit: true });
          boundModalActions.hideModal('ExportDatasetModal');
        } else { // 没选中数据集，则报错
          this.showSelectAtLeastOne();
        }
      } else if (excludes.length !== list.length) { // 全选时，没有排除所有的数据集，则下载
        await this.handleCheckBroken();
        await this.handleDownload();

        callback({ isSubmit: true });
        boundModalActions.hideModal('ExportDatasetModal');
      } else { // 全选时，排除所有的数据集，则报错
        this.showSelectAtLeastOne();
      }
    } catch {
      boundModalActions.showModal('ExportDatasetModal', null, false);
    } finally {
      this.setState({ isSubmiting: false, isDownloading: false });
    }
  }

  renderDatasetStatus = status => {
    const { intl } = this.props;
    const { formatMessage } = intl;
    switch (status) {
      case 'NORMAL':
        return <Tag type="success">{formatMessage(strings.NORMAL)}</Tag>;
      case 'BROKEN':
        return <Tag type="danger">{formatMessage(strings.BROKEN)}</Tag>;
      default:
        return <Tag type="gray">{status}</Tag>;
    }
  }

  render() {
    const { isShow, intl, list, form, isLoading, total, progress } = this.props;
    const { isSubmiting, isDownloading, waypoint } = this.state;
    const { formatMessage } = intl;

    const indeterminate = !form.selectAll
      ? form.includes.length !== 0 && form.includes.length !== list.length
      : list.length !== 0 && !!form.excludes.length && form.excludes.length !== list.length;

    const isSelectAll = !form.selectAll
      ? form.includes.length !== 0 && form.includes.length === list.length
      : list.length !== 0 && form.excludes.length === 0;

    return (
      <Dialog
        className="export-dataset-modal"
        closeOnClickModal={false}
        closeOnPressEscape={false}
        visible={isShow}
        title={formatMessage(strings.EXPORT_DATASET)}
        onCancel={this.handleCancel}
      >
        <Dialog.Body>
          <div className="warning-tip">
            <i className="icon-superset-warning_16" />
            <span className="warning-text">{formatMessage(strings.EXPORT_DATASET_DESC)}</span>
          </div>
          <div className="filter el-form-item">
            <div className="el-form-item__content">
              <Input
                prefixIcon="icon-superset-search"
                value={form.searchName}
                onChange={this.handleFilterName}
                placeholder={formatMessage(strings.SEARCH_DATASET)}
              />
            </div>
          </div>
          <div className="title el-form-item is-required">
            <div className="el-form-item__label">{formatMessage(strings.SELECT_DATASET)}</div>
          </div>
          <Loading className="div-table datasets" loading={isLoading}>
            <div className="table-header">
              <div className="table-header-item" style={{ width: '19px' }}>
                <Checkbox
                  disabled={isSubmiting}
                  checked={isSelectAll}
                  indeterminate={indeterminate}
                  onChange={this.handleToggleSelectAll}
                />
              </div>
              <div className="table-header-item" style={{ flex: '1', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {formatMessage(strings.DATASET_NAME)}
              </div>
              <div className="table-header-item" style={{ width: '72px' }}>
                {formatMessage(strings.STATUS)}
              </div>
            </div>
            <div className="table-body">
              {list.map(item => {
                const isSelected = this.isDatasetSelected(item.id);
                return (
                  <div className="table-row dataset" key={item.id} onClick={() => this.handleSelectDataset(item.id, isSelected)}>
                    <div className="table-row-item" style={{ width: '19px' }}>
                      <Checkbox
                        disabled={isSubmiting}
                        checked={isSelected}
                        onChange={() => this.handleSelectDataset(item.id, isSelected)}
                      />
                    </div>
                    <div className="table-row-item dataset-name" style={{ flex: '1', overflow: 'hidden', textOverflow: 'ellipsis' }}>{item.dataset}</div>
                    <div className="table-row-item dataset-status" style={{ width: '72px' }}>
                      {this.renderDatasetStatus(item.status)}
                    </div>
                  </div>
                );
              })}
              {total > list.length && (
                <div className="scroll-down-load-more">{formatMessage(strings.SCROLL_DOWN_LOAD_MROE)}</div>
              )}
              {waypoint && <Waypoint onEnter={this.handleLoadMore} />}
            </div>
          </Loading>
        </Dialog.Body>
        <Dialog.Footer className="dialog-footer">
          <Button onClick={this.handleCancel} disabled={isSubmiting}>
            {intl.formatMessage(strings.CANCEL)}
          </Button>
          <Button type="primary" onClick={this.handleSubmit} loading={isSubmiting}>
            {(() => {
              if (isDownloading) {
                return intl.formatMessage(strings.DOWNLOADING, { progress });
              }
              if (isSubmiting) {
                return intl.formatMessage(strings.VALIDATING);
              }
              return intl.formatMessage(strings.EXPORT);
            })()}
          </Button>
        </Dialog.Footer>
      </Dialog>
    );
  }
}
