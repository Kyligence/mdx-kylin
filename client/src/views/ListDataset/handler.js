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
import React from 'react';
import { Tag } from 'kyligence-ui-react';
import { strings, configs } from '../../constants';
import { dataHelper, businessHelper } from '../../utils';

export const createUrl = businessHelper.findSiteByName('datasetInfo', configs.sitemap).url;
export const editParamsUrl = businessHelper.findSiteByName('datasetInfo', configs.sitemap).paramUrl;
export const accessParamsUrl = businessHelper.findSiteByName('datasetAccess', configs.sitemap).paramUrl;

/* eslint-disable react/jsx-filename-extension */
export function getRenderColumns({ intl }) {
  return [
    {
      prop: 'dataset',
      label: intl.formatMessage(strings.DATASET),
    },
    {
      prop: 'status',
      label: intl.formatMessage(strings.STATUS),
      render(row) {
        const { status } = row;
        const type = status === 'NORMAL' ? 'success' : 'danger';
        return <Tag type={type}>{intl.formatMessage(strings[status])}</Tag>;
      },
    },
    {
      prop: 'createUser',
      label: intl.formatMessage(strings.CREATE_USER),
    },
    {
      prop: 'modifyTime',
      label: intl.formatMessage(strings.MODIFY_TIME),
      render(row) {
        const { modifyTime } = row;
        return <span>{dataHelper.getDateString(modifyTime)}</span>;
      },
    },
  ];
}
