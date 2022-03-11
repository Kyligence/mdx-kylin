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
import * as actionTypes from '../../types';
import { storeHelper, datasetHelper } from '../../../utils';

export function getInitialState() {
  return {
    project: '',
    datasetName: '',
    access: false,
    type: 'MDX',
    translationTypes: [],
    modelRelations: [],
    models: [],
    modelsAndTablesList: [],
    calculateMeasures: [],
    namedSets: [],
    dimTableModelRelations: [],
    canvas: { models: [] },
    isLoading: false,
    isNeedDiffer: false,
  };
}

export function getInitialCanvasModel() {
  return {
    x: 0,
    y: 0,
    top: [],
    right: [],
    bottom: [],
    left: [],
  };
}

export default function dataset(state = getInitialState(), action = {}) {
  switch (action.type) {
    case actionTypes.SET_DATASET_BASIC_INFO: {
      return { ...state, ...action.json };
    }
    case actionTypes.ADD_RELATION_MODEL: {
      const newModels = [...state.models, action.model];
      return { ...state, models: newModels };
    }
    case actionTypes.UPDATE_MODELS: {
      const newModels = action.models;
      return { ...state, models: newModels };
    }
    case actionTypes.INIT_RELATIONSHIP: {
      const multipleRelations = state.modelRelations.filter(
        link => link.modelLeft && link.modelRight,
      );
      const checkHasRelations = model => !multipleRelations.some(
        ({ modelLeft, modelRight }) => [modelLeft, modelRight].includes(model.name),
      );
      const singleCubes = state.models.filter(model => checkHasRelations(model));
      const singleRelations = singleCubes.map(singleCube => ({
        modelLeft: singleCube.name,
        modelRight: '',
        relation: [],
      }));
      return { ...state, modelRelations: [...singleRelations, ...multipleRelations] };
    }
    case actionTypes.ADD_RELATIONSHIP: {
      const newRelations = [...state.modelRelations, action.relation];
      return { ...state, modelRelations: newRelations };
    }
    case actionTypes.UPDATE_RELATIONSHIP: {
      return { ...state, modelRelations: action.relation };
    }
    case actionTypes.PUSH_MODEL_TABLES: {
      const newTables = [...state.modelsAndTablesList, action.tables];
      return { ...state, modelsAndTablesList: newTables };
    }
    case actionTypes.CLEAR_MODELS_AND_TABLES: {
      return { ...state, modelsAndTablesList: [] };
    }
    case actionTypes.CLEAR_ALL_EXCEPT_BASIC: {
      return {
        ...state,
        modelsAndTablesList: [],
        modelRelations: [],
        models: [],
        calculateMeasures: [],
        namedSets: [],
        dimTableModelRelations: [],
        canvas: { models: [] },
      };
    }
    case actionTypes.CLEAR_ALL: {
      return getInitialState();
    }
    case actionTypes.INIT_DATASET: {
      return datasetHelper.generateNewDataset(state, action.modelsDetail);
    }
    case actionTypes.SET_DIM_TABLE: {
      const modelIndex = state.models.findIndex(item => item.name === action.model);
      const currentTables = state.models[modelIndex].dimensionTables;
      const tableIndex = currentTables.findIndex(item => item.name === action.form.name);

      const path = [modelIndex, 'dimensionTables', tableIndex];
      const models = storeHelper.set(state.models, path, action.form);
      return { ...state, models };
    }
    case actionTypes.SET_DIM_COLUMN: {
      const modelIndex = state.models.findIndex(item => item.name === action.model);
      const currentTables = state.models[modelIndex].dimensionTables;
      const tableIndex = currentTables.findIndex(item => item.name === action.table);
      const currentColumns = currentTables[tableIndex].dimCols;
      const columnIndex = currentColumns.findIndex(item => item.name === action.form.name);

      const path = [modelIndex, 'dimensionTables', tableIndex, 'dimCols', columnIndex];
      const models = storeHelper.set(state.models, path, action.form);
      return { ...state, models };
    }
    case actionTypes.SET_DIM_MEASURE: {
      const modelIndex = state.models.findIndex(item => item.name === action.model);
      const currentMeasures = state.models[modelIndex].measures;
      const measureIndex = currentMeasures.findIndex(item => item.name === action.form.name);

      const path = [modelIndex, 'measures', measureIndex];
      const models = storeHelper.set(state.models, path, action.form);
      return { ...state, models };
    }
    case actionTypes.SET_MEASURE_GROUP: {
      const modelIndex = state.models.findIndex(item => item.name === action.model);

      const path = [modelIndex];
      const models = storeHelper.set(state.models, path, action.form);
      return { ...state, models };
    }
    case actionTypes.SET_HIERARCHY: {
      const modelIndex = state.models.findIndex(item => item.name === action.model);
      const currentTables = state.models[modelIndex].dimensionTables;
      const tableIndex = currentTables.findIndex(item => item.name === action.table);

      const currentHierarchys = currentTables[tableIndex].hierarchys;
      const hierarchyIndex = currentHierarchys.findIndex(
        item => item.name === action.form.name || item.name === action.oldName,
      );

      if (hierarchyIndex === -1) {
        const path = [modelIndex, 'dimensionTables', tableIndex, 'hierarchys'];
        const newForm = { ...action.form, model: action.model, table: action.table, errors: [] };
        const models = storeHelper.set(state.models, path, [...currentHierarchys, newForm]);
        return { ...state, models };
      }
      const { errors } = currentHierarchys[hierarchyIndex];

      const path = [modelIndex, 'dimensionTables', tableIndex, 'hierarchys', hierarchyIndex];
      const newForm = { ...action.form, model: action.model, table: action.table, errors };
      const models = storeHelper.set(state.models, path, newForm);
      return { ...state, models };
    }
    case actionTypes.DELETE_HIERARCHY: {
      const modelIndex = state.models.findIndex(item => item.name === action.model);
      const currentTables = state.models[modelIndex].dimensionTables;
      const tableIndex = currentTables.findIndex(item => item.name === action.table);

      const newHierarchys = currentTables[tableIndex].hierarchys.filter(
        item => item.name !== action.form.name,
      );
      const path = [modelIndex, 'dimensionTables', tableIndex, 'hierarchys'];
      const models = storeHelper.set(state.models, path, newHierarchys);
      return { ...state, models };
    }
    case actionTypes.SET_CALCULATED_MEASURE: {
      const cMeasureIndex = state.calculateMeasures.findIndex(
        item => item.name === action.form.name || item.name === action.oldName,
      );

      if (cMeasureIndex === -1) {
        const calculateMeasures = [...state.calculateMeasures, action.form];
        return { ...state, calculateMeasures };
      }
      const path = [cMeasureIndex];
      const calculateMeasures = storeHelper.set(state.calculateMeasures, path, action.form);
      return { ...state, calculateMeasures };
    }
    case actionTypes.DELETE_CALCULATED_MEASURE: {
      const calculateMeasures = state.calculateMeasures.filter(
        item => item.name !== action.form.name,
      );
      return { ...state, calculateMeasures };
    }
    case actionTypes.SET_NAMEDSET: {
      const namedSetIndex = state.namedSets.findIndex(
        item => item.name === action.form.name || item.name === action.oldName,
      );

      if (namedSetIndex === -1) {
        const namedSets = [...state.namedSets, action.form];
        return { ...state, namedSets };
      }
      const path = [namedSetIndex];
      const namedSets = storeHelper.set(state.namedSets, path, action.form);
      return { ...state, namedSets };
    }
    case actionTypes.DELETE_NAMEDSET: {
      const namedSets = state.namedSets.filter(
        item => item.name !== action.form.name,
      );
      return { ...state, namedSets };
    }
    case actionTypes.SET_DIMENSION_USAGES: {
      const modelIdx = state.dimTableModelRelations.findIndex(
        model => model.modelName === action.model,
      );
      const currentModel = state.dimTableModelRelations[modelIdx];
      const relationIdx = currentModel.tableRelations.findIndex(
        item => item.tableName === action.form.tableName,
      );

      let factKeyError = null;
      let bridgeTableError = null;
      const currentRelation = currentModel.tableRelations[relationIdx];

      if (currentRelation.factKeyError) {
        const [fkTableName, fkColumnName] = currentRelation.factKeyError.name.split('.');
        factKeyError = `${fkTableName}.${fkColumnName}` === action.form.relationFactKey ? currentRelation.factKeyError : null;
      }
      if (currentRelation.bridgeTableError) {
        const brTableName = currentRelation.bridgeTableError.name.split('.')[1];
        bridgeTableError = brTableName === action.form.relationBridgeTableName
          ? currentRelation.bridgeTableError : null;
      }

      const path = [modelIdx, 'tableRelations', relationIdx];
      const newForm = { ...action.form, factKeyError, bridgeTableError };
      const dimTableModelRelations = storeHelper.set(
        state.dimTableModelRelations, path, newForm,
      );
      return { ...state, dimTableModelRelations };
    }
    case actionTypes.RECOVERY_DATASET_STORE: {
      return { ...state, ...action.store };
    }
    case actionTypes.SET_CANVAS_POSITION: {
      const { modelName, position } = action;
      const modelIndex = state.canvas.models.findIndex(model => model.name === modelName);

      let path = ['models'];
      let modelData = { ...getInitialCanvasModel(), name: modelName, ...position };
      let canvas = storeHelper.set(state.canvas, path, [...state.canvas.models, modelData]);

      if (modelIndex !== -1) {
        path = ['models', modelIndex];
        modelData = { ...state.canvas.models[modelIndex], ...position };
        canvas = storeHelper.set(state.canvas, path, modelData);
      }
      return { ...state, canvas };
    }
    case actionTypes.SET_CANVAS_CONNECTION: {
      const { modelName, connections } = action;
      const modelIndex = state.canvas.models.findIndex(model => model.name === modelName);

      let path = ['models'];
      let modelData = { ...getInitialCanvasModel(), name: modelName, ...connections };
      let canvas = storeHelper.set(state.canvas, path, [...state.canvas.models, modelData]);

      if (modelIndex !== -1) {
        path = ['models', modelIndex];
        modelData = { ...state.canvas.models[modelIndex], name: modelName, ...connections };
        canvas = storeHelper.set(state.canvas, path, modelData);
      }
      return { ...state, canvas };
    }
    case actionTypes.DELETE_CANVAS_MODEL: {
      const { modelName } = action;
      const path = ['models'];
      const models = state.canvas.models
        .filter(model => model.name !== modelName)
        .map(model => {
          const newModel = { ...model };
          for (const dirKey of Object.keys(model)) {
            if (model[dirKey] instanceof Array) {
              newModel[dirKey] = model[dirKey].filter(connection => connection.name !== modelName);
            }
          }
          return newModel;
        });
      const canvas = storeHelper.set(state.canvas, path, models);
      return { ...state, canvas };
    }
    case actionTypes.TOGGLE_DATASET_LOADING: {
      const { isLoading } = action;
      return { ...state, isLoading };
    }
    case actionTypes.DELETE_RESTRICT: {
      const { restrict, deleteItems, restrictType } = action;
      const markedModels = {};
      const markedTables = {};

      deleteItems.forEach(item => {
        if (item.model) {
          markedModels[item.model] = true;
        }
        if (item.table) {
          markedTables[item.table] = true;
        }
      });

      const models = state.models.map(model => ({
        ...model,
        dimensionTables: model.name in markedModels
          ? model.dimensionTables.map(table => ({
            ...table,
            dimCols: table.name in markedTables
              ? table.dimCols.map(dimCol => ({
                ...dimCol,
                // 当dimCol在deleteItems时，删除当前restrict；否则保留
                [restrictType]: deleteItems.some(item => (
                  item.nodeType === 'column' &&
                  item.model === model.name &&
                  item.table === table.name &&
                  item.name === dimCol.name
                ))
                  ? dimCol[restrictType].filter(restrictItem => (
                    restrictItem.type !== restrict.type || restrictItem.name !== restrict.name
                  ))
                  : dimCol[restrictType],
              })) : table.dimCols,
          }))
          : model.dimensionTables,
        measures: model.name in markedModels
          ? model.measures.map(measure => ({
            ...measure,
            [restrictType]: deleteItems.some(item => (
              item.nodeType === 'measure' &&
              item.model === model.name &&
              item.name === measure.name
            ))
              ? measure[restrictType].filter(restrictItem => (
                restrictItem.type !== restrict.type || restrictItem.name !== restrict.name
              ))
              : measure[restrictType],
          }))
          : model.measures,
      }));
      const calculateMeasures = state.calculateMeasures.map(cMeasure => ({
        ...cMeasure,
        [restrictType]: deleteItems.some(item => item.nodeType === 'calculateMeasure')
          ? cMeasure[restrictType].filter(restrictItem => (
            restrictItem.type !== restrict.type || restrictItem.name !== restrict.name
          ))
          : cMeasure[restrictType],
      }));
      const namedSets = state.namedSets.map(namedSet => ({
        ...namedSet,
        [restrictType]: deleteItems.some(item => item.nodeType === 'namedSet')
          ? namedSet[restrictType].filter(restrictItem => (
            restrictItem.type !== restrict.type || restrictItem.name !== restrict.name
          ))
          : namedSet[restrictType],
      }));
      return { ...state, models, calculateMeasures, namedSets };
    }
    case actionTypes.TOGGLE_DATASET_DIFFER: {
      return { ...state, isNeedDiffer: action.isNeedDiffer };
    }
    // 后端需要columnAlias去发SQL查询，columnName作为前端的唯一ID去patch columnAlias
    case actionTypes.REFRESH_DIM_COLUMN_PROPERTIES: {
      const models = state.models.map(model => {
        const isCurrentModel = model.name === action.form.model;

        return isCurrentModel
          ? {
            ...model,
            dimensionTables: model.dimensionTables.map(table => {
              const isCurrentTable = table.name === action.form.table;

              return isCurrentTable
                ? {
                  ...table,
                  dimCols: table.dimCols.map(dimCol => ({
                    ...dimCol,
                    properties: dimCol.properties.map(property => {
                      const isChangedProperty = property.columnName === action.form.name;

                      const alias = isChangedProperty ? action.form.alias : property.columnAlias;

                      return {
                        ...property,
                        name: alias,
                        columnAlias: alias,
                      };
                    }),
                  })),
                }
                : table;
            }),
          }
          : model;
      });
      return { ...state, models };
    }
    // 后端需要alias去发SQL查询，behavior.name作为前端的唯一ID去patch columnAlias
    case actionTypes.REFRESH_CMEASURE_NON_EMPTY_BEHAVIOR: {
      const calculateMeasures = state.calculateMeasures.map(cMeasure => ({
        ...cMeasure,
        nonEmptyBehavior: cMeasure.nonEmptyBehavior.map(behavior => {
          const isChangedBehavior = behavior.name === action.form.name &&
            behavior.model === action.model;
          return {
            ...behavior,
            alias: isChangedBehavior ? action.form.alias : behavior.alias,
          };
        }),
      }));
      return { ...state, calculateMeasures };
    }
    case actionTypes.SET_DATASET_TRANSLATION: {
      const { translationType } = action;
      const translationDataset = datasetHelper.setTranslationDataset(translationType, state);
      const translationTypes = [...state.translationTypes, translationType];
      return { ...state, translationTypes, ...translationDataset };
    }
    case actionTypes.DELETE_DATASET_TRANSLATION: {
      const { translationType } = action;
      const translationDataset = datasetHelper.setTranslationDataset(translationType, state, true);
      const translationTypes = state.translationTypes.filter(item => item !== translationType);
      return { ...state, translationTypes, ...translationDataset };
    }
    default:
      return state;
  }
}
