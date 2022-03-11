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