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
import { strings } from '../constants';
import { getQueryFromLocation, setQueryToLocation } from './browserHelper';
import { createServiceLock } from './serviceHelper';
import { getOptionalBaseParams } from './templateUrl';
import { isEmpty } from './dataHelper';

function defaultResponseFormator({ list, total }) {
  return { totalCount: total, data: list };
}

function getColumns(datas = []) {
  const columnMap = {};

  for (const data of datas) {
    for (const columnKey of Object.keys(data)) {
      if (!columnMap[columnKey]) {
        columnMap[columnKey] = {
          prop: columnKey,
          label: strings[columnKey],
        };
      }
    }
  }
  return Object.values(columnMap);
}

export function getPagination(options = {}) {
  const pagination = {};

  if (options.pageOffset && options.pageSize) {
    pagination.pageOffset = +options.pageOffset;
    pagination.pageSize = +options.pageSize;
  } else {
    pagination.pageOffset = +getQueryFromLocation('pageOffset') || undefined;
    pagination.pageSize = +getQueryFromLocation('pageSize') || undefined;
  }

  return pagination;
}

export function getOrder(options = {}) {
  const order = {};

  if (options.orderBy && options.direction) {
    order.orderBy = options.orderBy;
    order.direction = options.direction;
  } else {
    order.orderBy = getQueryFromLocation('orderBy') || undefined;
    order.direction = getQueryFromLocation('direction') || undefined;
  }

  return order;
}

export function fetchDataWithPagination({
  options = {},
  formatResponse = defaultResponseFormator,
  getState,
  fetchApi,
  setDataAction,
  setLoadingAction,
  resetLoadingAction,
}) {
  return async dispatch => {
    const {
      pageOffset = 0,
      pageSize = 10,
      orderBy = null,
      direction = null,
      withLocation = true,
      ...params
    } = options;

    const pagination = { pageOffset, pageSize };
    const order = { orderBy, direction };

    if (direction === 'ascending') {
      order.direction = 'asc';
    }
    if (direction === 'descending') {
      order.direction = 'desc';
    }

    dispatch({ type: setLoadingAction, isLoading: true });

    const baseParams = getOptionalBaseParams(getState);
    const response = await fetchApi({ ...pagination, ...order, ...baseParams, ...params });
    const { totalCount, data } = formatResponse(response);
    const columns = getColumns(data);
    dispatch({ type: setDataAction, pageSize, pageOffset, totalCount, data, columns });

    if (withLocation) {
      setQueryToLocation('pageOffset', pageOffset);
      setQueryToLocation('pageSize', pageSize);
      setQueryToLocation('orderBy', orderBy);
      setQueryToLocation('direction', direction);
    }

    dispatch({ type: setLoadingAction, isLoading: false });

    if (data.length === 0 && pageOffset > 0 && totalCount > 0) {
      const newPageOffset = pageOffset - 1;

      await dispatch(fetchDataWithPagination({
        options: {
          ...options,
          pageOffset: newPageOffset,
        },
        formatResponse,
        fetchApi,
        setDataAction,
        setLoadingAction,
        resetLoadingAction,
      }));
    }
  };
}

export function createFetchAllData(batchPageSize) {
  const batchRequest = createServiceLock(false);

  return function fetchAllData({
    options = {},
    fetchApi,
    formatResponse = defaultResponseFormator,
    pushDataAction,
    initListAction,
    setLoadingAction,
    setFullLoadingAction,
  }) {
    return async dispatch => {
      // 获取分页参数
      const { pageOffset = 0, pageSize = batchPageSize } = options;
      // 当从第一页开始获取数据时，清空store
      if (pageOffset === 0) {
        dispatch({ type: initListAction });
        dispatch({ type: setLoadingAction, isLoading: true });
        dispatch({ type: setFullLoadingAction, isFullLoading: true });
        batchRequest.lock();
      }
      // 请求数据
      const response = await fetchApi({ ...options, pageOffset, pageSize });
      const { totalCount, data } = formatResponse(response);
      // 计算下一页的pageOffset
      // 计算是否中断request请求，是否中断写入数据
      const nextPageOffset = pageOffset + 1;
      const hasNextPage = nextPageOffset * pageSize < totalCount;
      const isBreakLock = !isEmpty(options.lockAt) &&
        options.lockAt !== batchRequest.lockAt;

      if (!isBreakLock) {
        dispatch({ type: pushDataAction, data, totalCount });
      }
      if (pageOffset === 0) {
        dispatch({ type: setLoadingAction, isLoading: false });
      }

      let nextResponseList = [];
      // 判断是否需要请求下一页
      // 判断是否中断当前连续请求
      if (hasNextPage && !isBreakLock) {
        nextResponseList = await dispatch(fetchAllData({
          options: {
            ...options,
            pageOffset: nextPageOffset,
            pageSize,
            lockAt: batchRequest.lockAt,
          },
          fetchApi,
          formatResponse,
          pushDataAction,
          initListAction,
          setLoadingAction,
          setFullLoadingAction,
        }));
      }

      if (!hasNextPage) {
        batchRequest.unlock();
        dispatch({ type: setFullLoadingAction, isFullLoading: true });
      }

      return [...response.list, ...nextResponseList];
    };
  };
}

export function clone(object) {
  const newObject = object instanceof Array ? [...object] : { ...object };
  for (const key of Object.keys(object)) {
    if (newObject[key] instanceof Array) {
      newObject[key] = [...newObject[key]];

      // Note: typeof null === 'object'
    } else if (Object.prototype.toString.call(newObject[key]) === '[object Object]') {
      newObject[key] = clone(newObject[key]);
    }
  }
  return newObject;
}

export function merge(...objects) {
  const newObject = {};
  for (const object of objects) {
    for (const key of Object.keys(object || {})) {
      if (Object.prototype.toString.call(object[key]) !== '[object Object]') {
        newObject[key] = object[key];
      } else {
        newObject[key] = merge(newObject[key], object[key]);
      }
    }
  }
  return newObject;
}

export function set(object, objectPath = [], value) {
  const newObject = clone(object);
  const pathArray = objectPath instanceof Array
    ? objectPath
    : objectPath.split('.');

  let curObject = newObject;

  pathArray.forEach((path, index) => {
    if (index === pathArray.length - 1) {
      if (typeof curObject[path] === 'object' && !(curObject[path] instanceof Array)) {
        curObject[path] = { ...curObject[path], ...value };
      } else {
        curObject[path] = value;
      }
    } else {
      if (!curObject[path]) {
        curObject[path] = {};
      }
      curObject = curObject[path];
    }
  });

  return newObject;
}
