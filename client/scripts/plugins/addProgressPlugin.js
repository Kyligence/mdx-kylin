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
const { ProgressPlugin } = require('webpack');
const readline = require('readline');
const path = require('path');

function outputCurrentLine(text) {
  readline.clearLine(process.stdout, 0);
  readline.cursorTo(process.stdout, 0);
  process.stdout.write(text, 'utf-8');
}

function getFilePath(basePath, fullPath) {
  const splitPath = fullPath.replace(basePath, '.').replace(basePath, '.').split('!');
  return splitPath[splitPath.length - 1];
}

module.exports = function addProgressPlugin() {
  const basePath = path.resolve('./');
  const handler = (percentage, processType, ...args) => {
    const [modulesText, activeText] = args;
    const filePath = args[2] ? getFilePath(basePath, args[2]) : '';

    const percentageText = `${(percentage * 100).toFixed(2)}%`;

    if (processType === 'building') {
      const buildText = `${modulesText} ${activeText} ${filePath}`;
      outputCurrentLine(`${percentageText} ${processType} ${buildText}`);
    } else {
      outputCurrentLine(`${percentageText} ${processType}`);
    }
  };

  return configs => ({
    ...configs,
    plugins: [
      ...configs.plugins,
      new ProgressPlugin(handler),
    ],
  });
};
