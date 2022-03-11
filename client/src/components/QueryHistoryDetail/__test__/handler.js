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
      getQuerySqlByQueryId: sinon.spy(sinon.fake.returns({
        list: [
          {
            id: 1,
            sqlCacheUsed: true,
            sqlExecutionTime: 500,
            sqlText: 'select count(*) from "DEFAULT"."KYLIN_SALES_1"',
            status: true,
          },
          {
            id: 2,
            sqlCacheUsed: false,
            sqlText: 'select count(*) from "DEFAULT"."KYLIN_SALES_2"',
            status: false,
          },
          {
            id: 3,
            sqlCacheUsed: true,
            sqlExecutionTime: 1000 * 60 * 60 + 1000 * 60 + 1000,
            sqlText: 'select count(*) from "DEFAULT"."KYLIN_SALES_3"',
            status: false,
          },
        ],
        total: 10,
      })),
    },
    mdxQuery: {
      id: 1,
      startTime: 1603774566446,
      executeTime: 100,
      datasetName: 'dataset',
      application: 'Excel',
      queryId: '7af63b7b-e0eb-4278-b486-7924e05d743b',
      transferTime: 2000,
      isGateway: false,
      content: 'REFRESH CUBE [dataset]',
      status: true,
      username: 'Admin',
      isCached: false,
      isOtherQueryEngine: true,
      networkSize: 11556,
      isTimeout: false,
      multiDimDatasetTime: 1000 * 60 + 1000,
    },
  };
}
