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
const fs = require('fs');
const path = require('path');

const npmPath = process.env.PWD;
const chineseMap = {};

// Functions
function resolvePath(filePath) {
  return path.resolve(npmPath, filePath);
}

function readConfigFile(filePath) {
  /* eslint-disable import/no-dynamic-require, global-require */
  return require(resolvePath(filePath));
}

function parseArgConfig(argumentKeys = []) {
  let config = {
    rootPath: './src',
  };
  // 读取arguments里面的配置
  for (const key of argumentKeys) {
    if (typeof config[key.replace('--', '')] === 'boolean') {
      const keyIndex = process.argv.findIndex(arg => arg === key);
      config = { ...config, [key.replace('--', '')]: keyIndex !== -1 };
    } else {
      const keyIndex = process.argv.findIndex(arg => arg === key);
      const value = process.argv[keyIndex + 1];
      if (value && keyIndex !== -1) {
        config = { ...config, [key.replace('--', '')]: value };
      }
    }
  }
  // 如果arguments里面有配置文件，则用文件的配置覆盖arguments里面的配置
  if (config.configFile) {
    const fileConfig = readConfigFile(config.configFile);
    config = { ...config, ...fileConfig };
  }
  return config;
}

function travellingFiles(filePath, callback) {
  const stat = fs.statSync(filePath);

  if (stat.isDirectory()) {
    const children = fs.readdirSync(filePath);
    for (const child of children) {
      travellingFiles(path.resolve(filePath, child), callback);
    }
  } else if (stat.isFile()) {
    callback(filePath);
  }
}

function removeScriptComments(filePath = '') {
  return fs.readFileSync(filePath, 'utf-8')
    // 去掉 /* disable-explore-chinese-next-line */
    .replace(/\/\*\s*disable-explore-chinese-next-line\s*\*\/\n[\w\W]*?\n/, '')
    // 去掉 /* 注释 */
    .replace(/\/\*[\w\W\r\n]*?\*\//gmi, '')
    // 去掉 // 注释 TODO: 字符串里面带双斜杠会被干掉，这个有待研究
    .replace(/\/\/[\w\W]*?\n/gmi, '\n');
}

function removeFileComments(filePath) {
  switch (path.extname(filePath)) {
    case '.js':
    case '.css':
    case '.less':
      return removeScriptComments(filePath);
    default:
      return undefined;
  }
}

function findContentChinese(fileContent = '') {
  const results = [];
  const textLines = fileContent.split('\n');
  textLines.forEach((text, rowIdx) => {
    const result = text.match(/[\u4e00-\u9fa5]/);
    if (result) {
      const { index: colIdx } = result;
      results.push({ rowIdx, colIdx, text });
    }
  });
  return results;
}

function outputChineseLine({ rowIdx, colIdx, text, filePath }) {
  console.log(`Line ${rowIdx + 1}:${colIdx}:  Find chinese in file ${filePath}`);
  const perfixInfo = `> ${rowIdx + 1} | `;
  console.log(`${perfixInfo}${text}`);
  for (let i = 0; i < perfixInfo.length + colIdx; i += 1) {
    process.stdout.write(' ');
  }
  console.log('^');
  console.log('\r\n');
}

const configs = parseArgConfig([
  '--rootPath',
]);

travellingFiles(resolvePath(configs.rootPath), filePath => {
  process.stdout.write(`Scan chinese in ${filePath}... `);
  const content = removeFileComments(filePath);
  chineseMap[filePath] = findContentChinese(content);
  process.stdout.write('Done.\n');
});

const hasChinese = Object.values(chineseMap).some(chineseResults => chineseResults.length);

if (hasChinese) {
  for (const [filePath, chineseLines = []] of Object.entries(chineseMap)) {
    const isChineseFile = !!chineseLines.length;
    if (isChineseFile) {
      for (const chineseLine of chineseLines) {
        outputChineseLine({ ...chineseLine, filePath });
      }
    }
  }
  throw new Error('Frontend code has chinese! Please check logs.');
} else {
  console.log('Explore chinese in frontend project successfully!');
}
