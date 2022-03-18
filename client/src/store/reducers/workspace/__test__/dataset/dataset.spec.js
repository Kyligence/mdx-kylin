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
import { executeDispatch, randomString } from '../../../../../__test__/utils';
import { getDefaultStore } from '../../../../../__test__/mocks';

import datasetReducer from '../../dataset';
import * as actions from '../../../../actions/datasetActions';
import { getDropModelData, getMockRelations, getMockStoreModels, getMockStoreUsages, getMockSingleRelation, getMockStoreSingleModel, getMockStoreSingleUsages, getMockStoreDataset } from './mocks';
import { initMockService, clearMockService } from './handler';

jest.mock('../../../../../services');

describe('Redux: 数据集store', () => {
  beforeEach(() => {
    initMockService();
  });

  afterEach(() => {
    clearMockService();
  });

  describe('多模型数据集', () => {
    let state = datasetReducer();

    // dispatch执行器
    const dispatch = curAction => {
      state = datasetReducer(state, curAction);
    };

    const getState = () => {
      const globalState = getDefaultStore();
      globalState.system.currentProject = { name: '项目名称', permission: 'GLOBAL_ADMIN' };
      globalState.workspace.dataset = state;
      return globalState;
    };

    it('初始状态', () => {
      expect(state).toEqual({
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
      });
    });

    it('设置基本信息', async () => {
      const basicInfo = {
        project: '项目名称',
        datasetName: randomString(10),
        type: 'MDX',
      };
      const action = actions.setDatasetBasicInfo(basicInfo);
      await executeDispatch(action, dispatch);

      expect(state.project).toBe(basicInfo.project);
      expect(state.datasetName).toBe(basicInfo.datasetName);
      expect(state.type).toBe('MDX');
    });

    it('获取项目模型列表', async () => {
      const action = actions.getProjectModels(state);
      await executeDispatch(action, dispatch, getState);

      expect(state.modelsAndTablesList).toContainEqual({
        modelName: '模型A',
        lastModified: 1577836800000,
        tables: ['事实表A', '维度表A', '维度表B', '区分表A'],
      });
      expect(state.modelsAndTablesList).toContainEqual({
        modelName: '模型B',
        lastModified: 1577923200000,
        tables: ['事实表A', '维度表A', '维度表B', '区分表B'],
      });
      expect(state.modelsAndTablesList).toContainEqual({
        modelName: '模型C',
        lastModified: 1578009600000,
        tables: ['事实表A', '维度表A', '维度表B', '区分表C'],
      });
      expect(state.modelsAndTablesList).toContainEqual({
        modelName: '模型D',
        lastModified: 1578096000000,
        tables: ['事实表A', '维度表A', '维度表B', '区分表D'],
      });
      expect(state.modelsAndTablesList).toContainEqual({
        modelName: '模型E',
        lastModified: 1578182400000,
        tables: ['事实表A', '维度表A', '维度表B', '区分表E'],
      });
      expect(state.modelsAndTablesList).toContainEqual({
        modelName: '模型F',
        lastModified: 1578268800000,
        tables: ['事实表A', '维度表A', '维度表B', '区分表F'],
      });
    });

    it('乱序加入关联模型', async () => {
      const dropModel = getDropModelData(state.modelsAndTablesList[4]);
      const action = actions.addRelationModel(dropModel);
      await executeDispatch(action, dispatch, getState);

      const dropModel1 = getDropModelData(state.modelsAndTablesList[3]);
      const action1 = actions.addRelationModel(dropModel1);
      await executeDispatch(action1, dispatch, getState);

      const dropModel2 = getDropModelData(state.modelsAndTablesList[5]);
      const action2 = actions.addRelationModel(dropModel2);
      await executeDispatch(action2, dispatch, getState);

      const dropModel3 = getDropModelData(state.modelsAndTablesList[1]);
      const action3 = actions.addRelationModel(dropModel3);
      await executeDispatch(action3, dispatch, getState);

      const dropModel4 = getDropModelData(state.modelsAndTablesList[2]);
      const action4 = actions.addRelationModel(dropModel4);
      await executeDispatch(action4, dispatch, getState);

      const dropModel5 = getDropModelData(state.modelsAndTablesList[0]);
      const action5 = actions.addRelationModel(dropModel5);
      await executeDispatch(action5, dispatch, getState);

      expect(state.models.some(model => model.name === '模型A')).toBe(true);
      expect(state.models.some(model => model.name === '模型B')).toBe(true);
      expect(state.models.some(model => model.name === '模型C')).toBe(true);
      expect(state.models.some(model => model.name === '模型E')).toBe(true);
      expect(state.models.some(model => model.name === '模型D')).toBe(true);
      expect(state.models.some(model => model.name === '模型F')).toBe(true);
    });

    it('添加模型间关系', async () => {
      const relations = getMockRelations();
      const action = actions.updateRelationship(relations);
      await executeDispatch(action, dispatch, getState);

      expect(state.modelRelations).toEqual(relations);
    });

    it('生成数据集语义和维度用法', async () => {
      const action = actions.initDataset();
      await executeDispatch(action, dispatch, getState);

      const resultModels = getMockStoreModels();
      expect(state.models).toEqual(resultModels);

      const resultUsages = getMockStoreUsages();
      expect(state.dimTableModelRelations).toEqual(resultUsages);
    });

    it('编辑维度表', async () => {
      const newData = {
        name: '事实表A',
        alias: ' 我是一张事实表A ',
        type: 'time',
      };
      const action = actions.setDimTable('模型A', newData);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.models
        .find(m => m.name === '模型A').dimensionTables
        .find(t => t.name === '事实表A');

      expect(currentNewState.name).toBe('事实表A');
      expect(currentNewState.alias).toBe('我是一张事实表A');
      expect(currentNewState.type).toBe('time');
    });

    it('编辑维度列', async () => {
      const newData = {
        name: '维度列A',
        alias: ' 我是维度列A ',
        type: 5,
        dataType: 'date',
        isVisible: true,
        visible: [
          { type: 'user', label: '用来测试删除单条用户可见性', name: '用来测试删除单条用户可见性' },
          { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
          { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
        ],
      };
      const action = actions.setDimColumn('模型A', '事实表A', newData);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.models
        .find(m => m.name === '模型A').dimensionTables
        .find(t => t.name === '事实表A').dimCols
        .find(c => c.name === '维度列A');

      expect(currentNewState.name).toBe('维度列A');
      expect(currentNewState.alias).toBe('我是维度列A');
      expect(currentNewState.type).toBe(5);
      expect(currentNewState.dataType).toBe('date');
      expect(currentNewState.visible).toEqual([
        { type: 'user', label: '用来测试删除单条用户可见性', name: '用来测试删除单条用户可见性' },
        { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
        { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
      ]);
    });

    it('创建层级维度', async () => {
      const newData = {
        name: ' 层级维度A ',
        tablePath: ['模型A', '事实表A'],
        dimCols: ['维度列A', '维度列B'],
      };
      const action = actions.setHierarchy('模型A', '事实表A', null, newData);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.models
        .find(m => m.name === '模型A').dimensionTables
        .find(t => t.name === '事实表A').hierarchys
        .find(c => c.name === '层级维度A');

      expect(currentNewState.key).toBe('模型A-事实表A-h-层级维度A');
      expect(currentNewState.name).toBe('层级维度A');
      expect(currentNewState.tablePath).toEqual(['模型A', '事实表A']);
      expect(currentNewState.dimCols).toEqual(['维度列A', '维度列B']);
      expect(currentNewState.model).toBe('模型A');
      expect(currentNewState.table).toBe('事实表A');
    });

    it('编辑层级维度', async () => {
      const newData = {
        name: ' 层级维度A ',
        tablePath: ['模型A', '事实表A'],
        dimCols: ['维度列A'],
      };
      const action = actions.setHierarchy('模型A', '事实表A', '层级维度A', newData);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.models
        .find(m => m.name === '模型A').dimensionTables
        .find(t => t.name === '事实表A').hierarchys
        .find(c => c.name === '层级维度A');

      expect(currentNewState.key).toBe('模型A-事实表A-h-层级维度A');
      expect(currentNewState.name).toBe('层级维度A');
      expect(currentNewState.tablePath).toEqual(['模型A', '事实表A']);
      expect(currentNewState.dimCols).toEqual(['维度列A']);
      expect(currentNewState.model).toBe('模型A');
      expect(currentNewState.table).toBe('事实表A');
    });

    it('编辑度量', async () => {
      const newData = {
        name: '我是度量A',
        alias: ' 度量A ',
        expression: 'SUM',
        expressionParams: 'KYLIN_SALES.PRICE',
        isVisible: true,
        visible: [
          { type: 'user', label: '用来测试删除单条用户可见性', name: '用来测试删除单条用户可见性' },
          { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
          { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
        ],
      };
      const action = actions.setDimMeasure('模型A', undefined, newData);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.models
        .find(m => m.name === '模型A').measures
        .find(t => t.name === '我是度量A');

      expect(currentNewState.name).toBe('我是度量A');
      expect(currentNewState.alias).toBe('度量A');
      expect(currentNewState.expression).toBe('SUM');
      expect(currentNewState.expressionParams).toBe('KYLIN_SALES.PRICE');
      expect(currentNewState.visible).toEqual([
        { type: 'user', label: '用来测试删除单条用户可见性', name: '用来测试删除单条用户可见性' },
        { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
        { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
      ]);
    });

    it('创建计算度量', async () => {
      const newData = {
        name: ' 计算度量A ',
        format: '####',
        folder: 'Calculated Measure',
        expression: '1',
        isVisible: true,
        visible: [
          { type: 'user', label: '用来测试删除单条用户可见性', name: '用来测试删除单条用户可见性' },
          { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
          { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
        ],
      };
      const action = actions.setCalculatedMeasure(null, newData, false);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.calculateMeasures
        .find(cm => cm.name === '计算度量A');

      expect(currentNewState.name).toBe('计算度量A');
      expect(currentNewState.format).toBe('####');
      expect(currentNewState.folder).toBe('Calculated Measure');
      expect(currentNewState.expression).toBe('1');
      expect(currentNewState.visible).toEqual([
        { type: 'user', label: '用来测试删除单条用户可见性', name: '用来测试删除单条用户可见性' },
        { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
        { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
      ]);
    });

    it('编辑计算度量', async () => {
      const newData = {
        name: ' 计算度量A ',
        format: '####',
        folder: 'Calculated Measure',
        expression: '2',
        isVisible: true,
        visible: [
          { type: 'user', label: '用来测试删除单条用户可见性', name: '用来测试删除单条用户可见性' },
          { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
          { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
        ],
      };
      const action = actions.setCalculatedMeasure('计算度量A', newData, false);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.calculateMeasures
        .find(cm => cm.name === '计算度量A');

      expect(currentNewState.name).toBe('计算度量A');
      expect(currentNewState.format).toBe('####');
      expect(currentNewState.folder).toBe('Calculated Measure');
      expect(currentNewState.expression).toBe('2');
      expect(currentNewState.visible).toEqual([
        { type: 'user', label: '用来测试删除单条用户可见性', name: '用来测试删除单条用户可见性' },
        { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
        { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
      ]);
    });

    it('创建命名集', async () => {
      const newData = {
        name: ' 命名集A ',
        location: 'Named Sets',
        expression: '1',
        isVisible: true,
        visible: [
          { type: 'user', label: '用来测试删除单条用户可见性', name: '用来测试删除单条用户可见性' },
          { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
          { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
        ],
      };
      const action = actions.setNamedSet(null, newData, false);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.namedSets
        .find(namedSet => namedSet.name === '命名集A');

      expect(currentNewState.name).toBe('命名集A');
      expect(currentNewState.location).toBe('Named Sets');
      expect(currentNewState.expression).toBe('1');
      expect(currentNewState.visible).toEqual([
        { type: 'user', label: '用来测试删除单条用户可见性', name: '用来测试删除单条用户可见性' },
        { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
        { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
      ]);
    });

    it('编辑命名集', async () => {
      const newData = {
        name: ' 命名集A ',
        location: 'Named Sets',
        expression: '2',
        isVisible: true,
        visible: [
          { type: 'user', label: '用来测试删除单条用户可见性', name: '用来测试删除单条用户可见性' },
          { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
          { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
        ],
      };
      const action = actions.setNamedSet('命名集A', newData, false);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.namedSets
        .find(namedSet => namedSet.name === '命名集A');

      expect(currentNewState.name).toBe('命名集A');
      expect(currentNewState.location).toBe('Named Sets');
      expect(currentNewState.expression).toBe('2');
      expect(currentNewState.visible).toEqual([
        { type: 'user', label: '用来测试删除单条用户可见性', name: '用来测试删除单条用户可见性' },
        { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
        { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
      ]);
    });

    it('编辑度量组', async () => {
      const newData = {
        name: '模型A',
        alias: '度量组A',
      };
      const action = actions.setMeasureGroup('模型A', newData, false);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.models
        .find(model => model.name === '模型A');

      expect(currentNewState.name).toBe('模型A');
      expect(currentNewState.alias).toBe('度量组A');
    });

    it('删除某用户可见性', async () => {
      const deleteItems = [
        { name: '维度列A', model: '模型A', table: '事实表A', nodeType: 'column' },
        { name: '我是度量A', model: '模型A', nodeType: 'measure' },
        { name: '计算度量A', nodeType: 'calculateMeasure' },
        { name: '命名集A', nodeType: 'namedSet' },
      ];
      const restrict = { type: 'user', name: '用来测试删除单条用户可见性', items: deleteItems };
      const action = actions.deleteRestrict('visible', restrict, deleteItems);
      await executeDispatch(action, dispatch, getState);

      const currentModel = state.models.find(m => m.name === '模型A');
      const currentTable = currentModel.dimensionTables.find(t => t.name === '事实表A');
      const dimCol = currentTable.dimCols.find(c => c.name === '维度列A');
      const measure = currentModel.measures.find(m => m.name === '我是度量A');
      const cMeasure = state.calculateMeasures.find(cm => cm.name === '计算度量A');
      const namedSet = state.namedSets.find(n => n.name === '命名集A');

      expect(dimCol.visible).toEqual([
        { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
        { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
      ]);
      expect(measure.visible).toEqual([
        { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
        { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
      ]);
      expect(cMeasure.visible).toEqual([
        { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
        { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
      ]);
      expect(namedSet.visible).toEqual([
        { type: 'role', label: '用来测试删除单条角色可见性', name: '用来测试删除单条角色可见性' },
        { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
      ]);
    });

    it('删除某用户可见性', async () => {
      const deleteItems = [
        { name: '维度列A', model: '模型A', table: '事实表A', nodeType: 'column' },
        { name: '我是度量A', model: '模型A', nodeType: 'measure' },
        { name: '计算度量A', nodeType: 'calculateMeasure' },
        { name: '命名集A', nodeType: 'namedSet' },
      ];
      const restrict = { type: 'role', name: '用来测试删除单条角色可见性', items: deleteItems };
      const action = actions.deleteRestrict('visible', restrict, deleteItems);
      await executeDispatch(action, dispatch, getState);

      const currentModel = state.models.find(m => m.name === '模型A');
      const currentTable = currentModel.dimensionTables.find(t => t.name === '事实表A');
      const dimCol = currentTable.dimCols.find(c => c.name === '维度列A');
      const measure = currentModel.measures.find(m => m.name === '我是度量A');
      const cMeasure = state.calculateMeasures.find(cm => cm.name === '计算度量A');
      const namedSet = state.namedSets.find(n => n.name === '命名集A');

      expect(dimCol.visible).toEqual([
        { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
      ]);
      expect(measure.visible).toEqual([
        { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
      ]);
      expect(cMeasure.visible).toEqual([
        { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
      ]);
      expect(namedSet.visible).toEqual([
        { type: 'user', label: '用来测试可见性开关', name: '用来测试可见性开关' },
      ]);
    });

    it('关闭维度列可见性', async () => {
      const newData = { name: '维度列A', alias: '维度列A', isVisible: false };
      const action = actions.setDimColumn('模型A', '事实表A', newData);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.models
        .find(m => m.name === '模型A').dimensionTables
        .find(t => t.name === '事实表A').dimCols
        .find(c => c.name === '维度列A');

      expect(currentNewState.isVisible).toBeFalsy();
      expect(currentNewState.visible).toEqual([]);
    });

    it('关闭度量可见性', async () => {
      const newData = { name: '我是度量A', alias: '度量A', isVisible: false };
      const action = actions.setDimMeasure('模型A', undefined, newData);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.models
        .find(m => m.name === '模型A').measures
        .find(t => t.name === '我是度量A');

      expect(currentNewState.isVisible).toBeFalsy();
      expect(currentNewState.visible).toEqual([]);
    });

    it('关闭计算度量可见性', async () => {
      const newData = { name: '计算度量A', isVisible: false };
      const action = actions.setCalculatedMeasure(null, newData, false);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.calculateMeasures
        .find(cm => cm.name === '计算度量A');

      expect(currentNewState.isVisible).toBeFalsy();
      expect(currentNewState.visible).toEqual([]);
    });

    it('关闭命名集可见性', async () => {
      const newData = { name: '命名集A', isVisible: false };
      const action = actions.setNamedSet(null, newData, false);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.namedSets
        .find(namedSet => namedSet.name === '命名集A');

      expect(currentNewState.isVisible).toBeFalsy();
      expect(currentNewState.visible).toEqual([]);
    });

    it('删除层级维度', async () => {
      const newData = {
        name: '层级维度A',
        tablePath: ['模型A', '事实表A'],
        dimCols: ['维度列A', '维度列B'],
      };
      const action = actions.deleteHierarchy('模型A', '事实表A', newData);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.models
        .find(m => m.name === '模型A').dimensionTables
        .find(t => t.name === '事实表A').hierarchys
        .find(c => c.name === '层级维度A');

      expect(currentNewState).toBeUndefined();
    });

    it('删除计算度量', async () => {
      const newData = {
        name: '计算度量A',
        format: '####',
        folder: 'Calculated Measure',
        expression: '1',
        isVisible: false,
        visible: [],
      };
      const action = actions.deleteCalculatedMeasure(newData);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.calculateMeasures
        .find(cm => cm.name === '计算度量A');

      expect(currentNewState).toBeUndefined();
    });

    it('删除命名集', async () => {
      const newData = {
        name: '命名集A',
        location: 'Named Sets',
        expression: '1',
        isVisible: false,
        visible: [],
      };
      const action = actions.deleteNamedSet(newData);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.namedSets
        .find(namedSet => namedSet.name === '命名集A');

      expect(currentNewState).toBeUndefined();
    });

    it('设置维度用法', async () => {
      const newData = {
        tableName: '维度表A',
        relationType: 2,
        relationFactKey: '事实表A.维度列A',
        relationBridgeTableName: '维度表B',
        bridgeTableError: null,
        factKeyError: null,
      };
      const action = actions.setDimensionUsage('模型A', newData);
      await executeDispatch(action, dispatch, getState);

      const currentNewState = state.dimTableModelRelations
        .find(usages => usages.modelName === '模型A').tableRelations
        .find(usage => usage.tableName === '维度表A');

      expect(currentNewState.relationType).toBe(2);
      expect(currentNewState.relationFactKey).toBe('事实表A.维度列A');
      expect(currentNewState.relationBridgeTableName).toBe('维度表B');
      expect(currentNewState.bridgeTableError).toBe(null);
      expect(currentNewState.factKeyError).toBe(null);
    });

    it('添加模型A进入画布', async () => {
      const newData = { x: 0, y: 0 };
      const action = actions.setCanvasPosition('模型A', newData);
      await executeDispatch(action, dispatch, getState);
      const canvas = state.canvas.models.filter(c => c.name === '模型A');

      expect(canvas).toHaveLength(1);
      expect(canvas[0].x).toBe(0);
      expect(canvas[0].y).toBe(0);
      expect(canvas[0].top).toEqual([]);
      expect(canvas[0].right).toEqual([]);
      expect(canvas[0].bottom).toEqual([]);
      expect(canvas[0].left).toEqual([]);
    });

    it('添加模型B进入画布', async () => {
      const newData = { x: 0, y: 0 };
      const action = actions.setCanvasPosition('模型B', newData);
      await executeDispatch(action, dispatch, getState);
      const canvas = state.canvas.models.filter(c => c.name === '模型B');

      expect(canvas).toHaveLength(1);
      expect(canvas[0].x).toBe(0);
      expect(canvas[0].y).toBe(0);
      expect(canvas[0].top).toEqual([]);
      expect(canvas[0].right).toEqual([]);
      expect(canvas[0].bottom).toEqual([]);
      expect(canvas[0].left).toEqual([]);
    });

    it('改变模型A画布位置', async () => {
      const newData = { x: 100, y: 100 };
      const action = actions.setCanvasPosition('模型A', newData);
      await executeDispatch(action, dispatch, getState);
      const canvas = state.canvas.models.filter(c => c.name === '模型A');

      expect(canvas).toHaveLength(1);
      expect(canvas[0].x).toBe(100);
      expect(canvas[0].y).toBe(100);
    });

    it('画布连接模型', async () => {
      const newData = { right: [{ direction: 'left', name: '模型B' }] };
      const action = actions.setCanvasConnection('模型A', newData);
      await executeDispatch(action, dispatch, getState);
      const canvas = state.canvas.models.filter(c => c.name === '模型A')[0];
      const connection = canvas.right.filter(c => c.name === '模型B');

      expect(connection).toHaveLength(1);
      expect(connection[0].direction).toBe('left');
    });

    it('删除画布上的模型', async () => {
      const action = actions.deleteCanvasModel('模型B');
      await executeDispatch(action, dispatch, getState);
      const canvasA = state.canvas.models.filter(c => c.name === '模型A');
      const canvasB = state.canvas.models.filter(c => c.name === '模型B');
      const connection = canvasA[0].right.filter(c => c.name === '模型B');

      expect(canvasA).toHaveLength(1);
      expect(canvasB).toHaveLength(0);
      expect(connection).toHaveLength(0);
    });

    it('退出界面，清空数据集工作区', async () => {
      const action = actions.clearAll();
      await executeDispatch(action, dispatch, getState);

      expect(state).toEqual({
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
      });
    });

    it('从localStorage恢复数据集', async () => {
      const action = actions.recoveryStore(getMockStoreDataset());
      await executeDispatch(action, dispatch, getState);

      expect(state).toEqual(getMockStoreDataset());
    });

    it('在数据集中删除模型', async () => {
      const action = actions.updateModels(state.models.filter(m => m.name !== '模型E'));
      await executeDispatch(action, dispatch, getState);

      expect(state.models).toHaveLength(5);
      expect(state.models.some(m => m.name === '模型E')).toBeFalsy();
    });
  });

  describe('单模型数据集', () => {
    let state = datasetReducer();

    // dispatch执行器
    const dispatch = curAction => {
      state = datasetReducer(state, curAction);
    };

    const getState = () => {
      const globalState = getDefaultStore();
      globalState.system.currentProject = { name: '项目名称', permission: 'GLOBAL_ADMIN' };
      globalState.workspace.dataset = state;
      return globalState;
    };

    beforeAll(async () => {
      const basicInfo = {
        project: '项目名称',
        datasetName: randomString(10),
        type: 'MDX',
      };
      let action = actions.setDatasetBasicInfo(basicInfo);
      await executeDispatch(action, dispatch);

      // 创建获取数据集模型
      action = actions.getProjectModels(state);
      await executeDispatch(action, dispatch, getState);

      // 加入一个模型
      const dropModel = getDropModelData(state.modelsAndTablesList[0]);
      action = actions.addRelationModel(dropModel);
      await executeDispatch(action, dispatch, getState);
    });

    it('添加单模型关系', async () => {
      const relations = getMockSingleRelation();
      const action = actions.updateRelationship(relations);
      await executeDispatch(action, dispatch, getState);

      expect(state.modelRelations).toEqual(relations);
    });

    it('生成数据集语义和维度用法', async () => {
      const action = actions.initDataset();
      await executeDispatch(action, dispatch, getState);

      const resultModels = getMockStoreSingleModel();
      expect(state.models).toEqual(resultModels);

      const resultUsages = getMockStoreSingleUsages();
      expect(state.dimTableModelRelations).toEqual(resultUsages);
    });
  });
});
