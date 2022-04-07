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
import { createSelector } from 'reselect';

import { configs, strings } from '../../constants';

const { nodeTypes } = configs;

export const getPopoverFolderTree = createSelector(
  state => state.intl,
  state => state.name,
  state => state.nodeType,
  (intl, name, nodeType) => [
    {
      label: intl.formatMessage(strings.FOLDER_EXAMPLE_1),
      nodeType: nodeTypes.SUB_FOLDER,
      children: [
        {
          label: intl.formatMessage(strings.FOLDER_EXAMPLE_2),
          nodeType: nodeTypes.SUB_FOLDER,
          children: [
            {
              label: name,
              nodeType,
            },
          ],
        },
      ],
    },
  ],
);

export const getSuggestionsFolderTree = createSelector(
  state => state.value,
  state => state.name,
  state => state.nodeType,
  (value, name, nodeType) => {
    const tree = [];
    const folders = value.replace(/^\\|\\$/g, '').split('\\').filter(v => !!v);

    let currentChildren = tree;

    for (const folder of folders) {
      const currentNode = { label: folder, nodeType: nodeTypes.SUB_FOLDER, children: [] };
      currentChildren.push(currentNode);

      currentChildren = currentNode.children;
    }

    if (tree.length) {
      currentChildren.push({ label: name, nodeType });
    }

    return tree;
  },
);
