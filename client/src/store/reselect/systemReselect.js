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
import { specHelper, businessHelper } from '../../utils';
import { configs } from '../../constants';

const { projectAccessInKE } = configs;

function getAvailableMenus(menus = [], availableSiteList = []) {
  const availableMenus = [];
  for (const menu of menus) {
    if (menu.children && menu.children.length) {
      menu.children = getAvailableMenus(menu.children, availableSiteList);
    }
    if (availableSiteList.includes(menu.name)) {
      availableMenus.push(menu);
    }
  }
  return availableMenus;
}

export const getCurrentSitePaths = createSelector(
  state => state.router,
  router => {
    const { location } = router;
    const { pathname } = location;
    return businessHelper.findSitePathByUrl(pathname, configs.sitemap);
  },
);

export const getAvailableSiteList = createSelector(
  state => state.system.currentProject,
  currentProject => {
    const { access: projectAccess } = currentProject;
    if (projectAccess) {
      const availableSiteNames = specHelper.getAvailableOptions('siteName', { projectAccess });
      return availableSiteNames
        .map(siteName => businessHelper.findSiteByName(siteName, configs.sitemap));
    }
    return [businessHelper.findSiteByName('login', configs.sitemap)];
  },
);

export const getCurrentSite = createSelector(
  getCurrentSitePaths,
  getAvailableSiteList,
  (currentSitePaths, availableSites) => {
    const currentSite = currentSitePaths[currentSitePaths.length - 1];

    if (currentSite) {
      return availableSites.some(site => site.name === currentSite.name) ? currentSite : null;
    }

    return null;
  },
);

export const getMenus = createSelector(
  getCurrentSite,
  currentSite => {
    const siteName = currentSite && currentSite.name;
    const availableSiteList = specHelper.getAvailableOptions('menu', { siteName });
    const menus = businessHelper.findMenusInSites(configs.sitemap);
    return getAvailableMenus(menus, availableSiteList);
  },
);

export const getIsAdminMode = createSelector(
  getCurrentSite,
  currentSite => currentSite && configs.adminSites.includes(currentSite.name),
);

export const getIsLoginMode = createSelector(
  getCurrentSite,
  currentSite => currentSite && !!currentSite.isLoginPage,
);

export const getProjects = createSelector(
  state => state.data.projectList,
  projects => projects.data.filter(project => (
    configs.enabledProjectAccess.includes(project.access)
  )),
);

export const getIsGlobalAdmin = createSelector(
  state => state.data.projectList,
  projects => projects.data
    .every(project => project.access === projectAccessInKE.GLOBAL_ADMIN),
);

export const getCapability = createSelector(
  state => state.system.currentProject,
  currentProject => {
    const { access: projectAccess } = currentProject;
    if (projectAccess) {
      return specHelper.getAvailableOptions('capability', { projectAccess });
    }
    return [];
  },
);

export const getCanManageSystem = createSelector(
  getCapability,
  (capabilities = []) => !!capabilities.length && capabilities.every(capability => (
    configs.systemManageCapabilities.includes(capability)
  )),
);
