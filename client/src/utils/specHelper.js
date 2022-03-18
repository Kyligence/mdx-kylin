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
import spec from '../constants/spec';

/**
 * 将值转化为数组，如果是数组则保持不变
 * @param {any} value
 */
function toArray(value) {
  return value instanceof Array ? value : [value];
}

function mergeEnableOptions(availableOptions, enableOptions) {
  return availableOptions.filter(availableOption => enableOptions.includes(availableOption));
}

function mergeDisableOptions(availableOptions, disableOptions) {
  return availableOptions.filter(availableOption => !disableOptions.includes(availableOption));
}

/**
 * 判断是否是数组字符串
 * @param {*} str 字符串
 */
function isArrayString(str) {
  return str.indexOf('[') === 0 && str.indexOf(']') === str.length - 1;
}

/**
 * 将字符串转换成数组
 * @param {*} str 字符串
 */
function string2Array(str) {
  return str.replace(/^\[/g, '').replace(/\]$/g, '').split(',');
}

/**
 * 警告报错
 * @param {*} msg
 */
/* eslint-disable no-console */
function warn(msg) {
  console.warn(msg);
}

/**
 * 检查参数是否合法
 * @param {*} pattern
 * @param {*} parameters
 * @param {*} allOptionMaps
 */
function checkVaild(pattern, parameters, allOptionMaps) {
  // 检查pattern在parameters对象中是否存在
  if (parameters[pattern] === undefined) {
    warn(`Key Pattern cannot get '${pattern}' in parameters, please check parameters`);
    return false;
  }
  // 检查pattern在Spec的allOptionMaps中是否存在
  if (allOptionMaps[pattern] === undefined) {
    warn(`Key Pattern cannot get '${pattern}' in allOptionMaps, please check allOptionMaps in spec.js`);
    return false;
  }
  return true;
}

/**
 * 映射parameter的值为option的id
 * @description 防止parameter的变化造成entry的大量修改
 * @param {*} patternOptions
 * @param {*} parameterValues
 */
function mapValueToId(patternOptions, parameterValues) {
  // 找到parameter的value对应的option.value的option
  // 如果有对应的option，则使用option.id，作为匹配pattern的键值
  // 如果没有，则使用原来的parameter对象的值，作为匹配pattern的键值
  const currentOption = patternOptions.find(option => parameterValues.includes(option.value));
  return currentOption && currentOption.id ? [currentOption.id] : parameterValues;
}

/**
 * 核心：判断entry的key是否匹配KeyPattern
 * @param {*} keyPatternString
 * @param {*} entryKeyString
 * @param {*} parameters
 * @param {*} allOptionMap
 */
function isMatchEntryKey(keyPatternString, entryKeyString, parameters, allOptionMap) {
  const keyPatterns = keyPatternString.split('-');
  const entryKeys = entryKeyString.split('-');

  return keyPatterns.every((pattern, index) => {
    // 检查parameters和allOptionMaps是否都含有pattern，不合法不予匹配
    if (checkVaild(pattern, parameters, allOptionMap)) {
      // 解构出当前pattern位置上的Entry key值
      const entryKey = entryKeys[index];
      // 映射parameter的value为spec中的option.id
      const idsOrValues = mapValueToId(allOptionMap[pattern], toArray(parameters[pattern]));

      let isMatchedPattern = false;
      // 判断Entry的某一个Key是否是数组字符串(pattern值的集合)
      if (isArrayString(entryKey)) {
        // 如果是字符串，则把key拆成数组；判断parameter的值是否属于Entry的keys
        const patternValues = string2Array(entryKey);
        isMatchedPattern = idsOrValues.some(idOrValue => patternValues.includes(idOrValue));
      } else if (entryKey === '*') {
        // 如果是通配符，则符合匹配
        isMatchedPattern = true;
      } else {
        // 如果既不是数组，也不是通配符，则判断是否字符串值相等
        isMatchedPattern = idsOrValues.some(idOrValue => entryKey === idOrValue);
      }
      // 返回parameter的value是否匹配Entry的key
      return isMatchedPattern;
    }
    return false;
  });
}

/**
 * 获取匹配的options
 * @param {*} param0
 * @param {*} type
 * @param {*} parameters
 * @param {*} allOptionMaps
 */
function getMatchedOptions(type, { keyPattern = '', entries = [] }, parameters = {}, allOptionMaps = []) {
  for (const entry of entries) {
    if (isMatchEntryKey(keyPattern, entry.key, parameters, allOptionMaps)) {
      switch (entry.value) {
        case 'none': return [];
        case '*': return allOptionMaps[type].map(option => option.value);
        default: return entry.value.split(',');
      }
    }
  }
  return [];
}

/**
 * 根据传入的parameters对象，从spec中获取可用options
 * @param {*} type: 获取哪个类型的可用option
 * @param {*} parameters: 可用option的参数依据
 */
export function getAvailableOptions(type, parameters) {
  const { allOptionMaps = {}, enableOptionMaps = {}, disableOptionMaps = {} } = spec;
  const allOptionMap = allOptionMaps[type];
  const enableOptionMap = enableOptionMaps[type];
  const disableOptionMap = disableOptionMaps[type];

  let availableOptions = [];

  if (allOptionMap) {
    availableOptions = allOptionMap.map(optionMap => optionMap.id);

    if (enableOptionMap) {
      const enableOptions = getMatchedOptions(type, enableOptionMap, parameters, allOptionMaps);
      availableOptions = mergeEnableOptions(availableOptions, enableOptions);
    }

    if (disableOptionMap) {
      const disableOptions = getMatchedOptions(type, disableOptionMap, parameters, allOptionMaps);
      availableOptions = mergeDisableOptions(availableOptions, disableOptions);
    }
  }

  return allOptionMap
    .filter(optionMap => availableOptions.includes(optionMap.id))
    .map(optionMap => optionMap.value || optionMap.id);
}
