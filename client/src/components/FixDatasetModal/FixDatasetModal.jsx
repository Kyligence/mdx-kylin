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
import React, { PureComponent } from 'react';
import PropTypes from 'prop-types';
import { Dialog, Button, Table, Message, MessageBox, Input, Tag } from 'kyligence-ui-react';

import './index.less';
import { strings, configs } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { getDefaultState, getSelectedCount } from './handler';
import { datasetHelper, dataHelper } from '../../utils';
import TableFilterBox from '../TableFilterBox/TableFilterBox';

const { getHumanizeJoinString } = dataHelper;
const { getErrorCount } = datasetHelper;
const { nodeTypes: { CALCULATE_MEASURE, NAMEDSET, HIERARCHY } } = configs;

export default
@Connect({
  namespace: 'modal/FixDatasetModal',
  defaultState: getDefaultState(),
  mapState: {
    isShow: state => state.isShow,
    callback: state => state.callback,
    errorList: state => state.errorList,
    dataset: state => state.dataset,
    form: state => state.form,
    filter: state => state.filter,
  },
})
@InjectIntl()
class FixDatasetModal extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    boundDatasetActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    isShow: PropTypes.bool.isRequired,
    callback: PropTypes.func.isRequired,
    errorList: PropTypes.array.isRequired,
    dataset: PropTypes.object,
    form: PropTypes.object.isRequired,
    filter: PropTypes.object.isRequired,
  };

  static defaultProps = {
    dataset: null,
  };

  state = {
    isSubmiting: false,
  };

  constructor(props) {
    super(props);

    const { boundModalActions } = props;
    boundModalActions.registerModal('FixDatasetModal', getDefaultState());
  }

  get datasetKeyMap() {
    const { dataset } = this.props;
    return dataset ? datasetHelper.getDatasetKeyMap({ dataset }) : {};
  }

  get errorCount() {
    const { errorList } = this.props;
    return getErrorCount({ errorList });
  }

  get fixEntitiesMessage() {
    const { intl } = this.props;
    const { errorCount } = this;

    const entities = [];
    if (errorCount[CALCULATE_MEASURE]) {
      const count = errorCount[CALCULATE_MEASURE];
      entities.push(intl.formatMessage(strings.FIX_DATASET_CM, { count }));
    }
    if (errorCount[NAMEDSET]) {
      const count = errorCount[NAMEDSET];
      entities.push(intl.formatMessage(strings.FIX_DATASET_NS, { count }));
    }
    if (errorCount[HIERARCHY]) {
      const count = errorCount[HIERARCHY];
      entities.push(intl.formatMessage(strings.FIX_DATASET_HI, { count }));
    }
    return getHumanizeJoinString(intl, entities);
  }

  get selectedEntitiesMessage() {
    const { intl, form: { selectedKeys } } = this.props;
    const { datasetKeyMap } = this;
    const selectCount = getSelectedCount({ datasetKeyMap, selectedKeys });

    const entities = [];
    if (selectCount[CALCULATE_MEASURE]) {
      const count = selectCount[CALCULATE_MEASURE];
      entities.push(intl.formatMessage(strings.FIX_DATASET_CM, { count }));
    }
    if (selectCount[NAMEDSET]) {
      const count = selectCount[NAMEDSET];
      entities.push(intl.formatMessage(strings.FIX_DATASET_NS, { count }));
    }
    if (selectCount[HIERARCHY]) {
      const count = selectCount[HIERARCHY];
      entities.push(intl.formatMessage(strings.FIX_DATASET_HI, { count }));
    }
    return getHumanizeJoinString(intl, entities);
  }

  get columns() {
    const { intl, filter } = this.props;
    const { handleFilter } = this;

    return [
      {
        type: 'selection',
      },
      {
        label: intl.formatMessage(strings.INVALID_CM_NS_HI),
        prop: 'name',
        render: data => data.alias || data.name,
      },
      {
        label: intl.formatMessage(strings.TYPE),
        prop: 'nodeType',
        width: 160,
        filters: [
          {
            label: intl.formatMessage(strings.CALCULATED_MEASURE),
            value: CALCULATE_MEASURE,
          },
          {
            label: intl.formatMessage(strings.NAMEDSET),
            value: NAMEDSET,
          },
          {
            label: intl.formatMessage(strings.HIERARCHY),
            value: HIERARCHY,
          },
        ],
        renderHeader(column) {
          const { label, prop, filters } = column;
          return (
            <TableFilterBox
              single={false}
              value={filter[prop]}
              headerText={label}
              filters={filters}
              onFilter={value => handleFilter(prop, value)}
            />
          );
        },
        render: data => {
          const value = (() => {
            switch (data.nodeType) {
              case HIERARCHY:
                return intl.formatMessage(strings.HIERARCHY);
              case CALCULATE_MEASURE:
                return intl.formatMessage(strings.CALCULATED_MEASURE);
              case NAMEDSET:
                return intl.formatMessage(strings.NAMEDSET);
              default:
                return data.nodeType || '';
            }
          })();
          return value ? <Tag type="gray">{value}</Tag> : '';
        },
      },
    ];
  }

  get tableList() {
    const { errorList, filter } = this.props;
    return errorList.filter(item => (
      filter.nodeType.length ? filter.nodeType.includes(item.nodeType) : true
    ));
  }

  showDeleteConfirm = () => {
    const { intl, form } = this.props;
    const { datasetKeyMap } = this;
    const { selectedKeys } = form;

    const messageContent = (
      <div className="fix-dataset-confirm">
        <div className="description">
          {intl.formatMessage(
            strings.CONFIRM_DELETE_UNRESOLVE_ITEMS,
            { entities: this.selectedEntitiesMessage },
          )}
        </div>
        <Input
          readOnly
          type="textarea"
          className="fix-item-list"
          rows={6}
          value={selectedKeys
            .map(selectedKey => {
              const selected = datasetKeyMap[selectedKey];
              return selected ? selected.alias || selected.name : selectedKey;
            })
            .join('\r\n')}
        />
      </div>
    );

    const messageTitle = intl.formatMessage(strings.CONFIRM_DELETE_TITLE);
    const type = 'warning';
    const confirmButtonText = intl.formatMessage(strings.CONFIRM_DELETE);
    const cancelButtonText = intl.formatMessage(strings.BACK_TO_EDIT);
    const messageOptions = { type, confirmButtonText, cancelButtonText };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  showSelectAtLeastOne = () => {
    const { intl } = this.props;

    const message = intl.formatMessage(strings.PLEASE_SELECT_ITEM);
    Message.warning({ message });
  }

  handleBatchDelete = async () => {
    const { boundDatasetActions, form } = this.props;
    const { datasetKeyMap } = this;
    const selecteds = form.selectedKeys.map(key => datasetKeyMap[key]);

    await boundDatasetActions.batchDelete(selecteds);
  }

  handleInput = (key, value) => {
    const { boundModalActions } = this.props;
    boundModalActions.setModalForm('FixDatasetModal', { [key]: value });
  }

  handleFilter = (prop, value) => {
    const { boundModalActions, filter: oldFilter } = this.props;
    const filter = { ...oldFilter, [prop]: value };
    boundModalActions.setModalData('FixDatasetModal', { filter });
    this.handleInput('selectedKeys', []);
  }

  handleCancel = () => {
    const { boundModalActions, callback } = this.props;

    callback({ isSubmit: false });
    boundModalActions.hideModal('FixDatasetModal');
  }

  handleSubmit = async () => {
    const { boundModalActions, callback, form } = this.props;
    const { selectedKeys } = form;

    try {
      if (selectedKeys.length) {
        boundModalActions.hideModal('FixDatasetModal', false);
        await this.showDeleteConfirm();

        this.setState({ isSubmiting: true });
        await this.handleBatchDelete();

        callback({ isSubmit: true });
        boundModalActions.hideModal('FixDatasetModal');
        this.setState({ isSubmiting: false });
      } else {
        await this.showSelectAtLeastOne();
      }
    } catch {
      boundModalActions.showModal('FixDatasetModal', null, false);
    }
  }

  render() {
    const { isShow, intl, form } = this.props;
    const { isSubmiting } = this.state;
    const { tableList, fixEntitiesMessage } = this;

    return (
      <Dialog
        className="fix-dataset-modal"
        closeOnClickModal={false}
        visible={isShow}
        title={intl.formatMessage(strings.FIX_DATASET_MODAL_TITLE)}
        onCancel={this.handleCancel}
        closeOnPressEscape={false}
      >
        <Dialog.Body>
          <div className="fix-dataset-desc">
            {intl.formatMessage(strings.FIX_DATASET_MODAL_DESC, { entities: fixEntitiesMessage })}
          </div>
          <Table
            className="invalid-entity-list"
            rowKey="key"
            currentRowKey={form.selectedKeys}
            maxHeight={350}
            emptyText={intl.formatMessage(strings.NO_DATA)}
            columns={this.columns}
            data={tableList}
            onSelectChange={value => this.handleInput('selectedKeys', value)}
            onSelectAll={value => this.handleInput('selectedKeys', value)}
          />
        </Dialog.Body>
        <Dialog.Footer className="dialog-footer">
          <Button onClick={this.handleCancel} disabled={isSubmiting}>
            {intl.formatMessage(strings.CANCEL)}
          </Button>
          <Button type="primary" onClick={this.handleSubmit} loading={isSubmiting}>
            {intl.formatMessage(strings.OK)}
          </Button>
        </Dialog.Footer>
      </Dialog>
    );
  }
}
