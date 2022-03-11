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
