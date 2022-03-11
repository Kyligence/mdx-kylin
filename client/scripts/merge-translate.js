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
const globSync = require('glob').sync;
const DIST_FOLDER = getDistFolder();
const I18N_FOLDER = path.resolve(DIST_FOLDER);
const MESSAGES_PATTERN = './public/messages/**/*.json';

ensureLocaleFolder();

const defaultMessages = getDefaultMessages();
const messageKeys = Object.keys(defaultMessages).sort();
const sortedMessages = sortDefaultMessages(defaultMessages, messageKeys);
const fileLocales = getInitLocalesFromFiles();
const fileLocaleKeys = Object.keys(fileLocales);

for (const localeKey of fileLocaleKeys) {
  const localeMessages = {};

  for (const messageKey of messageKeys) {
    const fileMessages = fileLocales[localeKey];

    const currentMessage = fileMessages[messageKey];
    const { defaultMessage } = defaultMessages[messageKey];

    const isMessageEmpty = !currentMessage;
    const isMessageSet = currentMessage && currentMessage !== defaultMessage;

    if (isMessageEmpty) {
      localeMessages[messageKey] = defaultMessage;
    } else {
      localeMessages[messageKey] = currentMessage;
    }
  }

  writeFileSync(localeKey, localeMessages);
}

function ensureLocaleFolder() {
  try {
    fs.accessSync(I18N_FOLDER, fs.constants.F_OK);
  } catch (e) {
    fs.mkdirSync(I18N_FOLDER);
  }
}

function getDefaultMessages() {
  const pathArray = globSync(MESSAGES_PATTERN);
  const fileArray = pathArray.map(path => fs.readFileSync(path, 'utf8'));
  const messageArray = fileArray.map(file => JSON.parse(file));
  return messageArray.reduce((collection, messageGroup) => {
    for (const message of messageGroup) {
      if (message.id in collection) {
        const id = message.id;
        const path = message.file;
        const line = message.start.line;
        throw new Error(`Duplicate message id: ${id} in ${path} at line ${line}`);
      } else {
        collection[message.id] = message;
      }
    }
    return collection;
  }, {});
}

function sortDefaultMessages(messages, messageKeys) {
  return messageKeys.reduce((acc, key) => {
    acc[key] = messages[key];
    return acc;
  }, {});
}

function getInitLocalesFromFiles() {
  const localeFiles = fs.readdirSync(I18N_FOLDER);
  const localeFileMap = localeFiles.reduce((fileMap, fileName) => {
    const locale = fileName.replace(/\.json$/, '');
    const filePath = path.resolve(I18N_FOLDER, fileName);
    const fileMessages = JSON.parse(fs.readFileSync(filePath, 'utf8') || '{}');

    fileMap[locale] = fileMessages;

    return fileMap;
  }, {});
  return localeFileMap;
}

function writeFileSync(locale, messages) {
  const localePath = path.resolve(I18N_FOLDER, `${locale}.json`);
  const localeContent = JSON.stringify(messages, null, 2);
  fs.writeFileSync(localePath, localeContent);
}

function getDistFolder() {
  const argIndex = process.argv.indexOf('--dist');

  return process.argv[argIndex + 1] || './public/locale';
}
