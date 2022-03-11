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
import axios from 'axios';
import strings, { stringsMap } from '../constants/strings';
import * as business from '../constants/business';
import { requestTypes } from '../constants/configs';

class ServiceLock {
  constructor(isLock) {
    this.isLock = isLock;
    this.lockAt = null;
  }

  lock() {
    this.isLock = true;
    this.lockAt = Date.now();
  }

  unlock() {
    this.isLock = false;
    this.lockAt = null;
  }
}

function handleRequest(configs) {
  return configs;
}

function tryJsonStringify(error) {
  try {
    return JSON.stringify(error, null, 2).replace(/\\n/g, '\n');
  } catch {
    return null;
  }
}

async function handleResponseSuccess({ response, boundModalActions }) {
  if (!response.data) {
    return Promise.resolve(response);
  }

  const { status, data: responseData } = response.data;
  const { headers: requestHeaders } = response.config;

  const isResponseError = status === 1;
  const isFetchI18n = status === undefined;

  if (response.data && typeof response.data === 'object') {
    // eslint-disable-next-line no-underscore-dangle
    response.data.__headers = () => response.headers;
  }

  if (responseData && typeof responseData === 'object') {
    // eslint-disable-next-line no-underscore-dangle
    responseData.__headers = () => response.headers;
  }

  if (isFetchI18n) {
    return response.data;
  }

  if (isResponseError) {
    if (requestHeaders['X-Error-Silence']) {
      return Promise.reject(response.data);
    }

    try {
      const responseMesssage = responseData;
      const isCodeCatched = !!responseMesssage.match(business.responseRegx);

      let message = strings.ERROR_UNKNOW;
      let detail = responseMesssage;
      let data = {};

      if (isCodeCatched) {
        const messageCode = responseMesssage.match(business.responseRegx)[0].replace(/\[|\]/g, '');
        detail = responseMesssage.replace(`[${messageCode}] `, '');
        const result = stringsMap[messageCode]
          ? stringsMap[messageCode](detail)
          : { message: strings[messageCode] };
        message = result.message;
        data = result.data || {};
      }

      boundModalActions.setModalData('GlobalMessageModal', { message, detail, data });
      boundModalActions.showModal('GlobalMessageModal');

      return Promise.reject(responseData);
    } catch (e) {
      return Promise.resolve(responseData);
    }
  }

  return responseData;
}

async function handleResponseFailed({ boundModalActions, boundSystemActions, error }) {
  // if (reqHeaders['X-Error-Silence']) return;
  let message;
  let detail;
  let data;

  if (error.message === 'Network Error') {
    // MDX网络连接异常
    message = strings.ERROR_MDX_CONNECT;
    detail = tryJsonStringify(error);
  } else if (error.message === requestTypes.CANCEL) {
    // MDX取消请求
  } else if (!error.response.config.headers['X-Error-Silence'] && error.response && error.response.data) {
    if (error.response.data.type === 'application/json' && error.response.data instanceof Blob) {
      // eslint-disable-next-line no-param-reassign
      error.response.data = JSON.parse(await error.response.data.text());
    }

    // MDX后端报错异常
    const { status: httpCode } = error.response;
    const { status, data: errorInData = '', errorMesg: errorInMsg = '' } = error.response.data;

    if (status !== 1 && status !== undefined) {
      // 响应status报错
      message = stringsMap[status] || errorInData;
    } else if (status === 1) {
      const isSessionOut = errorInMsg.includes('Access denied!');
      if (isSessionOut) {
        await boundSystemActions.getCurrentUser();
      }

      if (httpCode >= 400 && httpCode < 500) {
        // 后端捕获的报错ID
        const errorCode = errorInMsg.match(business.responseRegx)[0].replace(/\[|\]/g, '');
        const [errorMsg, ...errorStacks] = errorInMsg.split('\n');
        const errorStack = errorStacks.join('\n');
        const isKeOff = errorInMsg.includes('connect timed out') ||
          errorInMsg.includes('Network is unreachable') ||
          errorInMsg.includes('Connection refused');

        if (isKeOff) {
          // 如果是KE挂掉的情况下
          message = strings['MDX-01050001'];
          detail = errorStack;
          // 如果是有报错码的情况下
        } else if (errorCode && stringsMap[errorCode]) {
          // 在报错映射函数中寻找报错信息
          message = stringsMap[errorCode](errorMsg).message;
          data = stringsMap[errorCode](errorMsg).data;
          detail = errorStack;
        } else if (errorCode && strings[errorCode]) {
          // 在翻译列表中寻找报错信息
          message = strings[errorCode];
          detail = errorStack;
        } else {
          message = strings.ERROR_400;
          detail = errorInMsg;
        }
      } else if (httpCode >= 500) {
        // 后端未捕获的报错
        const errorCode = errorInMsg.match(business.responseRegx)[0].replace(/\[|\]/g, '');
        const [errorMsg, ...errorStacks] = errorInMsg.split('\n');
        const errorStack = errorStacks.join('\n');

        if (errorCode && stringsMap[errorCode]) {
          // 在报错映射函数中寻找报错信息
          message = stringsMap[errorCode](errorMsg).message;
          data = stringsMap[errorCode](errorMsg).data;
          detail = errorStack;
        } else if (errorCode && strings[errorCode]) {
          // 在翻译列表中寻找报错信息
          message = strings[errorCode];
          detail = errorStack;
        } else {
          message = strings.ERROR_500;
          detail = errorInMsg || tryJsonStringify(error.response);
        }
      }
    } else if (error.response.data.msg) {
      message = error.response.data.msg;
      detail = tryJsonStringify(error.response.data);
    } else {
      // 未知报错
      message = strings.ERROR_UNKNOW;
      detail = tryJsonStringify(error);
    }

    boundModalActions.setModalData('GlobalMessageModal', { message, detail, data });
    boundModalActions.showModal('GlobalMessageModal');
  }

  return Promise.reject(error);
}

export function initService({ boundModalActions, boundSystemActions }) {
  axios.interceptors.request.use(
    config => handleRequest(config),
    error => Promise.reject(error),
  );

  axios.interceptors.response.use(
    response => handleResponseSuccess({ boundModalActions, boundSystemActions, response }),
    error => handleResponseFailed({ boundModalActions, boundSystemActions, error }),
  );
}

export function createServiceLock(isLock = false) {
  return new ServiceLock(isLock);
}
