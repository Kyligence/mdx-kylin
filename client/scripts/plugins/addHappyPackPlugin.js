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
const HappyPackPlugin = require('happypack');

/* eslint-disable no-param-reassign */
module.exports = function addHappyPackPlugin(loaderName, threads) {
  return configs => {
    const loaderRules = configs.module.rules.find(rule => !!rule.oneOf);

    const moduleIdx = loaderRules.oneOf.findIndex(rule => rule.loader.includes(loaderName));
    const module = loaderRules.oneOf[moduleIdx];

    const { test, include, loader, options } = module;

    loaderRules.oneOf = [
      ...loaderRules.oneOf.slice(0, moduleIdx),
      { test, include, use: `happypack/loader?id=${loaderName}` },
      ...loaderRules.oneOf.slice(moduleIdx + 1),
    ];

    configs.plugins = [
      ...configs.plugins,
      new HappyPackPlugin({ id: loaderName, threads, loaders: [{ loader, options }] }),
    ];

    return configs;
  };
};
/* eslint-enable */
