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
import * as actionTypes from '../../types';
import { configs } from '../../../constants';

function getInitialState() {
  return {
    data: [],
    columns: [],
    pageOffset: 0,
    pageSize: configs.pageCount.userList,
    totalCount: 0,
    orderBy: null,
    direction: null,
    isLoading: false,
    isFullLoading: false,
  };
}

export default function userGroupList(state = getInitialState(), action) {
  const { type, ...payload } = action;

  switch (action.type) {
    case actionTypes.INIT_USER_GROUP_LIST: {
      return getInitialState();
    }
    case actionTypes.SET_USER_GROUP_LIST: {
      return { ...state, ...payload };
    }
    case actionTypes.PUSH_USER_GROUP_LIST: {
      const { data } = payload;
      return { ...state, ...payload, data: [...state.data, ...data] };
    }
    case actionTypes.SET_USER_GROUP_LIST_LOADING: {
      return { ...state, ...payload };
    }
    case actionTypes.SET_USER_GROUP_LIST_FULL_LOADING: {
      return { ...state, ...payload };
    }
    default:
      return state;
  }
}
