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
import * as actionTypes from '../types';
import { ProjectService, SystemService } from '../../services';
import { storeHelper } from '../../utils';
import { configs } from '../../constants';

function formatProjectResponse(response = {}, permissionMap = {}) {
  const data = response.map(name => ({
    name, access: permissionMap[name],
  }));
  const totalCount = data.length;
  return { totalCount, data };
}

export function getProjects() {
  return async dispatch => {
    const permissionMap = await SystemService.fetchPermission();

    await dispatch(storeHelper.fetchDataWithPagination({
      options: {
        pageOffset: 0,
        pageSize: configs.pageCount.projectList,
        withLocation: false,
      },
      formatResponse: response => formatProjectResponse(response, permissionMap),
      fetchApi: ProjectService.fetchProjects,
      setDataAction: actionTypes.SET_PROJECT_LIST,
      setLoadingAction: actionTypes.SET_PROJECT_LIST_LOADING,
    }));
  };
}

export function clearProjects() {
  return { type: actionTypes.CLEAR_PROJECT_LIST };
}
