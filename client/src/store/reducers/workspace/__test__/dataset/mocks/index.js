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
