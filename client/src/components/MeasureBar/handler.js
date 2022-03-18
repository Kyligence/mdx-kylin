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
  return configs.nodeIconMaps[node.nodeType] || null;
}

export function getFilteredNodeTypes({ nodeType }) {
  switch (nodeType) {
    case 'measure': return ['measure'];
    case 'calculateMeasure': return ['calculateMeasure'];
    default: return [];
  }
}

export function checkValidNodeType(node) {
  return ['measure', 'calculateMeasure', 'measureGroup']
    .includes(node && node.nodeType);
}

export function findDefaultMeasureGroup(measureTree, nodeTypes) {
  for (const measureGroup of measureTree) {
    if (nodeTypes.includes(measureGroup.nodeType)) {
      return measureGroup;
    }
  }
  return null;
}

export function checkIsCreatedNode(oldNode, newNode) {
  return oldNode && newNode && oldNode !== newNode && !oldNode.key && newNode.key;
}

export function checkIsEditedNode(oldNode, newNode) {
  return oldNode && newNode && oldNode !== newNode && oldNode.key === newNode.key;
}

export function hasNonEmptyBehaviorError(cMeasure) {
  return cMeasure.nonEmptyBehaviorErrors &&
    cMeasure.nonEmptyBehaviorErrors.some(nonEmptyBehaviorError => (
      cMeasure.nonEmptyBehavior.some(nonEmptyBehavior => `${nonEmptyBehavior.model}.${nonEmptyBehavior.name}` === nonEmptyBehaviorError.obj)
    ));
}
