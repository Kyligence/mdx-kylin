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
