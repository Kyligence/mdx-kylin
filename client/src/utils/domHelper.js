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
import { isIE } from './browserHelper';

export function setHtmlLanguage(locale) {
  window.document.querySelector('html').lang = locale;
}

export function setPageTitle(title) {
  const titleEl = document.querySelector('title');
  titleEl.innerHTML = title;
}

export function findParents(el) {
  let currentEl = el;
  const parents = [];
  while (currentEl.parentNode && currentEl.nodeType !== 9) {
    currentEl = currentEl.parentNode;
    if (currentEl.nodeType === 1) {
      parents.push(currentEl);
    }
  }
  return parents;
}

/* eslint-disable no-param-reassign */
export function scrollTo(el = window, scrollTop = 0, scrollLeft = 0) {
  if (isIE || !el.scrollTo) {
    el.scrollTop = scrollTop;
    el.scrollLeft = scrollLeft;
  } else {
    el.scrollTo({ top: scrollTop, left: scrollLeft, behavior: 'smooth' });
  }
}

export const download = {
  get(url, filename, isSilence = false) {
    const a = document.createElement('a');
    a.href = url;
    a.target = isSilence ? '_self' : '_blank';
    if (filename) {
      a.download = filename;
    }
    document.body.appendChild(a);
    a.click();
    setTimeout(() => {
      document.body.removeChild(a);
    });
  },
};

/* eslint-disable no-console */
export function error(funcName, errorMsg) {
  console.log(`An error occurred by function "${funcName}", please check.`);
  console.error(errorMsg);
}
/* eslint-enable */
