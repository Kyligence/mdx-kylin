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
import { batch } from 'react-redux';
import dayjs from 'dayjs';
import { DatasetService } from '../../services';
import * as actionTypes from '../types';
import { configs, storagePath } from '../../constants';
import { storeHelper, browserHelper, dataGenerator, datasetHelper, specHelper, domHelper } from '../../utils';

const { datasetCreateTypes, nodeTypes: NODE_TYPES } = configs;

function refreshTranslation(getState, form) {
  const { translationTypes } = getState().workspace.dataset;
  return translationTypes.reduce((map, translationType) => ({
    ...map,
    [translationType]: form.translation[translationType] || '',
  }), {});
}

function formatDatasetsResponse({ list, total }, getState) {
  const { access: projectAccess } = getState().system.currentProject;
  const data = list.map(item => ({
    ...item, access: specHelper.getAvailableOptions('datasetAccess', { projectAccess }),
  }));
  return { totalCount: total, data };
}

export function getDatasets(options = {}) {
  return (dispatch, getState) => dispatch(
    storeHelper.fetchDataWithPagination({
      getState,
      options,
      formatResponse: response => formatDatasetsResponse(response, getState),
      fetchApi: DatasetService.fetchDatasets,
      setDataAction: actionTypes.SET_DATASET_LIST,
      setLoadingAction: actionTypes.SET_DATASET_LIST_LOADING,
    }),
  );
}

export function toggleDatasetLoading(isLoading) {
  return { type: actionTypes.TOGGLE_DATASET_LOADING, isLoading };
}

export function toggleDatasetDiffer(isNeedDiffer) {
  return { type: actionTypes.TOGGLE_DATASET_DIFFER, isNeedDiffer };
}

export function recoveryStore(store) {
  return { type: actionTypes.RECOVERY_DATASET_STORE, store };
}

export function initRelationship() {
  return { type: actionTypes.INIT_RELATIONSHIP };
}

export function setDatasetBasicInfo(datasetInfo) {
  const json = { ...datasetInfo };

  if (datasetInfo.datasetName) {
    json.datasetName = datasetInfo.datasetName.trim();
  }

  return { type: actionTypes.SET_DATASET_BASIC_INFO, json };
}

export function updateModels(models) {
  return { type: actionTypes.UPDATE_MODELS, models };
}

export function pushModelTables(tables) {
  return { type: actionTypes.PUSH_MODEL_TABLES, tables };
}

export function clearModelsAndTables() {
  return { type: actionTypes.CLEAR_MODELS_AND_TABLES };
}

export function clearAllExceptBasic() {
  return { type: actionTypes.CLEAR_ALL_EXCEPT_BASIC };
}

export function setDimensionUsage(model, form) {
  return { type: actionTypes.SET_DIMENSION_USAGES, model, form };
}

export function setCanvasPosition(modelName, position) {
  return { type: actionTypes.SET_CANVAS_POSITION, modelName, position };
}

export function setCanvasConnection(modelName, connections) {
  return { type: actionTypes.SET_CANVAS_CONNECTION, modelName, connections };
}

export function deleteCanvasModel(modelName) {
  return { type: actionTypes.DELETE_CANVAS_MODEL, modelName };
}

export function deleteRestrict(restrictType, restrict, deleteItems) {
  return {
    type: actionTypes.DELETE_RESTRICT,
    restrictType,
    restrict,
    deleteItems,
  };
}

export function setTranslationType(translationType) {
  return { type: actionTypes.SET_DATASET_TRANSLATION, translationType };
}

export function deleteTranslationType(translationType) {
  return { type: actionTypes.DELETE_DATASET_TRANSLATION, translationType };
}

export function clearAll() {
  return async dispatch => {
    await browserHelper.clearLargeStorage(storagePath.WORKSPACE_DATASET);
    dispatch({ type: actionTypes.CLEAR_ALL });
  };
}

export function setMeasureGroup(model, form) {
  return (dispatch, getState) => {
    const alias = form.alias.trim();
    const key = `${model}`;
    const nodeType = 'measureGroup';
    const translation = refreshTranslation(getState, form);
    const newForm = { ...form, key, nodeType, alias, translation };

    dispatch({ type: actionTypes.SET_MEASURE_GROUP, model, form: newForm });

    return newForm;
  };
}

/* eslint-disable no-use-before-define */
export function validateAllExpressionForMDX() {
  return async (dispatch, getState) => {
    const { dataset } = getState().workspace;
    const schema = dataGenerator.generateExpressionSchema(dataset);

    // 校验CM表达式
    const cMeasureExpressions = dataset.calculateMeasures.map(cMeasure => cMeasure.expression);
    const cMeasuresPayload = { calc_member_str_array: cMeasureExpressions, simple_schema: schema };
    const cMeasureErrors = await DatasetService
      .validateCMeasureExpressionsForMDX({}, cMeasuresPayload);

    const cMeasuresWithError = dataset.calculateMeasures
      .map((cMeasure, index) => ({ ...cMeasure, error: cMeasureErrors[index] }));

    dispatch(batchSetCMeasures(cMeasuresWithError, false));

    // 校验命名集表达式
    const namedSetExpressions = dataset.namedSets.map(namedSet => namedSet.expression);
    const namedSetsPayload = { named_set_str_array: namedSetExpressions, simple_schema: schema };
    const namedSetErrors = await DatasetService
      .validateNamedSetExpressionForMDX({}, namedSetsPayload);

    const namedSetsWithError = dataset.namedSets
      .map((namedSet, index) => ({
        ...namedSet,
        error: namedSetErrors[index].error,
        location: namedSetErrors[index].location && namedSetErrors[index].location !== 'Measures'
          ? namedSetErrors[index].location
          : 'Named Set',
      }));

    dispatch(batchSetNamedSets(namedSetsWithError, false));

    // 校验Default Member表达式
    const validateDefaultMemberColumns = [];
    // 先遍历平铺有defaultMember的dimCols
    for (const model of dataset.models) {
      for (const table of model.dimensionTables) {
        for (const column of table.dimCols) {
          const { defaultMember } = column;
          if (defaultMember) {
            const { alias: tableAlias } = table;
            validateDefaultMemberColumns.push({ ...column, tableAlias });
          }
        }
      }
    }
    // 获取defaultMember的simple schema
    const defaultMemberPayload = {
      default_member: validateDefaultMemberColumns.map(({ defaultMember }) => defaultMember),
      default_member_path: validateDefaultMemberColumns.map(({ model, tableAlias, alias }) => `${model}.${tableAlias}.${alias}`),
      simple_schema: schema,
    };
    // 发送给后端进行校验，获取error数组列表
    const defaultMemberErrors = await DatasetService
      .validateDefaultMemberForMDX({}, defaultMemberPayload);
    // 组合出带着error的dimCols
    const dimColsWithDefaultMemberError = validateDefaultMemberColumns
      .map((column, index) => ({
        ...column,
        tableAlias: undefined,
        defaultMemberError: defaultMemberErrors[index],
      }));
    // 将报错信息批量更新进store中
    dispatch(batchSetDimColumns(dimColsWithDefaultMemberError, false));
  };
}

export function setDimTable(model, form, isValidateExpression = true) {
  return (dispatch, getState) => {
    if (!form.isVisible) {
      Object.assign(form, { invisible: [], visible: [] });
    }

    const alias = form.alias.trim();
    const key = `${model}-${form.name}`;
    const nodeType = 'table';
    const translation = refreshTranslation(getState, form);
    const newForm = { ...form, key, nodeType, model, alias, translation };
    dispatch({ type: actionTypes.SET_DIM_TABLE, model, form: newForm });

    if (isValidateExpression) {
      dispatch(validateAllExpressionForMDX());
    }

    return newForm;
  };
}

export function setDimColumn(model, table, form, isValidateExpression = true) {
  return (dispatch, getState) => {
    if (!form.isVisible) {
      Object.assign(form, { invisible: [], visible: [] });
    }

    const alias = form.alias.trim();
    const key = `${model}-${table}-c-${form.name}`;
    const nodeType = 'column';
    const translation = refreshTranslation(getState, form);
    const newForm = { ...form, key, nodeType, model, table, alias, translation };
    dispatch({ type: actionTypes.SET_DIM_COLUMN, model, table, form: newForm });
    dispatch({ type: actionTypes.REFRESH_DIM_COLUMN_PROPERTIES, form: newForm });

    if (isValidateExpression) {
      dispatch(validateAllExpressionForMDX());
    }

    return newForm;
  };
}

export function batchSetDimColumns(columns, isValidate = true) {
  return dispatch => {
    batch(() => {
      columns.map(column => (
        dispatch(setDimColumn(column.model, column.table, column, false))
      ));
    });

    if (isValidate) {
      dispatch(validateAllExpressionForMDX());
    }
  };
}

export function setDimMeasure(model, table, form, isValidateExpression = true) {
  return (dispatch, getState) => {
    if (!form.isVisible) {
      Object.assign(form, { invisible: [], visible: [] });
    }

    const alias = form.alias.trim();
    const key = `${model}-${form.name}`;
    const nodeType = 'measure';
    const translation = refreshTranslation(getState, form);
    const newForm = { ...form, key, nodeType, model, table, alias, translation };
    dispatch({ type: actionTypes.SET_DIM_MEASURE, model, table, form: newForm });
    dispatch({ type: actionTypes.REFRESH_CMEASURE_NON_EMPTY_BEHAVIOR, form: newForm, model });

    if (isValidateExpression) {
      dispatch(validateAllExpressionForMDX());
    }

    return newForm;
  };
}

export function batchSetDimMeasures(measures, isValidate = true) {
  return dispatch => {
    batch(() => {
      measures.map(measure => (
        dispatch(setDimMeasure(measure.model, measure.table, measure, false))
      ));
    });

    if (isValidate) {
      dispatch(validateAllExpressionForMDX());
    }
  };
}

export function setHierarchy(model, table, oldName, form) {
  return (dispatch, getState) => {
    const name = form.name.trim();
    const key = `${model}-${table}-h-${form.name.trim()}`;
    const nodeType = 'hierarchy';
    const translation = refreshTranslation(getState, form);
    const newForm = { ...form, key, name, nodeType, model, table, translation };

    dispatch({ type: actionTypes.SET_HIERARCHY, model, table, oldName, form: newForm });
    dispatch(validateAllExpressionForMDX());

    return newForm;
  };
}

export function deleteHierarchy(model, table, form, isValidate = true) {
  return dispatch => {
    dispatch({ type: actionTypes.DELETE_HIERARCHY, model, table, form });
    if (isValidate) {
      dispatch(validateAllExpressionForMDX());
    }
  };
}

export function setCalculatedMeasure(oldName, form, isValidateExpression = true) {
  return (dispatch, getState) => {
    if (!form.isVisible) {
      Object.assign(form, { invisible: [], visible: [] });
    }

    const name = form.name.trim();
    const key = `calculateMeasureRoot-${name}`;
    const nodeType = 'calculateMeasure';
    const translation = refreshTranslation(getState, form);
    const newForm = { ...form, key, name, nodeType, translation };

    dispatch({ type: actionTypes.SET_CALCULATED_MEASURE, oldName, form: newForm });
    if (isValidateExpression) {
      dispatch(validateAllExpressionForMDX());
    }

    return newForm;
  };
}

export function batchSetCMeasures(cMeasures, isValidate = true) {
  return dispatch => {
    batch(() => {
      cMeasures.map(cMeasure => (
        dispatch(setCalculatedMeasure(cMeasure.name, cMeasure, false))
      ));
    });

    if (isValidate) {
      dispatch(validateAllExpressionForMDX());
    }
  };
}

export function deleteCalculatedMeasure(form, isValidate = true) {
  return dispatch => {
    dispatch({ type: actionTypes.DELETE_CALCULATED_MEASURE, form });
    if (isValidate) {
      dispatch(validateAllExpressionForMDX());
    }
  };
}

export function setNamedSet(oldName, form, isValidateExpression) {
  return (dispatch, getState) => {
    if (!form.isVisible) {
      Object.assign(form, { invisible: [], visible: [] });
    }
    const name = form.name.trim();
    const key = `namedSetRoot-${name}`;
    const nodeType = 'namedSet';
    const translation = refreshTranslation(getState, form);
    const newForm = { ...form, name, key, nodeType, translation };

    dispatch({ type: actionTypes.SET_NAMEDSET, oldName, form: newForm });
    if (isValidateExpression) {
      dispatch(validateAllExpressionForMDX());
    }
    return newForm;
  };
}

export function batchSetNamedSets(namedSets, isValidate = true) {
  return dispatch => {
    batch(() => {
      namedSets.map(namedSet => (
        dispatch(setNamedSet(namedSet.name, namedSet, false))
      ));
    });

    if (isValidate) {
      dispatch(validateAllExpressionForMDX());
    }
  };
}

export function deleteNamedSet(form, isValidate = true) {
  return dispatch => {
    dispatch({ type: actionTypes.DELETE_NAMEDSET, form });
    if (isValidate) {
      dispatch(validateAllExpressionForMDX());
    }
  };
}

export function batchDelete(items = []) {
  return dispatch => {
    batch(() => {
      for (const item of items) {
        switch (item.nodeType) {
          case NODE_TYPES.NAMEDSET:
            dispatch(deleteNamedSet(item, false));
            break;
          case NODE_TYPES.HIERARCHY:
            dispatch(deleteHierarchy(item.model, item.table, item, false));
            break;
          case NODE_TYPES.CALCULATE_MEASURE:
            dispatch(deleteCalculatedMeasure(item, false));
            break;
          default:
            break;
        }
      }
    });
    dispatch(validateAllExpressionForMDX());
  };
}

export function addRelationModel(model) {
  return async dispatch => {
    dispatch({ type: actionTypes.ADD_RELATION_MODEL, model });
    dispatch(toggleDatasetDiffer(true));
  };
}

export function addRelationship(relation) {
  return async dispatch => {
    dispatch({ type: actionTypes.ADD_RELATIONSHIP, relation });
    dispatch(toggleDatasetDiffer(true));
  };
}

export function updateRelationship(relation) {
  return async dispatch => {
    dispatch({ type: actionTypes.UPDATE_RELATIONSHIP, relation });
    dispatch(toggleDatasetDiffer(true));
  };
}

export function resetDataset(modelsDetail) {
  return async dispatch => {
    dispatch(initRelationship());
    dispatch({ type: actionTypes.INIT_DATASET, modelsDetail });
  };
}

export function initDataset() {
  return async (dispatch, getState) => {
    const datasetStore = getState().workspace.dataset;

    dispatch(toggleDatasetLoading(true));

    try {
      const modelsDetail = await datasetHelper.getSemanticModelInfos(datasetStore, getState);
      dispatch(resetDataset(modelsDetail));
    } finally {
      dispatch(toggleDatasetLoading(false));
    }
  };
}

export function checkDatasetName(options) {
  return () => DatasetService.checkDatasetName(options);
}

export function validateCMeasureExpressionForMDX(value) {
  return (dispatch, getState) => {
    const { dataset } = getState().workspace;
    const schema = dataGenerator.generateExpressionSchema(dataset);
    const cMeasuresPayload = { calc_member_str_array: [value], simple_schema: schema };
    return DatasetService.validateCMeasureExpressionsForMDX({}, cMeasuresPayload);
  };
}

export function validateDefaultMemberForMDX(value, modelName, tableAlias, columnAlias) {
  return (dispatch, getState) => {
    const { dataset } = getState().workspace;
    const schema = dataGenerator.generateExpressionSchema(dataset);
    const path = `${modelName}.${tableAlias}.${columnAlias}`;
    const defaultMemberPayload = {
      default_member: [value],
      default_member_path: [path],
      simple_schema: schema,
    };
    return DatasetService.validateDefaultMemberForMDX({}, defaultMemberPayload);
  };
}

export function validateNamedSetExpressionForMDX(value) {
  return (dispatch, getState) => {
    const { dataset } = getState().workspace;
    const schema = dataGenerator.generateExpressionSchema(dataset);
    const namedSetsPayload = { named_set_str_array: [value], simple_schema: schema };
    return DatasetService.validateNamedSetExpressionForMDX({}, namedSetsPayload);
  };
}

export function getProjectModels() {
  return async (dispatch, getState) => {
    const { name: project } = getState().system.currentProject;
    const models = await DatasetService.fetchProjectModels(null, null, { getState });
    if (models) {
      dispatch(clearModelsAndTables());

      const responses = [];
      for (const model of models) {
        const params = { project, model: model.modelName };
        responses.push(await DatasetService.fetchModelTables(params, null, { getState }));
      }

      batch(() => {
        responses.forEach((tables, idx) => {
          if (tables) {
            const modelsAndTablesList = { ...models[idx], tables };
            dispatch(pushModelTables(modelsAndTablesList));
          }
        });
      });
    }
  };
}

export function saveDatasetJson({ createType = datasetCreateTypes.NEW } = {}) {
  return async (dispatch, getState) => {
    const { dataset } = getState().workspace;
    const datasetJson = dataGenerator.generateDatasetJson(dataset);
    return DatasetService.saveDatasetJson({ createType }, datasetJson);
  };
}

export function updateDatasetJson(datasetId, updateType) {
  return async (dispatch, getState) => {
    const { dataset } = getState().workspace;
    const datasetJson = dataGenerator.generateDatasetJson(dataset);
    return DatasetService.updateDatasetJson({ datasetId, updateType }, datasetJson);
  };
}

export function deleteDataset(datasetId, options = {}) {
  const { resolveException = false, isRefresh = true } = options;
  return async (dispatch, getState) => {
    try {
      const state = getState();
      const { datasetList } = state.data;
      const { pageOffset, pageSize } = datasetList;

      const status = await DatasetService.deleteDataset({ datasetId });
      if (isRefresh) {
        await dispatch(getDatasets({ ...options, pageOffset, pageSize }));
      }
      return status;
    } catch (e) {
      if (resolveException) {
        return e;
      }
      throw e;
    }
  };
}

export function setDatasetStore(datasetId) {
  return async (dispatch, getState) => {
    const datasetJson = await DatasetService.fetchDatasetJson({ datasetId });
    let storeJson = null;

    if (datasetJson) {
      const currentVersion = datasetJson.front_v ? datasetJson.front_v : 'v0.1';
      const nextVersion = configs.datasetVersionMaps[currentVersion];

      storeJson = datasetJson;
      if (nextVersion !== 'newest') {
        storeJson = await datasetHelper.upgradeDatasetJson(datasetJson, currentVersion, getState);
      }
      storeJson = datasetHelper.formatDatasetJsonToStore(storeJson);
      dispatch(recoveryStore(storeJson));
    }
    return storeJson;
  };
}

export function diffDatasetChange() {
  return async (dispatch, getState) => {
    const datasetStore = getState().workspace.dataset;

    dispatch(toggleDatasetLoading(true));

    try {
      const modelsDetail = await datasetHelper.getSemanticModelInfos(datasetStore);
      const datasetSchema = await datasetHelper.generateNewDataset(datasetStore, modelsDetail);
      const storeJson = datasetHelper.getDifferedDatasetStore(datasetStore, datasetSchema);

      dispatch(recoveryStore(storeJson));
      dispatch(initRelationship());
      dispatch(toggleDatasetDiffer(false));
    } finally {
      dispatch(toggleDatasetLoading(false));
    }
  };
}

export function exportDatasetJSON(datasetId) {
  return async () => {
    const datasetJson = await DatasetService.fetchDatasetJson({ datasetId });
    // 生成下载文件名称
    const downloadAt = dayjs().format('YYYYMMDDHHmmss');
    const fileName = `${datasetJson.project}_${datasetJson.dataset_name}_${downloadAt}.json`;
    // 生成下载文件内容
    const contentType = { type: 'application/json,charset=utf-8;' };
    const blob = new Blob([JSON.stringify(datasetJson, null, 2)], contentType);
    // 下载文件
    if ('msSaveBlob' in window.navigator) {
      window.navigator.msSaveBlob(blob, fileName);
    } else {
      const url = window.URL.createObjectURL(blob);
      domHelper.download.get(url, fileName);
    }
  };
}

export function uploadDatasetPackage(file, projectName) {
  return async () => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('projectName', projectName);
    const response = await DatasetService.uploadDatasetPackage(formData);
    response.differences = response.dataDiffList.map(item => ({
      ...item,
      brife_items: item.brife_items || [],
      new_items: item.new_items || [],
      reduce_items: item.reduce_items || [],
      update_items: item.update_items || [],
    }));
    return response;
  };
}

export function importDatasetPackage(form, host, port) {
  return () => {
    const formData = {
      token: form.token,
      projectName: form.projectName,
      datasets: form.datasets
        .map(dataset => ({
          id: dataset.id,
          name: dataset.name,
          acl: dataset.acl,
          type: dataset.type,
        })),
    };
    return DatasetService.importDatasetPackage(formData, host, port);
  };
}

export function getKeAccessMapping(name, type) {
  return async (dispatch, getState) => {
    const { dataset } = getState().workspace;
    const accessSchema = dataGenerator.generateAccessSchema(dataset);
    // 从后端获取所有维度，度量，计算度量，命名集的KE列级权限
    const accessContent = await DatasetService
      .fetchDatasetKeAccesses({ name, type }, accessSchema, { getState });
    // 生成权限Mapping，并返回
    return datasetHelper.getAccessMapping(accessSchema, accessContent);
  };
}

export function previewFormatSample(format, value) {
  return () => DatasetService.previewFormatSample({ format, value });
}
