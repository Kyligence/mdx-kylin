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
import { strings } from '../../constants';

const errorMap = {
  MANY_TO_MANY_FACT_KEY_DELETED: strings.DIM_COL_DELETED,
  BRIDGE_TABLE_DELETED: strings.BRIDGE_TABLE_DELETED,
};

export const validator = {
  relationFactKey({ intl, form }, factKeyOptions = []) {
    return (rule, value, callback) => {
      const { factKeyError } = form;

      if (!factKeyOptions.length) {
        const message = intl.formatMessage(strings.RELATIONSHIP_ERROR_FACT_KEY);
        callback(new Error(message));
      } else if (!value) {
        const message = intl.formatMessage(strings.PLEASE_SELECT_FACT_KEY);
        callback(new Error(message));
      } else if (factKeyError) {
        const [modelName, tableName, columnName] = factKeyError.name.split('.');
        if (`${tableName}.${columnName}` === value) {
          const params = { modelName, tableName, columnName };
          const message = intl.formatMessage(errorMap[factKeyError.type], params);
          callback(new Error(message));
        } else {
          callback();
        }
      } else {
        callback();
      }
    };
  },
  relationBridgeTableName({ intl, form }, dimensionTableOptions = []) {
    return (rule, value, callback) => {
      const { bridgeTableError } = form;

      if (!dimensionTableOptions.length) {
        const message = intl.formatMessage(strings.RELATIONSHIP_ERROR_TABLE);
        callback(new Error(message));
      } else if (!value) {
        const message = intl.formatMessage(strings.PLEASE_SELECT_INTERMEDIATE_TABLE);
        callback(new Error(message));
      } else if (bridgeTableError) {
        const [modelName, tableName] = bridgeTableError.name.split('.');
        if (tableName === value) {
          const params = { modelName, tableName };
          const message = intl.formatMessage(errorMap[bridgeTableError.type], params);
          callback(new Error(message));
        } else {
          callback();
        }
      } else {
        callback();
      }
    };
  },
};

export function getDefaultState() {
  return {
    isShow: false,
    callback: () => {},
    model: '',
    form: {
      tableName: '',
      relationType: 0,
      relationFactKey: null,
      relationBridgeTableName: null,
      bridgeTableError: null,
      factKeyError: null,
    },
  };
}
