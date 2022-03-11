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
