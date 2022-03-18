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

const dirPaths = [
  path.resolve('./src'),
  path.resolve('./e2e'),
  path.resolve('./public'),
  path.resolve('./scripts'),
];

(() => {
  const copyrightRegex = /#*\s*\n*Licensed to the Apache Software Foundation \(ASF\) under one\n#*\s*or more contributor license agreements\.\s*See the NOTICE file\n#*\s*distributed with this work for additional information\n#*\s*regarding copyright ownership\.\s*The ASF licenses this file\n#*\s*to you under the Apache License, Version 2\.0 \(the\n#*\s*"License"\); you may not use this file except in compliance\n#*\s*with the License\.\s*You may obtain a copy of the License at\n#*\s*\n#*\s*http:\/\/www\.apache\.org\/licenses\/LICENSE-2\.0\n#*\s*\n#*\s*Unless required by applicable law or agreed to in writing, software\n#*\s*distributed under the License is distributed on an "AS IS" BASIS,\n#*\s*WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied\.\n#*\s*See the License for the specific language governing permissions and\n#*\s*limitations under the License\.\n*/;
  const copyrightContent = `  Licensed to the Apache Software Foundation (ASF) under one
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
  limitations under the License.`;

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
      .split('\n  ')
      .map((line, index) => (
        index === 0 ? `${prefix} ${line}` : `\n${prefix} ${line}`
      ))
      .join('');
    fs.writeFileSync(filePath, `${copyrightContentWithFix}\n${fileContent}`, 'utf8');
  }

  function doCopyrightInBlock(filePath, [prefix, suffix]) {
    const fileContent = fs.readFileSync(filePath, 'utf8');
    const hasCopyright = copyrightRegex.test(fileContent);

    if (hasCopyright) {
      const replacedContent = fileContent.replace(copyrightRegex, `\n${copyrightContent}\n`);
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
