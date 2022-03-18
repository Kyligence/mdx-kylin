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

function sortByName(array = []) {
  return array.sort((itemA, itemB) => {
    const nameA = typeof itemA.name === 'string' ? itemA.name : itemA.name.defaultMessage;
    const nameB = typeof itemB.name === 'string' ? itemB.name : itemB.name.defaultMessage;

    const lowerNameA = nameA.toLowerCase();
    const lowerNameB = nameB.toLowerCase();

    return lowerNameA.localeCompare(lowerNameB);
  });
}

export const translationConfigTypes = [
  'zh-CN', 'en-UK', 'en-US',
];

export function getNodeIcon(node) {
  const nodeType = node.isPublicTable ? 'commonTable' : node.nodeType;
  return configs.nodeIconMaps[nodeType] || null;
}

export function filterNodes(nodes = [], filter) {
  return nodes.filter(node => node.name.toLowerCase().includes(filter.name.toLowerCase()));
}

export const getTranslationTree = createSelector(
  state => state.dataset,
  state => state.filter,
  (dataset, filter) => {
    const modelNodes = dataset.models.map(model => {
      // 维度表节点
      let tableNodes = [];
      if (['table', 'column', 'hierarchy', 'namedSet'].includes(filter.nodeType) || !filter.name) {
        tableNodes = model.dimensionTables.map(table => {
          // 维度列节点
          let columnNodes = [];
          if (['table', 'column'].includes(filter.nodeType) || !filter.name) {
            columnNodes = table.dimCols.map(column => ({
              key: column.key,
              name: column.alias,
              nodeType: column.nodeType,
              translation: column.translation,
            }));
            if (filter.nodeType === 'column') {
              columnNodes = filterNodes(columnNodes, filter);
            }
          }

          // 层级维度节点
          let hierarchyNodes = [];
          if (['table', 'hierarchy'].includes(filter.nodeType) || !filter.name) {
            hierarchyNodes = table.hierarchys.map(hierarchy => ({
              key: hierarchy.key,
              name: hierarchy.name,
              nodeType: hierarchy.nodeType,
              translation: hierarchy.translation,
            }));
            if (filter.nodeType === 'hierarchy') {
              hierarchyNodes = filterNodes(hierarchyNodes, filter);
            }
          }

          // // 表级命名集节点 暂时不启用
          // let namedSetNodes = [];
          // if (['table', 'namedSet'].includes(filter.nodeType) || !filter.name) {
          //   namedSetNodes = dataset.namedSets
          //     .filter(namedSet => namedSet.location === `${model.name}.${table.name}`)
          //     .map(namedSet => ({
          //       key: namedSet.key,
          //       name: namedSet.name,
          //       nodeType: namedSet.nodeType,
          //       translation: namedSet.translation,
          //     }));
          //   if (filter.nodeType === 'namedSet') {
          //     namedSetNodes = filterNodes(namedSetNodes, filter);
          //   }
          // }
          // const namedSetRoot = {
          //   key: 'namedSetRoot',
          //   name: strings.NAMEDSET,
          //   nodeType: 'namedSetRoot',
          //   children: sortByName(namedSetNodes),
          // };

          return {
            key: table.key,
            name: table.alias,
            nodeType: table.nodeType,
            translation: table.translation,
            children: [
              ...sortByName(columnNodes),
              ...sortByName(hierarchyNodes),
              // 暂时不启用
              // ...(namedSetRoot.children.length ? [namedSetRoot] : []),
            ],
          };
        });
        if (filter.nodeType === 'table') {
          tableNodes = filterNodes(tableNodes, filter);
        }
      }

      // 度量节点
      let measureNodes = [];
      if (['measure'].includes(filter.nodeType) || !filter.name) {
        measureNodes = model.measures.map(measure => ({
          key: measure.key,
          name: measure.alias,
          nodeType: measure.nodeType,
          translation: measure.translation,
        }));
        if (filter.nodeType === 'measure') {
          measureNodes = filterNodes(measureNodes, filter);
        }
      }
      const measureRootNode = {
        key: `${model.key}-measureRoot`,
        name: strings.MEASURE,
        nodeType: 'measureRoot',
        children: sortByName(measureNodes),
      };

      return {
        key: model.key,
        name: model.name,
        nodeType: model.nodeType,
        // 度量组翻译 暂时不启用
        // translation: model.translation,
        children: [
          ...sortByName(tableNodes.filter(table => table.children.length)),
          ...(measureRootNode.children.length ? [measureRootNode] : []),
        ],
      };
    });

    // // 命名集节点 暂时不启用
    // let namedSetNodes = [];
    // if (['namedSet'].includes(filter.nodeType) || !filter.name) {
    //   namedSetNodes = dataset.namedSets
    //     .filter(namedSet => namedSet.location === 'Named Set')
    //     .map(namedSet => ({
    //       key: namedSet.key,
    //       name: namedSet.name,
    //       nodeType: namedSet.nodeType,
    //       translation: namedSet.translation,
    //     }));
    //   namedSetNodes = filterNodes(namedSetNodes, filter);
    // }
    // const namedSetRoot = {
    //   key: 'namedSetRoot',
    //   name: strings.NAMEDSET,
    //   nodeType: 'namedSetRoot',
    //   children: sortByName(namedSetNodes),
    // };

    // 计算度量节点
    let cMeasureNodes = [];
    if (['calculateMeasure'].includes(filter.nodeType) || !filter.name) {
      cMeasureNodes = dataset.calculateMeasures.map(cMeasure => ({
        key: cMeasure.key,
        name: cMeasure.name,
        nodeType: cMeasure.nodeType,
        translation: cMeasure.translation,
      }));
      cMeasureNodes = filterNodes(cMeasureNodes, filter);
    }
    const cMeasureRoot = {
      key: 'calculateMeasureRoot',
      name: strings.CALCULATED_MEASURE,
      nodeType: 'calculateMeasureRoot',
      children: sortByName(cMeasureNodes),
    };

    return [
      ...sortByName(modelNodes.filter(model => model.children.length)),
      ...(cMeasureRoot.children.length ? [cMeasureRoot] : []),
      // 暂时不启用
      // ...(namedSetRoot.children.length ? [namedSetRoot] : []),
    ];
  },
);

export function calcColSpan(translationTypes, isCreateMode) {
  return function getColSpan(size) {
    const colSpanSize = `${size}-${translationTypes.length}`;

    let span = 0;
    if (isCreateMode) {
      switch (colSpanSize) {
        case 'xxl-0': span = 13; break;
        case 'xl-0': span = 15; break;
        case 'lg-0': span = 16; break;
        case 'md-0': span = 20; break;
        case 'sm-0': span = 22; break;

        case 'xxl-1': span = 18; break;
        case 'xl-1': span = 19; break;
        case 'lg-1':
        case 'md-1':
        case 'sm-1':
        default: span = 24; break;
      }
    } else {
      switch (colSpanSize) {
        case 'xxl-0': span = 9; break;
        case 'xl-0': span = 10; break;
        case 'lg-0': span = 11; break;
        case 'md-0': span = 14; break;
        case 'sm-0': span = 17; break;
        case 'xxl-1': span = 13; break;
        case 'xl-1': span = 14; break;
        case 'lg-1': span = 20; break;
        case 'md-1':
        case 'xxl-2': span = 21; break;
        case 'xl-2': span = 23; break;
        default: span = 24; break;
      }
    }
    return { span };
  };
}
