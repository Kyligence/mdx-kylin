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
