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
