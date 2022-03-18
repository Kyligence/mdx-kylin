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

export function getInitProps() {
  return {
    boundQueryHistoryActions: {
      getQueryHistory: sinon.spy(),
      getQueryHistoryCluster: sinon.spy(sinon.fake.returns(['localhost:7080'])),
    },
  };
}

export function getListData() {
  return {
    data: [
      { application: 'Excel', content: 'SQL 1', datasetName: 'dataset', executeTime: 1, id: 1, isCached: false, isGateway: false, isOtherQueryEngine: false, isTimeout: false, networkSize: 1, queryId: '340bb08a-594f-4e2c-898a-fcca4eb8a221', startTime: 1603774591013, status: false, transferTime: 1, username: 'user', node: 'localhost:7080' },
      { application: 'Excel', content: 'SQL 2', datasetName: 'dataset', executeTime: 2000, id: 2, isCached: true, isGateway: true, isOtherQueryEngine: true, isTimeout: true, networkSize: 2000, queryId: '340bb08a-594f-4e2c-898a-fcca4eb8a222', startTime: 1603774591013, status: true, transferTime: 2000, username: 'user', node: 'localhost:7080' },
      ...[3, 4, 5, 6, 7, 8, 9, 10]
        .map(id => (
          { application: 'Excel', content: `SQL ${id}`, datasetName: 'dataset', executeTime: 2000, id, isCached: true, isGateway: true, isOtherQueryEngine: true, isTimeout: true, networkSize: 2000, queryId: `340bb08a-594f-4e2c-898a-fcca4eb8a23${id}`, startTime: 1603774591013, status: true, transferTime: 2000, username: 'user', node: 'localhost:7080' }
        )),
    ],
    columns: [{ prop: 'id' }, { prop: 'startTime' }, { prop: 'executeTime' }, { prop: 'datasetName' }, { prop: 'application' }, { prop: 'queryId' }, { prop: 'transferTime' }, { prop: 'isGateway' }, { prop: 'content' }, { prop: 'status' }, { prop: 'username' }, { prop: 'isCached' }, { prop: 'isOtherQueryEngine' }, { prop: 'networkSize' }, { prop: 'isTimeout' }, { prop: 'multiDimDatasetTime' }],
    pageOffset: 0,
    pageSize: 10,
    totalCount: 31,
    orderBy: null,
    direction: null,
    isLoading: false,
  };
}
