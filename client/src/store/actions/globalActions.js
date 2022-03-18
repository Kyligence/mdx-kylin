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
import { browserHelper } from '../../utils';
import { storagePath } from '../../constants';

export function toggleFlag(key, value) {
  return { type: actionTypes.TOGGLE_FLAG, key, value };
}

export function setGlobalMaskMessage(message, data) {
  return { type: actionTypes.SET_GLOBAL_MASK_MESSAGE, message, data };
}

export function disableMenuPreventFlag() {
  return { type: actionTypes.DISABLE_MENU_PREVENT };
}

export function setMenuCollapsed(collapsed) {
  const { IS_MENU_COLLAPSED } = storagePath;
  browserHelper.setStorage(IS_MENU_COLLAPSED, collapsed);
  return { type: actionTypes.SET_MENU_COLLAPSED, collapsed };
}
