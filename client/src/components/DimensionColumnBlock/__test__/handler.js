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
import sinon from 'sinon';

export function getInitPorps() {
  return {
    boundDatasetActions: {
      setDimColumn: sinon.spy(sinon.fake.returns({ name: '维度列A' })),
      validateDefaultMemberForMDX: sinon.spy(sinon.fake.returns([''])),
    },
    boundGlobalActions: {
      toggleFlag: sinon.spy(),
    },
    data: {
      name: '维度列A',
      alias: '维度列A',
      desc: '',
      type: 0,
      dataType: 'date',
      isVisible: true,
      invisible: [],
      visible: [],
      nameColumn: null,
      valueColumn: null,
      properties: [],
      subfolder: '',
      model: '模型A',
      table: '维度表A',
      defaultMember: '',
    },
    onSubmit: sinon.spy(),
  };
}
