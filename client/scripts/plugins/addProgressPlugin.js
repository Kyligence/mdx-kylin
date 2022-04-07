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
