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
import { createSelector } from 'reselect';
import { configs, strings } from '../constants';
import { DatasetService } from '../services';
import * as dataHelper from './dataHelper';

const { nodeTypes: { CALCULATE_MEASURE, NAMEDSET, HIERARCHY }, formatTypes } = configs;

export async function getSemanticModelInfos(datasetStore, getState) {
  const { project, models } = datasetStore;
  const modelNames = models.map(model => model.name).filter(model => model);
  const reponses = await Promise.all(modelNames.map(model => (
    DatasetService.fetchSemanticModelInfo({ project, model }, null, { getState })
  )));
  return reponses.filter(item => item);
}

export function getDefaultCanvasJson(json) {
  return {
    models: json.models.map((model, index) => ({
      name: model.model_name,
      x: index * 50,
      y: index * 50,
      top: [],
      right: json.model_relations
        .filter(relation => relation.model_left === model.model_name)
        .map(relation => ({
          name: relation.model_right,
          direction: 'Left',
        })),
      bottom: [],
      left: [],
    })),
  };
}

export function formatModelRelations(json) {
  return {
    project: json.project,
    modelRelations: json.model_relations.map(item => ({
      modelLeft: item.model_left,
      modelRight: item.model_right,
      relation: item.relation,
    })),
    models: json.models.map(model => ({
      name: model.model_name,
    })),
  };
}

export const upgradeProcess = {
  'v0.1': async (json, getState) => {
    const formatedRelations = formatModelRelations(json);
    const upgradeData = await getSemanticModelInfos(formatedRelations, getState);

    const { upgradeDatasetV01 } = await import(/* webpackChunkName: "dataset_v0.1" */'../extensions/dataUpgrade/dataset_v0.1');
    return upgradeDatasetV01(json, upgradeData);
  },
  'v0.2': async (json, upgradeData) => {
    const { upgradeDatasetV02 } = await import(/* webpackChunkName: "dataset_v0.2" */'../extensions/dataUpgrade/dataset_v0.2');
    return upgradeDatasetV02(json, upgradeData);
  },
  'v0.3': async json => {
    const { upgradeDatasetV03 } = await import(/* webpackChunkName: "dataset_v0.3" */'../extensions/dataUpgrade/dataset_v0.3');
    return upgradeDatasetV03(json);
  },
  'v0.4': json => json,
};

export async function upgradeDatasetJson(json, version, getState) {
  let newJson = await upgradeProcess[version](json, getState);

  const nextVersion = configs.datasetVersionMaps[version];
  if (nextVersion !== 'newest') {
    newJson = await upgradeDatasetJson(newJson, nextVersion, getState);
  }
  return newJson;
}

export const datasetErrorTypes = {
  model(modelName, brokenInfo) {
    if (brokenInfo && brokenInfo.models) {
      return brokenInfo.models.find(item => item.name === modelName);
    }
    return null;
  },
  commonTable(modelName, tableName, brokenInfo) {
    if (brokenInfo && brokenInfo.dimension_tables) {
      const commonTableErrors = brokenInfo.dimension_tables.filter(item => item.type === 'COMMON_TABLE_DELETED');
      const tableFullName = `${modelName}.${tableName}`;
      return commonTableErrors.find(item => item.name === tableFullName);
    }
    return null;
  },
  hierarchy(modelName, tableName, hierarchyName, brokenInfo) {
    if (brokenInfo && brokenInfo.hierarchys) {
      const hierarchyFullName = `${modelName}.${tableName}.${hierarchyName}`;
      return brokenInfo.hierarchys.filter(item => item.name === hierarchyFullName);
    }
    return [];
  },
  bridgeTable(modelName, tableName, brokenInfo) {
    if (brokenInfo && brokenInfo.dimension_tables) {
      const bridgeTableErrors = brokenInfo.dimension_tables.filter(item => item.type === 'BRIDGE_TABLE_DELETED');
      const tableFullName = `${modelName}.${tableName}`;
      return bridgeTableErrors.find(item => item.name === tableFullName);
    }
    return null;
  },
  factKey(modelName, factKeyName, brokenInfo) {
    if (brokenInfo && brokenInfo.dimension_cols) {
      const tableFullName = `${modelName}.${factKeyName}`;
      return brokenInfo.dimension_cols.find(item => item.name === tableFullName);
    }
    return null;
  },
  property(modelName, tableName, columnName, brokenInfo) {
    if (brokenInfo && brokenInfo.property) {
      const columnFullName = `${modelName}.${tableName}.${columnName}`;
      return brokenInfo.property.filter(item => item.name === columnFullName);
    }
    return null;
  },
  nameColumn(modelName, tableName, columnName, brokenInfo) {
    if (brokenInfo && brokenInfo.name_columns) {
      const columnFullName = `${modelName}.${tableName}.${columnName}`;
      return brokenInfo.name_columns.find(item => item.name === columnFullName);
    }
    return null;
  },
  valueColumn(modelName, tableName, columnName, brokenInfo) {
    if (brokenInfo && brokenInfo.value_columns) {
      const columnFullName = `${modelName}.${tableName}.${columnName}`;
      return brokenInfo.value_columns.find(item => item.name === columnFullName);
    }
    return null;
  },
  nonEmptyBehavior(cMeasureName, brokenInfo) {
    if (brokenInfo && brokenInfo.nonEmptyBehavior) {
      return brokenInfo.nonEmptyBehavior.filter(item => item.name === cMeasureName);
    }
    return null;
  },
};

export function uniqueNameGenerator() {
  const uniqueIdxMap = {};
  return function getUniqueName(name) {
    let baseName = name;
    let idx = null;
    let suffix = uniqueIdxMap[name] ? `_${uniqueIdxMap[name]}` : '';

    if (/_[0-9]+$/.test(name)) {
      baseName = name.replace(/_[0-9]+$/, '');
      idx = +name.replace(baseName, '').replace(/^_/, '') + 1;
      suffix = `_${idx}`;
    }

    if (idx && idx > (uniqueIdxMap[baseName] || 0)) {
      uniqueIdxMap[baseName] = idx;
    } else if (uniqueIdxMap[baseName]) {
      uniqueIdxMap[baseName] += 1;
    } else {
      uniqueIdxMap[baseName] = 1;
    }
    return `${baseName}${suffix}`;
  };
}

/* eslint-disable no-param-reassign */
function findPublicRoot(publicMap, tableOrder = [], leftTableID, rightTableID) {
  if (!publicMap[leftTableID]) {
    return publicMap[rightTableID];
  }
  if (!publicMap[rightTableID]) {
    return publicMap[leftTableID];
  }
  if (publicMap[leftTableID] !== publicMap[rightTableID]) {
    const leftCursorIdx = tableOrder.indexOf(publicMap[leftTableID]);
    const rightCursorIdx = tableOrder.indexOf(publicMap[rightTableID]);

    const cursor = leftCursorIdx < rightCursorIdx
      ? publicMap[leftTableID]
      : publicMap[rightTableID];
    const replaceCursor = leftCursorIdx < rightCursorIdx
      ? publicMap[rightTableID]
      : publicMap[leftTableID];

    for (const [mapKey, mapCursor] of Object.entries(publicMap)) {
      if (mapCursor === replaceCursor) {
        publicMap[mapKey] = cursor;
      }
    }
    return cursor;
  }
  if (publicMap[leftTableID] === publicMap[rightTableID]) {
    return publicMap[leftTableID];
  }
  return null;
}

export function getPublicTableMap(datasetStore) {
  const publicMap = {};
  const tableOrder = [];

  for (const link of datasetStore.modelRelations) {
    for (const { left, right } of link.relation) {
      const leftTableID = `${link.modelLeft}.${left}`;
      const rightTableID = `${link.modelRight}.${right}`;

      if (!tableOrder.includes(leftTableID)) {
        tableOrder.push(leftTableID);
      }

      if (!tableOrder.includes(rightTableID)) {
        tableOrder.push(rightTableID);
      }

      const publicTableID = findPublicRoot(
        publicMap, tableOrder, leftTableID, rightTableID,
      ) || leftTableID;

      publicMap[leftTableID] = publicTableID;
      publicMap[rightTableID] = publicTableID;
    }
  }

  return publicMap;
}

export function initUniqueName(datasetStore, getTableAlias, getColumnAlias, getMeasureAlias) {
  // 调用一次uniqueNameGenerator，使原先存在datasetStore里的别名去重
  for (const model of datasetStore.models) {
    // 缓存去重原先store中的table
    for (const dimTable of model.dimensionTables || []) {
      getTableAlias(dimTable.alias || dimTable.name || '');
      // 缓存去重原先store中的dimCol
      for (const dimCol of dimTable.dimCols || []) {
        getColumnAlias(dimCol.alias || dimCol.name || '');
      }
    }
    // 缓存去重原先store中的measure
    for (const measure of model.measures || []) {
      getMeasureAlias(measure.alias || measure.name || '');
    }
  }
}

export function generateNewDataset(datasetStore, modelsDetail) {
  const getTableAlias = uniqueNameGenerator();
  const getColumnAlias = val => val;
  const getMeasureAlias = uniqueNameGenerator();
  initUniqueName(datasetStore, getTableAlias, getColumnAlias, getMeasureAlias);

  const publicTableMap = getPublicTableMap(datasetStore);

  const models = datasetStore.models.sort((modelA, modelB) => (
    modelA.name < modelB.name ? -1 : 1
  )).map(model => {
    const modelDetail = modelsDetail.find(m => m.modelName === model.name);

    let dimensionTables = [];
    let allTables = [];
    let factTableAlias = null;
    let cubeMeasures = [];

    if (modelDetail) {
      dimensionTables = modelDetail.dimensionTables.filter(table => {
        const tableMapIdx = `${model.name}.${table.name}`;
        return !publicTableMap[tableMapIdx] || publicTableMap[tableMapIdx] === tableMapIdx;
      });
      allTables = modelDetail.dimensionTables.map(table => table.name);
      factTableAlias = `${modelDetail.factTableSchema}.${modelDetail.factTableAlias}`;
      cubeMeasures = modelDetail.cubeMeasures;
    }
    return {
      key: model.name,
      name: model.name,
      alias: model.name,
      factTableAlias,
      nodeType: 'model',
      translation: {},
      // allTables只有在创建的时候会用到，用于生成维度用法表格
      // 编辑不会修改表关系，所以不会使用allTables
      allTables,
      dimensionTables: dimensionTables.map(table => ({
        key: `${model.name}-${table.name}`,
        name: table.name,
        alias: getTableAlias(table.name),
        nodeType: 'table',
        translation: {},
        type: 'regular',
        model: model.name,
        actualTable: table.actualTable,
        isPublicTable: (() => {
          const tableMapIdx = `${model.name}.${table.name}`;
          return publicTableMap[tableMapIdx] === tableMapIdx;
        })(),
        dimCols: table.dimensionColumns.map(column => ({
          key: `${model.name}-${table.name}-c-${column.columnName}`,
          name: column.columnName,
          desc: '',
          nodeType: 'column',
          subfolder: '',
          translation: {},
          type: configs.dimColumnTypes.REGULAR,
          alias: getColumnAlias(column.columnAlias || column.columnName),
          dataType: column.dataType.toLowerCase(),
          isVisible: true,
          invisible: [],
          visible: [],
          model: model.name,
          table: table.name,
          defaultMember: '',
          // Issue: https://github.com/Kyligence/MDX/issues/218
          nameColumn: null,
          valueColumn: null,
          properties: [],
        })),
        hierarchys: [],
      })),
      measures: cubeMeasures
        .map(measure => ({
          key: `${model.name}-${measure.measureName}`,
          name: measure.measureName,
          desc: '',
          nodeType: 'measure',
          format: 'regular',
          formatType: formatTypes.NO_FORMAT,
          subfolder: '',
          translation: {},
          alias: getMeasureAlias(measure.alias || measure.measureName),
          dataType: measure.dataType,
          expression: measure.expression,
          expressionParams: (() => {
            if (measure.colMeasured) {
              const { tableAlias, colName } = measure.colMeasured;
              return tableAlias !== 'constant' ? `${tableAlias}.${colName}` : colName;
            }
            return '';
          })(),
          model: model.name,
          isVisible: true,
          invisible: [],
          visible: [],
        })),
      error: !modelDetail ? {
        name: model.name,
        type: 'MODEL_DELETED',
      } : null,
    };
  });

  const dimTableModelRelations = models.map(model => ({
    modelName: model.name,
    tableRelations: model.allTables.map(tableName => ({
      tableName,
      relationType: configs.tableRelationTypes.JOINT,
      relationFactKey: null,
      relationBridgeTableName: null,
    })),
  }));
  return { ...datasetStore, models, dimTableModelRelations };
}

export function formatDatasetJsonToStore(json) {
  const modelRelations = json.model_relations.map(link => ({
    modelLeft: link.model_left,
    modelRight: link.model_right,
    name: `${link.model_left}&&${link.model_right}`,
    relation: link.relation.map(relation => ({
      left: relation.left,
      right: relation.right,
      // Error Message
      leftError: datasetErrorTypes.commonTable(
        link.model_left,
        relation.left,
        json.broken_info,
      ),
      // Error Message
      rightError: datasetErrorTypes.commonTable(
        link.model_right,
        relation.right,
        json.broken_info,
      ),
    })),
  }));

  const leftTablesMap = modelRelations.reduce((models, link) => ({
    ...models,
    [link.modelLeft]: [
      ...(models[link.modelLeft] || []),
      ...link.relation.map(({ left }) => left),
    ],
  }), {});

  return {
    project: json.project,
    datasetName: json.dataset_name,
    type: json.type,
    access: !!json.access,
    version: configs.version,
    translationTypes: json.translation_types || [],
    modelRelations,
    models: json.models
      .sort((modelA, modelB) => (
        modelA.model_name < modelB.model_name ? -1 : 1
      ))
      .map(model => ({
        key: model.model_name,
        name: model.model_name,
        alias: model.model_alias || model.model_name,
        factTableAlias: model.fact_table,
        modify: model.modify,
        nodeType: 'model',
        translation: model.translation || {},
        dimensionTables: model.dimension_tables.map(table => ({
          key: `${model.model_name}-${table.name}`,
          name: table.name,
          type: table.type,
          alias: table.alias,
          actualTable: table.actual_table,
          nodeType: 'table',
          translation: table.translation || {},
          // 如果undefined会短路返回undefined
          isPublicTable: !!(leftTablesMap[model.model_name] &&
            leftTablesMap[model.model_name].includes(table.name)),
          // parent info
          model: model.model_name,
          dimCols: table.dim_cols.map(column => ({
            key: `${model.model_name}-${table.name}-c-${column.name}`,
            name: column.name,
            desc: column.desc,
            type: column.type,
            alias: column.alias,
            dataType: column.data_type,
            nodeType: 'column',
            subfolder: column.subfolder || '',
            translation: column.translation || {},
            isVisible: !dataHelper.isEmpty(column.is_visible) ? column.is_visible : true,
            invisible: column.invisible || [],
            visible: column.visible || [],
            // parent info
            model: model.model_name,
            table: table.name,
            // Issue: https://github.com/Kyligence/MDX/issues/218
            nameColumn: column.name_column || null,
            valueColumn: column.value_column || null,
            defaultMember: column.default_member || '',
            properties: column.properties
              ? column.properties.map(property => ({
                name: property.name,
                columnName: property.col_name,
                columnAlias: property.col_alias,
              }))
              : [],
            propertyErrors: datasetErrorTypes.property(
              model.model_name,
              table.name,
              column.name,
              json.broken_info,
            ),
            nameColumnError: datasetErrorTypes.nameColumn(
              model.model_name,
              table.name,
              column.name,
              json.broken_info,
            ),
            valueColumnError: datasetErrorTypes.valueColumn(
              model.model_name,
              table.name,
              column.name,
              json.broken_info,
            ),
          })),
          hierarchys: table.hierarchys.map(hierarchy => ({
            key: `${model.model_name}-${table.name}-h-${hierarchy.name}`,
            name: hierarchy.name,
            desc: hierarchy.desc,
            // 当且仅当weight_cols有内容时，不赋予null值
            weightCols: hierarchy.weight_cols && hierarchy.weight_cols.length
              ? hierarchy.weight_cols
              : hierarchy.dim_cols.map(() => null),
            dimCols: hierarchy.dim_cols,
            nodeType: 'hierarchy',
            translation: hierarchy.translation || {},
            // Error Message
            errors: datasetErrorTypes.hierarchy(
              model.model_name,
              table.name,
              hierarchy.name,
              json.broken_info,
            ),
            // parent info
            model: model.model_name,
            table: table.name,
          })),
        })),
        measures: model.measures.map(measure => ({
          key: `${model.model_name}-${measure.name}`,
          name: measure.name,
          desc: measure.desc,
          subfolder: measure.subfolder || '',
          alias: measure.alias,
          dataType: measure.data_type,
          expression: measure.expression,
          expressionParams: measure.dim_column,
          nodeType: 'measure',
          format: measure.format || 'regular',
          formatType: measure.format === 'regular' && !measure.format_type ? formatTypes.NO_FORMAT : (measure.format_type || formatTypes.CUSTOM),
          translation: measure.translation || {},
          isVisible: !dataHelper.isEmpty(measure.is_visible) ? measure.is_visible : true,
          invisible: measure.invisible || [],
          visible: measure.visible || [],
          // parent info
          model: model.model_name,
        })),
        // Error Message
        error: datasetErrorTypes.model(model.model_name, json.broken_info),
      })),
    dimTableModelRelations: json.dim_table_model_relations.map(item => ({
      modelName: item.model_name,
      tableRelations: item.table_relations.map(usage => ({
        tableName: usage.table_name,
        relationType: usage.relation_type,
        relationFactKey: usage.relation_fact_key,
        relationBridgeTableName: usage.relation_bridge_table_name,
        // Error Message
        bridgeTableError: datasetErrorTypes.bridgeTable(
          item.model_name,
          usage.relation_bridge_table_name,
          json.broken_info,
        ),
        // Error Message
        factKeyError: datasetErrorTypes.factKey(
          item.model_name,
          usage.relation_fact_key,
          json.broken_info,
        ),
      })),
    })),
    calculateMeasures: json.calculate_measures.map(cMeasure => ({
      name: cMeasure.name,
      format: cMeasure.format,
      formatType: cMeasure.format === 'regular' && !cMeasure.format_type ? formatTypes.NO_FORMAT : (cMeasure.format_type || formatTypes.CUSTOM),
      desc: cMeasure.desc,
      folder: cMeasure.folder,
      subfolder: cMeasure.subfolder || '',
      expression: cMeasure.expression,
      isVisible: !dataHelper.isEmpty(cMeasure.is_visible) ? cMeasure.is_visible : true,
      invisible: cMeasure.invisible || [],
      visible: cMeasure.visible || [],
      key: `calculateMeasureRoot-${cMeasure.name}`,
      nodeType: 'calculateMeasure',
      translation: cMeasure.translation || {},
      error: '',
      nonEmptyBehavior: cMeasure.non_empty_behavior || [],
      nonEmptyBehaviorErrors: datasetErrorTypes.nonEmptyBehavior(
        cMeasure.name,
        json.broken_info,
      ),
    })),
    namedSets: (json.named_sets && json.named_sets.map(namedSet => ({
      name: namedSet.name,
      location: namedSet.location,
      expression: namedSet.expression,
      isVisible: !dataHelper.isEmpty(namedSet.is_visible) ? namedSet.is_visible : true,
      invisible: namedSet.invisible || [],
      visible: namedSet.visible || [],
      key: `namedSetRoot-${namedSet.name}`,
      nodeType: 'namedSet',
      translation: namedSet.translation || {},
      error: '',
    }))) || [],
    modelsAndTablesList: [],
    canvas: json.canvas ? JSON.parse(json.canvas) : getDefaultCanvasJson(json),
  };
}

function getLeftTablesMap(store) {
  return store.modelRelations.reduce((models, link) => ({
    ...models,
    [link.modelLeft]: [
      ...(models[link.modelLeft] || []),
      ...link.relation.map(({ left }) => left),
    ],
  }), {});
}

function checkRelationPublic(publicTableMap, modelName, tableName) {
  const tableMapIdx = `${modelName}.${tableName}`;
  return publicTableMap[tableMapIdx] === tableMapIdx;
}

function checkLeftPublic(leftTablesMap, modelName, tableName) {
  return leftTablesMap[modelName] && leftTablesMap[modelName].includes(tableName);
}

function getAllPublicTable(store, schema, publicTableMap, leftTablesMap) {
  const publicTables = {};

  for (const model of store.models) {
    const schemaModel = schema.models.find(m => m.name === model.name);
    const dimensionTables = model.dimensionTables || schemaModel.dimensionTables;
    for (const table of dimensionTables) {
      const isRelationPublic = checkRelationPublic(publicTableMap, model.name, table.name);
      const isLeftPublic = checkLeftPublic(leftTablesMap, model.name, table.name);

      const tableMapIdx = `${model.name}.${table.name}`;
      const isPublicTable = isRelationPublic || isLeftPublic;
      const isNotInResult = !publicTables[tableMapIdx];

      if (isPublicTable && isNotInResult) {
        table.isPublicTable = true;
        publicTables[tableMapIdx] = table;
      }
    }
  }

  return publicTables;
}

function fixPublicToModel(model, publicTable) {
  return {
    ...publicTable,
    key: publicTable.key.replace(/^[^~-]+-/, `${model.name}-`),
    model: model.name,
    dimCols: publicTable.dimCols.map(column => ({
      ...column,
      key: column.key.replace(/^[^~-]+-/, `${model.name}-`),
      model: model.name,
    })),
    hierarchys: publicTable.hierarchys.map(hierarchy => ({
      ...hierarchy,
      key: hierarchy.key.replace(/^[^~-]+-/, `${model.name}-`),
      tablePath: hierarchy.tablePath && [model.name, hierarchy.tablePath[1]],
      model: model.name,
    })),
  };
}

export function getDifferedDatasetStore(store, schema) {
  const publicTableMap = getPublicTableMap(store);
  const leftTablesMap = getLeftTablesMap(store);
  const allPublicTable = getAllPublicTable(store, schema, publicTableMap, leftTablesMap);

  // 去掉删除的models，补上新增的models
  const differedModels = schema.models.map(schemaModel => {
    // 如果有继承的model，则使用继承model；如果没有，则使用新增model
    const currentStoreModel = store.models
      .find(storeModel => storeModel.key === schemaModel.key);
    const differedModel = currentStoreModel || schemaModel;
    // MDX-649: factTableAlias字段强制刷新，防止后端不返回fact_table字段，当该模型下出现维表却没有fact_table导致的保存报错
    differedModel.factTableAlias = schemaModel.factTableAlias;

    if (currentStoreModel) {
      differedModel.dimensionTables = schemaModel.dimensionTables.map(schemaTable => {
        // 如果table.key和table.isPublicTable未发生变化，继承原来table
        const currentStoreTable = differedModel.dimensionTables
          .find(storeTable => storeTable.key === schemaTable.key &&
            storeTable.isPublicTable === schemaTable.isPublicTable);
        let differedTable = currentStoreTable || schemaTable;

        const tableMapIdx = `${schemaModel.name}.${schemaTable.name}`;
        const schemaPublicTable = allPublicTable[tableMapIdx];

        if (schemaTable.isPublicTable && allPublicTable[tableMapIdx]) {
          differedTable = fixPublicToModel(schemaModel, schemaPublicTable);
        }

        return differedTable;
      });
    }
    return differedModel;
  });

  // 对于已经移除的度量组，把其中所有的计算度量移入默认度量组
  const differedCMeasures = store.calculateMeasures.map(cMeasure => {
    const isFolderExisted = schema.models.some(model => model.name === cMeasure.folder);
    return isFolderExisted ? cMeasure : { ...cMeasure, folder: 'Calculated Measure' };
  });

  // 对于已经移除的所属文件夹，把其中所有的计算度量移入默认命名集中
  const differedNamedSets = store.namedSets.map(namedSet => {
    const isLocationExisted = schema.models.some(model => (
      model.dimensionTables.some(table => `${model.name}.${table.name}` === namedSet.location)
    ));
    return isLocationExisted ? namedSet : { ...namedSet, location: 'Named Set' };
  });

  // 去掉删除的维度用法，补上新增的维度用法
  const differedUsages = schema.dimTableModelRelations.map(schemaUsage => {
    const currentStoreUsage = store.dimTableModelRelations
      .find(storeUsage => storeUsage.modelName === schemaUsage.modelName);
    const differedUsage = currentStoreUsage || schemaUsage;

    if (currentStoreUsage) {
      differedUsage.tableRelations = schemaUsage.tableRelations.map(schemaUsageTable => {
        // 如果table.key和table.isPublicTable未发生变化，继承原来table
        const currentStoreUsageTable = differedUsage.tableRelations
          .find(storeUsageTable => storeUsageTable.tableName === schemaUsageTable.tableName);
        const differedUsageTables = currentStoreUsageTable || schemaUsageTable;
        return differedUsageTables;
      });
    }
    return differedUsage;
  });

  return {
    ...store,
    models: differedModels,
    calculateMeasures: differedCMeasures,
    dimTableModelRelations: differedUsages,
    namedSets: differedNamedSets,
  };
}

export const getSqlDatasetWords = dataset => {
  const models = [];
  const tables = [];
  const columns = [];
  const measures = [];

  for (const model of dataset.models) {
    models.push({ meta: 'model', caption: model.name, value: model.name, scope: 1 });
    for (const table of model.dimensionTables) {
      const tableFullName = `${model.name}.${table.alias}`;
      tables.push({ meta: 'table', caption: table.alias, value: table.alias, scope: 1 });
      tables.push({ meta: 'table', caption: tableFullName, value: tableFullName, scope: 1 });
      for (const column of table.dimCols) {
        const columnFullName = `${table.alias}.${column.alias}`;
        columns.push({ meta: 'column', caption: column.alias, value: column.alias, scope: 1 });
        columns.push({ meta: 'column', caption: columnFullName, value: columnFullName, scope: 1 });
      }
    }
    for (const measure of model.measures) {
      measures.push({ meta: 'measure', caption: `[Measures].[${measure.alias}]`, value: `[Measures].[${measure.alias}]`, scope: 1 });
    }
  }

  return { models, tables, columns, measures };
};

export const getMdxDatasetWords = dataset => {
  const models = [];
  const tables = [];
  const columns = [];
  const measures = [];
  const hierarchys = [];
  const calcMeasures = [];
  const namedSets = [];

  for (const model of dataset.models) {
    for (const table of model.dimensionTables) {
      tables.push({ meta: 'table', caption: `[${table.alias}]`, value: `[${table.alias}]`, scope: 1 });
      for (const column of table.dimCols) {
        columns.push({ meta: 'dimension', caption: `[${column.alias}]`, value: `[${column.alias}]`, scope: 1 });
        columns.push({ meta: 'dimension', caption: `[${table.alias}].[${column.alias}]`, value: `[${table.alias}].[${column.alias}]`, scope: 1 });
        columns.push({ meta: 'level', caption: `[${table.alias}].[${column.alias}].[ALL]`, value: `[${table.alias}].[${column.alias}].[ALL]`, scope: 1 });
        columns.push({ meta: 'level', caption: `[${table.alias}].[${column.alias}].[${column.alias}]`, value: `[${table.alias}].[${column.alias}].[${column.alias}]`, scope: 1 });
      }
      for (const hierarchy of table.hierarchys) {
        hierarchys.push({ meta: 'hierarchy', caption: `[${table.alias}].[${hierarchy.name}-Hierarchy]`, value: `[${table.alias}].[${hierarchy.name}-Hierarchy]`, scope: 1 });
        hierarchys.push({ meta: 'hierarchy', caption: `[${table.alias}].[${hierarchy.name}-Hierarchy].[ALL]`, value: `[${table.alias}].[${hierarchy.name}-Hierarchy].[ALL]`, scope: 1 });
        hierarchys.push({ meta: 'hierarchy', caption: `[${table.alias}].[${hierarchy.name}-Hierarchy].Members`, value: `[${table.alias}].[${hierarchy.name}-Hierarchy].Members`, scope: 1 });
        hierarchys.push({ meta: 'hierarchy', caption: `[${table.alias}].[${hierarchy.name}-Hierarchy].CurrentMember`, value: `[${table.alias}].[${hierarchy.name}-Hierarchy].CurrentMember`, scope: 1 });
        for (const columnName of hierarchy.dimCols) {
          const column = table.dimCols.find(col => col.name === columnName);
          if (column) {
            hierarchys.push({ meta: 'level', caption: `[${table.alias}].[${hierarchy.name}-Hierarchy].[${column.alias}]`, value: `[${table.alias}].[${hierarchy.name}-Hierarchy].[${column.alias}]`, scope: 1 });
          }
        }
      }
    }
    for (const measure of model.measures) {
      measures.push({ meta: 'measure', caption: `[Measures].[${measure.alias}]`, value: `[Measures].[${measure.alias}]`, scope: 1 });
    }
  }

  for (const calcMeasure of dataset.calculateMeasures) {
    calcMeasures.push({ meta: 'measure', caption: `[Measures].[${calcMeasure.name}]`, value: `[Measures].[${calcMeasure.name}]`, scope: 1 });
  }

  for (const namedSet of dataset.namedSets) {
    namedSets.push({ meta: 'named set', caption: `{[${namedSet.name}]}`, value: `{[${namedSet.name}]}`, scope: 1 });
  }

  return { models, tables, columns, measures, hierarchys, calcMeasures, namedSets };
};

export function getIsAccessInKE(key, accessMapping) {
  return !accessMapping[key] || (accessMapping[key] && accessMapping[key].access);
}

export function getFullVisibilityItems(dataset, nodeType) {
  const items = [];

  if (nodeType === 'column') {
    for (const model of dataset.models) {
      for (const table of model.dimensionTables) {
        for (const dimCol of table.dimCols) {
          items.push(dimCol);
        }
      }
    }
  }
  if (nodeType === 'measure') {
    for (const model of dataset.models) {
      for (const measure of model.measures) {
        items.push(measure);
      }
    }
  }
  if (nodeType === 'calculateMeasure') {
    for (const cMeasure of dataset.calculateMeasures) {
      items.push(cMeasure);
    }
  }
  if (nodeType === 'namedSet') {
    for (const namedSet of dataset.namedSets) {
      items.push(namedSet);
    }
  }
  return items;
}

export function setTranslationDataset(translationType, dataset, isRemove) {
  const models = dataset.models.map(model => ({
    ...model,
    translation: {
      ...model.translation,
      [translationType]: isRemove ? undefined : '',
    },
    dimensionTables: model.dimensionTables.map(table => ({
      ...table,
      translation: {
        ...table.translation,
        [translationType]: isRemove ? undefined : '',
      },
      dimCols: table.dimCols.map(column => ({
        ...column,
        translation: {
          ...column.translation,
          [translationType]: isRemove ? undefined : '',
        },
      })),
      hierarchys: table.hierarchys.map(hierarchy => ({
        ...hierarchy,
        translation: {
          ...hierarchy.translation,
          [translationType]: isRemove ? undefined : '',
        },
      })),
    })),
    measures: model.measures.map(measure => ({
      ...measure,
      translation: {
        ...measure.translation,
        [translationType]: isRemove ? undefined : '',
      },
    })),
  }));
  const namedSets = dataset.namedSets.map(namedSet => ({
    ...namedSet,
    translation: {
      ...namedSet.translation,
      [translationType]: isRemove ? undefined : '',
    },
  }));
  const calculateMeasures = dataset.calculateMeasures.map(cMeasure => ({
    ...cMeasure,
    translation: {
      ...cMeasure.translation,
      [translationType]: isRemove ? undefined : '',
    },
  }));

  return { models, namedSets, calculateMeasures };
}

function sortByLabel(array = []) {
  return array.sort((itemA, itemB) => {
    const labelA = typeof itemA.label === 'string' ? itemA.label : itemA.label.defaultMessage;
    const labelB = typeof itemB.label === 'string' ? itemB.label : itemB.label.defaultMessage;

    const lowerLabelA = labelA.toLowerCase();
    const lowerLabelB = labelB.toLowerCase();

    return lowerLabelA.localeCompare(lowerLabelB);
  });
}

export const getDimensionTree = createSelector(
  state => state.dataset,
  dataset => [
    ...(() => {
      const currentRootNamedSets = dataset.namedSets
        .filter(namedSet => namedSet.location === 'Named Set');
      const hasNamedSets = !!currentRootNamedSets.length;

      return hasNamedSets ? [{
        key: 'namedSetRoot',
        label: strings.NAMEDSET,
        nodeType: 'namedSetRoot',
        children: sortByLabel(currentRootNamedSets.map(namedSet => ({
          ...dataHelper.getExcludeTypeValue(['object'], namedSet),
          label: namedSet.name,
          nodeType: 'namedSet',
          key: `namedSetRoot-${namedSet.name}`,
          invisible: namedSet.invisible,
          visible: namedSet.visible,
          error: namedSet.error,
          translation: namedSet.translation,
        }))),
      }] : [];
    })(),
    ...sortByLabel(dataset.models.map(model => ({
      ...dataHelper.getExcludeTypeValue(['object'], model),
      label: model.name,
      nodeType: 'model',
      key: model.name,
      translation: model.translation,
      children: sortByLabel(model.dimensionTables ? model.dimensionTables.map(table => ({
        ...dataHelper.getExcludeTypeValue(['object'], table),
        label: table.alias,
        nodeType: 'table',
        model: model.name,
        key: `${model.name}-${table.name}`,
        translation: table.translation,
        children: [
          ...(() => {
            const currentNamedSets = dataset.namedSets
              .filter(namedSet => namedSet.location === `${model.name}.${table.alias}`);
            const hasNamedSets = !!currentNamedSets.length;

            return hasNamedSets ? [{
              label: strings.NAMEDSET,
              key: `${model.name}-${table.name}-namedSetRoot`,
              nodeType: 'namedSetRoot',
              model: model.name,
              table: table.name,
              children: sortByLabel(currentNamedSets.map(namedSet => ({
                ...dataHelper.getExcludeTypeValue(['object'], namedSet),
                label: namedSet.name,
                nodeType: 'namedSet',
                model: model.name,
                table: table.name,
                key: `namedSetRoot-${namedSet.name}`,
                invisible: namedSet.invisible,
                visible: namedSet.visible,
                error: namedSet.error,
                translation: namedSet.translation,
              }))),
            }] : [];
          })(),
          ...sortByLabel(table.dimCols ? table.dimCols.map(column => ({
            ...dataHelper.getExcludeTypeValue(['object'], column),
            label: column.alias,
            nodeType: 'column',
            model: model.name,
            table: table.name,
            invisible: column.invisible,
            visible: column.visible,
            key: `${model.name}-${table.name}-c-${column.name}`,
            properties: column.properties,
            translation: column.translation,
            nameColumnError: column.nameColumnError,
            valueColumnError: column.valueColumnError,
            propertyErrors: column.propertyErrors,
            defaultMemberError: column.defaultMemberError,
          })) : []),
          ...sortByLabel(table.hierarchys ? table.hierarchys.map(hierarchy => ({
            ...dataHelper.getExcludeTypeValue(['object'], hierarchy),
            label: hierarchy.name,
            nodeType: 'hierarchy',
            model: model.name,
            table: table.name,
            weightCols: hierarchy.weightCols,
            dimCols: hierarchy.dimCols,
            translation: hierarchy.translation,
            key: `${model.name}-${table.name}-h-${hierarchy.name}`,
            errors: hierarchy.errors,
          })) : []),
        ],
      })) : []),
    }))),
  ],
);

export const getMeasureTree = createSelector(
  state => state.dataset,
  dataset => {
    const folderMap = {};

    function addToFolder(item) {
      const { folder } = item;
      if (folderMap[folder]) {
        folderMap[folder].push(item);
      } else {
        folderMap[folder] = [item];
      }
    }

    for (const cMeasure of dataset.calculateMeasures) {
      addToFolder({
        ...dataHelper.getExcludeTypeValue(['object'], cMeasure),
        label: cMeasure.name,
        nodeType: 'calculateMeasure',
        folder: cMeasure.folder || 'Calculated Measure',
        subfolder: cMeasure.subfolder,
        invisible: cMeasure.invisible,
        visible: cMeasure.visible,
        translation: cMeasure.translation,
        error: cMeasure.error,
        key: `calculateMeasureRoot-${cMeasure.name}`,
        nonEmptyBehavior: cMeasure.nonEmptyBehavior,
        nonEmptyBehaviorErrors: cMeasure.nonEmptyBehaviorErrors,
      });
    }

    for (const model of dataset.models) {
      for (const measure of model.measures || []) {
        addToFolder({
          ...dataHelper.getExcludeTypeValue(['object'], measure),
          label: measure.alias,
          nodeType: 'measure',
          model: model.name,
          folder: measure.folder || model.name,
          subfolder: measure.subfolder,
          invisible: measure.invisible,
          fullExpression: `${measure.expression}(${measure.expressionParams ? measure.expressionParams : ''})`,
          visible: measure.visible,
          translation: measure.translation,
          key: `${model.name}-${measure.name}`,
        });
      }
    }

    const result = Object.entries(folderMap)
      .map(([folder, children]) => {
        const currentModel = dataset.models.find(model => model.name === folder);
        const cMeasureFolder = strings.CALCULATED_MEASURES;

        return {
          label: folder === 'Calculated Measure' ? cMeasureFolder : folder,
          name: folder === 'Calculated Measure' ? cMeasureFolder : folder,
          alias: (folder === 'Calculated Measure' || !currentModel) ? cMeasureFolder : currentModel.alias,
          nodeType: folder === 'Calculated Measure' ? 'calculateMeasureRoot' : 'measureGroup',
          key: folder === 'Calculated Measure' ? 'calculateMeasureRoot' : folder,
          translation: currentModel ? currentModel.translation : undefined,
          children: sortByLabel(children),
        };
      });

    // 过滤把 计算度量 的度量组放在第一个
    return [
      ...result.filter(itemA => itemA.nodeType === 'calculateMeasureRoot'),
      ...sortByLabel(result.filter(itemA => itemA.nodeType !== 'calculateMeasureRoot')),
    ];
  },
);

export const getDatasetAliasMap = createSelector(
  state => state.dataset,
  dataset => {
    const map = {};
    for (const model of dataset.models) {
      if (model.dimensionTables) {
        for (const table of model.dimensionTables) {
          const tableKey = `${model.name}.${table.name}`;
          map[tableKey] = table.alias;
          for (const dimCol of table.dimCols) {
            const dimColKey = `${model.name}.${table.name}.${dimCol.name}`;
            map[dimColKey] = dimCol.alias;
          }
        }
        for (const measure of model.measures) {
          const measureKey = `${model.name}.${measure.name}`;
          map[measureKey] = measure.alias;
        }
      }
    }
    return map;
  },
);

export const getDatasetKeyMap = createSelector(
  state => state.dataset,
  dataset => {
    const map = {};
    for (const model of dataset.models) {
      map[model.key] = model;
      if (model.dimensionTables) {
        for (const table of model.dimensionTables) {
          map[table.key] = table;
          for (const dimCol of table.dimCols) {
            map[dimCol.key] = dimCol;
          }
          for (const hierarchy of table.hierarchys) {
            map[hierarchy.key] = hierarchy;
          }
        }
        for (const measure of model.measures) {
          map[measure.key] = measure;
        }
      }
    }
    for (const cMeasure of dataset.calculateMeasures) {
      map[cMeasure.key] = cMeasure;
    }
    for (const namedSet of dataset.namedSets) {
      map[namedSet.key] = namedSet;
    }
    return map;
  },
);

export const getErrorCount = createSelector(
  state => state.errorList,
  errorList => {
    const errorCount = {
      [CALCULATE_MEASURE]: 0,
      [NAMEDSET]: 0,
      [HIERARCHY]: 0,
    };

    for (const error of errorList) {
      errorCount[error.nodeType] += 1;
    }

    return errorCount;
  },
);

export function getDatasetKey(nodeType, data) {
  switch (nodeType) {
    case 'column': return `${data.model}-${data.table}-c-${data.name}`;
    case 'measure': return `${data.model}-${data.name}`;
    case 'calculateMeasure': return `calculateMeasureRoot-${data.name}`;
    case 'namedSet': return `namedSetRoot-${data.name}`;
    default: return null;
  }
}

function pushColumnAccessInMapping({
  schemaColumn, accessColumn, schemaModel, schemaTable, accessMapping,
}) {
  const { name } = schemaColumn;
  const { access } = accessColumn;
  const effectedBy = !access ? accessColumn.effected_by.map(column => ({ type: 'column', name: column })) : [];

  const model = schemaModel.model_name;
  const table = schemaTable.name;
  const columnKey = getDatasetKey('column', { name, model, table });

  accessMapping[columnKey] = { access, effectedBy };
}

function pushColumnDenyInMapping({
  schemaColumn, schemaModel, schemaTable, accessMapping,
}) {
  const { name } = schemaColumn;
  const access = false;
  const effectedBy = [{ type: 'table', name: schemaTable.actual_table }];

  const model = schemaModel.model_name;
  const table = schemaTable.name;
  const columnKey = getDatasetKey('column', { name, model, table });

  accessMapping[columnKey] = { access, effectedBy };
}

/**
 * 将Schema与Result合并，生成AccessMapping
 * 维度部分未对 AccessResult 直接做map，是为了处理表级权限.
 * 当用户没有表级权限时，后端仅返回 access 字段，但不一定有 dim_cols 字段
 *
 * @param {*} accessSchema 权限Schema
 * @param {*} accessResult 权限Result
 */
export function getAccessMapping(accessSchema, accessResult) {
  const accessMapping = {};
  const { models: schemaModels } = accessSchema;
  const { models: accessModels } = accessResult;

  for (const schemaModel of schemaModels) {
    // 获得 SchemaModel 和 accessModel
    const accessModel = accessModels.find(model => model.model_name === schemaModel.model_name);

    const { dimension_tables: accessTables = [] } = accessModel || {};
    const { dimension_tables: schemaTables } = schemaModel;

    for (const schemaTable of schemaTables) {
      // 获得 SchemaTable 和 accessTable
      const accessTable = accessTables.find(table => table.name === schemaTable.name);

      const { dim_cols: accessColumns = [] } = accessTable || {};
      const { dim_cols: schemaColumns } = schemaTable;

      for (const schemaColumn of schemaColumns) {
        const accessColumn = accessColumns.find(column => column.name === schemaColumn.name);

        if (accessTable.access === false) {
          // 当用户没有表级权限，设置成无权限
          pushColumnDenyInMapping({ schemaColumn, schemaModel, schemaTable, accessMapping });
        } else if (accessColumn) {
          // 当 AccessColumn 查出了权限
          /* eslint-disable-next-line max-len */
          pushColumnAccessInMapping({ schemaColumn, accessColumn, schemaModel, schemaTable, accessMapping });
        }
      }
    }

    for (const measure of accessModel.measures) {
      const data = { name: measure.name, model: accessModel.model_name };
      const measureKey = getDatasetKey('measure', data);
      accessMapping[measureKey] = {
        access: measure.access,
        effectedBy: !measure.access ? measure.effected_by.map(column => ({ type: 'column', name: column })) : [],
      };
    }
  }

  for (const cMeasure of accessResult.calculate_measures) {
    const data = { name: cMeasure.name };
    const cMeasureKey = getDatasetKey('calculateMeasure', data);
    accessMapping[cMeasureKey] = {
      access: cMeasure.access,
      effectedBy: !cMeasure.access ? cMeasure.effected_by.map(column => ({ type: 'column', name: column })) : [],
    };
  }

  for (const namedSet of accessResult.named_sets) {
    const data = { name: namedSet.name };
    const namedSetKey = getDatasetKey('namedSet', data);
    accessMapping[namedSetKey] = {
      access: namedSet.access,
      effectedBy: !namedSet.access ? namedSet.effected_by.map(column => ({ type: 'column', name: column })) : [],
    };
  }

  return accessMapping;
}

export const getMeasureFolderOptions = createSelector(
  state => state.dataset,
  state => state.measureGroup,
  (dataset, measureGroup) => {
    const currModel =
      dataset.models.find(model => model.name === measureGroup) ?? { measures: [] };

    const folderOptions = [];

    for (const item of currModel.measures) {
      if (item.subfolder && !folderOptions.includes(item.subfolder)) {
        folderOptions.push(item.subfolder);
      }
    }

    for (const item of dataset.calculateMeasures) {
      if (
        item.subfolder &&
        item.folder === measureGroup &&
        !folderOptions.includes(item.subfolder)
      ) {
        folderOptions.push(item.subfolder);
      }
    }

    return folderOptions.map(option => ({ label: option, value: option }));
  },
);

export const getDimensionFolderOptions = createSelector(
  state => state.dataset,
  state => state.modelName,
  state => state.tableName,
  (dataset, modelName, tableName) => {
    const currModel =
      dataset.models.find(model => model.name === modelName) ?? { dimensionTables: [] };
    const currTable =
      currModel.dimensionTables.find(table => table.name === tableName) ?? { dimCols: [] };

    const folderOptions = [];

    for (const item of currTable.dimCols) {
      if (item.subfolder && !folderOptions.includes(item.subfolder)) {
        folderOptions.push(item.subfolder);
      }
    }

    return folderOptions.map(option => ({ label: option, value: option }));
  },
);

export const getCustomFormats = createSelector(
  state => state.dataset,
  dataset => {
    const formatOptions = [];

    for (const model of dataset.models) {
      for (const measure of model.measures) {
        if (measure.formatType === formatTypes.CUSTOM) {
          formatOptions.push(measure.format);
        }
      }
    }

    for (const cMeasure of dataset.calculateMeasures) {
      if (cMeasure.formatType === formatTypes.CUSTOM) {
        formatOptions.push(cMeasure.format);
      }
    }

    return formatOptions;
  },
);
