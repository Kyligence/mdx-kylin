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
