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
