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

const dirPaths = [
  path.resolve('./src'),
  path.resolve('./e2e'),
  path.resolve('./public'),
  path.resolve('./scripts'),
];

(() => {
  const copyrightRegex = /#* *Copyright \(C\) \d+ Kyligence Inc\. All rights reserved\.\n*#*\s*http:\/\/kyligence\.io\n*#*\s*This software is the confidential and proprietary information of\n*#*\s*Kyligence Inc\. \("Confidential Information"\)\. You shall not disclose\n*#*\s*such Confidential Information and shall use it only in accordance\n*#*\s*with the terms of the license agreement you entered into with\n*#*\s*Kyligence Inc\.\n*#*\s*THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS\n*#*\s*"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT\n*#*\s*LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR\n*#*\s*A PARTICULAR PURPOSE ARE DISCLAIMED\. IN NO EVENT SHALL THE COPYRIGHT\n*#*\s*OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,\n*#*\s*SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES \(INCLUDING, BUT NOT\n*#*\s*LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,\n*#*\s*DATA, OR PROFITS; OR BUSINESS INTERRUPTION\) HOWEVER CAUSED AND ON ANY\n*#*\s*THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n*#*\s*\(INCLUDING NEGLIGENCE OR OTHERWISE\) ARISING IN ANY WAY OUT OF THE USE\n*#*\s*OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE\.\n/;
  const copyrightContent = `  Copyright (C) ${new Date().getFullYear()} Kyligence Inc. All rights reserved.

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
  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.`;

  const comments = {
    '.jsx': ['/*', '*/'],
    '.js': ['/*', '*/'],
    '.tsx': ['/*', '*/'],
    '.ts': ['/*', '*/'],
    '.less': ['/*', '*/'],
    '.vue': ['<!--', '-->'],
    '.html': ['<!--', '-->'],
    '.sh': ['##'],
  };

  function doCopyrightInLine(filePath, [prefix]) {
    let fileContent = fs.readFileSync(filePath, 'utf8');
    const hasCopyright = copyrightRegex.test(fileContent);

    if (hasCopyright) {
      fileContent = fileContent.replace(copyrightRegex, '');
    }

    const copyrightContentWithFix = copyrightContent.trim()
      .split('  ').map(line => `${prefix} ${line}`).join('');
    fs.writeFileSync(filePath, `${copyrightContentWithFix}\n${fileContent}`, 'utf8');
  }

  function doCopyrightInBlock(filePath, [prefix, suffix]) {
    const fileContent = fs.readFileSync(filePath, 'utf8');
    const hasCopyright = copyrightRegex.test(fileContent);

    if (hasCopyright) {
      const replacedContent = fileContent.replace(copyrightRegex, `${copyrightContent}\n`);
      fs.writeFileSync(filePath, replacedContent, 'utf8');
    } else {
      const copyrightContentWithFix = `${prefix}\n${copyrightContent}\n${suffix}`;
      fs.writeFileSync(filePath, `${copyrightContentWithFix}\n${fileContent}`, 'utf8');
    }
  }

  function doCopyright(dirPath) {
    const files = fs.readdirSync(dirPath);

    for (const filename of files) {
      const filePath = path.join(dirPath, filename);
      const fileStat = fs.statSync(filePath);
      const isFile = fileStat.isFile();
      const isDir = fileStat.isDirectory();

      if (isFile) {
        const extName = path.extname(filePath);
        if (['.sh'].includes(extName)) {
          doCopyrightInLine(filePath, comments[extName]);
        } else if (comments[extName]) {
          doCopyrightInBlock(filePath, comments[extName]);
        }
      } else if (isDir) {
        doCopyright(filePath);
      }
    }
  }

  for (const dirPath of dirPaths) {
    doCopyright(dirPath);
  }
})();
