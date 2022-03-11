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
