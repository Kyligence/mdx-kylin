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
