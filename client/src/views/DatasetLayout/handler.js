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
