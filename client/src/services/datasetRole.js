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
import axios from 'axios';
import { apiUrls } from '../constants';
import templateUrl from '../utils/templateUrl';

export function fetchDatasetRoles(params) {
  const url = templateUrl(apiUrls.GET_DATASET_ROLES, params);
  return axios.get(url);
}

export function fetchRoleDetail(params) {
  const url = templateUrl(apiUrls.GET_DATASET_ROLE, params);
  return axios.get(url);
}

export function createRole(params, data) {
  const url = templateUrl(apiUrls.CREATE_DATASET_ROLE, params);
  return axios.post(url, data);
}

export function editRole(params, data) {
  const url = templateUrl(apiUrls.EDIT_DATASET_ROLE, params);
  return axios.put(url, data);
}

export function deleteRole(params) {
  const url = templateUrl(apiUrls.DELETE_DATASET_ROLE, params);
  return axios.delete(url);
}
