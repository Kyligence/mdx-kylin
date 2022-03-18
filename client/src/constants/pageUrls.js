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
const baseEl = document.querySelector('base') || { getAttribute: () => '/' };
const baseUrl = baseEl.getAttribute('href');

export function wrapperBaseUrl(url) {
  return `${baseUrl}${url}`;
}

export default {
  login: wrapperBaseUrl('login'),
  overview: wrapperBaseUrl('overview'),
  notFound: wrapperBaseUrl('not-found'),
  // list page
  listDataset: wrapperBaseUrl('dataset/list'),
  queryHistory: wrapperBaseUrl('query-history'),
  // create dataset page
  dataset: wrapperBaseUrl('dataset'),
  datasetNew: wrapperBaseUrl('dataset/new'),
  datasetInfo: wrapperBaseUrl('dataset/new/basic-info'),
  datasetRelation: wrapperBaseUrl('dataset/new/relationship'),
  datasetSemantic: wrapperBaseUrl('dataset/new/semantic'),
  datasetTranslation: wrapperBaseUrl('dataset/new/translation'),
  datasetUsages: wrapperBaseUrl('dataset/new/dimension-usages'),
  datasetAccess: wrapperBaseUrl('dataset/new/access'),
  // edit dataset page
  datasetWithId: wrapperBaseUrl('dataset/:datasetId'),
  datasetInfoWithId: wrapperBaseUrl('dataset/:datasetId/basic-info'),
  datasetRelationWithId: wrapperBaseUrl('dataset/:datasetId/relationship'),
  datasetSemanticWithId: wrapperBaseUrl('dataset/:datasetId/semantic'),
  datasetTranslationWithId: wrapperBaseUrl('dataset/:datasetId/translation'),
  datasetUsagesWithId: wrapperBaseUrl('dataset/:datasetId/dimension-usages'),
  datasetAccessWithId: wrapperBaseUrl('dataset/:datasetId/access'),
  // management page
  listDatasetRole: wrapperBaseUrl('dataset-role/list'),
  diagnosis: wrapperBaseUrl('diagnosis'),
  configuration: wrapperBaseUrl('configuration'),
  toolkit: wrapperBaseUrl('toolkit'),
};
