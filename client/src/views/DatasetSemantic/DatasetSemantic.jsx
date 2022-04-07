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
import { generatePath } from 'react-router';
import { Resizable, Message, MessageBox } from 'kyligence-ui-react';

import './index.less';
import { strings, configs } from '../../constants';
import { Connect, InjectIntl } from '../../store';
import { selectableNodeTypes, editableNodeTypes } from './handler';
import { businessHelper, dataHelper, datasetHelper } from '../../utils';
import SemanticWelcome from './SemanticWelcome';
import DimensionBar from '../../components/DimensionBar/DimensionBar';
import MeasureBar from '../../components/MeasureBar/MeasureBar';
import HierarchyBlock from '../../components/HierarchyBlock/HierarchyBlock';
import DimensionTableBlock from '../../components/DimensionTableBlock/DimensionTableBlock';
import DimensionColumnBlock from '../../components/DimensionColumnBlock/DimensionColumnBlock';
import DimensionMeasureBlock from '../../components/DimensionMeasureBlock/DimensionMeasureBlock';
import CalculateMeasureBlock from '../../components/CalculateMeasureBlock/CalculateMeasureBlock';
import NamedSetBlock from '../../components/NamedSetBlock/NamedSetBlock';
import MeasureGroupBlock from '../../components/MeasureGroupBlock/MeasureGroupBlock';

const { getHumanizeJoinString } = dataHelper;
const { getErrorCount } = datasetHelper;
const { nodeTypes: { CALCULATE_MEASURE, NAMEDSET, HIERARCHY } } = configs;

export default
@Connect({
  mapState: {
    dataset: state => state.workspace.dataset,
    isSemanticEdit: state => state.global.isSemanticEdit,
  },
  mapReselect: {
    errorList: reselect => reselect.workspace.dataset.errorList,
  },
})
@InjectIntl()
class DatasetSemantic extends PureComponent {
  static propTypes = {
    boundModalActions: PropTypes.object.isRequired,
    boundGlobalActions: PropTypes.object.isRequired,
    boundDatasetActions: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
    dataset: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
    match: PropTypes.object.isRequired,
    isSemanticEdit: PropTypes.bool.isRequired,
    errorList: PropTypes.array.isRequired,
  };

  $dimensionBar = React.createRef();
  $measureBar = React.createRef();
  $block = React.createRef();
  messages = [];

  state = {
    selectedItem: null,
    defaultSize: {
      sidebar: '33.3333%',
      content: '33.3333%',
    },
  };

  constructor(props) {
    super(props);
    this.handleCreate = this.handleCreate.bind(this);
    this.handleClickItem = this.handleClickItem.bind(this);
    this.handleEditItem = this.handleEditItem.bind(this);
  }

  async componentDidMount() {
    const { boundDatasetActions, dataset, history } = this.props;
    const { isEditMode } = this;
    const isDatasetInited = !!dataset.models[0] && dataset.models[0].dimensionTables;

    this.setDefaultSize();

    if (!isEditMode && !isDatasetInited) {
      await boundDatasetActions.initDataset();
    } else if (dataset.isNeedDiffer) {
      await boundDatasetActions.diffDatasetChange();
    }
    if (this.isHasErrorModel()) {
      history.push(this.urls.relationUrl);
      this.showErrorConfirm();
    } else {
      this.showDatasetErrorMessage();
      await boundDatasetActions.validateAllExpressionForMDX();
    }
  }

  componentDidUpdate(prevProps) {
    this.onErrorListChange(prevProps);
  }

  componentWillUnmount() {
    this.clearupErrorMessage();
  }

  get datasetId() {
    const { match } = this.props;
    return match.params.datasetId;
  }

  get errorCount() {
    const { errorList } = this.props;
    return getErrorCount({ errorList });
  }

  /* eslint-disable max-len */
  get urls() {
    const { datasetId } = this;
    const listSite = businessHelper.findSiteByName('listDataset', configs.sitemap);
    const infoSite = businessHelper.findSiteByName('datasetInfo', configs.sitemap);
    const relationSite = businessHelper.findSiteByName('datasetRelation', configs.sitemap);
    const semanticSite = businessHelper.findSiteByName('datasetSemantic', configs.sitemap);
    const translationSite = businessHelper.findSiteByName('datasetTranslation', configs.sitemap);
    const usagesSite = businessHelper.findSiteByName('datasetUsages', configs.sitemap);
    const accessSite = businessHelper.findSiteByName('datasetAccess', configs.sitemap);
    const params = { datasetId };

    return {
      listUrl: listSite.url,
      infoUrl: datasetId ? generatePath(infoSite.paramUrl, params) : infoSite.url,
      relationUrl: datasetId ? generatePath(relationSite.paramUrl, params) : relationSite.url,
      semanticUrl: datasetId ? generatePath(semanticSite.paramUrl, params) : semanticSite.url,
      translationUrl: datasetId ? generatePath(translationSite.paramUrl, params) : translationSite.url,
      usagesUrl: datasetId ? generatePath(usagesSite.paramUrl, params) : usagesSite.url,
      accessUrl: datasetId ? generatePath(accessSite.paramUrl, params) : accessSite.url,
    };
  }

  get isEditMode() {
    return !!this.datasetId;
  }

  get fixDatasetMessage() {
    const { intl } = this.props;
    const { renderFixButton, errorCount } = this;

    const fixIt = renderFixButton();
    const entities = [];
    if (errorCount[CALCULATE_MEASURE]) {
      entities.push(intl.formatMessage(strings.FIX_DATASET_CM, { count: errorCount[CALCULATE_MEASURE] }));
    }
    if (errorCount[NAMEDSET]) {
      entities.push(intl.formatMessage(strings.FIX_DATASET_NS, { count: errorCount[NAMEDSET] }));
    }
    if (errorCount[HIERARCHY]) {
      entities.push(intl.formatMessage(strings.FIX_DATASET_HI, { count: errorCount[HIERARCHY] }));
    }
    return intl.formatMessage(strings.FIX_DATASET_MESSAGE, { entities: getHumanizeJoinString(intl, entities), fixIt });
  }

  setDimensionBarNodeKey(selectedItem) {
    const { current: $dimensionBar = {} } = this.$dimensionBar || {};
    const { current: $dimensionTree } = $dimensionBar.$tree || {};
    if (['table', 'column', 'namedSet', 'hierarchy'].includes(selectedItem.nodeType) && $dimensionTree) {
      $dimensionTree.setCurrentNodeKey(selectedItem.key);
    }
  }

  setMeasureBarNodeKey(selectedItem) {
    const { current: $measureBar = {} } = this.$measureBar || {};
    const { current: $measureTree } = $measureBar.$tree || {};
    if (['measure', 'measureGroup', 'calculateMeasure'].includes(selectedItem.nodeType) && $measureTree) {
      $measureTree.setCurrentNodeKey(selectedItem.key);
    }
  }

  setDefaultSize = () => {
    const bodyWidth = document.body.clientWidth;
    if (bodyWidth >= 1200) {
      this.setState({
        defaultSize: {
          sidebar: '25%',
          content: '50%',
        },
      });
    } else if (bodyWidth >= 992) {
      this.setState({
        defaultSize: {
          sidebar: '30%',
          content: '40%',
        },
      });
    } else {
      this.setState({
        defaultSize: {
          sidebar: '33.3333%',
          content: '33.3333%',
        },
      });
    }
  }

  // 由于ref的赋值，相比于error list的改变是滞后的，会导致
  // 还没有对ref赋值的时候，就再次提示error message
  // 造成同时展示多条message
  keepMostOneErrorMessage = () => {
    setTimeout(() => {
      let { messages } = this;
      const lastMessage = messages[messages.length - 1];
      const otherMessages = messages.filter(message => message !== lastMessage);

      otherMessages.forEach(message => message?.onClose());
      messages = lastMessage ? [lastMessage] : [];
    });
  }

  clearupErrorMessage = () => {
    let { messages } = this;

    messages.forEach(message => message?.onClose());
    messages = [];
  }

  showDatasetErrorMessage = () => {
    const { errorList } = this.props;
    const { messages } = this;

    if (errorList.length) {
      const message = this.fixDatasetMessage;
      const duration = 0;
      const showClose = true;
      Message.warning({ message, duration, showClose, ref: instance => messages.push(instance) });
    }
  }

  showFixDatasetModal = async () => {
    const { boundModalActions, errorList, dataset } = this.props;

    this.clearupErrorMessage();
    boundModalActions.setModalData('FixDatasetModal', { errorList, dataset });
    const { isSubmit } = await boundModalActions.showModal('FixDatasetModal');

    if (!isSubmit) {
      this.showDatasetErrorMessage();
    }
  }

  renderFixButton = () => {
    const { intl } = this.props;
    return (
      <span className="fix-dataset-button" onClick={this.showFixDatasetModal}>
        {intl.formatMessage(strings.FIX_IT)}
      </span>
    );
  }

  onErrorListChange = prevProps => {
    const { errorList: currErrorList } = this.props;
    const { errorList: prevErrorList } = prevProps || {};
    if (JSON.stringify(prevErrorList) !== JSON.stringify(currErrorList)) {
      this.showDatasetErrorMessage();
      this.keepMostOneErrorMessage();
    }
  }

  isHasErrorModel() {
    const { dataset } = this.props;
    return dataset.models.some(model => !!model.error);
  }

  async showLostAlert() {
    const { boundGlobalActions, intl } = this.props;
    const messageContent = intl.formatMessage(strings.SWITCH_LOST_EDIT);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    await MessageBox.confirm(messageContent, messageTitle, messageOptions);
    boundGlobalActions.toggleFlag('isSemanticEdit', false);
  }

  async showErrorConfirm() {
    const { intl } = this.props;
    const messageContent = intl.formatMessage(strings.CONFIRM_FIX_MODEL);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const confirmButtonText = intl.formatMessage(strings.OK);
    const showCancelButton = false;
    const messageOptions = { type: 'error', showCancelButton, confirmButtonText };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  resetToWelcome() {
    this.setState({ selectedItem: null });
  }

  async handleClickItem(selectedItem) {
    const { isSemanticEdit } = this.props;
    const { selectedItem: oldSelectedItem } = this.state;
    const isSelectableItem = selectableNodeTypes.includes(selectedItem.nodeType);
    const isChangedItem = selectedItem !== oldSelectedItem;

    if (isSelectableItem && isChangedItem) {
      if (isSemanticEdit) {
        await this.showLostAlert();
      }
      this.setState({ selectedItem });
    }
  }

  async handleEditItem(selectedItem) {
    const { isSemanticEdit } = this.props;
    const isEditableItem = editableNodeTypes.includes(selectedItem.nodeType);

    if (isEditableItem) {
      if (isSemanticEdit) {
        await this.showLostAlert();
      }
      this.setState({ selectedItem }, () => {
        this.$block.current.handleClickEdit();
        this.setDimensionBarNodeKey(selectedItem);
        this.setMeasureBarNodeKey(selectedItem);
      });
    }
  }

  handleCreate(type) {
    this.handleClickItem({ nodeType: type });
  }

  renderInfoBock() {
    const { selectedItem } = this.state;
    const { $block } = this;
    const blockProps = {
      data: selectedItem,
      className: 'item-detail-block',
      onCancelCreate: () => this.resetToWelcome(),
      onDelete: () => this.resetToWelcome(),
      onSubmit: createdItem => this.handleClickItem(createdItem),
    };
    if (selectedItem) {
      switch (selectedItem.nodeType) {
        case 'table':
          return <DimensionTableBlock ref={$block} {...blockProps} />;
        case 'hierarchy':
          return <HierarchyBlock ref={$block} {...blockProps} />;
        case 'column':
          return <DimensionColumnBlock ref={$block} {...blockProps} />;
        case 'measure':
          return <DimensionMeasureBlock ref={$block} {...blockProps} />;
        case 'calculateMeasure':
          return <CalculateMeasureBlock ref={$block} {...blockProps} />;
        case 'namedSet':
          return <NamedSetBlock ref={$block} {...blockProps} />;
        case 'measureGroup':
          return <MeasureGroupBlock ref={$block} {...blockProps} />;
        default:
          return <SemanticWelcome />;
      }
    }
    return <SemanticWelcome />;
  }

  render() {
    const { $dimensionBar, $measureBar } = this;
    const { selectedItem, defaultSize } = this.state;

    return (
      <div className="semantic-maker">
        <Resizable.Wrapper>
          <Resizable.Item defaultSize={defaultSize.sidebar} minSize="170px">
            <DimensionBar
              isLazy
              ref={$dimensionBar}
              editableNodeTypes={editableNodeTypes}
              currentNode={selectedItem}
              onClick={this.handleClickItem}
              onCreate={this.handleCreate}
              onEdit={this.handleEditItem}
            />
          </Resizable.Item>
          <Resizable.Item defaultSize={defaultSize.sidebar} minSize="170px">
            <MeasureBar
              isLazy
              ref={$measureBar}
              editableNodeTypes={editableNodeTypes}
              currentNode={selectedItem}
              onClick={this.handleClickItem}
              onCreate={this.handleCreate}
              onEdit={this.handleEditItem}
            />
          </Resizable.Item>
          <Resizable.Item defaultSize={defaultSize.content} maxSize="100% - 170px * 2">
            {this.renderInfoBock()}
          </Resizable.Item>
        </Resizable.Wrapper>
      </div>
    );
  }
}
