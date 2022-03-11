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
import { Steps, Button, MessageBox, Loading } from 'kyligence-ui-react';

import './index.less';
import { Connect, InjectIntl } from '../../store';
import { strings, storagePath, business, configs } from '../../constants';
import { browserHelper, messageHelper, templateUrl, datasetHelper } from '../../utils';
import { getUrls, getIsolatedModels, popperProps, lastPopperProps } from './handler';
import { validator } from '../DatasetInfo/handler';
import { formTypes } from '../../components/SaveNotExistedModal/handler';
import { getTabsConfig } from '../DatasetAccess/handler';

export default
@Connect({
  mapReselect: {
    currentSite: reselect => reselect.system.currentSite,
  },
  mapState: {
    dataset: state => state.workspace.dataset,
    isSemanticEdit: state => state.global.isSemanticEdit,
    defaultDatasetAccess: state => state.system.configurations['insight.dataset.allow-access-by-default'],
  },
})
@InjectIntl()
class DatasetLayout extends PureComponent {
  static propTypes = {
    boundDatasetActions: PropTypes.object.isRequired,
    boundModalActions: PropTypes.object.isRequired,
    boundGlobalActions: PropTypes.object.isRequired,
    isSemanticEdit: PropTypes.bool.isRequired,
    defaultDatasetAccess: PropTypes.bool.isRequired,
    children: PropTypes.node.isRequired,
    dataset: PropTypes.object.isRequired,
    currentSite: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
    match: PropTypes.object.isRequired,
    intl: PropTypes.object.isRequired,
  };

  state = {
    isLoading: true,
    isValidating: false,
    isSaveLoading: false,
  };

  // Todo:  据官方说法，判断unmounted并不完美。逻辑问题解决了，但组件依旧驻留，GC会清不掉。
  //        未来所有的异步请求，都要封装成 Cancelable Promise
  // Issue: https://github.com/facebook/react/issues/5465#issuecomment-157888325
  unmounted = false;

  async componentDidMount() {
    const {
      boundDatasetActions,
      boundGlobalActions,
      defaultDatasetAccess,
      dataset,
    } = this.props;
    const { datasetId } = this;
    const isEmptyStore = !dataset.project;
    const datasetStore = await browserHelper.getLargeStorage(storagePath.WORKSPACE_DATASET);

    if (datasetId) {
      const isDatasetExisted = await boundDatasetActions.setDatasetStore(datasetId);
      if (!isDatasetExisted) {
        this.handleNotFound();
      }
    } else if (isEmptyStore && datasetStore) {
      boundDatasetActions.recoveryStore(datasetStore);
    }
    if (!datasetId) {
      boundDatasetActions.setDatasetBasicInfo({ access: defaultDatasetAccess });
    }
    this.toggleDatasetLoading(false);

    if (!this.unmounted) {
      boundGlobalActions.toggleFlag('isDatasetEdit', true);
    }
  }

  componentDidUpdate() {
    const { dataset } = this.props;
    browserHelper.setLargeStorage(storagePath.WORKSPACE_DATASET, dataset);
  }

  componentWillUnmount() {
    const { boundDatasetActions, boundGlobalActions } = this.props;
    boundDatasetActions.clearAll();
    this.unmounted = true;

    boundGlobalActions.toggleFlag('isDatasetEdit', false);
    boundGlobalActions.toggleFlag('isSemanticEdit', false);
  }

  get stepKey() {
    const { currentSite } = this.props;
    switch (currentSite.name) {
      case 'datasetInfo': return 0;
      case 'datasetRelation': return 1;
      case 'datasetSemantic': return 2;
      case 'datasetTranslation': return 3;
      case 'datasetUsages': return 4;
      case 'datasetAccess': return 5;
      default: return null;
    }
  }

  get datasetId() {
    const { match } = this.props;
    return match.params.datasetId;
  }

  get urls() {
    const { datasetId } = this;
    return getUrls({ datasetId });
  }

  get saveDataset() {
    const { boundDatasetActions: { saveDatasetJson, updateDatasetJson } } = this.props;
    const { datasetId } = this;
    return !datasetId ? saveDatasetJson : updateDatasetJson;
  }

  get isModelError() {
    const { dataset: { models } } = this.props;
    return models.some(model => model.error);
  }

  get isRelationError() {
    const { dataset: { modelRelations } } = this.props;
    return modelRelations.some(item => (
      item.relation.some(relation => relation.leftError || relation.rightError)
    ));
  }

  get isEmptyModel() {
    const { dataset: { models } } = this.props;
    return !models.length;
  }

  get isDatasetInited() {
    const { dataset: { models: [firstModel] } } = this.props;
    return !!firstModel && !!firstModel.dimensionTables;
  }

  get isolatedModels() {
    const { dataset } = this.props;
    return getIsolatedModels({ dataset });
  }

  // #region 写入数据集默认权限部分
  get tabsConfig() {
    const { intl, dataset } = this.props;
    return getTabsConfig({ intl, dataset });
  }

  get isVisibleMode() {
    const { dataset } = this.props;
    // false取visible字段, true取invisible字段
    return !dataset.access;
  }

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
  }
  // #endregion

  pushUrlByStepIdx = stepIdx => {
    const { history } = this.props;
    const { urls } = this;
    switch (stepIdx) {
      case 0: history.push(urls.infoUrl); break;
      case 1: history.push(urls.relationUrl); break;
      case 2: history.push(urls.semanticUrl); break;
      case 3: history.push(urls.translationUrl); break;
      case 4: history.push(urls.usagesUrl); break;
      default: break;
    }
  }

  checkBasicFormInvalid = async () => {
    const { history } = this.props;
    const { urls } = this;

    if (!await this.validateBaiscForm()) {
      history.push(urls.infoUrl);
      return true;
    }
    return false;
  }

  checkRelationInvalid = async () => {
    const { history, intl } = this.props;
    const { urls, isEmptyModel, isModelError, isRelationError, isolatedModels } = this;

    if (isEmptyModel) {
      messageHelper.notifyInfo(intl.formatMessage(strings.DRAG_TO_ADD_MODEL));
      history.push(urls.relationUrl);
      return true;
    }
    if (isModelError || isRelationError) {
      this.showErrorConfirm();
      history.push(urls.relationUrl);
      return true;
    }
    if (isolatedModels.length > 0) {
      try {
        history.push(urls.relationUrl);
        await this.showIsolatedModelsAlert(isolatedModels.length);
      } catch {
        return true;
      }
    }
    return false;
  }

  checkSemanticInited = async () => {
    const { boundDatasetActions, dataset } = this.props;
    const { isDatasetInited, datasetId } = this;

    if (!!datasetId && !isDatasetInited) {
      await boundDatasetActions.initDataset();
    } else if (dataset.isNeedDiffer) {
      await boundDatasetActions.diffDatasetChange();
    }
  }

  toggleDatasetLoading = isLoading => {
    this.setState({ isLoading });
  }

  showErrorConfirm = async () => {
    const { intl } = this.props;
    const messageContent = intl.formatMessage(strings.CONFIRM_FIX_MODEL);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const confirmButtonText = intl.formatMessage(strings.OK);
    const showCancelButton = false;
    const messageOptions = { type: 'error', showCancelButton, confirmButtonText };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  showIsolatedModelsAlert = async modelCount => {
    const { intl } = this.props;
    const messageContent = intl.formatMessage(strings.CONFIRM_ISOLATED_MODEL, { modelCount });
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const cancelButtonText = intl.formatMessage(strings.RETURN_TO_EDIT);
    const confirmButtonText = intl.formatMessage(strings.CONFIRM);
    const messageOptions = { type: 'warning', cancelButtonText, confirmButtonText };

    return MessageBox.confirm(messageContent, messageTitle, messageOptions);
  }

  validateBaiscForm = async (forceValidate = false) => {
    const { boundDatasetActions, dataset, intl } = this.props;
    const { project, datasetName, type } = dataset;
    const { datasetId } = this;

    // 当 强制校验 或者 创建数据集 时，验证是否重名
    if (!datasetId || forceValidate) {
      const isFormValid = project && type && datasetName &&
        business.nameRegExpInDatasetName.test(datasetName) &&
        datasetName.length <= configs.datasetMaxLength.datasetName;

      if (isFormValid) {
        const options = { project, datasetName, type };
        const isDuplicateName = await boundDatasetActions.checkDatasetName(options);

        if (isDuplicateName) {
          messageHelper.notifyFailed(intl.formatMessage(strings.DATASET_NAME_DUPLICATE));
          return false;
        }
      } else {
        messageHelper.notifyFailed(intl.formatMessage(strings.NOT_VALID_FORM));
        return false;
      }
    }
    return true;
  }

  showLostAlert = async () => {
    const { boundGlobalActions, intl } = this.props;
    const messageContent = intl.formatMessage(strings.SWITCH_LOST_EDIT);
    const messageTitle = intl.formatMessage(strings.NOTICE);
    const type = 'warning';
    const messageOptions = { type };

    await MessageBox.confirm(messageContent, messageTitle, messageOptions);

    boundGlobalActions.toggleFlag('isDatasetEdit', false);
    boundGlobalActions.toggleFlag('isSemanticEdit', false);
  }

  /* eslint-disable max-len */
  handleNext = async () => {
    const { stepKey, isEmptyModel, isModelError, isRelationError, isolatedModels, pushUrlByStepIdx } = this;
    const { isSemanticEdit, intl } = this.props;

    if (isSemanticEdit) {
      await this.showLostAlert();
    }
    try {
      this.setState({ isValidating: true });

      if (stepKey === 0) {
        const valid = await this.validateBaiscForm();
        if (valid) pushUrlByStepIdx(stepKey + 1);
      } else if (stepKey === 1) {
        if (isEmptyModel) {
          messageHelper.notifyInfo(intl.formatMessage(strings.DRAG_TO_ADD_MODEL));
        } else if (isModelError || isRelationError) {
          await this.showErrorConfirm();
        } else {
          if (isolatedModels.length > 0) {
            await this.showIsolatedModelsAlert(isolatedModels.length);
          }
          pushUrlByStepIdx(stepKey + 1);
        }
      } else {
        pushUrlByStepIdx(stepKey + 1);
      }
    } finally {
      this.setState({ isValidating: false });
    }
  }
  /* eslint-enable */

  handleBack = async () => {
    const { history } = this.props;
    const { stepKey, urls, pushUrlByStepIdx } = this;

    if (stepKey === 0) {
      history.push(urls.listUrl);
    } else {
      pushUrlByStepIdx(stepKey - 1);
    }
  }

  // 点击步骤条时，对所点的步骤进行校验处理
  // 当某一步不合法时，跳到不合法的步骤
  // 当每一步都合法时，直接跳到点击的步骤
  handleChangeStep = async nextStepIdx => {
    const { isSemanticEdit } = this.props;
    const { pushUrlByStepIdx, stepKey: currStepIdx } = this;

    if (isSemanticEdit) {
      await this.showLostAlert();
    }

    switch (`${currStepIdx}=>${nextStepIdx}`) {
      case '0=>1': {
        if (await this.checkBasicFormInvalid()) break;
        pushUrlByStepIdx(nextStepIdx);
        break;
      }
      case '0=>2': {
        if (await this.checkBasicFormInvalid()) break;
        if (await this.checkRelationInvalid()) break;
        pushUrlByStepIdx(nextStepIdx);
        break;
      }
      case '0=>3':
      case '0=>4': {
        if (await this.checkBasicFormInvalid()) break;
        if (await this.checkRelationInvalid()) break;
        await this.checkSemanticInited();
        pushUrlByStepIdx(nextStepIdx);
        break;
      }
      case '1=>2':
      case '1=>3':
      case '1=>4': {
        if (await this.checkRelationInvalid()) break;
        await this.checkSemanticInited();
        pushUrlByStepIdx(nextStepIdx);
        break;
      }
      default: pushUrlByStepIdx(nextStepIdx); break;
    }
  }

  handleExit = () => {
    const { history } = this.props;
    const { urls } = this;
    history.push(urls.listUrl);
  }

  // 加setTimeout，让组件渲染代码执行完成之后，再跳转页面，
  // 消除react警告。
  handleNotFound = () => {
    setTimeout(() => {
      const { history } = this.props;
      const { urls: { listUrl: fallbackUrl } } = this;
      history.push(templateUrl.getNotFoundUrl({
        fallbackUrl,
        duration: 5000,
        pageId: strings.DATASET_LIST.id,
        icon: 'icon-superset-sad',
        messageId: strings.NOT_FOUND_ENTITY.id,
        entityId: strings.DATASET.id,
      }));
    });
  }

  handleCheckExpression = async res => {
    const { boundModalActions } = this.props;
    const { semanticUrl } = this.urls;
    boundModalActions.showModal('CheckDatasetDialog');
    boundModalActions.setModalData('CheckDatasetDialog', { ...res, semanticUrl });
  }

  handleSaveNotExisted = async () => {
    const { boundModalActions, intl, dataset } = this.props;
    const entityId = strings.DATASET.id;

    const rules = { name: [{ validator: validator.datasetName }] };
    // 已删除的数据集save方法
    const handleSave = async form => {
      const { boundDatasetActions } = this.props;
      await boundDatasetActions.setDatasetBasicInfo({ datasetName: form.name });
      // 校验重名
      const valid = await this.validateBaiscForm(true);
      if (valid) {
        // 通过校验后保存
        await boundDatasetActions.saveDatasetJson();
      } else {
        // 不通过校验报错
        throw intl.formatMessage(strings.DATASET_NAME_DUPLICATE);
      }
    };

    boundModalActions.setModalData('SaveNotExistedModal', { entityId, handleSave, rules });
    boundModalActions.setModalForm('SaveNotExistedModal', { name: dataset.datasetName });
    return boundModalActions.showModal('SaveNotExistedModal');
  }

  handleSaveSuccess = datasetName => {
    const { boundGlobalActions, history, intl } = this.props;
    const { urls } = this;

    const message = intl.formatMessage(strings.SAVE_DATASET_SUCCESS, { datasetName });
    messageHelper.notifySuccess(message);

    boundGlobalActions.toggleFlag('isDatasetEdit', false);
    boundGlobalActions.toggleFlag('isSemanticEdit', false);
    // 返回数据集列表页
    setTimeout(() => history.push(urls.listUrl));
  }

  handleSave = async () => {
    const { dataset, history } = this.props;
    const { datasetId, saveDataset, urls } = this;
    const { datasetName } = dataset;

    this.setState({ isSaveLoading: true });
    try {
      await this.setAdminRestrict();
      const resSave = await saveDataset(datasetId);

      // 当保存失败时，走重命名保存
      if (!resSave) {
        const { isSubmit, ...form } = await this.handleSaveNotExisted();
        if (isSubmit) {
          this.handleSaveSuccess(form.name); // 提示保存成功
        } else if (!isSubmit && form.formType === formTypes.GIVE_UP_EDIT) {
          setTimeout(() => history.push(urls.listUrl)); // 返回数据集列表页
        }
      } else if (resSave.CM || resSave.name_set || resSave.dimension) {
        this.handleCheckExpression(resSave);
      } else {
        this.handleSaveSuccess(datasetName);
      }
    } catch {} finally {
      this.setState({ isSaveLoading: false });
    }
  }

  /* eslint-disable max-len */
  render() {
    const { isLoading, isValidating, isSaveLoading } = this.state;
    const { isSemanticEdit, dataset, intl, children } = this.props;
    const isRequesting = isValidating || isSaveLoading || dataset.isLoading;

    return (
      <div className="layout-wrapper dataset-workspace">
        <div className="steps-wrapper">
          <div className="steps-header">
            <Steps active={this.stepKey}>
              <Steps.Step title={intl.formatMessage(strings.BASIC_INFORMATION)} onClick={this.handleChangeStep} />
              <Steps.Step popper popperProps={popperProps} title={intl.formatMessage(strings.DEFINE_RELATIONSHIPS)} description={intl.formatMessage(strings.DEFINE_RELATIONSHIPS_TIP)} onClick={this.handleChangeStep} />
              <Steps.Step popper popperProps={popperProps} title={intl.formatMessage(strings.DEFINE_SEMANTICS)} description={intl.formatMessage(strings.DEFINE_SEMANTICS_TIP)} onClick={this.handleChangeStep} />
              <Steps.Step popper popperProps={popperProps} title={intl.formatMessage(strings.TRANSLATION)} description={intl.formatMessage(strings.TRANSLATION_TIP)} onClick={this.handleChangeStep} />
              <Steps.Step popper popperProps={lastPopperProps} title={intl.formatMessage(strings.DIMENSION_USAGE)} description={intl.formatMessage(strings.DIMENSION_USAGE_TIP)} onClick={this.handleChangeStep} />
            </Steps>
          </div>
          <div className="steps-content">
            <Loading style={{ height: '100%' }} loading={isLoading || dataset.isLoading}>
              {!isLoading && children}
            </Loading>
          </div>
          <div className="steps-actions">
            {this.stepKey === 0 && (
              <Button disabled={isRequesting} onClick={this.handleExit}>
                {intl.formatMessage(strings.EXIT)}
              </Button>
            )}
            {this.stepKey !== 0 && (
              <Button
                onClick={this.handleBack}
                disabled={isSemanticEdit || isRequesting || dataset.isLoading}
              >
                <span style={{ marginRight: '2px' }} className="icon-superset-more_05" />
                {intl.formatMessage(strings.PREVIOUS)}
              </Button>
            )}
            {this.stepKey !== 4 && (
              <Button type="primary" loading={isValidating} disabled={isSemanticEdit || dataset.isLoading} onClick={this.handleNext}>
                {intl.formatMessage(strings.NEXT)}
                <span style={{ marginLeft: '2px' }} className="icon-superset-more_02" />
              </Button>
            )}
            {this.stepKey === 4 && (
              <Button type="primary" loading={isSaveLoading} disabled={isSemanticEdit || dataset.isLoading} onClick={this.handleSave}>
                {intl.formatMessage(strings.OK)}
              </Button>
            )}
          </div>
        </div>
      </div>
    );
  }
}
/* eslint-enable */
