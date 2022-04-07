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
import template from 'lodash/template';
import { isIE } from './browserHelper';
import { isEmpty } from './dataHelper';

function getBaseParams() {
  // Issue: https://github.com/Kyligence/Insight/issues/2225#issuecomment-541312367
  return {
    randomNumber: isIE ? Math.round(Math.random() * 10000000000000) : null,
  };
}

function pushSearchResults(searchResults = [], searchKey, searchValue, config = {}) {
  const { splitArrayMode = false } = config;
  const isEmptyValue = isEmpty(searchValue);
  const isArray = searchValue instanceof Array;

  if (!isEmptyValue) {
    if (!splitArrayMode || !isArray) {
      const value = searchValue instanceof Array ? searchValue.toString() : searchValue;
      const searchResult = searchResults.length
        ? `${searchKey}=${encodeURIComponent(value)}`
        : `?${searchKey}=${encodeURIComponent(value)}`;
      searchResults.push(searchResult);
    } else {
      searchValue.forEach(valueItem => {
        const searchResult = searchResults.length
          ? `${searchKey}=${encodeURIComponent(valueItem)}`
          : `?${searchKey}=${encodeURIComponent(valueItem)}`;
        searchResults.push(searchResult);
      });
    }
  }
}

export function getOptionalBaseParams(getState) {
  let baseParams = {
    publicUrl: process.env.PUBLIC_URL,
  };

  if (getState) {
    const { currentProject } = getState().system;
    baseParams = {
      ...baseParams,
      project: currentProject.name,
    };
  }
  return baseParams;
}

export default function templateUrl(templateString = '', data = {}, config = {}) {
  const { getState } = config;
  // 拼接上可选的base search字符串
  const paramsData = { ...data, ...getOptionalBaseParams(getState) };
  const [pathnameString, searchString] = templateString.split('?');
  const pathname = template(pathnameString)(paramsData);
  let searchResults = [];

  if (searchString) {
    const searchArray = searchString.split('&');

    for (const searchItem of searchArray) {
      const [searchKey] = searchItem.split('=');

      let searchValue = searchItem.replace(`${searchKey}=`, '');

      const dataKey = searchValue.replace(/^<%=\s*|\s*%>$/g, '');
      const isComplexTemplate = /<%=\s*/.test(dataKey) && /\s*%>/.test(dataKey);
      const isSimpleTemplate = /^<%=\s*/.test(searchValue) && /\s*%>$/.test(searchValue);

      if (isComplexTemplate) {
        searchValue = template(searchValue)(paramsData);
        pushSearchResults(searchResults, searchKey, searchValue);
      } else if (isSimpleTemplate) {
        searchValue = paramsData[dataKey];
        pushSearchResults(searchResults, searchKey, searchValue, config);
      } else {
        pushSearchResults(searchResults, searchKey, searchValue, config);
      }
    }
  }

  // 拼接上必带的base search字符串
  const baseParams = getBaseParams(getState);
  const baseParamPairs = Object.entries(baseParams)
    .map(([key, value]) => (!isEmpty(value) ? `${key}=${encodeURIComponent(value)}` : ''))
    .filter(item => item);
  if (baseParamPairs.length) {
    if (searchResults.length) {
      searchResults = [...searchResults, ...baseParamPairs];
    } else {
      const [firstSearch, ...otherSearchs] = baseParamPairs;
      searchResults = [`?${firstSearch}`, ...otherSearchs];
    }
  }

  return `${pathname}${searchResults.join('&')}`;
}

export function getNotFoundUrl({ duration = 0, fallbackUrl = '', icon = 'icon-superset-table_discard', messageId = '', step = 1000, pageId = '', entityId = '' }) {
  return `/not-found?fallbackUrl=${encodeURIComponent(fallbackUrl)}&duration=${duration}&icon=${encodeURIComponent(icon)}&messageId=${encodeURIComponent(messageId)}&step=${step}&pageId=${encodeURIComponent(pageId)}&entityId=${encodeURIComponent(entityId)}`;
}
