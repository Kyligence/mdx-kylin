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
import localforage from 'localforage';
import { isEmpty } from './dataHelper';
import { getHistory } from '../store/history';

// Opera 8.0+
export const isOpera = (!!window.opr && !!window.opr.addons) || !!window.opera || navigator.userAgent.indexOf(' OPR/') >= 0;

// Firefox 1.0+
export const isFirefox = typeof InstallTrigger !== 'undefined';

// Safari <= 9 "[object HTMLElementConstructor]"
export const isSafari = /constructor/i.test(window.HTMLElement) || (p => p.toString() === '[object SafariRemoteNotification]')(!window.safari || (typeof window.safari !== 'undefined' && window.safari.pushNotification));

// Internet Explorer 6-11
export const isIE = !!document.documentMode;

// Edge 20+
export const isEdge = !isIE && !!window.StyleMedia;

// Chrome 1+
export const isChrome = !!window.chrome && !!window.chrome.webstore;

// Blink engine detection
export const isBlink = (isChrome || isOpera) && !!window.CSS;

export const isMac = (navigator.platform === 'Mac68K') || (navigator.platform === 'MacPPC') || (navigator.platform === 'Macintosh') || (navigator.platform === 'MacIntel');

export function setStorage(key, value) {
  if (value !== undefined) {
    const string = typeof value === 'object' ? JSON.stringify(value) : String(value);
    localStorage.setItem(key, string);
  }
}

export function getStorage(key) {
  let value = null;
  try {
    value = JSON.parse(localStorage.getItem(key));
  } catch (e) {
    value = localStorage.getItem(key);
  }
  return value;
}

export function clearStorage(key) {
  localStorage.removeItem(key);
}

export function setLargeStorage(key, value) {
  if (value !== undefined) {
    const string = typeof value === 'object' ? JSON.stringify(value) : String(value);
    return localforage.setItem(key, string);
  }
  return Promise.resolve();
}

export async function getLargeStorage(key) {
  let value = null;
  try {
    value = JSON.parse(await localforage.getItem(key));
  } catch (e) {
    value = await localforage.getItem(key);
  }
  return value;
}

export async function clearLargeStorage(key) {
  await localforage.setItem(key, null);
}

export function setQueryToLocation(key, value) {
  if (!isEmpty(value)) {
    const { pathname } = getHistory().location;
    const search = new URLSearchParams(window.location.search);
    search.set(key, value);
    window.history.pushState({}, '', `${pathname}?${search.toString()}`);
  }
}

export function getQueryFromLocation(key) {
  const search = new URLSearchParams(window.location.search);
  return search.get(key);
}

export function getLanguage() {
  const supportedLanguages = ['en', 'zh'];
  const browserLanguage = navigator.language.split('-')[0];
  return supportedLanguages.includes(browserLanguage) ? browserLanguage : null;
}
