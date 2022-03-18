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
// System API
export const GET_CURRENT_USER = 'api/current_user';
export const LOGIN = 'api/login';
export const LOGOUT = 'api/logout';
export const GET_LICENSE = 'api/system/license';
export const GET_PERMISSION = 'api/user/permission';
export const GET_CLUSTERS_INFO = 'api/system/clusters';
export const GET_CONFIGURATIONS = 'api/system/configurations';
export const UPDATE_CONFIGURATIONS = 'api/system/configurations';
export const RESTART_SYNC_TASK = 'api/system/sync/jobs';
export const GET_STATISTICS_BASIC = 'api/statistics/basic?startTime=<%=startTime%>&endTime=<%=endTime%>&projectName=<%=project%>';
export const GET_STATISTICS_TREND = 'api/statistics/trend?projectName=<%=project%>';
export const GET_STATISTICS_QUERY_COST = 'api/statistics/query-cost?projectName=<%=project%>';
export const GET_STATISTICS_RANKING = 'api/statistics/ranking?startTime=<%=startTime%>&endTime=<%=endTime%>&count=<%=count%>&direction=<%=direction%>&projectName=<%=project%>&count=<%=count%>';
export const GET_AAD_SETTINGS = 'api/system/aad-settings';
export const REPAIR_SCHEMA = 'api/helper/schema-repair/excel';
export const CHECK_SCHEMA_SOURCE = 'api/helper/schema-source/excel';
export const GET_TOOLKIT_CONFIGURATION = 'api/helper/configuration';

// Diagnosis API
export const GENERATE_DIAGNOSIS_PACKAGE = 'api/system/diagnose';
export const GET_DIAGNOSIS_PACKAGE_STATE = 'api/system/diagnose';
export const STOP_DIAGNOSIS_PACKAGE = 'api/system/diagnose';
export const DOWNLOAD_DIAGNOSIS_PACKAGE = 'api/download/<%=fileName%>';

// Project API
export const GET_PROJECTS = 'api/projects';

// User API
export const GET_USERS = 'api/system/allUsers?pageNum=<%=pageOffset%>&pageSize=<%=pageSize%>&project=<%=project%>';

// User Group API
export const GET_USER_GROUPS = 'api/system/groups?page_offset=<%=pageOffset%>&page_size=<%=pageSize%>&project=<%=project%>';

// Dataset Role API
export const GET_DATASET_ROLES = 'api/roles?pageNum=<%=pageOffset%>&pageSize=<%=pageSize%>&containsDesc=false';
export const GET_DATASET_ROLE = 'api/role/<%=datasetRoleId%>';
export const CREATE_DATASET_ROLE = 'api/role';
export const EDIT_DATASET_ROLE = 'api/role/<%=datasetRoleId%>';
export const DELETE_DATASET_ROLE = 'api/role/<%=datasetRoleId%>';

// Dataset API
export const GET_DATASETS = 'api/datasets?projectName=<%=project%>&pageNum=<%=pageOffset%>&pageSize=<%=pageSize%>&datasetName=<%=datasetName%>&orderBy=<%=orderBy%>&direction=<%=direction%>';
export const GET_SEMANTIC_MODEL_INFO = 'api/model/detail/<%=project%>/<%=model%>';
export const GET_PROJECT_MODELS = 'api/model/list/<%=project%>';
export const GET_MODEL_TABLES = 'api/dimtable/<%=project%>/<%=model%>';
export const SAVE_DATASET = 'api/dataset?createType=<%=createType%>';
export const UPDATE_DATASET = 'api/dataset/<%=datasetId%>?update_type=<%=updateType%>';
export const DELETE_DATASET = 'api/dataset/<%=datasetId%>';
export const CHECK_DATASET_NAME = 'api/dataset/<%=project%>/<%=datasetName%>/<%=type%>';
export const GET_DATASET = 'api/dataset/<%=datasetId%>';
export const PREVIEW_FORMAT_SAMPLE = 'api/dataset/format/preview';
export const VALIDATE_ALL_CMEASURE_EXPRESSION = 'api/semantic/cm/list/check';
export const VALIDATE_ALL_NAMEDSET_EXPRESSION = 'api/semantic/namedset/list/check';
export const VALIDATE_ALL_DEFAULT_MEMBER = 'api/semantic/default-member/list/check';
export const GET_SEMANTIC_USERS = 'api/system/users';
export const GET_KE_ACCESS_FROM_DATASET = 'api/acl/<%=project%>?name=<%=name%>&type=<%=type%>';
export const GENERATE_DATASET_PACKAGE = 'api/datasets/package';
export const DOWNLOAD_DATASET_PACKAGE = 'api/datasets/package/<%=token%>?projectName=<%=projectName%>';
export const VALIDATE_DATASET_BROKEN = 'api/datasets/validation/broken';
export const UPLOAD_DATASET_PACKAGE = 'api/datasets?type=import';
export const IMPORT_DATASET_BY_PACKAGE = 'api/datasets?type=import';

// Query History API
export const GET_QUERY_HISTORY = 'api/query-history?projectName=<%=project%>&pageNum=<%=pageOffset%>&pageSize=<%=pageSize%>&orderBy=<%=orderBy%>&direction=<%=direction%>';
export const GET_QUERY_HISTORY_CLUSTER = 'api/query-history/cluster?projectName=<%=project%>';
export const GET_QUERY_SQLS = 'api/query-history/<%=queryId%>?projectName=<%=project%>&pageNum=<%=pageOffset%>&pageSize=<%=pageSize%>&status=<%=onlyFailure%>';

// Indicator API
export const GET_INDICATORS = 'api/indicators?projectName=<%=project%>&pageNum=<%=pageOffset%>&pageSize=<%=pageSize%>&orderBy=<%=orderBy%>&direction=<%=direction%>';
