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
import { strings, configs } from '../../constants';
import { storeHelper } from '../../utils';

const { nodeTypes } = configs;

export function getDefaultState() {
  return {
    isShow: false,
    originValue: '',
    category: '',
    title: '',
    filter: {
      indicatorName: '',
    },
    form: {
      name: '',
      template: '',
      params: [],
      data: [],
    },
    callback: () => {},
  };
}

function setTreeLeaf([folder, ...nextFolder], parentNodes = [], leaf) {
  const currentFolder = parentNodes.find(node => node.name === folder);

  if (!currentFolder && nextFolder.length) {
    // 当指标库中没有folder，且后面还有子folder时
    // 在父folder中，新建空folder
    const newNode = { name: folder, nodeType: nodeTypes.SUB_FOLDER, children: [] };
    parentNodes.push(newNode);
    // 继续递归查找子folder
    setTreeLeaf(nextFolder, newNode.children, leaf);
  } else if (!currentFolder && !nextFolder.length) {
    // 当指标库中没有folder，且没有子folder时
    // 在父folder中，新建folder，且插入叶子节点，结束
    const newNode = { name: folder, nodeType: nodeTypes.SUB_FOLDER, children: [leaf] };
    parentNodes.push(newNode);
  } else if (currentFolder && nextFolder.length) {
    // 当指标库中有folder，且后面还有子folder时
    // 继续递归查找子folder
    setTreeLeaf(nextFolder, currentFolder.children, leaf);
  } else if (currentFolder && !nextFolder.length) {
    // 当指标库中有folder，且没有子folder时
    // 直接插入叶子节点，结束
    currentFolder.children.push(leaf);
  }
}

export function translateIndicator(indicatorWithTranslate, locale) {
  const { translation, ...indicator } = indicatorWithTranslate;

  let translatedIndicator = indicator;

  if (translation && translation[locale]) {
    for (const [transKey, transValue] of Object.entries(translation[locale])) {
      translatedIndicator = storeHelper.set(translatedIndicator, transKey, transValue);
    }
  }
  return { ...translatedIndicator, nodeType: nodeTypes.INDICATOR };
}

function filterIndicator(indicator, filterName) {
  return indicator.name.toLowerCase().includes(filterName.toLowerCase());
}

function getColumnKey(model, table, column) {
  return `${model}-${table}-${column}`;
}

/* eslint-disable max-len */
export const getIndicatorTree = createSelector(
  state => state.intl,
  state => state.locale,
  state => state.indicators,
  state => state.filterName,
  (intl, locale, indicators, filterName) => {
    // 翻译所有指标
    const translatedIndicators = indicators.map(item => translateIndicator(item, locale));
    // 获取默认文件夹、自定义文件夹
    const defaultFolder = { name: intl.formatMessage(strings.DEFAULT_TEMPLATE), nodeType: nodeTypes.DEFAULT_FOLDER, children: [] };
    const customFolder = { name: intl.formatMessage(strings.CUSTOM_TEMPLATE), nodeType: nodeTypes.CUSTOM_FOLDER, children: [] };
    // 筛选出默认指标、自定义指标，并且过滤指标名字
    const defaultIndicators = translatedIndicators.filter(item => item.buildin && filterIndicator(item, filterName));
    const customIndicators = translatedIndicators.filter(item => !item.buildin && filterIndicator(item, filterName));

    // 遍历筛选默认指标进文件夹
    for (const indicator of defaultIndicators) {
      setTreeLeaf(indicator.folder.split('\\'), defaultFolder.children, indicator);
    }
    // 遍历筛选自定义指标进文件夹
    for (const indicator of customIndicators) {
      setTreeLeaf(indicator.folder.split('\\'), customFolder.children, indicator);
    }
    return [defaultFolder, customFolder];
  },
);

export const getColumnOptions = createSelector(
  state => state.dataset,
  dataset => {
    const options = [];
    for (const model of dataset.models) {
      for (const table of model.dimensionTables) {
        for (const column of table.dimCols) {
          options.push({
            label: `[${table.alias}].[${column.alias}]`,
            value: `[${table.alias}].[${column.alias}]`,
            name: column.name,
            alias: column.alias,
            table: table.name,
            model: model.name,
          });
        }
      }
    }
    return options;
  },
);

export const getColumnAliasMap = createSelector(
  getColumnOptions,
  columns => {
    const map = {};
    for (const column of columns) {
      const columnKey = getColumnKey(column.model, column.table, column.name);
      map[columnKey] = column.alias;
    }
    return map;
  },
);

export const getHierarchyOptions = createSelector(
  state => state.dataset,
  dataset => {
    const options = [];
    for (const model of dataset.models) {
      for (const table of model.dimensionTables) {
        for (const hierarchy of table.hierarchys) {
          options.push({
            label: `[${table.alias}].[${hierarchy.name}-Hierarchy]`,
            value: `[${table.alias}].[${hierarchy.name}-Hierarchy]`,
          });
        }
      }
    }
    return options;
  },
);

export const getHierarchyLevelOptions = createSelector(
  state => state.dataset,
  getColumnAliasMap,
  (dataset, aliasMap) => {
    const options = [];
    for (const model of dataset.models) {
      for (const table of model.dimensionTables) {
        for (const hierarchy of table.hierarchys) {
          for (const hierarchyLevel of hierarchy.dimCols) {
            const columnKey = getColumnKey(model.name, table.name, hierarchyLevel);
            options.push({
              label: `[${table.alias}].[${hierarchy.name}-Hierarchy].[${aliasMap[columnKey]}]`,
              value: `[${table.alias}].[${hierarchy.name}-Hierarchy].[${aliasMap[columnKey]}]`,
            });
          }
        }
      }
    }
    return options;
  },
);

export const getNamedSetOptions = createSelector(
  state => state.dataset,
  dataset => {
    const options = [];
    for (const namedSet of dataset.namedSets) {
      options.push({
        label: `{[${namedSet.name}]}`,
        value: `{[${namedSet.name}]}`,
      });
    }
    return options;
  },
);

export const getMeasureOptions = createSelector(
  state => state.dataset,
  dataset => {
    const options = [];
    for (const model of dataset.models) {
      for (const measure of model.measures) {
        options.push({
          label: `[Measures].[${measure.alias}]`,
          value: `[Measures].[${measure.alias}]`,
        });
      }
    }
    return options;
  },
);

export const getCMeasureOptions = createSelector(
  state => state.dataset,
  dataset => {
    const options = [];
    for (const cMeasure of dataset.calculateMeasures) {
      options.push({
        label: `[Measures].[${cMeasure.name}]`,
        value: `[Measures].[${cMeasure.name}]`,
      });
    }
    return options;
  },
);
