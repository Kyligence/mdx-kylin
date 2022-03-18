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
import { createSelector } from 'reselect';
import { generatePath } from 'react-router';

import { businessHelper } from '../../utils';
import { configs } from '../../constants';

/* eslint-disable max-len */
export const getUrls = createSelector(
  state => state.datasetId,
  datasetId => {
    const listSite = businessHelper.findSiteByName('listDataset', configs.sitemap);
    const infoSite = businessHelper.findSiteByName('datasetInfo', configs.sitemap);
    const relationSite = businessHelper.findSiteByName('datasetRelation', configs.sitemap);
    const semanticSite = businessHelper.findSiteByName('datasetSemantic', configs.sitemap);
    const translationSite = businessHelper.findSiteByName('datasetTranslation', configs.sitemap);
    const usagesSite = businessHelper.findSiteByName('datasetUsages', configs.sitemap);
    const accessSite = businessHelper.findSiteByName('datasetAccess', configs.sitemap);
    const params = { datasetId };

    return {
      listUrl: listSite.url,
      infoUrl: datasetId ? generatePath(infoSite.paramUrl, params) : infoSite.url,
      relationUrl: datasetId ? generatePath(relationSite.paramUrl, params) : relationSite.url,
      semanticUrl: datasetId ? generatePath(semanticSite.paramUrl, params) : semanticSite.url,
      translationUrl: datasetId ? generatePath(translationSite.paramUrl, params) : translationSite.url,
      usagesUrl: datasetId ? generatePath(usagesSite.paramUrl, params) : usagesSite.url,
      accessUrl: datasetId ? generatePath(accessSite.paramUrl, params) : accessSite.url,
    };
  },
);
/* eslint-enable */

export function isIsolatedModel(modelRelations, modelName) {
  const relatedRelationships = modelRelations.filter(rel => (
    (rel.modelLeft === modelName && rel.modelRight) ||
    (rel.modelRight === modelName && rel.modelLeft)
  ));
  return relatedRelationships.length === 0;
}

export const getIsolatedModels = createSelector(
  state => state.dataset,
  ({ models, modelRelations }) => (
    models.length > 1
      ? models.filter(model => isIsolatedModel(modelRelations, model.name))
      : []
  ),
);

export const popperProps = {
  popperClass: 'step-description',
  popperProps: configs.disablePopperAutoFlip,
};

export const lastPopperProps = {
  ...popperProps,
  placement: 'top-end',
};
