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
/* eslint-disable */
export default {
  "allOptionMaps": {
    "menu": [
      { "id": "overview", "value": "overview" },
      { "id": "listDataset", "value": "listDataset" },
      { "id": "datasetRole", "value": "datasetRole" },
      { "id": "diagnosis", "value": "diagnosis" },
      { "id": "configuration", "value": "configuration" },
      { "id": "queryHistory", "value": "queryHistory" },
    ],
    "siteName": [
      { "id": "overview", "value": "overview" },
      { "id": "login", "value": "login" },
      { "id": "listDataset", "value": "listDataset" },
      { "id": "datasetRole", "value": "datasetRole" },
      { "id": "diagnosis", "value": "diagnosis" },
      { "id": "configuration", "value": "configuration" },
      { "id": "toolkit", "value": "toolkit" },
      { "id": "datasetInfo", "value": "datasetInfo" },
      { "id": "datasetRelation", "value": "datasetRelation" },
      { "id": "datasetSemantic", "value": "datasetSemantic" },
      { "id": "datasetTranslation", "value": "datasetTranslation" },
      { "id": "datasetUsages", "value": "datasetUsages" },
      { "id": "datasetAccess", "value": "datasetAccess" },
      { "id": "queryHistory", "value": "queryHistory" },
      { "id": "notFound", "value": "notFound" },
    ],
    "projectAccess": [
      { "id": "globalAdmin", "value": "GLOBAL_ADMIN" },
      { "id": "projectAdmin", "value": "ADMINISTRATION" },
      { "id": "projectManager", "value": "MANAGEMENT" },
      { "id": "projectOperator", "value": "OPERATION" },
      { "id": "projectReader", "value": "READ" },
    ],
    "capability": [
      { "id": "addDatasetRole", "value": "addDatasetRole" },
      { "id": "editDatasetRole", "value": "editDatasetRole" },
      { "id": "viewDatasetRole", "value": "viewDatasetRole" },
      { "id": "deleteDatasetRole", "value": "deleteDatasetRole" },
    ],
    "datasetAccess": [
      { "id": "addDataset", "value": "addDataset" },
      { "id": "editDataset", "value": "editDataset" },
      { "id": "cloneDataset", "value": "cloneDataset" },
      { "id": "deleteDataset", "value": "deleteDataset" },
      { "id": "exportDataset", "value": "exportDataset" },
      { "id": "importDataset", "value": "importDataset" },
      { "id": "accessDataset", "value": "accessDataset" },
    ]
  },
  "enableOptionMaps": {
    // 菜单权限
    "menu": {
      "keyPattern": "siteName",
      "entries": [
        { "key": "[datasetRole,diagnosis,configuration,toolkit]", "value": "datasetRole,diagnosis,configuration" },
        { "key": "*", "value": "overview,listDataset,queryHistory" },
      ]
    },
    // 路由权限
    "siteName": {
      "keyPattern": "projectAccess",
      "entries": [
        { "key": "globalAdmin", "value": "*" },
        { "key": "*", "value": "login,overview,listDataset,queryHistory,datasetInfo,datasetRelation,datasetSemantic,datasetTranslation,datasetUsages,datasetAccess,notFound,toolkit" },
      ]
    },
    "capability": {
      "keyPattern": "projectAccess",
      "entries": [
        { "key": "globalAdmin", "value": "*" },
        { "key": "[projectAdmin,projectManager,projectOperator,projectReader]", "value": "none" },
      ]
    },
    "datasetAccess": {
      "keyPattern": "projectAccess",
      "entries": [
        { "key": "[globalAdmin,projectAdmin]", "value": "*" },
        { "key": "[projectManager,projectOperator,projectReader]", "value": "none" },
      ]
    }
  }
}