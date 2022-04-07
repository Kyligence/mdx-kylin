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
