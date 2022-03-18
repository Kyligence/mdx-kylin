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

function getInitialState() {
  return {};
}

export function getModalReducer() {
  return function modal(state = getInitialState(), action) {
    switch (action.type) {
      case actionTypes.REGISTER_MODAL: {
        const { name, data } = action;
        const modalState = state[name] || {};
        return { ...state, [name]: { ...data, ...modalState, initialData: data } };
      }
      case actionTypes.DESTROY_MODAL: {
        const { name } = action;
        return { ...state, [name]: undefined };
      }
      case actionTypes.SHOW_MODAL: {
        const { name, form, callback, isReset } = action;
        if (isReset) {
          const modalData = form
            ? { ...state[name], form, callback, isShow: true }
            : { ...state[name], callback, isShow: true };
          return { ...state, [name]: modalData };
        }
        const modalData = { ...state[name], isShow: true };
        return { ...state, [name]: modalData };
      }
      case actionTypes.HIDE_MODAL: {
        const { name, isReset } = action;
        const { initialData } = state[name];
        return isReset
          ? { ...state, [name]: { ...state[name], ...initialData, isShow: false } }
          : { ...state, [name]: { ...state[name], isShow: false } };
      }
      case actionTypes.SET_MODAL_FORM: {
        const { name, changedValue } = action;
        const newFormData = { ...state[name].form, ...changedValue };
        const newModalData = { ...state[name], form: newFormData };
        return { ...state, [name]: newModalData };
      }
      case actionTypes.SET_MODAL_DATA: {
        const { name, data } = action;
        return { ...state, [name]: { ...state[name], ...data } };
      }
      default:
        return state;
    }
  };
}
