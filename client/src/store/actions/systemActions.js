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
/* eslint-disable max-len */
import dayjs from 'dayjs';
import { i18n } from 'kyligence-ui-react';
import enLocale from 'kyligence-ui-react/dist/npm/es5/src/locale/lang/en';
import zhLocale from 'kyligence-ui-react/dist/npm/es5/src/locale/lang/zh-CN';
import { Base64 } from 'js-base64';

import * as actionTypes from '../types';
import { SystemService } from '../../services';
import { browserHelper, domHelper } from '../../utils';
import { storagePath } from '../../constants';
import { getProjects } from '../reselect/systemReselect';

export function setLocale(locale, messages) {
  dayjs.locale(locale === 'zh' ? 'zh-cn' : 'en');
  return { type: actionTypes.SET_LOCALE_AND_MESSAGES, locale, messages };
}

export function setCurrentUser(loginUser) {
  return { type: actionTypes.SET_CURRENT_USER, loginUser };
}

export function setLicense(license) {
  return { type: actionTypes.SET_LICENSE, ...license };
}

export function setAADSettings(settings) {
  return { type: actionTypes.SET_AAD_SETTINGS, settings };
}

export function getCurrentUser() {
  return async dispatch => {
    const currentUser = await SystemService.fetchCurrentUser();
    dispatch(setCurrentUser(currentUser));
  };
}

export function getLicense() {
  return async dispatch => {
    const license = await SystemService.fetchLicense();

    const version = license.ki_version;
    const liveDate = license.live_date_range;
    const licenseType = license.ki_type.toUpperCase();
    const commitId = license.commit_id;

    dispatch(setLicense({ version, liveDate, licenseType, commitId }));
  };
}

export function getAADSettings() {
  return async dispatch => {
    const settings = await SystemService.fetchAADSettings();

    dispatch(setAADSettings(settings));
  };
}

export function login({ username, password }) {
  return async dispatch => {
    const basicAuth = Base64.encode(`${username}:${password}`);
    await SystemService.login(null, { basicAuth });
    dispatch(getCurrentUser());
  };
}

export function logout() {
  return async (dispatch, getState) => {
    const { aadSettings } = getState().system;
    const isAADMode = aadSettings['insight.semantic.enable.aad'];
    const loginUrl = aadSettings['aad.login.url'];
    const logoutUrl = aadSettings['aad.logout.url'];

    if (isAADMode) {
      await SystemService.logoutAAD({ logoutUrl });
      await SystemService.logout();
      window.location.href = loginUrl;
    } else {
      await SystemService.logout();
      dispatch(getCurrentUser());
    }
  };
}

export function getLanguage(locale) {
  return async dispatch => {
    const messages = await SystemService.fetchLocales({ locale });

    browserHelper.setStorage(storagePath.LOCALE, locale);
    domHelper.setHtmlLanguage(locale);
    i18n.use(locale === 'zh' ? zhLocale : enLocale);
    dispatch(setLocale(locale, messages));
  };
}

export function setCurrentProject(project) {
  return async dispatch => {
    browserHelper.setStorage(storagePath.CURRENT_PROJECT, project);
    dispatch({ type: actionTypes.SET_CURRENT_PROJECT, project });
  };
}

export function clearCurrentProject() {
  const project = { name: '', access: 'EMPTY' };
  return async dispatch => {
    browserHelper.setStorage(storagePath.CURRENT_PROJECT, project);
    dispatch({ type: actionTypes.SET_CURRENT_PROJECT, project });
  };
}

export function setDefaultProject() {
  return (dispatch, getState) => {
    const { currentProject } = getState().system;
    const projects = getProjects(getState());
    const [defaultProject] = projects;
    const hasInvalidSelectedProject = projects.every(item => item.name !== currentProject.name);
    const hasNoSeletedProject = !currentProject.name;

    if (!defaultProject) {
      dispatch(clearCurrentProject());
    } else if (hasNoSeletedProject || hasInvalidSelectedProject) {
      dispatch(setCurrentProject(defaultProject));
    }
  };
}

export function getClusters() {
  return async dispatch => {
    const clusters = await SystemService.fetchClustersInfo();
    dispatch({ type: actionTypes.SET_CLUSTERS, clusters });
  };
}

export function getConfigurations() {
  return async dispatch => {
    const configurations = await SystemService.fetchConfigurations();
    dispatch({ type: actionTypes.SET_CONFIGURATIONS, configurations });
  };
}

export function updateConfigurations(configurations) {
  return () => SystemService.updateConfigurations(null, configurations);
}

export function restartSyncTask() {
  return () => SystemService.restartSyncTask();
}

export function getStatisticBasic(params) {
  return (dispatch, getState) => SystemService.fetchStatisticBasic(params, { getState });
}

export function getStatisticTrend(params, callback) {
  return (dispatch, getState) => SystemService.fetchStatisticTrend({}, params, { getState, callback });
}

export function getStatisticQueryCost(params) {
  return (dispatch, getState) => SystemService.fetchStatisticQueryCost({}, params, { getState });
}

export function getStatisticRanking(params) {
  return (dispatch, getState) => SystemService.fetchStatisticRanking(params, { getState });
}
