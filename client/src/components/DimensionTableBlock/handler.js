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
import { business, strings, configs } from '../../constants';

export const validator = {
  alias({ intl }, isTableAliasDuplicate) {
    return (rule, inputValue = '', callback) => {
      const value = inputValue.trim();
      // 空值校验报错
      if (!value) {
        const message = intl.formatMessage(strings.PLEASE_ENTER_TABLE_ALIAS);
        callback(new Error(message));
        // 别名占用关键字报错
      } else if (configs.blackListKeyWords.includes(value.toLowerCase())) {
        const name = value;
        const nodeType = intl.formatMessage(strings.DIMENSION_TABLE).toLowerCase();
        const message = intl.formatMessage(strings.INVALID_NAME_IS_KEY_WORD, { name, nodeType });
        callback(new Error(message));
        // 别名重名报错
      } else if (isTableAliasDuplicate) {
        const params = { tableAlias: value };
        const message = intl.formatMessage(strings.TABLE_NAME_DUPLICATE, params);
        callback(new Error(message));
        // 名称不符合规则报错
      } else if (!business.nameRegExpInDataset.defaultRule.test(value)) {
        const message = intl.formatMessage(strings.INVALID_ALIAS_IN_DATASET);
        callback(new Error(message));
        // 超过最大长度报错
      } else if (value.length > configs.datasetMaxLength.tableAlias) {
        const maxLength = configs.datasetMaxLength.tableAlias;
        const params = { maxLength };
        const message = intl.formatMessage(strings.DIM_TABLE_NAME_TOO_LONG, params);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
  type({ intl }) {
    return (rule, value = '', callback) => {
      // 空值校验报错
      if (!value.length) {
        const message = intl.formatMessage(strings.PLEASE_SELECT_TYPE);
        callback(new Error(message));
      } else {
        callback();
      }
    };
  },
};

export const getChildHierarchies = createSelector(
  state => state.children || [],
  children => children.filter(child => child.nodeType === configs.nodeTypes.HIERARCHY),
);

export const getHasWeightHierarchy = createSelector(
  getChildHierarchies,
  hierarchies => hierarchies.some(hierarchy => hierarchy.weightCols.filter(w => w).length),
);
