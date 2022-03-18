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
    boundModalActions: {
      setModalData: sinon.spy(),
      showModal: sinon.spy(sinon.fake.returns({
        isSubmit: true,
        dimCols: ['维度列B', '维度C'],
        weightCols: [null, '权重列A'],
      })),
    },
    boundDatasetActions: {
      setHierarchy: sinon.spy(sinon.fake.returns({ name: '层级维度A' })),
      deleteHierarchy: sinon.spy(),
    },
    boundGlobalActions: {
      toggleFlag: sinon.spy(),
    },
    data: {
      name: '',
      desc: '',
      tablePath: [],
      dimCols: [],
      weightCols: [],
      model: '',
      table: '',
      translation: {},
      errors: [],
    },
    onDelete: sinon.spy(),
    onCancelCreate: sinon.spy(),
    onSubmit: sinon.spy(),
  };
}
