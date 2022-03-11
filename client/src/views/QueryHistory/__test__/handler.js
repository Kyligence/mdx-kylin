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
