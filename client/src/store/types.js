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
// System Action types
export const SET_CURRENT_USER = 'SET_CURRENT_USER';
export const SET_CURRENT_PROJECT = 'SET_CURRENT_PROJECT';
export const SET_LOCALE_AND_MESSAGES = 'SET_LOCALE_AND_MESSAGES';
export const SET_LICENSE = 'SET_LICENSE';
export const SET_CLUSTERS = 'SET_CLUSTERS';
export const SET_CONFIGURATIONS = 'SET_CONFIGURATIONS';
export const SET_AAD_SETTINGS = 'SET_AAD_SETTINGS';
// Global Action types
export const TOGGLE_FLAG = 'TOGGLE_FLAG';
export const DISABLE_MENU_PREVENT = 'DISABLE_MENU_PREVENT';
export const SET_MENU_COLLAPSED = 'SET_MENU_COLLAPSED';
export const SET_GLOBAL_MASK_MESSAGE = 'SET_GLOBAL_MASK_MESSAGE';

// Modal Action types
export const REGISTER_MODAL = 'REGISTER_MODAL';
export const DESTROY_MODAL = 'DESTROY_MODAL';
export const SHOW_MODAL = 'SHOW_MODAL';
export const HIDE_MODAL = 'HIDE_MODAL';
export const SET_MODAL_FORM = 'SET_MODAL_FORM';
export const SET_MODAL_DATA = 'SET_MODAL_DATA';

// ProjectList Action types
export const SET_PROJECT_LIST = 'SET_PROJECT_LIST';
export const SET_PROJECT_LIST_LOADING = 'SET_PROJECT_LIST_LOADING';
export const CLEAR_PROJECT_LIST = 'CLEAR_PROJECT_LIST';

// UserList Action types
export const SET_USER_LIST = 'SET_USER_LIST';
export const INIT_USER_LIST = 'INIT_USER_LIST';
export const PUSH_USER_LIST = 'PUSH_USER_LIST';
export const SET_USER_LIST_LOADING = 'SET_USER_LIST_LOADING';
export const SET_USER_LIST_FULL_LOADING = 'SET_USER_LIST_FULL_LOADING';

// UserList Action types
export const SET_USER_GROUP_LIST = 'SET_USER_GROUP_LIST';
export const INIT_USER_GROUP_LIST = 'INIT_USER_GROUP_LIST';
export const PUSH_USER_GROUP_LIST = 'PUSH_USER_GROUP_LIST';
export const SET_USER_GROUP_LIST_LOADING = 'SET_USER_GROUP_LIST_LOADING';
export const SET_USER_GROUP_LIST_FULL_LOADING = 'SET_USER_GROUP_LIST_FULL_LOADING';

// DatasetList Action types
export const SET_DATASET_LIST = 'SET_DATASET_LIST';
export const SET_DATASET_LIST_LOADING = 'SET_DATASET_LIST_LOADING';

// QueryHistory Action types
export const SET_QUERY_HISTORY = 'SET_QUERY_HISTORY';
export const SET_QUERY_HISTORY_LOADING = 'SET_QUERY_HISTORY_LOADING';

// Indicator Action types
export const SET_INDICATOR = 'SET_INDICATOR';
export const SET_INDICATOR_LOADING = 'SET_INDICATOR_LOADING';

// DatasetRoleList Action types
export const SET_DATASET_ROLE_LIST = 'SET_DATASET_ROLE_LIST';
export const INIT_DATASET_ROLE_LIST = 'INIT_DATASET_ROLE_LIST';
export const PUSH_DATASET_ROLE_LIST = 'PUSH_DATASET_ROLE_LIST';
export const SET_DATASET_ROLE_LIST_LOADING = 'SET_DATASET_ROLE_LIST_LOADING';
export const SET_DATASET_ROLE_LIST_FULL_LOADING = 'SET_DATASET_ROLE_LIST_FULL_LOADING';

// Workspace: Window Action types
export const UPDATE_WINDOW_SIZE = 'UPDATE_WINDOW_SIZE';

// Workspace: Dataset role action types
export const INIT_DATASET_ROLE = 'INIT_DATASET_ROLE';
export const SET_DATASET_ROLE = 'SET_DATASET_ROLE';

// Workspace: Dataset action types
export const INIT_DATASET = 'INIT_DATASET';
export const SET_DIM_TABLE = 'SET_DIM_TABLE';
export const SET_DIM_COLUMN = 'SET_DIM_COLUMN';
export const SET_DIM_MEASURE = 'SET_DIM_MEASURE';
export const SET_MEASURE_GROUP = 'SET_MEASURE_GROUP';
export const DELETE_HIERARCHY = 'DELETE_HIERARCHY';
export const SET_HIERARCHY = 'SET_HIERARCHY';
export const SET_CALCULATED_MEASURE = 'SET_CALCULATED_MEASURE';
export const BATCH_SET_CALCULATED_MEASURES = 'BATCH_SET_CALCULATED_MEASURES';
export const REFRESH_DIM_COLUMN_PROPERTIES = 'REFRESH_DIM_COLUMN_PROPERTIES';
export const REFRESH_CMEASURE_NON_EMPTY_BEHAVIOR = 'REFRESH_CMEASURE_NON_EMPTY_BEHAVIOR';
export const BATCH_SET_NAMEDSETS = 'BATCH_SET_NAMEDSETS';
export const SET_NAMEDSET = 'SET_NAMEDSET';
export const DELETE_NAMEDSET = 'DELETE_NAMEDSET';
export const DELETE_CALCULATED_MEASURE = 'DELETE_CALCULATED_MEASURE';
export const SET_DATASET_BASIC_INFO = 'SET_DATASET_BASIC_INFO';
export const SET_DIMENSION_USAGES = 'SET_DIMENSION_USAGES';
export const ADD_RELATION_MODEL = 'ADD_RELATION_MODEL';
export const UPDATE_MODELS = 'UPDATE_MODELS';
export const PUSH_MODEL_TABLES = 'PUSH_MODEL_TABLES';
export const INIT_RELATIONSHIP = 'INIT_RELATIONSHIP';
export const UPDATE_RELATIONSHIP = 'UPDATE_RELATIONSHIP';
export const ADD_RELATIONSHIP = 'ADD_RELATIONSHIP';
export const CLEAR_MODELS_AND_TABLES = 'CLEAR_MODELS_AND_TABLES';
export const CLEAR_ALL_EXCEPT_BASIC = 'CLEAR_ALL_EXCEPT_BASIC';
export const RECOVERY_DATASET_STORE = 'RECOVERY_DATASET_STORE';
export const CLEAR_ALL = 'CLEAR_ALL';
export const SET_CANVAS_POSITION = 'SET_MODEL_TO_CANVAS';
export const SET_CANVAS_CONNECTION = 'SET_CANVAS_CONNECTION';
export const DELETE_CANVAS_MODEL = 'DELETE_CANVAS_MODEL';
export const TOGGLE_DATASET_LOADING = 'TOGGLE_DATASET_LOADING';
export const DELETE_RESTRICT = 'DELETE_RESTRICT';
export const TOGGLE_DATASET_DIFFER = 'TOGGLE_DATASET_DIFFER';
export const SET_DATASET_TRANSLATION = 'SET_DATASET_TRANSLATION';
export const DELETE_DATASET_TRANSLATION = 'DELETE_DATASET_TRANSLATION';
