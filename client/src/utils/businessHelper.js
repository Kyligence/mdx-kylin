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
import { matchPath } from 'react-router';

/**
 * 从sitemap中整合Menus出来
 * @param {*} sites
 * @param {*} parentSite
 */
export function findMenusInSites(sites = [], parentSite) {
  let currentMenus = parentSite ? parentSite.children : [];

  for (const site of sites) {
    const menuItem = { ...site, children: [] };

    if (menuItem.menuText) {
      currentMenus.push(menuItem);
    }

    if (site.children && site.children.length) {
      currentMenus = [...currentMenus, ...findMenusInSites(site.children, menuItem)];
    }
  }

  return currentMenus;
}

export function findSitePathByUrl(url, sites = []) {
  let matchedSitePath = [];

  for (const site of sites) {
    const isUrlMatched = !!matchPath(url, { path: site.url, exact: true });
    const isParamUrlMatched = !!matchPath(url, { path: site.paramUrl, exact: true });
    const isUrlBelongTo = !!matchPath(url, { path: site.url, exact: false });
    const isParamUrlBelongTo = !!matchPath(url, { path: site.paramUrl, exact: false });

    if (isUrlMatched || isParamUrlMatched) {
      matchedSitePath = [site];
    } else if (site.children && (isUrlBelongTo || isParamUrlBelongTo)) {
      const resultPath = findSitePathByUrl(url, site.children);
      if (resultPath.length) {
        matchedSitePath = [site, ...resultPath];
      }
    }
  }

  return matchedSitePath;
}

export function findSitePathByName(searchName, sites = []) {
  let matchedSitePath = [];

  for (const site of sites) {
    if (site.name === searchName) {
      matchedSitePath = [site];
    } else if (site.children) {
      const resultPath = findSitePathByName(searchName, site.children);
      if (resultPath.length) {
        matchedSitePath = [site, ...resultPath];
      }
    }
  }

  return matchedSitePath;
}

export function findSiteByName(searchName, sites = []) {
  const searchedSitePath = findSitePathByName(searchName, sites);
  return searchedSitePath[searchedSitePath.length - 1] || null;
}

// MDX-1489: 1.2.3版本后端将用户名统一处理成了大写，导致大小写敏感匹配不上。
// 因此在数据入口的时候进行转换。
export function formatDatasetRoleDetail(user) {
  const contains = user.contains.map(item => {
    const newItem = { ...item };
    if (newItem.type === 'user') {
      newItem.name = newItem.name.toUpperCase();
    }
    return newItem;
  });
  return { ...user, contains };
}
