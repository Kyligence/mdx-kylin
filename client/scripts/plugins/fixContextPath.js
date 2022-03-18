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
/* eslint-disable no-param-reassign */
// 支持Context Path：
// 设置 PUBLIC_URL 强制为""，使"/static/js"变为"static/js"，
// 配合使用<base href="base_url">的地址进行基地址索引。
function syncPublicPathFromEnv(configs) {
  const { PUBLIC_URL } = process.env;
  configs.output.publicPath = PUBLIC_URL;
  return configs;
}

// 支持Context Path：https://www.w3.org/TR/CSS1/#url
// 根据W3官方解释如下："Partial URLs are interpreted relative to the
// source of the style sheet, not relative to the document"，
// 由此得知base的href无法对css文件中的url()起作用。
// 需要修正fontPublicPath开头为"../../"，即可正确引导。
function fixFontPublicPath(configs) {
  const loaderRules = configs.module.rules.find(rule => !!rule.oneOf);
  const fontFileLoader = {
    test: [/\.woff$/, /\.ttf$/, /\.eot$/, /\.svg$/],
    loader: 'file-loader',
    options: {
      name: '[name].[hash:8].[ext]',
      outputPath: './static/media/',
      publicPath: '../../static/media',
    },
  };
  loaderRules.oneOf = [fontFileLoader, ...loaderRules.oneOf];
  return configs;
}

module.exports = function fixContextPath() {
  return configs => fixFontPublicPath(syncPublicPathFromEnv(configs));
};
/* eslint-enable */
