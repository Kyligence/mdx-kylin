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
