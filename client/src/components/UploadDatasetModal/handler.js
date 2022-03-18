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
import { dataHelper } from '../../utils';

export function getDefaultState() {
  return {
    isShow: false,
    callback: () => {},
    form: {
      fileName: '',
      file: null,
    },
  };
}

export const validator = {
  file({ intl }) {
    return async (rule, file, callback) => {
      const { formatMessage } = intl;

      if (!file) {
        callback(new Error(formatMessage(strings.FILE_IS_EMPTY)));
      } else if (file.type && ['application/zip', 'application/json', 'application/x-zip-compressed'].includes(file.type)) {
        callback();
      } else if (!file.type && [/\.zip$/i, /\.json$/i].some(regexp => regexp.test(file.name))) {
        callback();
      } else {
        callback(new Error(formatMessage(
          strings.SUPPORT_FILE,
          { type: dataHelper.getHumanizeJoinString(intl, ['.zip', '.json']) },
        )));
      }
    };
  },
};
