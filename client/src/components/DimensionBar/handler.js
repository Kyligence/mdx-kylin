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
import { configs } from '../../constants';

export function getNodeIcon(node) {
  const nodeType = node.isPublicTable ? 'commonTable' : node.nodeType;
  return configs.nodeIconMaps[nodeType] || null;
}

export function getFilteredNodeTypes({ nodeType }) {
  switch (nodeType) {
    case 'hierarchy': return ['hierarchy'];
    case 'column': return ['column'];
    case 'namedSet': return ['namedSet'];
    case 'model':
    case 'table':
    default:
      return ['model', 'table', 'hierarchy', 'column', 'namedSetRoot', 'namedSet'];
  }
}

export function checkValidNodeType(node) {
  return ['hierarchy', 'column', 'namedSetRoot', 'namedSet', 'table']
    .includes(node && node.nodeType);
}

export function findDefaultTable(dimensionTree) {
  for (const model of dimensionTree) {
    for (const table of model.children) {
      if (model.nodeType === 'model' && table.nodeType === 'table') {
        return table;
      }
    }
  }
  return null;
}

export function findDefaultNamedSetRoot(dimensionTree) {
  for (const model of dimensionTree) {
    if (model.nodeType === 'namedSetRoot') {
      return model;
    }
    for (const table of model.children) {
      for (const dimension of table.children) {
        if (dimension.nodeType === 'namedSetRoot') {
          return dimension;
        }
      }
    }
  }
  return null;
}

export function getErrorHierarchyWeights(errors = []) {
  return errors.reduce((columns, error) => {
    const [, , , weightName] = error.obj.split('.');

    if (error.type === 'HIERARCHY_WEIGHT_COL_DELETED') {
      columns.push(weightName);
    } else if (error.type === 'HIERARCHY_DIM_WEIGHT_COL_DELETED') {
      columns.push(weightName);
    }
    return columns;
  }, []);
}

export function getErrorHierarchyLevels(errors = []) {
  return errors.reduce((columns, error) => {
    const [, , levelName] = error.obj.split('.');

    if (error.type === 'HIERARCHY_DIM_COL_DELETED') {
      columns.push(levelName);
    } else if (error.type === 'HIERARCHY_DIM_WEIGHT_COL_DELETED') {
      columns.push(levelName);
    }
    return columns;
  }, []);
}

export function hasNameColumnError(column) {
  return column.nameColumnError &&
    column.nameColumnError.obj.split('.')[2] === column.nameColumn;
}

export function hasHierarchyError(hierarchy) {
  if (hierarchy.nodeType === 'hierarchy') {
    const errorLevels = getErrorHierarchyLevels(hierarchy.errors);
    const errorWeights = getErrorHierarchyWeights(hierarchy.errors);
    return (
      hierarchy.dimCols.length &&
      hierarchy.dimCols.some(dimCol => errorLevels.includes(dimCol))
    ) || (
      hierarchy.weightCols.length &&
      hierarchy.weightCols.some(weightCol => errorWeights.includes(weightCol))
    );
  }
  return false;
}

export function hasValueColumnError(column) {
  return column.valueColumnError &&
    column.valueColumnError.obj.split('.')[2] === column.valueColumn;
}

export function hasPropertiesError(column) {
  return column.propertyErrors &&
    column.propertyErrors.some(propertyError => (
      column.properties.includes(propertyError.obj.split('.')[2])
    ));
}

export function hasDefaultMemberError(column) {
  return !!column.defaultMemberError;
}

export function checkIsCreatedNode(oldNode, newNode) {
  return oldNode && newNode && oldNode !== newNode && !oldNode.key && newNode.key;
}

export function checkIsEditedNode(oldNode, newNode) {
  return oldNode && newNode && oldNode !== newNode && oldNode.key === newNode.key;
}
