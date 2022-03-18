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
import * as actionTypes from '../../types';

const emptyUser = { name: null, id: null };

function getInitialState() {
  return {
    id: null,
    username: null,
  };
}

export default function currentUser(state = getInitialState(), action) {
  switch (action.type) {
    case actionTypes.SET_CURRENT_USER: {
      const { loginUser } = action;
      const { id, name } = loginUser || emptyUser;
      return { ...state, id, username: name };
    }
    default:
      return state;
  }
}
