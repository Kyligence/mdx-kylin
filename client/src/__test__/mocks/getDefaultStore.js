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
export default function getDefaultStore() {
  return {
    system: {
      language: {
        locale: 'en',
        messages: {},
      },
      currentUser: {
        id: null,
        username: null,
      },
      currentProject: {
        name: null,
        permission: 'EMPTY',
      },
      license: {
        version: '',
        liveDate: '',
        licenseType: '',
        commitId: '',
      },
    },
    global: {},
    data: {
      datasetList: {
        data: [],
        columns: [],
        pageOffset: 0,
        pageSize: 10,
        totalCount: 0,
        orderBy: null,
        direction: null,
        isLoading: false,
      },
      queryHistory: {
        data: [],
        columns: [],
        pageOffset: 0,
        pageSize: 10,
        totalCount: 0,
        orderBy: null,
        direction: null,
        isLoading: false,
      },
      datasetRoleList: {
        data: [],
        columns: [],
        pageOffset: 0,
        pageSize: 10,
        totalCount: 0,
        orderBy: null,
        direction: null,
        isLoading: false,
      },
      projectList: {
        data: [],
        columns: [],
        pageOffset: 0,
        pageSize: 10,
        totalCount: 0,
        orderBy: null,
        direction: null,
        isLoading: false,
      },
      userList: {
        data: [],
        columns: [],
        pageOffset: 0,
        pageSize: 10,
        totalCount: 0,
        orderBy: null,
        direction: null,
        isLoading: false,
      },
    },
    modal: {},
    workspace: {
      dataset: {
        project: null,
        datasetName: null,
        type: 'MDX',
        translationTypes: [],
        modelRelations: [],
        models: [],
        modelsAndTablesList: [],
        calculateMeasures: [],
        namedSets: [],
        dimTableModelRelations: [],
        canvas: { models: [] },
        isLoading: false,
      },
    },
    router: {
      action: 'POP',
      location: {
        pathname: '/login',
        search: '',
        hash: '',
        key: 'o4i7df',
        query: {},
      },
    },
  };
}
