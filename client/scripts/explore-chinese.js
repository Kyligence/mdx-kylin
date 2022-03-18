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
