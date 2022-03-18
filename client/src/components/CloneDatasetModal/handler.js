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
import { business, strings, configs } from '../../constants';

export function getDefaultState() {
  return {
    isShow: false,
    callback: () => {},
    datasetId: null,
    form: {
      datasetName: '',
    },
  };
}

export const validator = {
  datasetName({ intl, boundDatasetActions, dataset, form: { datasetName } }) {
    return async (rule, value, callback) => {
      if (!value) {
        callback(new Error(intl.formatMessage(strings.SHOULD_HAVE_DATASET_NAME)));
      } else if (!business.nameRegExpInDatasetName.test(value)) {
        callback(new Error(intl.formatMessage(strings.INVALID_NAME_WITH_CHINESE)));
      } else if (value.length > configs.datasetMaxLength.datasetName) {
        const maxLength = configs.datasetMaxLength.datasetName;
        callback(new Error(intl.formatMessage(strings.DATASET_NAME_TOO_LONG, { maxLength })));
      } else if (await boundDatasetActions.checkDatasetName({ ...dataset, datasetName })) {
        callback(new Error(intl.formatMessage(strings.DATASET_NAME_DUPLICATE)));
      }
      callback();
    };
  },
};
