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
