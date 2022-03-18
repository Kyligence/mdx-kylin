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
export const formTypes = {
  SAVE_AS: 'SAVE_AS',
  GIVE_UP_EDIT: 'GIVE_UP_EDIT',
};

export function getDefaultState() {
  return {
    isShow: false,
    isSubmiting: false,
    form: {
      name: '',
      formType: formTypes.SAVE_AS,
    },
    rules: {},
    entityId: '',
    handleSave: () => {},
    callback: () => {},
  };
}
