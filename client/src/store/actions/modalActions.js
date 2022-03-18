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
import * as actionTypes from '../types';

export function registerModal(name, data) {
  return { type: actionTypes.REGISTER_MODAL, name, data };
}

export function destroyModal(name) {
  return { type: actionTypes.DESTROY_MODAL, name };
}

export function showModal(name, form, isReset = true) {
  return dispatch => new Promise(resolve => {
    if (isReset) {
      dispatch({ type: actionTypes.SHOW_MODAL, name, form, isReset, callback: resolve });
    } else {
      dispatch({ type: actionTypes.SHOW_MODAL, name });
    }
  });
}

export function hideModal(name, isReset = true) {
  return { type: actionTypes.HIDE_MODAL, name, isReset };
}

export function setModalForm(name, changedValue) {
  return { type: actionTypes.SET_MODAL_FORM, name, changedValue };
}

export function setModalData(name, data) {
  return { type: actionTypes.SET_MODAL_DATA, name, data };
}
