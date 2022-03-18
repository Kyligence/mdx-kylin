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
