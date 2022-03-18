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
import * as systemReselect from './systemReselect';
import * as datasetReselect from './datasetReselect';

export default function reselect(state) {
  return {
    system: {
      menus: systemReselect.getMenus(state),
      currentSite: systemReselect.getCurrentSite(state),
      currentSitePath: systemReselect.getCurrentSitePaths(state),
      availableSites: systemReselect.getAvailableSiteList(state),
      isAdminMode: systemReselect.getIsAdminMode(state),
      isGlobalAdmin: systemReselect.getIsGlobalAdmin(state),
      isLoginMode: systemReselect.getIsLoginMode(state),
      projects: systemReselect.getProjects(state),
      capability: systemReselect.getCapability(state),
      canManageSystem: systemReselect.getCanManageSystem(state),
    },
    workspace: {
      dataset: {
        errorList: datasetReselect.getErrorList(state),
      },
    },
  };
}
