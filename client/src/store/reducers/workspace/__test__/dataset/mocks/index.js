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
import storeDataset from './store_dataset';
import storeSingleDataset from './store_singleDataset';

export function responseProjectModels() {
  return [
    { modelName: '模型A', lastModified: new Date('2020-01-01').getTime() },
    { modelName: '模型B', lastModified: new Date('2020-01-02').getTime() },
    { modelName: '模型C', lastModified: new Date('2020-01-03').getTime() },
    { modelName: '模型D', lastModified: new Date('2020-01-04').getTime() },
    { modelName: '模型E', lastModified: new Date('2020-01-05').getTime() },
    { modelName: '模型F', lastModified: new Date('2020-01-06').getTime() },
  ];
}

export function responseModelTables(model) {
  const modelTables = {
    模型A: ['事实表A', '维度表A', '维度表B', '区分表A'],
    模型B: ['事实表A', '维度表A', '维度表B', '区分表B'],
    模型C: ['事实表A', '维度表A', '维度表B', '区分表C'],
    模型D: ['事实表A', '维度表A', '维度表B', '区分表D'],
    模型E: ['事实表A', '维度表A', '维度表B', '区分表E'],
    模型F: ['事实表A', '维度表A', '维度表B', '区分表F'],
  };

  return modelTables[model];
}

export function responseModelDetails({ model }) {
  const getModelIdx = model.replace('模型', '');
  const tableNames = ['事实表A', '维度表A', '维度表B', `区分表${getModelIdx}`];

  return {
    project: '项目名称',
    modelName: model,
    factTableSchema: '项目名称',
    factTableAlias: '事实表A',
    dimensionTables: tableNames.map(tableName => ({
      name: tableName,
      actualTable: `项目名称.${tableName}`,
      dimensionColumns: [
        {
          columnName: '维度列A',
          columnAlias: null,
          tableAlias: tableName,
          dataType: 'INTEGER',
          actualTable: `项目名称.${tableName}`,
        },
        {
          columnName: '维度列B',
          columnAlias: '维度列B',
          tableAlias: tableName,
          dataType: 'INTEGER',
          actualTable: `项目名称.${tableName}`,
        },
        {
          columnName: '维度C',
          columnAlias: '维度列C',
          tableAlias: tableName,
          dataType: 'INTEGER',
          actualTable: `项目名称.${tableName}`,
        },
      ],
    })),
    cubeMeasures: [
      {
        measureName: '我是度量A',
        alias: '度量A',
        colMeasured: {
          tableAlias: 'constant',
          colName: '1',
        },
        dataType: 'bigint',
        expression: 'COUNT',
      },
      {
        measureName: '度量B',
        alias: '',
        colMeasured: {
          tableAlias: 'constant',
          colName: '1',
        },
        dataType: 'bigint',
        expression: 'COUNT',
      },
      {
        measureName: '度量C',
        alias: '度量C',
        colMeasured: {
          tableAlias: 'constant',
          colName: '1',
        },
        dataType: 'bigint',
        expression: 'COUNT',
      },
    ],
  };
}

export function responseExpressionNamedSet(payload) {
  return payload.named_set_str_array.map(() => ({ location: 'NamedSet', error: '' }));
}

export function responseExpressionCMeasure(payload) {
  return payload.calc_member_str_array.map(() => '');
}

export function getDropModelData(modelData) {
  return {
    name: modelData.modelName,
    modify: modelData.lastModified,
  };
}

export function getMockRelations() {
  return [
    { modelLeft: '模型A', modelRight: '模型B', name: '模型A&&模型B', relation: [{ left: '维度表A', right: '维度表A', leftError: null, rightError: null }, { left: '维度表B', right: '维度表B', leftError: null, rightError: null }] },
    { modelLeft: '模型F', modelRight: '模型E', name: '模型F&&模型E', relation: [{ left: '维度表A', right: '维度表A', leftError: null, rightError: null }, { left: '维度表B', right: '维度表B', leftError: null, rightError: null }] },
    { modelLeft: '模型D', modelRight: '模型E', name: '模型D&&模型E', relation: [{ left: '维度表A', right: '维度表A', leftError: null, rightError: null }, { left: '维度表B', right: '维度表B', leftError: null, rightError: null }] },
    { modelLeft: '模型C', modelRight: '模型B', name: '模型C&&模型B', relation: [{ left: '维度表A', right: '维度表A', leftError: null, rightError: null }, { left: '维度表B', right: '维度表B', leftError: null, rightError: null }] },
    { modelLeft: '模型D', modelRight: '模型B', name: '模型D&&模型B', relation: [{ left: '维度表A', right: '维度表A', leftError: null, rightError: null }] },
  ];
}

export function getMockSingleRelation() {
  return [
    { modelLeft: '模型A', modelRight: '', name: '模型A&&', relation: [] },
  ];
}

export function getMockStoreModels() {
  return storeDataset.models;
}

export function getMockStoreUsages() {
  return storeDataset.dimTableModelRelations;
}

export function getMockStoreDataset() {
  return storeDataset;
}

export function getMockStoreSingleModel() {
  return storeSingleDataset.models;
}

export function getMockStoreSingleUsages() {
  return storeSingleDataset.dimTableModelRelations;
}
